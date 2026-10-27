import { onBeforeUnmount, onMounted, ref } from 'vue'

/** H5：用 visualViewport 估算软键盘占用高度，让输入栏始终贴在键盘上方 */
export function useChatKeyboardInset() {
  const keyboardHeight = ref(0)

  // #ifdef H5
  let viewport: VisualViewport | null = null
  let sync: (() => void) | null = null

  onMounted(() => {
    viewport = window.visualViewport
    if (!viewport) return
    sync = () => {
      keyboardHeight.value = Math.max(0, window.innerHeight - viewport!.height - viewport!.offsetTop)
    }
    sync()
    viewport.addEventListener('resize', sync)
    viewport.addEventListener('scroll', sync)
  })

  onBeforeUnmount(() => {
    if (!viewport || !sync) return
    viewport.removeEventListener('resize', sync)
    viewport.removeEventListener('scroll', sync)
  })
  // #endif

  return { keyboardHeight }
}
