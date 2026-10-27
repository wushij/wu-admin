<template>
  <view class="globe-wrap" :style="wrapStyle">
    <canvas
      :canvas-id="canvasId"
      :id="canvasId"
      :style="{ width: `${size}px`, height: `${size}px` }"
      class="globe-canvas"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref, getCurrentInstance, onMounted, onBeforeUnmount } from 'vue'

const props = withDefaults(
  defineProps<{
    size?: number
    color?: string
    glowColor?: string
    speed?: number
    lineWidth?: number
    meridians?: number
    parallels?: number
    tilt?: number
    opacity?: number
    dataBarCount?: number
    /** 对齐 PC 登录页透明地球的高亮模式 */
    vivid?: boolean
  }>(),
  {
    size: 200,
    color: '#7ad4ff',
    glowColor: '#38bdf8',
    speed: 0.002,
    lineWidth: 0.75,
    meridians: 10,
    parallels: 7,
    tilt: 0.38,
    opacity: 0.42,
    dataBarCount: 28,
    vivid: true,
  },
)

const canvasId = `globe-${Math.random().toString(36).slice(2, 10)}`
const instance = getCurrentInstance()
const ctx = ref<UniApp.CanvasContext | null>(null)
let animId = 0
let angle = 0
let running = true

const wrapStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  filter: props.vivid
    ? 'drop-shadow(0 0 28px rgba(56, 189, 248, 0.28)) drop-shadow(0 0 48px rgba(99, 102, 241, 0.15))'
    : 'none',
}))

function project(
  lat: number,
  lon: number,
  r: number,
  cx: number,
  cy: number,
  tiltAngle: number,
  rotation: number,
): { x: number; y: number; z: number } {
  const phi = (lat * Math.PI) / 180
  const theta = ((lon + rotation) * Math.PI) / 180

  const x3 = r * Math.cos(phi) * Math.sin(theta)
  const y3 = r * Math.sin(phi)
  const z3 = r * Math.cos(phi) * Math.cos(theta)

  const cosT = Math.cos(tiltAngle)
  const sinT = Math.sin(tiltAngle)
  const y2 = y3 * cosT - z3 * sinT
  const z2 = y3 * sinT + z3 * cosT

  return { x: cx + x3, y: cy - y2, z: z2 }
}

function hexToRgba(hex: string, alpha: number): string {
  const a = Math.max(0, Math.min(1, alpha))
  if (hex.startsWith('rgba') || hex.startsWith('rgb')) {
    const match = hex.match(/[\d.]+/g)
    if (match && match.length >= 3) return `rgba(${match[0]}, ${match[1]}, ${match[2]}, ${a})`
  }
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${a})`
}

function drawDataBars(c: UniApp.CanvasContext, r: number, cx: number, cy: number, rotationDeg: number) {
  for (let i = 0; i < props.dataBarCount; i++) {
    const seed = i * 137.508
    const lat = Math.sin(seed * 0.71) * 62
    const lon = (seed * 2.17 + rotationDeg * 0.15) % 360
    const inner = project(lat, lon, r * 0.99, cx, cy, props.tilt, rotationDeg)
    const barLen = 4 + (Math.sin(seed * 1.3) * 0.5 + 0.5) * 10
    const outer = project(lat, lon, r * 0.99 + barLen, cx, cy, props.tilt, rotationDeg)
    if (inner.z <= 0) continue
    const hue = 180 + ((i * 17) % 80)
    c.beginPath()
    c.moveTo(inner.x, inner.y)
    c.lineTo(outer.x, outer.y)
    c.setStrokeStyle(`hsla(${hue}, 92%, ${props.vivid ? 64 : 58}%, ${props.opacity * 1.6})`)
    c.setLineWidth(1.1)
    c.stroke()
  }
}

function drawGlobe() {
  if (!ctx.value) return
  const c = ctx.value
  const s = props.size
  const cx = s / 2
  const cy = s / 2
  const r = s * 0.38

  c.clearRect(0, 0, s, s)

  const gradient = (
    c as UniApp.CanvasContext & {
      createRadialGradient: (...args: number[]) => UniApp.CanvasGradient
    }
  ).createRadialGradient(cx, cy, r * 0.15, cx, cy, r * 1.35)
  gradient.addColorStop(0, hexToRgba(props.glowColor, props.opacity * 0.22))
  gradient.addColorStop(0.55, hexToRgba(props.glowColor, props.opacity * 0.08))
  gradient.addColorStop(1, 'rgba(0,0,0,0)')
  c.setFillStyle(gradient as unknown as string)
  c.fillRect(0, 0, s, s)

  // 实心暗色球体（模拟 PC 地球本体）
  c.beginPath()
  c.arc(cx, cy, r, 0, Math.PI * 2)
  c.setFillStyle(hexToRgba('#0a1628', props.vivid ? 0.55 : 0.45))
  c.fill()

  const rotationDeg = (angle * 180) / Math.PI

  for (let i = 0; i < props.meridians; i++) {
    const lon = (360 / props.meridians) * i
    c.beginPath()
    let started = false
    for (let lat = -90; lat <= 90; lat += 4) {
      const p = project(lat, lon, r, cx, cy, props.tilt, rotationDeg)
      const alpha = p.z > 0 ? props.opacity : props.opacity * 0.18
      c.setStrokeStyle(hexToRgba(props.color, alpha))
      c.setLineWidth(props.lineWidth)
      if (!started) {
        c.moveTo(p.x, p.y)
        started = true
      } else {
        c.lineTo(p.x, p.y)
      }
    }
    c.stroke()
  }

  for (let i = 1; i <= props.parallels; i++) {
    const lat = -90 + (180 / (props.parallels + 1)) * i
    c.beginPath()
    let started = false
    for (let lon = 0; lon <= 360; lon += 4) {
      const p = project(lat, lon, r, cx, cy, props.tilt, rotationDeg)
      const alpha = p.z > 0 ? props.opacity : props.opacity * 0.18
      c.setStrokeStyle(hexToRgba(props.color, alpha))
      c.setLineWidth(props.lineWidth)
      if (!started) {
        c.moveTo(p.x, p.y)
        started = true
      } else {
        c.lineTo(p.x, p.y)
      }
    }
    c.stroke()
  }

  drawDataBars(c, r, cx, cy, rotationDeg)

  // 赤道高亮
  c.beginPath()
  let eqStarted = false
  for (let lon = 0; lon <= 360; lon += 3) {
    const p = project(0, lon, r, cx, cy, props.tilt, rotationDeg)
    const alpha = p.z > 0 ? props.opacity * 1.35 : props.opacity * 0.25
    c.setStrokeStyle(hexToRgba(props.color, alpha))
    c.setLineWidth(props.lineWidth * 1.15)
    if (!eqStarted) {
      c.moveTo(p.x, p.y)
      eqStarted = true
    } else {
      c.lineTo(p.x, p.y)
    }
  }
  c.stroke()

  // 大气外缘
  c.beginPath()
  c.arc(cx, cy, r, 0, Math.PI * 2)
  c.setStrokeStyle(hexToRgba(props.glowColor, props.opacity * 0.65))
  c.setLineWidth(props.lineWidth * 1.6)
  c.stroke()

  // 城市光点
  for (let i = 0; i < 14; i++) {
    const seed = i * 137.508 + angle * 40
    const lat = Math.sin(seed * 0.7) * 68
    const lon = (seed * 2.3) % 360
    const p = project(lat, lon, r * 0.98, cx, cy, props.tilt, rotationDeg)
    if (p.z > 0) {
      c.beginPath()
      c.arc(p.x, p.y, 1.3, 0, Math.PI * 2)
      c.setFillStyle(hexToRgba('#a8d8ff', props.opacity * 2))
      c.fill()
    }
  }

  c.draw()
}

const raf =
  typeof requestAnimationFrame !== 'undefined'
    ? requestAnimationFrame
    : (cb: FrameRequestCallback) => setTimeout(() => cb(Date.now()), 16) as unknown as number
const caf =
  typeof cancelAnimationFrame !== 'undefined' ? cancelAnimationFrame : clearTimeout

function animate() {
  if (!running) return
  angle += props.speed
  drawGlobe()
  animId = raf(animate)
}

onMounted(() => {
  setTimeout(() => {
    ctx.value = uni.createCanvasContext(canvasId, instance?.proxy || undefined)
    animate()
  }, 120)
})

onBeforeUnmount(() => {
  running = false
  caf(animId)
})
</script>

<style scoped>
.globe-wrap {
  display: block;
  margin: 0 auto;
}

.globe-canvas {
  display: block;
}
</style>
