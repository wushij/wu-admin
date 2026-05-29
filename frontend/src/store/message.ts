import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMessageSummary } from '@/api/message'
import { getMyNoticeList } from '@/api/system/notice'
import {
  connectMessageWebSocket,
  disconnectMessageWebSocket,
  onMessageWebSocket,
  type WsPushMessage,
} from '@/utils/messageWebSocket'

export interface PushNotification {
  id: number
  type: 'notice' | 'chat'
  title: string
  content: string
  time: number | string
  senderId?: number
  groupId?: number
}

export interface InboxNoticeItem {
  id: number
  title: string
  content: string
  readStatus?: number
  bizType?: string
  bizId?: number
  createTime?: string
}

export interface ActiveChatTarget {
  type: 'user' | 'group'
  id: number
}

export const useMessageStore = defineStore('message', () => {
  const inboxCount = ref(0)
  const announceCount = ref(0)
  /** 私聊未读（后端统计） */
  const privateChatCount = ref(0)
  /** 群聊未读（按群 ID 记录，后端暂无群已读表） */
  const groupUnreadById = ref<Record<number, number>>({})
  const groupChatUnread = computed(() =>
    Object.values(groupUnreadById.value).reduce((sum, n) => sum + (Number(n) || 0), 0),
  )
  const inboxList = ref<InboxNoticeItem[]>([])
  const showNotification = ref(false)
  const currentNotification = ref<PushNotification | null>(null)
  /** 当前正在查看的会话，用于抑制重复弹窗 */
  const activeChatTarget = ref<ActiveChatTarget | null>(null)
  let offWs: (() => void) | null = null
  let notifyTimer: ReturnType<typeof setTimeout> | null = null
  /** 递增后通知 layout 重新拉取系统通知列表 */
  const announceListTick = ref(0)

  const chatCount = computed(() => privateChatCount.value + groupChatUnread.value)
  const noticeCount = computed(() => inboxCount.value + announceCount.value)
  const totalUnread = computed(() => noticeCount.value + chatCount.value)

  function setActiveChatTarget(target: ActiveChatTarget | null) {
    activeChatTarget.value = target
  }

  function shouldNotifyChat(msg: WsPushMessage): boolean {
    const active = activeChatTarget.value
    if (!active) return true
    if (msg.type === 'groupChat' && active.type === 'group' && active.id === msg.groupId) {
      return false
    }
    if (msg.type === 'chat' && active.type === 'user' && active.id === msg.senderId) {
      return false
    }
    return true
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
      if (res.data) {
        inboxCount.value = Number(res.data.inboxCount || 0)
        announceCount.value = Number(res.data.announceCount || 0)
        privateChatCount.value = Number(res.data.chatCount || 0)
      }
    } catch (e) {
      console.error('refreshSummary failed', e)
    }
  }

  async function loadInbox() {
    try {
      const res = await getMyNoticeList()
      inboxList.value = (res.data || []) as InboxNoticeItem[]
    } catch (e) {
      console.error('loadInbox failed', e)
    }
  }

  function showPushNotification(msg: WsPushMessage) {
    let title = '新消息'
    let type: PushNotification['type'] = 'chat'
    if (msg.type === 'notice') {
      type = 'notice'
      title = msg.title || '系统通知'
    } else if (msg.type === 'groupChat') {
      title = msg.senderName ? `${msg.senderName}(群消息)` : '群消息'
    } else {
      title = msg.senderName || '新消息'
    }
    const notification: PushNotification = {
      id: Date.now(),
      type,
      title,
      content: msg.content || '',
      time: msg.time ?? Date.now(),
      senderId: msg.senderId,
      groupId: msg.groupId,
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
    if (msg.type === 'notice') {
      showPushNotification(msg)
      announceListTick.value++
      refreshSummary()
      return
    }
    if (msg.type === 'chat') {
      if (shouldNotifyChat(msg)) {
        showPushNotification(msg)
      }
      refreshSummary()
      return
    }
    if (msg.type === 'groupChat' && msg.groupId != null) {
      if (shouldNotifyChat(msg)) {
        incrementGroupUnread(msg.groupId)
        showPushNotification(msg)
      }
    }
  }

  function initWebSocket() {
    if (offWs) return
    connectMessageWebSocket()
    offWs = onMessageWebSocket(handleWsMessage)
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
  }

  return {
    inboxCount,
    announceCount,
    privateChatCount,
    groupUnreadById,
    groupChatUnread,
    chatCount,
    noticeCount,
    totalUnread,
    inboxList,
    showNotification,
    currentNotification,
    announceListTick,
    activeChatTarget,
    refreshSummary,
    loadInbox,
    initWebSocket,
    destroyWebSocket,
    closeNotification,
    showPushNotification,
    setActiveChatTarget,
    incrementGroupUnread,
    clearGroupUnread,
    getGroupUnread,
    handleWsMessage,
  }
})
