<template>
  <teleport to="body">
    <transition name="aiwu-viewer-fade">
      <div
        v-if="visible"
        class="aiwu-viewer-overlay"
        role="dialog"
        aria-modal="true"
        aria-label="流程图全屏预览"
      >
        <div class="aiwu-viewer">
          <header class="aiwu-viewer-head">
            <span class="aiwu-viewer-title">
              <el-icon :size="14"><Share /></el-icon>
              流程图 · 全景预览
            </span>
            <div class="aiwu-viewer-tools">
              <span class="aiwu-viewer-scale">{{ Math.round(scale * 100) }}%</span>
              <el-tooltip content="缩小" placement="bottom" :show-after="1000">
                <button type="button" aria-label="缩小" @click="zoom(-0.15)">
                  <el-icon :size="14"><ZoomOut /></el-icon>
                </button>
              </el-tooltip>
              <el-tooltip content="重置视图" placement="bottom" :show-after="1000">
                <button type="button" aria-label="重置视图" @click="reset">
                  <el-icon :size="14"><RefreshLeft /></el-icon>
                </button>
              </el-tooltip>
              <el-tooltip content="放大" placement="bottom" :show-after="1000">
                <button type="button" aria-label="放大" @click="zoom(0.15)">
                  <el-icon :size="14"><ZoomIn /></el-icon>
                </button>
              </el-tooltip>
              <el-tooltip content="下载 SVG" placement="bottom" :show-after="1000">
                <button
                  type="button"
                  aria-label="下载 SVG"
                  :disabled="!svg"
                  @click="download"
                >
                  <el-icon :size="14"><Download /></el-icon>
                </button>
              </el-tooltip>
              <el-tooltip content="关闭 (Esc)" placement="bottom" :show-after="1000">
                <button
                  class="is-close"
                  type="button"
                  aria-label="关闭预览"
                  @click="close"
                >
                  <el-icon :size="15"><Close /></el-icon>
                </button>
              </el-tooltip>
            </div>
          </header>

          <div
            class="aiwu-viewer-body"
            @wheel.prevent="handleWheel"
            @pointerdown="startDrag"
            @dblclick="reset"
          >
            <div
              v-if="svg"
              class="aiwu-viewer-canvas"
              :style="{ transform: `translate(${offsetX}px, ${offsetY}px) scale(${scale})` }"
              v-html="svg"
            />
            <p v-else class="aiwu-viewer-empty">暂无可预览的流程图</p>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Close, Download, RefreshLeft, Share, ZoomIn, ZoomOut } from '@element-plus/icons-vue'

interface MermaidZoomDetail {
  svg?: string
  source?: string
}

const visible = ref(false)
const svg = ref('')
const scale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)

let dragging = false
let dragStartX = 0
let dragStartY = 0
let originX = 0
let originY = 0
let previousBodyOverflow = ''

function handleMermaidZoom(event: Event) {
  const detail = (event as CustomEvent<MermaidZoomDetail>).detail || {}
  svg.value = detail.svg || ''
  scale.value = 1
  offsetX.value = 0
  offsetY.value = 0
  visible.value = true
  previousBodyOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  fitToViewport()
}

/** 打开时按容器尺寸自动适配，避免超大流程图在 100% 下缩成一小团 */
function fitToViewport() {
  nextTick(() => {
    setTimeout(() => {
      const body = document.querySelector<HTMLElement>('.aiwu-viewer-body')
      const svgEl = document.querySelector<SVGSVGElement>('.aiwu-viewer-canvas svg')
      if (!body || !svgEl) return
      const viewBox = svgEl.viewBox?.baseVal
      const width = viewBox?.width || svgEl.getBoundingClientRect().width
      const height = viewBox?.height || svgEl.getBoundingClientRect().height
      if (!width || !height) return
      const fit = Math.min((body.clientWidth - 56) / width, (body.clientHeight - 56) / height)
      // 小图自动放大铺满视口；超大流程图保持 100% 原尺寸（可拖拽/滚轮放大），避免反向缩成小团
      scale.value = Math.min(2.4, Math.max(1, Number(fit.toFixed(2))))
      // 超高流程图初始从顶部开始展示，符合自上而下的阅读顺序
      const scaledHeight = height * scale.value
      offsetY.value =
        scaledHeight > body.clientHeight
          ? Math.round((scaledHeight - body.clientHeight) / 2) - 8
          : 0
      offsetX.value = 0
    }, 80)
  })
}

function close() {
  visible.value = false
  document.body.style.overflow = previousBodyOverflow
}

function zoom(delta: number) {
  scale.value = Math.min(3, Math.max(0.4, Number((scale.value + delta).toFixed(2))))
}

function reset() {
  fitToViewport()
}

function handleWheel(event: WheelEvent) {
  zoom(event.deltaY < 0 ? 0.12 : -0.12)
}

function startDrag(event: PointerEvent) {
  if (event.button !== 0) return
  dragging = true
  dragStartX = event.clientX
  dragStartY = event.clientY
  originX = offsetX.value
  originY = offsetY.value
  window.addEventListener('pointermove', handleDragMove)
  window.addEventListener('pointerup', stopDrag)
  window.addEventListener('pointercancel', stopDrag)
}

function handleDragMove(event: PointerEvent) {
  if (!dragging) return
  offsetX.value = originX + (event.clientX - dragStartX)
  offsetY.value = originY + (event.clientY - dragStartY)
}

function stopDrag() {
  dragging = false
  window.removeEventListener('pointermove', handleDragMove)
  window.removeEventListener('pointerup', stopDrag)
  window.removeEventListener('pointercancel', stopDrag)
}

function download() {
  if (!svg.value) return
  const blob = new Blob([svg.value], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `ai-wu-flowchart-${Date.now()}.svg`
  link.click()
  URL.revokeObjectURL(url)
}

function handleKeydown(event: KeyboardEvent) {
  if (!visible.value) return
  if (event.key === 'Escape') {
    event.stopPropagation()
    close()
  }
}

onMounted(() => {
  window.addEventListener('aiwu:mermaid-zoom', handleMermaidZoom)
  window.addEventListener('keydown', handleKeydown, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('aiwu:mermaid-zoom', handleMermaidZoom)
  window.removeEventListener('keydown', handleKeydown, true)
  stopDrag()
  document.body.style.overflow = previousBodyOverflow
})
</script>
