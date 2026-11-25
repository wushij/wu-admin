<template>
  <section
    v-if="visible"
    ref="cardRef"
    class="aiwu-thinking"
    :class="{ 'is-active': isThinking }"
  >
    <button
      class="aiwu-thinking-head"
      type="button"
      :aria-expanded="expanded"
      :aria-label="expanded ? '收起深度思考' : '展开深度思考'"
      @click="toggleExpanded"
    >
      <div class="aiwu-thinking-head-left">
        <span class="aiwu-thinking-tag">
          <span class="aiwu-thinking-icon" :class="{ 'is-pulsing': isThinking }" aria-hidden="true">
            <span class="thinking-pulse" />
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
          </span>
          <span class="aiwu-thinking-title">{{ headTitle }}</span>
        </span>

        <span v-if="timerText" class="aiwu-thinking-time" :class="{ 'is-ticking': isThinking }">
          <span v-if="isThinking" class="time-dot" aria-hidden="true" />
          <span>{{ timerText }}</span>
        </span>
      </div>

      <span class="aiwu-thinking-toggle-text">{{ expanded ? '收起 ▲' : '展开 ▼' }}</span>
    </button>

    <div v-show="expanded" ref="bodyRef" class="aiwu-thinking-body">
      <!-- 真实系统工具/联网检索执行轨迹：轻量胶囊展示 -->
      <div v-if="toolItems.length" class="aiwu-thinking-tools">
        <span
          v-for="(tool, idx) in toolItems"
          :key="tool.id"
          class="aiwu-thinking-tool-chip"
          :class="{ 'is-running': isThinking && !cleanedReasoning && idx === toolItems.length - 1 }"
        >
          <span class="tool-dot" aria-hidden="true" />
          <span>{{ tool.label }}</span>
        </span>
      </div>

      <!-- 思考正文：整体自然流式平铺，无内部竖向滚动条 -->
      <div v-if="cleanedReasoning" class="aiwu-thinking-prose">
        <AiWuMarkdown
          :content="cleanedReasoning"
          :streaming="isThinking"
          :enable-mermaid="false"
        />
      </div>
      <div v-else-if="isThinking && !toolItems.length" class="aiwu-thinking-inline-status">
        <span class="thinking-spinner" />
        <span>正在推理思考与检索切片中…</span>
      </div>

      <button
        type="button"
        class="aiwu-thinking-collapse-btn"
        aria-label="收起深度思考"
        title="收起"
        @click.stop="toggleExpanded"
      >
        <svg class="aiwu-thinking-collapse-icon" viewBox="0 0 16 16" fill="none" aria-hidden="true">
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
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, useId, watch } from 'vue'
import AiWuMarkdown from './AiWuMarkdown.vue'
import {
  cleanThinkingText,
  formatThinkingDuration,
  normalizeToolLabel,
  type AiToolStepInput,
} from '@/utils/ai/thinking-steps'

const gradId = `aiwuThinkingGrad_${useId()}`

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
    /** 当前轮用户问题（保留透传兼容） */
    question?: string
    startedAt?: number
    finishedAt?: number
  }>(),
  {
    reasoning: '',
    tools: () => [],
    active: false,
    hasAnswerBody: false,
    reasoningStreaming: false,
    question: '',
    startedAt: 0,
    finishedAt: 0,
  },
)

const cardRef = ref<HTMLElement | null>(null)
const bodyRef = ref<HTMLElement | null>(null)

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
const visible = computed(() => Boolean(cleanedReasoning.value || toolItems.value.length || isThinking.value))

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

onBeforeUnmount(stopTimer)

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

/** 对标 EduMind：默认折叠成紧凑横条，点击「展开 ▼」后自然平铺展开 */
const expanded = ref(false)

function findChatScrollParent(el: HTMLElement | null): HTMLElement | null {
  let node = el?.parentElement ?? null
  while (node) {
    if (node.classList.contains('aiwu-messages')) return node
    const { overflowY } = window.getComputedStyle(node)
    if (overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay') {
      return node
    }
    node = node.parentElement
  }
  return null
}

/** 折叠长思考链后补偿外层滚动条位置，防止视口突然向下弹跳 */
function toggleExpanded() {
  if (!expanded.value) {
    expanded.value = true
    return
  }
  const scrollEl = findChatScrollParent(cardRef.value)
  const bodyEl = bodyRef.value
  if (!scrollEl || !bodyEl) {
    expanded.value = false
    return
  }
  const scrollTop = scrollEl.scrollTop
  const collapseTop = bodyEl.getBoundingClientRect().top - scrollEl.getBoundingClientRect().top + scrollTop
  const collapseHeight = bodyEl.offsetHeight
  expanded.value = false
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      if (collapseHeight > 0 && scrollTop > collapseTop) {
        scrollEl.scrollTop = Math.max(0, scrollTop - collapseHeight)
      }
    })
  })
}
</script>

<style scoped lang="scss" src="./ai-wu-thinking.scss"></style>
