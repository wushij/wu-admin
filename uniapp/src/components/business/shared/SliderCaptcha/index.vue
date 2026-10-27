<template>
  <view v-if="show" class="slider-mask" @click="onClose">
    <view class="slider-panel" @click.stop>
      <view class="slider-panel__header">
        <text>请完成下列验证后继续</text>
        <text class="slider-panel__close" @click="onClose">×</text>
      </view>

      <view class="slider-panel__image" :style="{ height: imageHeightPx + 'px' }">
        <template v-if="challenge">
          <image
            :src="challenge.background"
            class="slider-panel__bg"
            mode="scaleToFill"
            @load="measure"
          />
          <view class="slider-panel__piece" :class="{ verified }" :style="pieceStyle">
            <image :src="challenge.piece" class="slider-panel__piece-img" mode="scaleToFill" />
          </view>
        </template>
        <view v-else class="slider-panel__loading">
          <text>加载中…</text>
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
          :class="{ dragging, verified, disabled: verifying || loadingChallenge }"
          :style="{ left: `${offsetX}px` }"
          @touchstart.stop.prevent="onDragStart"
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
import { ref, computed, watch, nextTick } from 'vue'
import { getSliderChallenge, verifySlider } from '@/api/system/auth'
import type { SliderChallengeResult } from '@/api/system/auth'
import type { SliderTrackEvent, SliderVerifyPayload } from '@/utils/security/slider-captcha'

const props = withDefaults(
  defineProps<{
    scene?: string
  }>(),
  { scene: 'login' },
)

const show = defineModel<boolean>('show', { default: false })
const emit = defineEmits<{
  success: [payload: SliderVerifyPayload]
  close: []
}>()

const MAX_TRACK_EVENTS = 240

const challenge = ref<SliderChallengeResult | null>(null)
const displayWidth = ref(340)
const offsetX = ref(0)
const dragging = ref(false)
const verifying = ref(false)
const verified = ref(false)
const loadingChallenge = ref(false)

let dragStartX = 0
let dragStartTime = 0
let lastY = 0
let events: SliderTrackEvent[] = []

const displayScale = computed(() => {
  const width = challenge.value?.width || 340
  return displayWidth.value > 0 ? displayWidth.value / width : 1
})

const pieceDisplaySize = computed(() => {
  const size = challenge.value?.pieceSize || 52
  return Math.max(1, Math.round(size * displayScale.value))
})

const maxOffset = computed(() => Math.max(0, displayWidth.value - pieceDisplaySize.value))

const imageHeightPx = computed(() => {
  const width = challenge.value?.width || 340
  const height = challenge.value?.height || 200
  return Math.max(1, Math.round(displayWidth.value * height / width))
})

const pieceStyle = computed(() => {
  const top = challenge.value?.pieceTop || 0
  return {
    left: `${offsetX.value}px`,
    top: `${Math.round(top * displayScale.value)}px`,
    width: `${pieceDisplaySize.value}px`,
    height: `${pieceDisplaySize.value}px`,
  }
})

function measure() {
  nextTick(() => {
    uni.createSelectorQuery()
      .select('.slider-panel__image')
      .boundingClientRect((rect) => {
        const box = Array.isArray(rect) ? rect[0] : rect
        if (box && 'width' in box && box.width) {
          displayWidth.value = box.width
        }
      })
      .exec()
  })
}

async function refresh() {
  if (verifying.value) return
  offsetX.value = 0
  verified.value = false
  dragging.value = false
  events = []
  loadingChallenge.value = true
  try {
    const res = await getSliderChallenge(props.scene)
    const data = res.data
    if (!data?.token || !data.background || !data.piece) {
      throw new Error('滑块挑战数据不完整')
    }
    challenge.value = data
    await nextTick()
    measure()
  } catch {
    challenge.value = null
    uni.showToast({ title: '加载滑块验证失败', icon: 'none' })
  } finally {
    loadingChallenge.value = false
  }
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

function getClientY(e: TouchEvent) {
  return e.touches[0]?.clientY ?? e.changedTouches[0]?.clientY ?? 0
}

function onDragStart(e: TouchEvent) {
  if (verified.value || verifying.value || loadingChallenge.value || !challenge.value) return
  dragging.value = true
  const clientX = getClientX(e)
  dragStartX = clientX - offsetX.value
  dragStartTime = Date.now()
  const query = uni.createSelectorQuery().select('.slider-panel__image')
  query.boundingClientRect((rect) => {
    const box = Array.isArray(rect) ? rect[0] : rect
    if (box && 'top' in box && typeof box.top === 'number') {
      lastY = Math.round((getClientY(e) - box.top) / displayScale.value)
    }
  }).exec()
  events = [{ x: Math.round(offsetX.value / displayScale.value), y: lastY, t: 0 }]
}

function onDragMove(e: TouchEvent) {
  if (!dragging.value || verified.value) return
  offsetX.value = clampX(getClientX(e) - dragStartX)
  if (events.length < MAX_TRACK_EVENTS) {
    events.push({
      x: Math.round(offsetX.value / displayScale.value),
      y: lastY,
      t: Date.now() - dragStartTime,
    })
  }
}

async function onDragEnd() {
  if (!dragging.value || verified.value) return
  dragging.value = false

  if (!challenge.value) return
  if (offsetX.value < 2) {
    offsetX.value = 0
    events = []
    return
  }
  if (verifying.value) return

  const durationMs = Date.now() - dragStartTime
  const serverOffsetX = Math.round(offsetX.value / displayScale.value)
  events.push({
    x: serverOffsetX,
    y: lastY,
    t: durationMs,
  })
  // 轨迹点达到上限时保留最后一个终点，服务端会校验“最后一点与提交偏移量一致”，
  // 直接截断会把终点丢掉，长距离拖动会稳定失败。
  const track = events.length > MAX_TRACK_EVENTS
    ? [...events.slice(0, MAX_TRACK_EVENTS - 1), events[events.length - 1]]
    : events

  verifying.value = true
  try {
    const res = await verifySlider({
      token: challenge.value.token,
      offsetX: serverOffsetX,
      durationMs,
      events: track,
    })
    const ticket = res.data?.ticket
    if (!ticket) {
      throw new Error('服务端未返回验证票据')
    }
    verified.value = true
    uni.showToast({ title: '验证成功', icon: 'success' })
    setTimeout(() => {
      show.value = false
      emit('success', { token: ticket, offsetX: serverOffsetX })
    }, 450)
  } catch (error) {
    const message = (error as { message?: string })?.message || ''
    if (!message) {
      uni.showToast({ title: '验证失败，请重试', icon: 'none' })
    }
    offsetX.value = 0
    events = []
    if (message.includes('刷新') || message.includes('过期') || message.includes('次数过多')) {
      await refresh()
    }
  } finally {
    verifying.value = false
  }
}

watch(show, (v) => {
  if (v) {
    void refresh()
  }
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
  user-select: none;
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
  line-height: 1;
  padding: 0 8rpx;
}

.slider-panel__image {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 4rpx;
  background: #374151;
}

.slider-panel__bg {
  width: 100%;
  height: 100%;
}

.slider-panel__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #d1d5db;
  font-size: 26rpx;
}

.slider-panel__piece {
  position: absolute;
  overflow: hidden;
  z-index: 2;
  filter: drop-shadow(0 0 0 2px #fff) drop-shadow(0 3px 10px rgba(0, 0, 0, 0.35));

  &.verified {
    filter: drop-shadow(0 0 0 2px #22c55e) drop-shadow(0 3px 10px rgba(34, 197, 94, 0.45));
  }
}

.slider-panel__piece-img {
  width: 100%;
  height: 100%;
}

.slider-panel__track {
  position: relative;
  height: 100rpx;
  margin-top: 24rpx;
}

.slider-panel__track-bg {
  position: absolute;
  left: 0;
  right: 0;
  top: 14rpx;
  height: 72rpx;
  background: #f5f7fa;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  overflow: hidden;
}

.slider-panel__track-fill {
  height: 100%;
  background: linear-gradient(90deg, #bae0ff, #91caff);
}

.slider-panel__handle {
  position: absolute;
  top: 14rpx;
  width: 100rpx;
  height: 72rpx;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #6b7280;
  z-index: 2;

  &.dragging {
    border-color: #91caff;
    color: #1677ff;
  }

  &.verified {
    background: #22c55e;
    border-color: #22c55e;
    color: #fff;
  }

  &.disabled {
    opacity: 0.65;
  }
}

.slider-panel__tip {
  position: absolute;
  left: 116rpx;
  right: 16rpx;
  top: 14rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  font-size: 26rpx;
  color: #9ca3af;
}

.slider-panel__footer {
  margin-top: 20rpx;
  text-align: right;
}

.slider-panel__refresh {
  font-size: 26rpx;
  color: #6b7280;
}
</style>
