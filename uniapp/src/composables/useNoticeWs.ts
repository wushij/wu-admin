import { onMounted, onBeforeUnmount } from 'vue'
import { onMessageWebSocket } from '@/utils/webSocket'

/** WS 推送时刷新列表（公告 / 站内信） */
export function useNoticeWs(refresh: () => void | Promise<void>) {
  let off: (() => void) | null = null

  onMounted(() => {
    off = onMessageWebSocket((msg) => {
      if (msg.type === 'notice') refresh()
    })
  })

  onBeforeUnmount(() => {
    off?.()
    off = null
  })
}
