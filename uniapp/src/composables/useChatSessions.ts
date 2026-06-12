import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getChatUsers, getChatGroups } from '@/api/message'
import { useMessageStore } from '@/store/message'
import { previewMessageText } from '@/utils/chat-message'
import { isViewingChat } from '@/utils/message-push'
import { onMessageWebSocket, type WsPushMessage } from '@/utils/webSocket'
import type { ChatGroup, ChatUser } from '@/types/message'

export function useChatSessions() {
  const messageStore = useMessageStore()
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
    refresh()
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
      if (!isViewingChat(messageStore.activeChatTarget, msg)) {
        g.unreadCount = (g.unreadCount || 0) + 1
      }
      return
    }
    refresh()
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
      messageStore.refreshSummary()
    }
  }

  async function refresh() {
    loading.value = true
    try {
      const [userRes, groupRes] = await Promise.all([getChatUsers(), getChatGroups()])
      users.value = userRes.data || []
      groups.value = groupRes.data || []
    } finally {
      loading.value = false
    }
  }

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
    loading,
    refresh,
  }
}
