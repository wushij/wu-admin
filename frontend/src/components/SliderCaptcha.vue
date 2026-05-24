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
        @mousemove="onDragMove"
        @mouseup="onDragEnd"
        @mouseleave="onDragEnd"
        @touchmove.prevent="onDragMove"
        @touchend="onDragEnd"
      >
        <img
          :src="currentBgUrl"
          class="slider-panel__bg"
          alt=""
          draggable="false"
          @load="onImageLoad"
        />
        <!-- 缺口：与拼图块完全相同的 clip-path -->
        <div class="slider-panel__slot" :style="puzzleBoxStyle(targetX)" />
        <!-- 拼图块：显示缺口处背景切片，拖到 targetX 与缺口重合 -->
        <div class="slider-panel__piece" :class="{ verified }" :style="puzzleBoxStyle(offsetX)">
          <img
            :src="currentBgUrl"
            class="slider-panel__piece-img"
            alt=""
            draggable="false"
            :style="pieceImgStyle"
          />
        </div>
      </div>

      <div class="slider-panel__track">
        <div class="slider-panel__track-bg">
          <div class="slider-panel__track-fill" :style="{ width: `${offsetX}px` }" />
        </div>
        <div
          class="slider-panel__handle"
          :class="{ dragging, verified }"
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
          <span>刷新</span>
        </button>
        <span class="slider-panel__id">{{ captchaId }}</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, ArrowRight, Refresh, Check } from '@element-plus/icons-vue'
import { getSliderChallenge } from '@/api/system/auth'
import type { SliderVerifyPayload } from '@/types/slider-captcha'

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

const PIECE_W = 52
const PIECE_H = 52
const IMAGE_H = 200
const TOLERANCE = 6

/** 缺口与拼图块共用造型（右侧凸起拼图） */
const PUZZLE_CLIP =
  'polygon(0% 0%, 78% 0%, 78% 28%, 100% 50%, 78% 72%, 78% 100%, 0% 100%)'

const BG_URLS = [
  '/captcha/bg1.jpg',
  '/captcha/bg2.jpg',
  '/captcha/bg3.jpg',
  '/captcha/bg4.jpg'
]

const imageRef = ref<HTMLElement | null>(null)
const imageWidth = ref(340)
const offsetX = ref(0)
const targetX = ref(180)
const pieceTop = ref(74)
const maxOffset = ref(280)
const dragging = ref(false)
const verified = ref(false)
const bgIndex = ref(0)
const captchaId = ref('')
const challengeToken = ref('')
const loadingChallenge = ref(false)

let dragStartX = 0

const currentBgUrl = computed(() => BG_URLS[bgIndex.value % BG_URLS.length])

function puzzleBoxStyle(left: number) {
  return {
    left: `${left}px`,
    top: `${pieceTop.value}px`,
    width: `${PIECE_W}px`,
    height: `${PIECE_H}px`,
    clipPath: PUZZLE_CLIP,
    WebkitClipPath: PUZZLE_CLIP
  }
}

/** 拼图内大图与底图同源、同 cover，偏移到缺口坐标，对齐后无缝补全 */
const pieceImgStyle = computed(() => ({
  width: `${imageWidth.value}px`,
  height: `${IMAGE_H}px`,
  transform: `translate3d(${-targetX.value}px, ${-pieceTop.value}px, 0)`
}))

function measure() {
  nextTick(() => {
    const box = imageRef.value
    if (box) {
      imageWidth.value = box.clientWidth || 340
      maxOffset.value = Math.max(PIECE_W, box.clientWidth - PIECE_W)
      if (targetX.value > maxOffset.value - PIECE_W) {
        targetX.value = Math.max(0, maxOffset.value - PIECE_W)
      }
    }
  })
}

async function refresh() {
  offsetX.value = 0
  verified.value = false
  dragging.value = false
  loadingChallenge.value = true
  try {
    const res = await getSliderChallenge(props.scene)
    const data = res.data
    challengeToken.value = data.token
    captchaId.value = data.token.slice(0, 16).toUpperCase()
    bgIndex.value = data.bgIndex ?? 0
    pieceTop.value = data.pieceTop ?? 74
    targetX.value = data.targetX ?? 180
    measure()
  } catch {
    ElMessage.error('加载滑块验证失败，请重试')
    challengeToken.value = ''
  } finally {
    loadingChallenge.value = false
  }
}

function onImageLoad() {
  measure()
}

function closePanel() {
  visible.value = false
}

function onClosed() {
  stopDrag()
  offsetX.value = 0
  emit('close')
}

function clampX(x: number) {
  return Math.max(0, Math.min(maxOffset.value, x))
}

function getClientX(e: MouseEvent | TouchEvent): number {
  if ('touches' in e && e.touches.length) return e.touches[0].clientX
  return (e as MouseEvent).clientX
}

function onDragStart(e: MouseEvent | TouchEvent) {
  if (verified.value) return
  dragging.value = true
  const clientX = getClientX(e)
  dragStartX = clientX - offsetX.value
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  document.addEventListener('touchmove', onDragMove, { passive: false })
  document.addEventListener('touchend', onDragEnd)
  document.addEventListener('touchcancel', onDragEnd)
}

function onDragMove(e: MouseEvent | TouchEvent) {
  if (!dragging.value || verified.value) return
  if (e.cancelable) e.preventDefault()
  const clientX = getClientX(e)
  offsetX.value = clampX(clientX - dragStartX)
}

function onDragEnd() {
  if (!dragging.value || verified.value) return
  stopDrag()
  if (Math.abs(offsetX.value - targetX.value) <= TOLERANCE) {
    verified.value = true
    offsetX.value = targetX.value
    if (!challengeToken.value) {
      ElMessage.error('验证数据无效，请刷新')
      return
    }
    ElMessage.success('验证成功')
    const payload: SliderVerifyPayload = { token: challengeToken.value, offsetX: offsetX.value }
    setTimeout(() => {
      visible.value = false
      emit('success', payload)
    }, 450)
  } else {
    ElMessage.warning('验证失败，请重试')
    setTimeout(() => {
      offsetX.value = 0
    }, 280)
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
  if (v) refresh()
})

onBeforeUnmount(stopDrag)
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
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
  color: #1f2937;
  font-weight: 500;
}

.slider-panel__close {
  border: none;
  background: transparent;
  padding: 4px;
  cursor: pointer;
  color: #9ca3af;
  line-height: 1;
  border-radius: 4px;
  &:hover {
    color: #6b7280;
    background: #f3f4f6;
  }
}

.slider-panel__image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  border-radius: 2px;
  background: #374151;
}

.slider-panel__bg {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
  display: block;
  pointer-events: none;
}

.slider-panel__slot {
  position: absolute;
  background: rgba(0, 0, 0, 0.5);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.65);
  z-index: 1;
  pointer-events: none;
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
  position: absolute;
  top: 0;
  left: 0;
  max-width: none;
  object-fit: cover;
  object-position: center center;
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
  background: #f5f7fa;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.slider-panel__track-fill {
  height: 100%;
  background: linear-gradient(90deg, #bae0ff, #91caff);
  transition: width 0.05s linear;
}

.slider-panel__handle {
  position: absolute;
  top: 7px;
  width: 50px;
  height: 36px;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  cursor: grab;
  z-index: 2;

  &.dragging {
    cursor: grabbing;
    border-color: #91caff;
    color: #1677ff;
  }

  &.verified {
    background: #22c55e;
    border-color: #22c55e;
    color: #fff;
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
  color: #9ca3af;
  pointer-events: none;
}

.slider-panel__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.slider-panel__refresh {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  padding: 0;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  &:hover {
    color: #1677ff;
  }
}

.slider-panel__id {
  font-size: 11px;
  color: #c0c4cc;
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
