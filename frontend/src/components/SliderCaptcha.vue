<template>
  <el-dialog
    v-model="visible"
    width="380px"
    :show-close="false"
    :close-on-click-modal="false"
    destroy-on-close
    append-to-body
    align-center
    modal-class="slider-captcha-overlay"
    class="slider-captcha-dialog"
    @closed="onClosed"
  >
    <div class="slider-panel">
      <div class="slider-panel__header">
        <span>请完成下列验证后继续</span>
        <button type="button" class="slider-panel__close" aria-label="关闭" @click="closePanel">
          <el-icon :size="18"><Close /></el-icon>
        </button>
      </div>

      <div
        ref="imageRef"
        class="slider-panel__image"
        :style="imageBoxStyle"
      >
        <template v-if="challenge">
          <img
            :src="challenge.background"
            class="slider-panel__bg"
            alt=""
            draggable="false"
            @load="measure"
          />
          <!-- 拼图块由服务端从缺口位置裁切，缺口位置本身只存在于服务端 -->
          <div class="slider-panel__piece" :class="{ verified }" :style="pieceStyle">
            <img :src="challenge.piece" class="slider-panel__piece-img" alt="" draggable="false" />
          </div>
        </template>
        <el-skeleton v-else :rows="4" animated class="slider-panel__skeleton" />
      </div>

      <div class="slider-panel__track">
        <div class="slider-panel__track-bg">
          <div class="slider-panel__track-fill" :style="{ width: `${offsetX}px` }" />
        </div>
        <div
          class="slider-panel__handle"
          :class="{ dragging, verified, disabled: verifying || loadingChallenge }"
          :style="{ left: `${offsetX}px` }"
          @mousedown="onDragStart"
          @touchstart.prevent="onDragStart"
        >
          <el-icon v-if="!verified" :size="18"><ArrowRight /></el-icon>
          <el-icon v-else :size="18"><Check /></el-icon>
        </div>
        <span v-if="offsetX < 2 && !verified" class="slider-panel__tip">
          按住左边按钮拖动完成上方拼图
        </span>
      </div>

      <div class="slider-panel__footer">
        <button type="button" class="slider-panel__refresh" @click="refresh">
          <el-icon :size="14"><Refresh /></el-icon>
          刷新
        </button>
        <span class="slider-panel__id">{{ captchaId }}</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, ArrowRight, Refresh, Check } from '@element-plus/icons-vue'
import { getSliderChallenge, verifySlider } from '@/api/system/auth'
import type { SliderChallengeResult } from '@/api/system/auth'
import type { SliderTrackEvent, SliderVerifyPayload } from '@/types/slider-captcha'
import { getErrorMessage, isErrorToastShown } from '@/utils/axiosError'

const visible = defineModel('show', { type: Boolean, default: false })

const props = withDefaults(
  defineProps<{
    /** login | register | sms | profile | forgot */
    scene?: string
  }>(),
  { scene: 'login' },
)

const emit = defineEmits<{
  success: [payload: SliderVerifyPayload]
  close: []
}>()

const MAX_TRACK_EVENTS = 240

const imageRef = ref<HTMLElement | null>(null)
const challenge = ref<SliderChallengeResult | null>(null)
const displayWidth = ref(340)
const offsetX = ref(0)
const dragging = ref(false)
const verifying = ref(false)
const verified = ref(false)
const loadingChallenge = ref(false)
const captchaId = ref('')

let dragStartX = 0
let dragStartTime = 0
let lastY = 0
let events: SliderTrackEvent[] = []

/** 显示宽度与服务端逻辑宽度的比例，用于把拖动距离换算成服务端坐标 */
const displayScale = computed(() => {
  const width = challenge.value?.width || 340
  return displayWidth.value > 0 ? displayWidth.value / width : 1
})

const pieceDisplaySize = computed(() => {
  const size = challenge.value?.pieceSize || 52
  return Math.max(1, Math.round(size * displayScale.value))
})

const maxOffset = computed(() => Math.max(0, displayWidth.value - pieceDisplaySize.value))

const imageBoxStyle = computed(() => {
  const width = challenge.value?.width || 340
  const height = challenge.value?.height || 200
  return { aspectRatio: `${width} / ${height}` }
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
    const box = imageRef.value
    if (box && box.clientWidth > 0) {
      displayWidth.value = box.clientWidth
    }
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
    captchaId.value = data.token.slice(0, 16).toUpperCase()
    await nextTick()
    measure()
  } catch (error) {
    challenge.value = null
    captchaId.value = ''
    if (!isErrorToastShown(error)) {
      ElMessage.error(getErrorMessage(error) || '加载滑块验证失败，请重试')
    }
  } finally {
    loadingChallenge.value = false
  }
}

function closePanel() {
  visible.value = false
}

function onClosed() {
  stopDrag()
  offsetX.value = 0
  events = []
  emit('close')
}

function clampX(x: number) {
  return Math.max(0, Math.min(maxOffset.value, x))
}

function getClientX(e: MouseEvent | TouchEvent): number {
  if ('touches' in e && e.touches.length) return e.touches[0].clientX
  if ('changedTouches' in e && e.changedTouches.length) return e.changedTouches[0].clientX
  return (e as MouseEvent).clientX
}

function getClientY(e: MouseEvent | TouchEvent): number {
  if ('touches' in e && e.touches.length) return e.touches[0].clientY
  if ('changedTouches' in e && e.changedTouches.length) return e.changedTouches[0].clientY
  return (e as MouseEvent).clientY
}

function onDragStart(e: MouseEvent | TouchEvent) {
  if (verified.value || verifying.value || loadingChallenge.value || !challenge.value) return
  dragging.value = true
  dragStartX = getClientX(e) - offsetX.value
  dragStartTime = performance.now()
  const rect = imageRef.value?.getBoundingClientRect()
  lastY = rect ? Math.round((getClientY(e) - rect.top) / displayScale.value) : 0
  events = [{ x: Math.round(offsetX.value / displayScale.value), y: lastY, t: 0 }]
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  document.addEventListener('touchmove', onDragMove, { passive: false })
  document.addEventListener('touchend', onDragEnd)
  document.addEventListener('touchcancel', onDragEnd)
}

function onDragMove(e: MouseEvent | TouchEvent) {
  if (!dragging.value || verified.value) return
  if (e.cancelable) e.preventDefault()
  offsetX.value = clampX(getClientX(e) - dragStartX)

  const rect = imageRef.value?.getBoundingClientRect()
  if (rect) {
    lastY = Math.round((getClientY(e) - rect.top) / displayScale.value)
  }
  if (events.length < MAX_TRACK_EVENTS) {
    events.push({
      x: Math.round(offsetX.value / displayScale.value),
      y: lastY,
      t: Math.round(performance.now() - dragStartTime),
    })
  }
}

async function onDragEnd() {
  if (!dragging.value || verified.value) return
  stopDrag()

  if (!challenge.value) return
  if (offsetX.value < 2) {
    offsetX.value = 0
    events = []
    return
  }
  if (verifying.value) return

  const durationMs = Math.round(performance.now() - dragStartTime)
  const serverOffsetX = Math.round(offsetX.value / displayScale.value)
  events.push({
    x: serverOffsetX,
    y: lastY,
    t: durationMs,
  })
  // 轨迹点已到上限时，必须保留最后一个终点：服务端会校验「最后一点与提交偏移量一致」，
  // 直接用 slice(0, MAX) 会把终点切掉，导致长距离拖动被误判为“轨迹与提交位置不一致”。
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
    ElMessage.success('验证成功')
    setTimeout(() => {
      visible.value = false
      emit('success', { token: ticket, offsetX: serverOffsetX })
    }, 350)
  } catch (error) {
    if (!isErrorToastShown(error)) {
      ElMessage.warning(getErrorMessage(error) || '验证失败，请重试')
    }
    offsetX.value = 0
    events = []
    const message = getErrorMessage(error) || ''
    if (message.includes('刷新') || message.includes('过期') || message.includes('次数过多')) {
      await refresh()
    }
  } finally {
    verifying.value = false
  }
}

function stopDrag() {
  dragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('touchmove', onDragMove)
  document.removeEventListener('touchend', onDragEnd)
  document.removeEventListener('touchcancel', onDragEnd)
}

watch(visible, (v) => {
  if (v) {
    void refresh()
  } else {
    stopDrag()
  }
})

onMounted(() => {
  window.addEventListener('resize', measure)
})

onBeforeUnmount(() => {
  stopDrag()
  window.removeEventListener('resize', measure)
})
</script>

<style scoped lang="scss">
.slider-panel {
  user-select: none;
}

.slider-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid var(--color-border-subtle);
  font-size: 15px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.slider-panel__close {
  border: none;
  background: transparent;
  padding: 4px;
  cursor: pointer;
  color: var(--color-text-muted);
  line-height: 1;
  border-radius: 4px;
  &:hover {
    color: var(--color-text-secondary);
    background: var(--color-bg-hover);
  }
}

.slider-panel__image {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 2px;
  background: #374151;
}

.slider-panel__bg {
  width: 100%;
  height: auto;
  object-fit: fill;
  display: block;
  pointer-events: none;
}

.slider-panel__skeleton {
  padding: 16px;
}

.slider-panel__piece {
  position: absolute;
  overflow: hidden;
  z-index: 2;
  pointer-events: none;
  filter: drop-shadow(0 0 0 2px #fff) drop-shadow(0 3px 10px rgba(0, 0, 0, 0.35));

  &.verified {
    filter: drop-shadow(0 0 0 2px #22c55e) drop-shadow(0 3px 10px rgba(34, 197, 94, 0.45));
  }
}

.slider-panel__piece-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: fill;
  pointer-events: none;
}

.slider-panel__track {
  position: relative;
  height: 50px;
  margin-top: 12px;
}

.slider-panel__track-bg {
  position: absolute;
  left: 0;
  right: 0;
  top: 7px;
  height: 36px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

.slider-panel__track-fill {
  height: 100%;
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--el-color-primary) 24%, transparent),
    color-mix(in srgb, var(--el-color-primary) 42%, transparent)
  );
  transition: width 0.05s linear;
}

.slider-panel__handle {
  position: absolute;
  top: 7px;
  width: 50px;
  height: 36px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-strong);
  border-radius: 4px;
  box-shadow: var(--shadow-xs);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  cursor: grab;
  z-index: 2;

  &.dragging {
    cursor: grabbing;
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
  }

  &.verified {
    background: #22c55e;
    border-color: #22c55e;
    color: #fff;
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.65;
  }
}

.slider-panel__tip {
  position: absolute;
  left: 58px;
  right: 8px;
  top: 7px;
  height: 36px;
  line-height: 36px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
  pointer-events: none;
}

.slider-panel__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border-subtle);
}

.slider-panel__refresh {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  padding: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  &:hover {
    color: var(--el-color-primary);
  }
}

.slider-panel__id {
  font-size: 11px;
  color: var(--color-text-muted);
  font-family: monospace;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

<style lang="scss">
.slider-captcha-overlay {
  background-color: rgba(0, 0, 0, 0.45) !important;
}

.slider-captcha-dialog {
  border-radius: 8px;
  overflow: hidden;

  .el-dialog__header {
    display: none;
    margin: 0;
    padding: 0;
  }

  .el-dialog__body {
    padding: 16px 20px 14px;
  }
}
</style>
