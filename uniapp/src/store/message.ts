import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMessageSummary, getChatGroups } from '@/api/message'
import {
  connectMessageWebSocket,
  disconnectMessageWebSocket,
  onMessageWebSocket,
  type WsPushMessage,
} from '@/utils/webSocket'
import {
  resolvePushTitle,
  shouldNotifyChat,
  shouldNotifyGroupChat,
  shouldCountGroupUnread,
  type ActiveChatTarget,
} from '@/utils/message-push'
import type { ChatGroup } from '@/types/message'
import { useUserStore } from '@/store/user'
import { logger } from '@/utils/logger'

export type { ActiveChatTarget } from '@/utils/message-push'

export interface PushNotification {
  id: number
  type: 'notice' | 'chat'
  title: string
  content: string
  time: number | string
  senderId?: number
  groupId?: number
  announceId?: number
}

export const useMessageStore = defineStore('message', () => {
  const inboxCount = ref(0)
  const announceCount = ref(0)
  const privateChatCount = ref(0)
  const groupUnreadById = ref<Record<number, number>>({})
  const groupNotifyMutedById = ref<Record<number, boolean>>({})
  const activeChatTarget = ref<ActiveChatTarget | null>(null)
  const showNotification = ref(false)
  const currentNotification = ref<PushNotification | null>(null)
  const announceListTick = ref(0)
  const recallTick = ref(0)
  const lastRecall = ref<WsPushMessage | null>(null)
  const groupAnnouncementTick = ref(0)
  const lastGroupAnnouncement = ref<WsPushMessage | null>(null)

  let offWs: (() => void) | null = null
  let notifyTimer: ReturnType<typeof setTimeout> | null = null

  const groupChatUnread = computed(() =>
    Object.values(groupUnreadById.value).reduce((sum, n) => sum + (Number(n) || 0), 0),
  )
  const chatCount = computed(() => privateChatCount.value + groupChatUnread.value)
  const noticeCount = computed(() => inboxCount.value + announceCount.value)
  const totalUnread = computed(() => noticeCount.value + chatCount.value)

  function setActiveChatTarget(target: ActiveChatTarget | null) {
    activeChatTarget.value = target
  }

  function syncGroupNotifySettings(groups: ChatGroup[]) {
    const map: Record<number, boolean> = { ...groupNotifyMutedById.value }
    for (const g of groups) {
      if (g.notifyMuted) map[g.id] = true
      else delete map[g.id]
    }
    groupNotifyMutedById.value = map
  }

  async function loadGroupNotifySettings() {
    try {
      const res = await getChatGroups()
      if (res.data) syncGroupNotifySettings(res.data)
    } catch (e) {
      logger.error('loadGroupNotifySettings failed', e)
    }
  }

  function setGroupNotifyMutedLocal(groupId: number, muted: boolean) {
    const next = { ...groupNotifyMutedById.value }
    if (muted) next[groupId] = true
    else delete next[groupId]
    groupNotifyMutedById.value = next
  }

  function incrementGroupUnread(groupId: number) {
    if (!groupId) return
    const prev = groupUnreadById.value[groupId] || 0
    groupUnreadById.value = { ...groupUnreadById.value, [groupId]: prev + 1 }
  }

  function clearGroupUnread(groupId: number) {
    if (!groupId || !groupUnreadById.value[groupId]) return
    const next = { ...groupUnreadById.value }
    delete next[groupId]
    groupUnreadById.value = next
  }

  function getGroupUnread(groupId: number) {
    return groupUnreadById.value[groupId] || 0
  }

  async function refreshSummary() {
    try {
      const res = await getMessageSummary()
      if (!res.data) return
      inboxCount.value = Number(res.data.inboxCount || 0)
      announceCount.value = Number(res.data.announceCount || 0)
      privateChatCount.value = Number(res.data.chatCount || 0)
    } catch (e) {
      logger.error('refreshSummary failed', e)
    }
  }

  function showPushNotification(msg: WsPushMessage) {
    const type: PushNotification['type'] = msg.type === 'notice' ? 'notice' : 'chat'
    const notification: PushNotification = {
      id: Date.now(),
      type,
      title: resolvePushTitle(msg),
      content: msg.content || '',
      time: msg.time ?? Date.now(),
      senderId: msg.senderId,
      groupId: msg.groupId,
      announceId: msg.announceId,
    }
    currentNotification.value = notification
    showNotification.value = true
    if (notifyTimer) clearTimeout(notifyTimer)
    notifyTimer = setTimeout(() => {
      if (currentNotification.value?.id === notification.id) {
        closeNotification()
      }
    }, 5000)
  }

  function closeNotification() {
    showNotification.value = false
    currentNotification.value = null
  }

  function handleWsMessage(msg: WsPushMessage) {
    if (msg.type === 'typing' || msg.type === 'presence' || msg.type === 'ping') return

    if (msg.recall && (msg.messageId != null || msg.id != null)) {
      lastRecall.value = msg
      recallTick.value++
      return
    }

    if (msg.type === 'notice') {
      showPushNotification(msg)
      announceListTick.value++
      refreshSummary()
      return
    }

    if (msg.type === 'chat') {
      if (shouldNotifyChat(activeChatTarget.value, msg)) {
        showPushNotification(msg)
      }
      refreshSummary()
      return
    }

    if (msg.type === 'groupAnnouncement' && msg.groupId != null) {
      lastGroupAnnouncement.value = msg
      groupAnnouncementTick.value++
      const selfId = useUserStore().userInfo?.userId
      const isSelf = selfId != null && Number(msg.senderId) === Number(selfId)
      if (shouldNotifyChat(activeChatTarget.value, msg) && !isSelf) {
        if (shouldCountGroupUnread(msg, groupNotifyMutedById.value)) {
          incrementGroupUnread(msg.groupId)
        }
        if (shouldNotifyGroupChat(msg, groupNotifyMutedById.value)) {
          showPushNotification(msg)
        }
      }
      return
    }

    if (msg.type === 'groupChat' && msg.groupId != null) {
      if (shouldNotifyChat(activeChatTarget.value, msg)) {
        if (shouldCountGroupUnread(msg, groupNotifyMutedById.value)) {
          incrementGroupUnread(msg.groupId)
        }
        if (shouldNotifyGroupChat(msg, groupNotifyMutedById.value)) {
          showPushNotification(msg)
        }
      }
      return
    }

    refreshSummary()
  }

  function initWebSocket() {
    if (offWs) return
    connectMessageWebSocket()
    offWs = onMessageWebSocket(handleWsMessage)
    loadGroupNotifySettings()
    refreshSummary()
  }

  function destroyWebSocket() {
    offWs?.()
    offWs = null
    disconnectMessageWebSocket()
    activeChatTarget.value = null
    if (notifyTimer) {
      clearTimeout(notifyTimer)
      notifyTimer = null
    }
    inboxCount.value = 0
    announceCount.value = 0
    privateChatCount.value = 0
    groupUnreadById.value = {}
    groupNotifyMutedById.value = {}
    closeNotification()
  }

  return {
    inboxCount,
    announceCount,
    privateChatCount,
    groupUnreadById,
    groupNotifyMutedById,
    groupChatUnread,
    activeChatTarget,
    showNotification,
    currentNotification,
    announceListTick,
    recallTick,
    lastRecall,
    groupAnnouncementTick,
    lastGroupAnnouncement,
    chatCount,
    noticeCount,
    totalUnread,
    refreshSummary,
    setActiveChatTarget,
    syncGroupNotifySettings,
    loadGroupNotifySettings,
    setGroupNotifyMutedLocal,
    incrementGroupUnread,
    clearGroupUnread,
    getGroupUnread,
    initWebSocket,
    destroyWebSocket,
    closeNotification,
    showPushNotification,
    handleWsMessage,
  }
})
