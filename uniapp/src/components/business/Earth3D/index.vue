<script setup lang="ts">
import * as THREE from 'three'
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { createEarthGroup } from './three/earth'
import { createGlowMesh } from './three/glow'
import { createDataBars } from './three/dataBars'
import { createStars } from './three/stars'

const props = withDefaults(
  defineProps<{
    autoRotate?: boolean
    rotateSpeed?: number
    barCount?: number
    showBars?: boolean
    showStars?: boolean
    transparent?: boolean
    /** 允许手指/鼠标拖动旋转视角 */
    enableOrbit?: boolean
    /** 允许缩放；登录页应关闭 */
    enableZoom?: boolean
    segments?: number
  }>(),
  {
    autoRotate: true,
    rotateSpeed: 0.0015,
    barCount: 220,
    showBars: true,
    showStars: false,
    transparent: true,
    enableOrbit: true,
    enableZoom: true,
    segments: 96,
  },
)

const containerRef = ref<HTMLDivElement | null>(null)
const loading = ref(true)

let scene: any
let renderer: any
let camera: any
let controls: any
let earthGroup: any
let glowUpdate: (() => void) | undefined
let animationId = 0
let resizeObserver: ResizeObserver | undefined

onMounted(() => {
  const container = containerRef.value
  if (!container) return

  const width = container.clientWidth
  const height = container.clientHeight

  scene = new THREE.Scene()
  if (!props.transparent) {
    scene.background = new THREE.Color(0x000000)
  }

  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 2000)
  camera.position.set(0, 0, 18)

  renderer = new THREE.WebGLRenderer({
    antialias: true,
    alpha: props.transparent,
    premultipliedAlpha: false,
  })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  const vivid = props.transparent
  renderer.toneMappingExposure = vivid ? 1.28 : 1.05
  if (props.transparent) {
    renderer.setClearColor(0x000000, 0)
    renderer.domElement.style.background = 'transparent'
  }
  container.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.autoRotate = props.autoRotate
  controls.autoRotateSpeed = 0.55
  controls.enableRotate = props.enableOrbit
  controls.enableZoom = props.enableZoom
  controls.enablePan = false
  controls.enabled = props.enableOrbit || props.enableZoom
  if (!props.enableZoom) {
    controls.minDistance = 18
    controls.maxDistance = 18
  } else {
    controls.minDistance = 10
    controls.maxDistance = 32
  }

  scene.add(new THREE.AmbientLight(0xffffff, vivid ? 0.52 : 0.35))

  const keyLight = new THREE.DirectionalLight(0xffffff, vivid ? 2.85 : 2.2)
  keyLight.position.set(5, 3, 5)
  scene.add(keyLight)

  const rimLight = new THREE.DirectionalLight(0x22d3ee, vivid ? 1.15 : 0.6)
  rimLight.position.set(-4, -2, -6)
  scene.add(rimLight)

  if (vivid) {
    scene.add(new THREE.HemisphereLight(0x6eb8ff, 0x080c18, 0.42))
    const fill = new THREE.DirectionalLight(0xaaccff, 0.35)
    fill.position.set(-6, 2, 8)
    scene.add(fill)
  }

  const { group, radius } = createEarthGroup(5, { vivid, segments: props.segments })
  earthGroup = group
  scene.add(earthGroup)

  const { glowMesh, update } = createGlowMesh(
    radius,
    camera,
    vivid ? 0x7ad4ff : undefined,
    vivid ? { opacityScale: 0.62, rimPower: 7.2, segments: props.segments } : { segments: props.segments },
  )
  glowUpdate = update
  earthGroup.add(glowMesh)

  if (props.showStars) {
    scene.add(createStars())
  }

  if (props.showBars) {
    earthGroup.add(createDataBars(radius, props.barCount, { vivid }))
  }

  const animate = () => {
    animationId = requestAnimationFrame(animate)
    if (props.autoRotate && earthGroup) {
      earthGroup.rotation.y += props.rotateSpeed
    }
    glowUpdate?.()
    controls?.update()
    if (renderer && camera && scene) {
      renderer.render(scene, camera)
    }
  }

  animate()
  loading.value = false

  const onResize = () => {
    const w = container.clientWidth
    const h = container.clientHeight
    if (!w || !h || !camera || !renderer) return
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
  }

  resizeObserver = new ResizeObserver(onResize)
  resizeObserver.observe(container)
})

watch(
  () => props.autoRotate,
  (val) => {
    if (controls) controls.autoRotate = val
  },
)

watch(
  () => [props.enableOrbit, props.enableZoom] as const,
  ([orbit, zoom]) => {
    if (!controls) return
    controls.enableRotate = orbit
    controls.enableZoom = zoom
    controls.enablePan = false
    controls.enabled = orbit || zoom
    if (!zoom) {
      controls.minDistance = 18
      controls.maxDistance = 18
    } else {
      controls.minDistance = 10
      controls.maxDistance = 32
    }
  },
)

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  resizeObserver?.disconnect()
  controls?.dispose()
  renderer?.dispose()
  if (containerRef.value && renderer?.domElement) {
    containerRef.value.removeChild(renderer.domElement)
  }
})
</script>

<template>
  <div class="earth3d" :class="{ 'earth3d--transparent': transparent }">
    <div ref="containerRef" class="earth3d__canvas" />
    <div v-if="loading" class="earth3d__loading" :class="{ 'earth3d__loading--transparent': transparent }">
      <span class="earth3d__spinner" />
    </div>
  </div>
</template>

<style scoped>
.earth3d {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: transparent;
}

.earth3d--transparent {
  background: transparent;
}

.earth3d__canvas {
  width: 100%;
  height: 100%;
  background: transparent;
}

.earth3d--transparent .earth3d__canvas,
.earth3d--transparent .earth3d__canvas :deep(canvas) {
  background: transparent !important;
}

.earth3d__canvas :deep(canvas) {
  display: block;
  touch-action: none;
}

.earth3d__loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2;
}

.earth3d__loading--transparent {
  background: transparent;
}

.earth3d__spinner {
  width: 28px;
  height: 28px;
  border: 2px solid rgba(34, 211, 238, 0.2);
  border-top-color: #22d3ee;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
