<template>
  <view v-if="show" class="slider-mask" @click="onClose">
    <view class="slider-panel" @click.stop>
      <view class="slider-panel__header">
        <text>请完成下列验证后继续</text>
        <text class="slider-panel__close" @click="onClose">×</text>
      </view>

      <view class="slider-panel__image">
        <image :src="currentBgUrl" class="slider-panel__bg" mode="aspectFill" @load="measure" />
        <view class="slider-panel__slot" :style="puzzleBoxStyle(targetX)" />
        <view class="slider-panel__piece" :class="{ verified }" :style="puzzleBoxStyle(offsetX)">
          <image :src="currentBgUrl" class="slider-panel__piece-img" mode="aspectFill" :style="pieceImgStyle" />
        </view>
      </view>

      <view
        class="slider-panel__track"
        @touchmove.stop.prevent="onDragMove"
        @touchend.stop="onDragEnd"
        @touchcancel.stop="onDragEnd"
      >
        <view class="slider-panel__track-bg">
          <view class="slider-panel__track-fill" :style="{ width: `${offsetX}px` }" />
        </view>
        <view
          class="slider-panel__handle"
          :class="{ dragging, verified }"
          :style="{ left: `${offsetX}px` }"
          @touchstart.stop.prevent="onDragStart"
          @touchmove.stop.prevent="onDragMove"
          @touchend.stop="onDragEnd"
          @touchcancel.stop="onDragEnd"
        >
          <text v-if="!verified">›</text>
          <text v-else>✓</text>
        </view>
        <text v-if="offsetX < 2 && !verified" class="slider-panel__tip">按住左边按钮拖动完成上方拼图</text>
      </view>

      <view class="slider-panel__footer">
        <text class="slider-panel__refresh" @click="refresh">刷新</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const show = defineModel<boolean>('show', { default: false })
const emit = defineEmits<{ success: []; close: [] }>()

const PIECE_W = 52
const PIECE_H = 52
const IMAGE_H = 200
const TOLERANCE = 6
const TARGET_MIN = 130
const TARGET_RANGE = 110
const PUZZLE_CLIP = 'polygon(0% 0%, 78% 0%, 78% 28%, 100% 50%, 78% 72%, 78% 100%, 0% 100%)'

const BG_URLS = [
  '/static/captcha/bg1.jpg',
  '/static/captcha/bg2.jpg',
  '/static/captcha/bg3.jpg',
  '/static/captcha/bg4.jpg',
]

const imageWidth = ref(340)
const offsetX = ref(0)
const targetX = ref(180)
const pieceTop = ref(74)
const maxOffset = ref(280)
const dragging = ref(false)
const verified = ref(false)
const bgIndex = ref(0)
let dragStartX = 0

const currentBgUrl = computed(() => BG_URLS[bgIndex.value % BG_URLS.length])

function puzzleBoxStyle(left: number) {
  return {
    left: `${left}px`,
    top: `${pieceTop.value}px`,
    width: `${PIECE_W}px`,
    height: `${PIECE_H}px`,
    clipPath: PUZZLE_CLIP,
  }
}

const pieceImgStyle = computed(() => ({
  width: `${imageWidth.value}px`,
  height: `${IMAGE_H}px`,
  transform: `translate3d(${-targetX.value}px, ${-pieceTop.value}px, 0)`,
}))

function measure() {
  uni.createSelectorQuery()
    .select('.slider-panel__image')
    .boundingClientRect((rect) => {
      const box = Array.isArray(rect) ? rect[0] : rect
      if (box && 'width' in box && box.width) {
        imageWidth.value = box.width
        maxOffset.value = Math.max(PIECE_W, box.width - PIECE_W)
      }
      targetX.value = TARGET_MIN + Math.floor(Math.random() * TARGET_RANGE)
      targetX.value = Math.min(targetX.value, maxOffset.value - PIECE_W)
      pieceTop.value = 28 + Math.floor(Math.random() * (IMAGE_H - PIECE_H - 56))
    })
    .exec()
}

function refresh() {
  offsetX.value = 0
  verified.value = false
  dragging.value = false
  bgIndex.value = Math.floor(Math.random() * BG_URLS.length)
  measure()
}

function onClose() {
  show.value = false
  emit('close')
}

function clampX(x: number) {
  return Math.max(0, Math.min(maxOffset.value, x))
}

function getClientX(e: TouchEvent) {
  return e.touches[0]?.clientX ?? e.changedTouches[0]?.clientX ?? 0
}

function onDragStart(e: TouchEvent) {
  if (verified.value) return
  dragging.value = true
  dragStartX = getClientX(e) - offsetX.value
}

function onDragMove(e: TouchEvent) {
  if (!dragging.value || verified.value) return
  offsetX.value = clampX(getClientX(e) - dragStartX)
}

function onDragEnd() {
  if (!dragging.value || verified.value) return
  dragging.value = false
  if (Math.abs(offsetX.value - targetX.value) <= TOLERANCE) {
    verified.value = true
    offsetX.value = targetX.value
    uni.showToast({ title: '验证成功', icon: 'success' })
    setTimeout(() => {
      show.value = false
      emit('success')
    }, 450)
  } else {
    uni.showToast({ title: '验证失败，请重试', icon: 'none' })
    setTimeout(() => {
      offsetX.value = 0
    }, 280)
  }
}

watch(show, (v) => {
  if (v) refresh()
})
</script>

<style lang="scss" scoped>
.slider-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32rpx;
}

.slider-panel {
  width: 100%;
  max-width: 680rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}

.slider-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  font-size: 30rpx;
  color: #1f2937;
}

.slider-panel__close {
  font-size: 40rpx;
  color: #9ca3af;
  padding: 0 8rpx;
}

.slider-panel__image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  border-radius: 4rpx;
  background: #374151;
}

.slider-panel__bg {
  width: 100%;
  height: 100%;
}

.slider-panel__slot {
  position: absolute;
  background: rgba(0, 0, 0, 0.5);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.65);
  z-index: 1;
}

.slider-panel__piece {
  position: absolute;
  overflow: hidden;
  z-index: 2;

  &.verified {
    filter: drop-shadow(0 0 0 2px #22c55e);
  }
}

.slider-panel__piece-img {
  position: absolute;
  top: 0;
  left: 0;
  max-width: none;
}

.slider-panel__track {
  position: relative;
  height: 50px;
  margin-top: 24rpx;
  touch-action: none;
}

.slider-panel__track-bg {
  position: absolute;
  left: 0;
  right: 0;
  top: 7px;
  height: 36px;
  background: #f5f7fa;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.slider-panel__track-fill {
  height: 100%;
  background: linear-gradient(90deg, #bae0ff, #91caff);
}

.slider-panel__tip {
  position: absolute;
  left: 58px;
  right: 8px;
  top: 7px;
  height: 36px;
  line-height: 36px;
  text-align: center;
  font-size: 24rpx;
  color: #9ca3af;
  pointer-events: none;
}

.slider-panel__handle {
  position: absolute;
  top: 7px;
  width: 50px;
  height: 36px;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  z-index: 3;
  touch-action: none;

  &.dragging {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  }

  &.verified {
    background: #22c55e;
    border-color: #22c55e;
    color: #fff;
  }
}

.slider-panel__footer {
  margin-top: 16rpx;
  text-align: right;
}

.slider-panel__refresh {
  font-size: 26rpx;
  color: #1677ff;
}
</style>
