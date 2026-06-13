import { ref, nextTick } from 'vue'

const SETTLE_DELAYS = [0, 150, 400, 800]

/** 聊天消息区滚到底部，使最新消息出现在输入框上方 */
export function useChatScroll() {
  const scrollTop = ref(0)
  const scrollAnimated = ref(false)
  let scrollSeq = 0
  let timers: ReturnType<typeof setTimeout>[] = []

  function cancelPending() {
    timers.forEach(clearTimeout)
    timers = []
  }

  function measureMaxScroll() {
    return new Promise<number>((resolve) => {
      uni.createSelectorQuery()
        .select('.chat-page__messages')
        .boundingClientRect()
        .select('#chat-scroll-content')
        .boundingClientRect()
        .exec((res) => {
          const viewHeight = res[0]?.height ?? 0
          const contentHeight = res[1]?.height ?? 0
          resolve(Math.max(contentHeight - viewHeight, 0))
        })
    })
  }

  /** 单次滚到底，不先归零，避免发送时跳动 */
  async function scrollToBottom(animated = false) {
    cancelPending()
    scrollSeq += 1
    const seq = scrollSeq
    scrollAnimated.value = animated
    await nextTick()
    if (seq !== scrollSeq) return
    // 等新消息插入 DOM 后再量高度
    await new Promise<void>((resolve) => setTimeout(resolve, 30))
    if (seq !== scrollSeq) return
    const maxScroll = await measureMaxScroll()
    scrollTop.value = (maxScroll > 0 ? maxScroll : 99999) + seq
  }

  /** 仅首屏打开时多次补滚，不重置 scrollTop */
  function scrollToBottomSettle() {
    cancelPending()
    scrollSeq += 1
    const seq = scrollSeq
    scrollAnimated.value = false

    SETTLE_DELAYS.forEach((delay) => {
      timers.push(
        setTimeout(async () => {
          if (seq !== scrollSeq) return
          const maxScroll = await measureMaxScroll()
          scrollTop.value = (maxScroll > 0 ? maxScroll : 99999) + seq
        }, delay),
      )
    })
  }

  function onMediaLoaded() {
    scrollToBottom(false)
  }

  function measureContentHeight() {
    return new Promise<number>((resolve) => {
      uni.createSelectorQuery()
        .select('#chat-scroll-content')
        .boundingClientRect()
        .exec((res) => resolve(res[0]?.height ?? 0))
    })
  }

  async function preserveScrollAfterPrepend(update: () => void | Promise<void>) {
    cancelPending()
    scrollSeq += 1
    const seq = scrollSeq
    const prevHeight = await measureContentHeight()
    const prevTop = scrollTop.value
    await update()
    await nextTick()
    await new Promise<void>((resolve) => setTimeout(resolve, 80))
    if (seq !== scrollSeq) return
    const nextHeight = await measureContentHeight()
    scrollAnimated.value = false
    scrollTop.value = Math.max(nextHeight - prevHeight + prevTop, 0) + seq
  }

  return {
    scrollTop,
    scrollAnimated,
    scrollToBottom,
    scrollToBottomSettle,
    onMediaLoaded,
    preserveScrollAfterPrepend,
  }
}
