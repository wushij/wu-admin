<template>
  <view class="auth-particles">
    <!-- #ifdef H5 -->
    <canvas ref="canvasRef" class="auth-particles__canvas" />
    <!-- #endif -->
    <!-- #ifndef H5 -->
    <view class="auth-particles__fallback">
      <view v-for="n in 32" :key="n" class="auth-particles__dot" :style="dotStyle(n)" />
    </view>
    <!-- #endif -->
    <view class="auth-particles__gradient" />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import {
  authParticleLayers,
  PARTICLE_DENSITY_BOX,
  PARTICLE_MOVE_SCALE,
  type ParticleLayerConfig,
} from '@/constants/authParticles'

interface Particle {
  layer: ParticleLayerConfig
  x: number
  y: number
  vx: number
  vy: number
  size: number
  baseOpacity: number
  opacity: number
  twinklePhase: number
  color: string
}

// #ifdef H5
const canvasRef = ref<HTMLCanvasElement | null>(null)
let animId = 0
let running = true
let particles: Particle[] = []
let width = 0
let height = 0
let lastFrameTs = 0

function rand(min: number, max: number) {
  return min + Math.random() * (max - min)
}

function scaleLayerCount(base: number) {
  const density = (width * height) / (PARTICLE_DENSITY_BOX.width * PARTICLE_DENSITY_BOX.height)
  return Math.round(base * Math.min(Math.max(density, 0.75), 1.15))
}

function randomVelocity(layer: ParticleLayerConfig) {
  const angle = Math.random() * Math.PI * 2
  const speed = layer.moveSpeed * PARTICLE_MOVE_SCALE * rand(0.94, 1.06)
  return {
    vx: Math.cos(angle) * speed,
    vy: Math.sin(angle) * speed,
  }
}

function spawnParticle(layer: ParticleLayerConfig, randomPosition = true): Particle {
  const velocity = randomVelocity(layer)
  return {
    layer,
    x: randomPosition ? Math.random() * width : width * 0.5,
    y: randomPosition ? Math.random() * height : height * 0.5,
    vx: velocity.vx,
    vy: velocity.vy,
    size: rand(layer.sizeMin, layer.sizeMax),
    baseOpacity: rand(layer.opacityMin, layer.opacityMax),
    opacity: rand(layer.opacityMin, layer.opacityMax),
    twinklePhase: Math.random() * Math.PI * 2,
    color: layer.color,
  }
}

function respawnParticle(p: Particle) {
  const next = spawnParticle(p.layer)
  p.x = next.x
  p.y = next.y
  p.vx = next.vx
  p.vy = next.vy
  p.size = next.size
  p.baseOpacity = next.baseOpacity
  p.opacity = next.opacity
  p.twinklePhase = next.twinklePhase
}

function initParticles() {
  particles = authParticleLayers.flatMap((layer) =>
    Array.from({ length: scaleLayerCount(layer.count) }, () => spawnParticle(layer)),
  )
}

function resolveCanvas(): HTMLCanvasElement | null {
  const el = canvasRef.value
  if (!el) return null
  if (el instanceof HTMLCanvasElement) return el
  const inner = (el as { $el?: HTMLElement }).$el
  if (inner instanceof HTMLCanvasElement) return inner
  if (inner?.querySelector) {
    const found = inner.querySelector('canvas')
    if (found instanceof HTMLCanvasElement) return found
  }
  return null
}

function resize() {
  const canvas = resolveCanvas()
  if (!canvas) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = window.innerWidth
  height = window.innerHeight
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  const ctx = canvas.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  initParticles()
  lastFrameTs = 0
}

function hexToRgba(hex: string, alpha: number) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function drawParticle(ctx: CanvasRenderingContext2D, p: Particle) {
  const glowRadius = Math.max(p.size * 1.85, 1.4)
  const gradient = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, glowRadius)
  gradient.addColorStop(0, hexToRgba(p.color, p.opacity * 0.95))
  gradient.addColorStop(0.45, hexToRgba(p.color, p.opacity * 0.28))
  gradient.addColorStop(1, hexToRgba(p.color, 0))
  ctx.fillStyle = gradient
  ctx.beginPath()
  ctx.arc(p.x, p.y, glowRadius, 0, Math.PI * 2)
  ctx.fill()

  ctx.beginPath()
  ctx.arc(p.x, p.y, Math.max(p.size * 0.72, 0.35), 0, Math.PI * 2)
  ctx.fillStyle = hexToRgba(p.color, Math.min(1, p.opacity))
  ctx.fill()
}

function tick(ts: number) {
  if (!running) return
  const canvas = resolveCanvas()
  const ctx = canvas?.getContext('2d')
  if (!ctx) return

  if (!lastFrameTs) lastFrameTs = ts
  const dt = Math.min((ts - lastFrameTs) / (1000 / 60), 2.5)
  lastFrameTs = ts

  ctx.clearRect(0, 0, width, height)
  ctx.globalCompositeOperation = 'lighter'

  const t = ts * 0.001

  for (const p of particles) {
    p.x += p.vx * dt
    p.y += p.vy * dt

    // 对齐 PC outModes: out — 离场后从随机位置重生
    if (p.x < -16 || p.x > width + 16 || p.y < -16 || p.y > height + 16) {
      respawnParticle(p)
    }

    const twinkle = 0.5 + 0.5 * Math.sin(t * p.layer.opacitySpeed * 0.55 + p.twinklePhase)
    p.opacity = Math.max(0.08, p.baseOpacity * (0.18 + 0.82 * twinkle))

    drawParticle(ctx, p)
  }

  ctx.globalCompositeOperation = 'source-over'
  animId = requestAnimationFrame(tick)
}

function onResize() {
  resize()
}

onMounted(async () => {
  await nextTick()
  resize()
  window.addEventListener('resize', onResize)
  animId = requestAnimationFrame(tick)
})

onBeforeUnmount(() => {
  running = false
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', onResize)
})
// #endif

// #ifndef H5
const dotColors = ['#6366f1', '#a855f7', '#ffffff']

function dotStyle(index: number) {
  const color = dotColors[index % 3]
  const left = `${(index * 17 + 11) % 100}%`
  const top = `${(index * 23 + 7) % 100}%`
  const size = index % 3 === 2 ? '3rpx' : index % 3 === 1 ? '5rpx' : '7rpx'
  const delay = `${(index % 10) * 0.55}s`
  const duration = `${4.8 + (index % 6) * 0.6}s`
  return {
    left,
    top,
    width: size,
    height: size,
    background: color,
    animationDelay: delay,
    animationDuration: duration,
  }
}
// #endif
</script>

<style lang="scss" scoped>
.auth-particles {
  position: fixed;
  inset: 0;
  z-index: 0;
  background: #000;
  overflow: hidden;
  pointer-events: none;
}

.auth-particles__canvas {
  display: block;
  width: 100%;
  height: 100%;
}

.auth-particles__fallback {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 90% 55% at 50% 16%, rgba(99, 102, 241, 0.26), transparent 62%),
    radial-gradient(ellipse 70% 45% at 18% 78%, rgba(168, 85, 247, 0.16), transparent 58%),
    radial-gradient(ellipse 55% 35% at 82% 68%, rgba(56, 189, 248, 0.1), transparent 55%),
    #000;
}

.auth-particles__dot {
  position: absolute;
  border-radius: 50%;
  opacity: 0.45;
  box-shadow: 0 0 10rpx currentColor;
  animation: particle-twinkle ease-in-out infinite;
}

.auth-particles__gradient {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at center,
    transparent 42%,
    rgba(0, 0, 0, 0.22) 72%,
    rgba(0, 0, 0, 0.55) 100%
  );
  pointer-events: none;
}

@keyframes particle-twinkle {
  0%,
  100% {
    opacity: 0.15;
    transform: scale(0.88) translate(0, 0);
  }
  50% {
    opacity: 0.82;
    transform: scale(1.08) translate(2rpx, -2rpx);
  }
}
</style>
