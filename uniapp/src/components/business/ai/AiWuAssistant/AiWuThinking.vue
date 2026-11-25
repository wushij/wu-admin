<template>
  <view v-if="visible" class="aiwu-thinking" :class="{ 'is-active': isThinking }">
    <view class="aiwu-thinking__head" @click="expanded = !expanded">
      <view class="aiwu-thinking__left">
        <view class="aiwu-thinking__tag">
          <view class="aiwu-thinking__icon" :class="{ 'is-pulsing': isThinking }">
            <view class="thinking-pulse" />
            <!-- #ifdef H5 -->
            <svg class="thinking-compass-icon" viewBox="0 0 16 16" fill="none">
              <defs>
                <linearGradient :id="gradId" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#1677ff" />
                  <stop offset="100%" stop-color="#722ed1" />
                </linearGradient>
              </defs>
              <circle
                cx="8"
                cy="8"
                r="6.6"
                stroke="currentColor"
                stroke-width="1"
                stroke-dasharray="1.6 1.6"
                opacity="0.6"
              />
              <circle cx="8" cy="8" r="4.8" stroke="currentColor" stroke-width="0.8" opacity="0.4" />
              <path
                class="thinking-compass-star"
                d="M8 2.6 L9.2 6.8 L13.4 8 L9.2 9.2 L8 13.4 L6.8 9.2 L2.6 8 L6.8 6.8 Z"
                :fill="`url(#${gradId})`"
              />
              <circle cx="8" cy="8" r="1.1" fill="#ffffff" />
            </svg>
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <IconFont name="cluster-o" :size="24" color="#1677ff" />
            <!-- #endif -->
          </view>
          <text class="aiwu-thinking-title">{{ headTitle }}</text>
        </view>

        <view v-if="timerText" class="aiwu-thinking__time" :class="{ 'is-ticking': isThinking }">
          <view v-if="isThinking" class="time-dot" />
          <text>{{ timerText }}</text>
        </view>
      </view>
      <text class="aiwu-thinking__toggle">{{ expanded ? '收起 ▲' : '展开 ▼' }}</text>
    </view>

    <view v-if="expanded" class="aiwu-thinking__body">
      <!-- 真实系统工具/联网检索执行轨迹：轻量胶囊展示 -->
      <view v-if="toolItems.length" class="aiwu-thinking__tools">
        <view
          v-for="(tool, idx) in toolItems"
          :key="tool.id"
          class="aiwu-thinking__tool-chip"
          :class="{ 'is-running': isThinking && !cleanedReasoning && idx === toolItems.length - 1 }"
        >
          <view class="tool-dot" />
          <text>{{ tool.label }}</text>
        </view>
      </view>

      <!-- 思考正文：自然流式平铺，无内部竖向滚动条 -->
      <view v-if="cleanedReasoning" class="aiwu-thinking__prose">
        <view
          v-if="isH5"
          class="aiwu-thinking__md"
          v-html="renderMarkdown(cleanedReasoning)"
        />
        <text v-else class="aiwu-thinking__plain">{{ cleanedReasoning }}</text>
      </view>
      <view v-else-if="isThinking && !toolItems.length" class="aiwu-thinking__status">
        <view class="thinking-spinner" />
        <text>正在推理思考与检索切片中…</text>
      </view>

      <view
        class="aiwu-thinking__collapse-btn"
        aria-label="收起深度思考"
        @click.stop="expanded = false"
      >
        <!-- #ifdef H5 -->
        <svg class="aiwu-thinking__collapse-icon" viewBox="0 0 16 16" fill="none">
          <path
            d="M4.5 10.5 8 7l3.5 3.5"
            stroke="currentColor"
            stroke-width="1.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            d="M4.5 6.5 8 3l3.5 3.5"
            stroke="currentColor"
            stroke-width="1.4"
            stroke-linecap="round"
            stroke-linejoin="round"
            opacity="0.55"
          />
        </svg>
        <!-- #endif -->
        <!-- #ifndef H5 -->
        <text class="aiwu-thinking__collapse-glyph">▲</text>
        <!-- #endif -->
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import IconFont from '@/components/base/IconFont/index.vue'
import { renderMarkdown } from '@/utils/display/chat-markdown'
import {
  cleanThinkingText,
  formatThinkingDuration,
  normalizeToolLabel,
  type AiToolStepInput,
} from '@/utils/ai/thinking-steps'

// UniApp 内置 uni-h5-vue 运行时不导出 Vue 3.5 的 useId，改用唯一后缀生成渐变 ID
const gradId = `aiwuThinkingGrad_${Math.random().toString(36).slice(2, 10)}`

const props = withDefaults(
  defineProps<{
    reasoning?: string
    tools?: AiToolStepInput[]
    /** 整条回答是否仍在流式生成 */
    active?: boolean
    /** 正文回答是否已开始输出 */
    hasAnswerBody?: boolean
    /** 思维链是否仍在增长 */
    reasoningStreaming?: boolean
    startedAt?: number
    finishedAt?: number
  }>(),
  {
    reasoning: '',
    tools: () => [],
    active: false,
    hasAnswerBody: false,
    reasoningStreaming: false,
    startedAt: 0,
    finishedAt: 0,
  },
)

// 仅 H5 支持 v-html 渲染 Markdown
let isH5 = false
// #ifdef H5
isH5 = true
// #endif

const cleanedReasoning = computed(() => cleanThinkingText(props.reasoning || ''))
const toolItems = computed(() =>
  (props.tools || [])
    .map((tool, index) => ({
      id: tool.id || `tool-${index}`,
      label: normalizeToolLabel(tool.message),
    }))
    .filter((item) => Boolean(item.label)),
)

const isThinking = computed(() => Boolean(props.active && !props.hasAnswerBody && !props.finishedAt))
const visible = computed(() =>
  Boolean(cleanedReasoning.value || toolItems.value.length || isThinking.value),
)

const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

function startTimer() {
  stopTimer()
  now.value = Date.now()
  timer = setInterval(() => {
    now.value = Date.now()
  }, 500)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

watch(
  () => isThinking.value,
  (thinking) => {
    if (thinking) startTimer()
    else stopTimer()
  },
  { immediate: true },
)

onUnmounted(stopTimer)

const elapsedMs = computed(() => {
  if (!props.startedAt) return 0
  const end = props.finishedAt || (isThinking.value ? now.value : props.startedAt)
  return Math.max(0, end - props.startedAt)
})

const timerText = computed(() => {
  if (!elapsedMs.value) return ''
  if (isThinking.value) {
    const totalSeconds = Math.floor(elapsedMs.value / 1000)
    if (totalSeconds <= 0) return ''
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    return `已思考 ${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  if (!cleanedReasoning.value && !toolItems.value.length) return ''
  return `用时 ${formatThinkingDuration(elapsedMs.value)}`
})

const headTitle = computed(() => {
  if (isThinking.value) return '深度思考中…'
  if (!cleanedReasoning.value && toolItems.value.length > 0) return '已完成系统查询'
  return '已深度思考'
})

/** 与 PC 一致：默认折叠为紧凑横条，点击后自然平铺展开 */
const expanded = ref(false)
</script>
