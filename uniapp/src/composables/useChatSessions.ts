import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { storeToRefs } from 'pinia'
import { getChatUsers, getChatGroups } from '@/api/message'
import { useMessageStore } from '@/store/message'
import { previewMessageText } from '@/utils/chat-message'
import { isViewingChat } from '@/utils/message-push'
import { onMessageWebSocket, type WsPushMessage } from '@/utils/webSocket'
import type { ChatGroup, ChatUser } from '@/types/message'

export function useChatSessions() {
  const messageStore = useMessageStore()
  const { groupUnreadById, groupAtMeById, privateChatCount, privateReadTick, lastPrivateReadUserId } =
    storeToRefs(messageStore)
  const mode = ref<'private' | 'group'>('private')
  const users = ref<ChatUser[]>([])
  const groups = ref<ChatGroup[]>([])
  const loading = ref(false)
  const keyword = ref('')

  let offWs: (() => void) | null = null

  const filteredUsers = computed(() => {
    const q = keyword.value.trim().toLowerCase()
    if (!q) return users.value
    return users.value.filter((u) =>
      `${u.nickname || ''}${u.username || ''}`.toLowerCase().includes(q),
    )
  })

  const filteredGroups = computed(() => {
    const q = keyword.value.trim().toLowerCase()
    if (!q) return groups.value
    return groups.value.filter((g) => g.name.toLowerCase().includes(q))
  })

  const privateTabDot = computed(() => {
    const fromList = users.value.reduce((sum, u) => sum + (Number(u.unreadCount) || 0), 0)
    return Math.max(fromList, privateChatCount.value) > 0
  })

  const groupTabDot = computed(() =>
    Object.values(groupUnreadById.value).some((n) => Number(n) > 0),
  )

  const chatTabs = computed(() => [
    { key: 'private', label: '私聊', dot: privateTabDot.value },
    { key: 'group', label: '群聊', dot: groupTabDot.value },
  ])

  function groupUnread(groupId: number) {
    return groupUnreadById.value[groupId] || 0
  }

  function groupAtMe(groupId: number) {
    return !!groupAtMeById.value[groupId]
  }

  function bumpUser(senderId: number, msg: WsPushMessage) {
    const u = users.value.find((x) => x.id === senderId)
    const preview = previewMessageText({ content: msg.content, msgType: msg.msgType })
    const time = msg.time ? String(msg.time) : new Date().toISOString()
    if (u) {
      u.lastMessage = preview
      u.lastMessageTime = time
      if (!isViewingChat(messageStore.activeChatTarget, msg)) {
        u.unreadCount = (u.unreadCount || 0) + 1
      }
      return
    }
    silentRefresh()
  }

  function bumpGroup(groupId: number, msg: WsPushMessage) {
    const g = groups.value.find((x) => x.id === groupId)
    const preview = msg.senderName
      ? `${msg.senderName}: ${previewMessageText({ content: msg.content, msgType: msg.msgType })}`
      : previewMessageText({ content: msg.content, msgType: msg.msgType })
    const time = msg.time ? String(msg.time) : new Date().toISOString()
    if (g) {
      g.lastMessage = preview
      g.lastMessageTime = time
      return
    }
    silentRefresh()
  }

  function patchGroupAnnouncement(msg: WsPushMessage) {
    if (msg.type !== 'groupAnnouncement' || msg.groupId == null) return
    const g = groups.value.find((x) => x.id === msg.groupId)
    if (!g) return
    g.announcementUnread = true
    if (msg.announcement != null) g.announcement = msg.announcement
    g.announcementPublisherId = msg.senderId
    g.announcementPublisherName = msg.senderName
  }

  function handleWs(msg: WsPushMessage) {
    if (msg.recall) return
    if (msg.type === 'groupAnnouncement') {
      patchGroupAnnouncement(msg)
      messageStore.refreshSummary()
      return
    }
    if (msg.type === 'chat' && msg.senderId) {
      bumpUser(msg.senderId, msg)
      messageStore.refreshSummary()
      return
    }
    if (msg.type === 'groupChat' && msg.groupId) {
      bumpGroup(msg.groupId, msg)
    }
  }

  async function silentRefresh() {
    try {
      const [userRes, groupRes] = await Promise.all([getChatUsers(), getChatGroups()])
      users.value = userRes.data || []
      groups.value = groupRes.data || []
      messageStore.syncGroupNotifySettings(groups.value)
      await messageStore.refreshSummary()
    } catch {
      /* ignore */
    }
  }

  async function refresh() {
    loading.value = true
    try {
      await silentRefresh()
    } finally {
      loading.value = false
    }
  }

  watch(privateReadTick, () => {
    const userId = lastPrivateReadUserId.value
    if (!userId) return
    const u = users.value.find((x) => x.id === userId)
    if (u) u.unreadCount = 0
  })

  onMounted(() => {
    offWs = onMessageWebSocket(handleWs)
  })

  onBeforeUnmount(() => {
    offWs?.()
    offWs = null
  })

  return {
    mode,
    keyword,
    users,
    groups,
    filteredUsers,
    filteredGroups,
    chatTabs,
    groupUnread,
    groupAtMe,
    loading,
    refresh,
    silentRefresh,
  }
}
