<template>
  <div
    ref="rootRef"
    class="chat-md-content ai-wu-markdown"
    :class="{ 'is-streaming': streaming }"
    v-html="html"
  />
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useStreamingMarkdown } from '@/composables/useStreamingMarkdown'
import { renderMermaidInElement } from '@/utils/chat/chat-markdown'

const props = withDefaults(
  defineProps<{
    content: string
    streaming?: boolean
    /** 深度思考详情等场景无需渲染流程图 */
    enableMermaid?: boolean
  }>(),
  {
    streaming: false,
    enableMermaid: true,
  },
)

const rootRef = ref<HTMLElement>()
const { html } = useStreamingMarkdown(
  computed(() => props.content),
  computed(() => props.streaming),
)

let mermaidTimer: ReturnType<typeof setTimeout> | null = null

function scheduleMermaidRender() {
  if (mermaidTimer) {
    clearTimeout(mermaidTimer)
    mermaidTimer = null
  }
  // 等流式结束后的 DOM 稳定，再统一渲染流程图（避免半截语法反复失败）
  mermaidTimer = setTimeout(() => {
    mermaidTimer = null
    if (!props.streaming && props.enableMermaid) {
      void renderMermaidInElement(rootRef.value || null)
    }
  }, 60)
}

watch(
  [html, () => props.streaming],
  async () => {
    await nextTick()
    scheduleMermaidRender()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (mermaidTimer) clearTimeout(mermaidTimer)
})
</script>
