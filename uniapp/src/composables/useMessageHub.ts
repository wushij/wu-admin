import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { useMessageStore } from '@/store/message'
import { usePermission } from '@/composables/usePermission'
import { MESSAGE_CHANNELS } from '@/constants/messageChannels'

export function useMessageHub() {
  const messageStore = useMessageStore()
  const { inboxCount, announceCount, chatCount } = storeToRefs(messageStore)
  const { hasPerm } = usePermission()

  function badgeOf(key: (typeof MESSAGE_CHANNELS)[number]['countKey']) {
    if (key === 'announceCount') return announceCount.value
    if (key === 'inboxCount') return inboxCount.value
    return chatCount.value
  }

  const channels = computed(() =>
    MESSAGE_CHANNELS.filter((ch) => !ch.permission || hasPerm(ch.permission)).map((ch) => ({
      ...ch,
      badge: badgeOf(ch.countKey),
    })),
  )

  onShow(() => {
    messageStore.refreshSummary()
  })

  return { channels, inboxCount, announceCount, chatCount }
}
