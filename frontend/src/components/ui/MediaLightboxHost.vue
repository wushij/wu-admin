<template>
  <Teleport to="body">
    <Transition name="lightbox-fade">
      <div
        v-if="lightboxState.visible && currentImage"
        class="ui-lightbox"
        role="dialog"
        aria-modal="true"
        aria-label="媒体灯箱预览"
        @click.self="closeLightbox"
        @wheel.prevent="onWheel"
      >
        <header class="ui-lightbox-bar">
          <div class="ui-lightbox-meta">
            <span class="ui-lightbox-title">{{ currentImage.title || '图片预览' }}</span>
            <span v-if="lightboxState.images.length > 1" class="ui-lightbox-counter">
              {{ lightboxState.activeIndex + 1 }} / {{ lightboxState.images.length }}
            </span>
          </div>
          <div class="ui-lightbox-tools">
            <el-tooltip content="缩小" placement="bottom" :show-after="1000">
              <button type="button" class="ui-lb-btn" aria-label="缩小" @click="zoom(-0.25)">
                <el-icon :size="16"><ZoomOut /></el-icon>
              </button>
            </el-tooltip>
            <span class="ui-lb-scale">{{ Math.round(scale * 100) }}%</span>
            <el-tooltip content="放大" placement="bottom" :show-after="1000">
              <button type="button" class="ui-lb-btn" aria-label="放大" @click="zoom(0.25)">
                <el-icon :size="16"><ZoomIn /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="旋转" placement="bottom" :show-after="1000">
              <button type="button" class="ui-lb-btn" aria-label="旋转" @click="rotate">
                <el-icon :size="16"><RefreshRight /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="重置比例" placement="bottom" :show-after="1000">
              <button type="button" class="ui-lb-btn" aria-label="重置比例" @click="resetTransform">
                <el-icon :size="16"><FullScreen /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="关闭 (Esc)" placement="bottom" :show-after="1000">
              <button type="button" class="ui-lb-btn is-close" aria-label="关闭灯箱" @click="closeLightbox">
                <el-icon :size="18"><Close /></el-icon>
              </button>
            </el-tooltip>
          </div>
        </header>

        <div class="ui-lightbox-stage" @click.self="closeLightbox">
          <button
            v-if="lightboxState.images.length > 1"
            type="button"
            class="ui-lb-nav prev"
            aria-label="上一张"
            @click="prev"
          >
            <el-icon :size="20"><ArrowLeft /></el-icon>
          </button>

          <div v-if="imgLoading" class="ui-lb-skeleton motion-shimmer" />

          <img
            v-show="!imgLoading && !imgError"
            :src="currentImage.src"
            :alt="currentImage.title || '预览大图'"
            class="ui-lb-img"
            :style="imgStyle"
            @load="imgLoading = false"
            @error="onImgError"
            @dblclick="toggleZoom"
          />

          <div v-if="imgError" class="ui-lb-error">
            <el-icon :size="40"><PictureFilled /></el-icon>
            <p>图片加载失败</p>
          </div>

          <button
            v-if="lightboxState.images.length > 1"
            type="button"
            class="ui-lb-nav next"
            aria-label="下一张"
            @click="next"
          >
            <el-icon :size="20"><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue'
import {
  Close,
  ZoomIn,
  ZoomOut,
  RefreshRight,
  FullScreen,
  ArrowLeft,
  ArrowRight,
  PictureFilled,
} from '@element-plus/icons-vue'
import { lightboxState, closeLightbox } from '@/composables/ui/useLightbox'

const scale = ref(1)
const deg = ref(0)
const imgLoading = ref(false)
const imgError = ref(false)

const currentImage = computed(() => lightboxState.images[lightboxState.activeIndex] ?? null)

const imgStyle = computed(() => ({
  transform: `scale(${scale.value}) rotate(${deg.value}deg)`,
}))

function resetTransform() {
  scale.value = 1
  deg.value = 0
}

function zoom(delta: number) {
  scale.value = Math.max(0.5, Math.min(3, Number((scale.value + delta).toFixed(2))))
}

function toggleZoom() {
  scale.value = scale.value > 1.2 ? 1 : 2
}

function rotate() {
  deg.value = (deg.value + 90) % 360
}

function prev() {
  const len = lightboxState.images.length
  if (len <= 1) return
  lightboxState.activeIndex = (lightboxState.activeIndex - 1 + len) % len
}

function next() {
  const len = lightboxState.images.length
  if (len <= 1) return
  lightboxState.activeIndex = (lightboxState.activeIndex + 1) % len
}

function onWheel(e: WheelEvent) {
  zoom(e.deltaY < 0 ? 0.15 : -0.15)
}

function onImgError() {
  imgLoading.value = false
  imgError.value = true
}

function onKeyDown(e: KeyboardEvent) {
  if (!lightboxState.visible) return
  if (e.key === 'Escape') {
    e.preventDefault()
    closeLightbox()
  } else if (e.key === 'ArrowLeft') {
    e.preventDefault()
    prev()
  } else if (e.key === 'ArrowRight') {
    e.preventDefault()
    next()
  }
}

watch(
  () => [lightboxState.visible, lightboxState.activeIndex, currentImage.value?.src] as const,
  ([visible]) => {
    if (typeof document !== 'undefined') {
      document.body.classList.toggle('lightbox-scroll-locked', visible)
    }
    if (visible) {
      resetTransform()
      imgLoading.value = true
      imgError.value = false
      window.addEventListener('keydown', onKeyDown)
    } else {
      window.removeEventListener('keydown', onKeyDown)
    }
  },
)

onUnmounted(() => {
  if (typeof document !== 'undefined') {
    document.body.classList.remove('lightbox-scroll-locked')
  }
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<style scoped lang="scss">
.ui-lightbox {
  position: fixed;
  inset: 0;
  z-index: var(--z-lightbox, 2800);
  background: rgba(5, 8, 16, 0.88);
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  color: #f8fafc;
}

.ui-lightbox-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-5);
  background: rgba(15, 23, 42, 0.56);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.ui-lightbox-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.ui-lightbox-title {
  font-size: var(--text-body);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ui-lightbox-counter {
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.12);
  font-size: var(--text-caption);
  font-variant-numeric: tabular-nums;
}

.ui-lightbox-tools {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.ui-lb-scale {
  min-width: 48px;
  text-align: center;
  font-size: var(--text-caption);
  font-variant-numeric: tabular-nums;
  color: rgba(248, 250, 252, 0.8);
}

.ui-lb-btn {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-circle);
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);

  &:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  &.is-close {
    margin-left: var(--space-2);
  }
}

.ui-lightbox-stage {
  flex: 1;
  min-height: 0;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
  overflow: hidden;
}

.ui-lb-img {
  max-width: 88vw;
  max-height: 82vh;
  border-radius: var(--radius-md);
  object-fit: contain;
  user-select: none;
  transition: transform var(--duration-fast) var(--ease-out);
}

.ui-lb-skeleton {
  width: 360px;
  height: 240px;
  border-radius: var(--radius-md);
}

.ui-lb-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  color: rgba(248, 250, 252, 0.72);
}

.ui-lb-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: var(--radius-circle);
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(15, 23, 42, 0.65);
  color: #f8fafc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
  transition: background-color var(--duration-fast) var(--ease-out);

  &:hover {
    background: rgba(255, 255, 255, 0.22);
  }

  &.prev {
    left: var(--space-5);
  }

  &.next {
    right: var(--space-5);
  }
}

.lightbox-fade-enter-active,
.lightbox-fade-leave-active {
  transition: opacity var(--duration-fast) var(--ease-out);
}

.lightbox-fade-enter-from,
.lightbox-fade-leave-to {
  opacity: 0;
}
</style>
