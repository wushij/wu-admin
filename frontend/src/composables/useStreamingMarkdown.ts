import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { renderMarkdown } from '@/utils/chat-markdown'

/** 流式输出期间用 rAF 节流 markdown 重渲染（AgentOne 模式） */
export function useStreamingMarkdown(content: Ref<string>, streaming: Ref<boolean | undefined>) {
  const displayContent = ref(content.value)
  let frame = 0

  watch(
    content,
    (value) => {
      if (!streaming.value) {
        displayContent.value = value
        return
      }
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(() => {
        displayContent.value = value
      })
    },
    { immediate: true },
  )

  watch(streaming, (active) => {
    if (!active) displayContent.value = content.value
  })

  onBeforeUnmount(() => cancelAnimationFrame(frame))

  const html = computed(() => renderMarkdown(displayContent.value))

  return { html }
}
