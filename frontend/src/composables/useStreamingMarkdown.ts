import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { renderMarkdown } from '@/utils/chat/chat-markdown'

/** 流式渲染节流间隔（与 EduMind 侧边栏一致：rAF + 80ms 节流，避免每个 token 全量重排） */
const STREAM_THROTTLE_MS = 80

/**
 * 流式输出期间对 Markdown 重渲染做节流，降低逐 token 解析与 KaTeX/Mermaid 计算开销。
 * 非流式状态（历史消息、生成完成）始终立即渲染完整内容。
 */
export function useStreamingMarkdown(content: Ref<string>, streaming: Ref<boolean | undefined>) {
  const displayContent = ref(content.value)
  let frame = 0
  let timer: ReturnType<typeof setTimeout> | null = null
  let lastRenderAt = 0
  let pendingValue = content.value

  function clearScheduled() {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    if (frame) {
      cancelAnimationFrame(frame)
      frame = 0
    }
  }

  function flush(value: string) {
    pendingValue = value
    displayContent.value = value
    lastRenderAt = Date.now()
  }

  function schedule(value: string) {
    pendingValue = value
    if (timer || frame) return
    const wait = Math.max(0, STREAM_THROTTLE_MS - (Date.now() - lastRenderAt))
    timer = setTimeout(() => {
      timer = null
      frame = requestAnimationFrame(() => {
        frame = 0
        displayContent.value = pendingValue
        lastRenderAt = Date.now()
      })
    }, wait)
  }

  watch(
    content,
    (value) => {
      if (!streaming.value) {
        clearScheduled()
        flush(value)
        return
      }
      schedule(value)
    },
    { immediate: true },
  )

  watch(streaming, (active) => {
    if (!active) {
      clearScheduled()
      flush(content.value)
    }
  })

  onBeforeUnmount(clearScheduled)

  const html = computed(() => renderMarkdown(displayContent.value))

  return { html }
}
