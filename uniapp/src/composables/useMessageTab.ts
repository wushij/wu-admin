import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import {
  getMyAnnounce,
  getChatGroups,
  getChatUsers,
  readAllAnnounce,
} from '@/api/message'
import { getMyNoticeList, readAllNotice } from '@/api/system/notice'
import { useMessageStore } from '@/store/message'
import { usePermission } from '@/composables/usePermission'
import { MESSAGE_CHANNELS } from '@/constants/messageChannels'
import { previewMessageText } from '@/utils/chat-message'
import { formatListTime, summarizeText } from '@/utils/format'
import { inboxContentPreview } from '@/utils/inbox-nav'
import { isViewingChat } from '@/utils/message-push'
import { onMessageWebSocket, type WsPushMessage } from '@/utils/webSocket'
import type { AnnounceMyVO, ChatGroup, ChatUser, NoticeVO } from '@/types/message'

export type MessageTabKey = 'announce' | 'inbox' | 'chat'

/** 消息 Tab 首页各频道预览条数 */
export const MESSAGE_PREVIEW_LIMIT = 8

export interface ChatSessionPreview {
  key: string
  type: 'user' | 'group'
  id: number
  title: string
  desc: string
  time?: string
  badge: number
  atMe?: boolean
  online?: boolean
  avatar?: string
}

const TAB_META: Record<MessageTabKey, { label: string; listPath: string }> = {
  announce: { label: '公告', listPath: '/pages-sub/msg/announce/index' },
  inbox: { label: '业务消息', listPath: '/pages-sub/msg/inbox/index' },
  chat: { label: '企业IM', listPath: '/pages-sub/msg/chat/index' },
}

const MESSAGE_TAB_STORAGE_KEY = 'message_tab_mode'

function readSavedTabMode(): MessageTabKey | null {
  try {
    const raw = uni.getStorageSync(MESSAGE_TAB_STORAGE_KEY)
    if (raw === 'announce' || raw === 'inbox' || raw === 'chat') return raw
  } catch {
    /* ignore */
  }
  return null
}

function saveTabMode(mode: MessageTabKey) {
  try {
    uni.setStorageSync(MESSAGE_TAB_STORAGE_KEY, mode)
  } catch {
    /* ignore */
  }
}

function pickDefaultTab(tabs: { key: string }[]): MessageTabKey {
  // 与 MESSAGE_CHANNELS 展示顺序一致：公告 → 企业 IM → 业务消息
  for (const ch of MESSAGE_CHANNELS) {
    if (tabs.some((t) => t.key === ch.key)) return ch.key as MessageTabKey
  }
  return (tabs[0]?.key || 'announce') as MessageTabKey
}

function resolveAvailableTabKeys(hasMenuPerm: (perm: string) => boolean): MessageTabKey[] {
  return MESSAGE_CHANNELS.filter(
    (ch) => !ch.permission || hasMenuPerm(ch.permission),
  ).map((ch) => ch.key as MessageTabKey)
}

function resolveInitialTab(hasMenuPerm: (perm: string) => boolean): MessageTabKey {
  const saved = readSavedTabMode()
  const keys = resolveAvailableTabKeys(hasMenuPerm)
  if (saved && keys.includes(saved)) return saved
  // 权限尚未加载完时先恢复用户上次停留的 tab，避免刷新/重进页面闪到业务消息
  if (saved) return saved
  return pickDefaultTab(keys.map((key) => ({ key })))
}

export function useMessageTab() {
  const messageStore = useMessageStore()
  const { groupUnreadById, groupAtMeById, privateReadTick, lastPrivateReadUserId } =
    storeToRefs(messageStore)
  const { hasMenuPerm } = usePermission()
  const tabInitialized = ref(true)
  const loading = ref(false)
  const announceList = ref<AnnounceMyVO[]>([])
  const inboxList = ref<NoticeVO[]>([])
  const chatUsers = ref<ChatUser[]>([])
  const chatGroups = ref<ChatGroup[]>([])

  // 同步从 storage 恢复 tab，避免刷新/重进时闪回默认业务消息
  const mode = ref<MessageTabKey>(resolveInitialTab(hasMenuPerm))

  let offWs: (() => void) | null = null

  const availableTabs = computed(() =>
    MESSAGE_CHANNELS.filter((ch) => !ch.permission || hasMenuPerm(ch.permission)).map((ch) => {
      const key = ch.key as MessageTabKey
      let badge = 0
      if (ch.countKey === 'announceCount') badge = messageStore.announceCount
      else if (ch.countKey === 'inboxCount') badge = messageStore.inboxCount
      else badge = messageStore.chatCount
      return {
        key,
        label: TAB_META[key].label,
        badge,
      }
    }),
  )

  const canMarkAllRead = computed(() => mode.value === 'announce' || mode.value === 'inbox')

  const listPath = computed(() => TAB_META[mode.value].listPath)

  const chatSessions = computed(() => {
    const unreadMap = groupUnreadById.value
    const atMeMap = groupAtMeById.value
    const sessions: ChatSessionPreview[] = [
      ...chatUsers.value.map((u) => ({
        key: `u-${u.id}`,
        type: 'user' as const,
        id: u.id,
        title: u.nickname || u.username || '用户',
        desc: u.lastMessage || '暂无消息',
        time: u.lastMessageTime,
        badge: u.unreadCount || 0,
        online: u.online,
        avatar: u.avatar,
      })),
      ...chatGroups.value.map((g) => ({
        key: `g-${g.id}`,
        type: 'group' as const,
        id: g.id,
        title: g.name,
        desc: g.lastMessage || `${g.memberCount || 0} 人`,
        time: g.lastMessageTime,
        badge: unreadMap[g.id] || 0,
        atMe: !!atMeMap[g.id],
      })),
    ]
    return sessions
      .sort((a, b) => {
        const ta = a.time ? new Date(a.time).getTime() : 0
        const tb = b.time ? new Date(b.time).getTime() : 0
        return tb - ta
      })
      .slice(0, MESSAGE_PREVIEW_LIMIT)
  })

  function patchChatPreview(msg: WsPushMessage) {
    if (msg.type === 'chat' && msg.senderId) {
      const u = chatUsers.value.find((x) => x.id === msg.senderId)
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
      loadChat()
      return
    }

    if (msg.type === 'groupChat' && msg.groupId) {
      const g = chatGroups.value.find((x) => x.id === msg.groupId)
      const preview = msg.senderName
        ? `${msg.senderName}: ${previewMessageText({ content: msg.content, msgType: msg.msgType })}`
        : previewMessageText({ content: msg.content, msgType: msg.msgType })
      const time = msg.time ? String(msg.time) : new Date().toISOString()
      if (g) {
        g.lastMessage = preview
        g.lastMessageTime = time
        return
      }
      loadChat()
    }
  }

  function handleWs(msg: WsPushMessage) {
    if (msg.recall) return
    if (msg.type === 'chat' || msg.type === 'groupChat') {
      patchChatPreview(msg)
    }
  }

  async function loadAnnounce() {
    const res = await getMyAnnounce({ pageNo: 1, pageSize: MESSAGE_PREVIEW_LIMIT })
    announceList.value = res.data?.list || []
  }

  async function loadInbox() {
    const res = await getMyNoticeList()
    inboxList.value = (res.data || []).slice(0, MESSAGE_PREVIEW_LIMIT)
  }

  async function loadChat() {
    const [userRes, groupRes] = await Promise.all([getChatUsers(), getChatGroups()])
    chatUsers.value = userRes.data || []
    chatGroups.value = groupRes.data || []
    messageStore.syncGroupNotifySettings(chatGroups.value)
  }

  /** 后台同步会话列表（不触发 loading），用于返回页面时刷新角标 */
  async function silentRefreshChat() {
    try {
      await loadChat()
      await messageStore.refreshSummary()
    } catch {
      /* ignore */
    }
  }

  async function loadCurrent() {
    loading.value = true
    try {
      if (mode.value === 'announce') await loadAnnounce()
      else if (mode.value === 'inbox') await loadInbox()
      else await loadChat()
    } finally {
      loading.value = false
    }
  }

  async function refresh() {
    const currentMode = mode.value
    await messageStore.refreshSummary()
    if (mode.value !== currentMode) {
      mode.value = currentMode
    }
    await loadCurrent()
  }

  async function markAllRead() {
    if (mode.value === 'announce') {
      await readAllAnnounce()
      announceList.value.forEach((item) => {
        item.isRead = 1
      })
    } else if (mode.value === 'inbox') {
      await readAllNotice()
      inboxList.value.forEach((item) => {
        item.readStatus = 1
      })
    }
    await messageStore.refreshSummary()
  }

  function announceDesc(item: AnnounceMyVO) {
    return summarizeText(item.content)
  }

  function inboxDesc(item: NoticeVO) {
    return inboxContentPreview(item)
  }

  function chatDesc(item: ChatSessionPreview) {
    let text = item.type === 'group' && item.desc.includes(':') ? item.desc : previewMessageText({ content: item.desc })
    if (item.atMe && item.badge > 0) {
      text = `[有人@你] ${text}`
    }
    return text
  }

  watch(
    () => resolveAvailableTabKeys(hasMenuPerm).join(','),
    () => {
      const keys = resolveAvailableTabKeys(hasMenuPerm)
      if (!keys.length) return
      if (keys.includes(mode.value)) return
      const saved = readSavedTabMode()
      if (saved && keys.includes(saved)) {
        mode.value = saved
        return
      }
      mode.value = pickDefaultTab(keys.map((key) => ({ key })))
    },
  )

  watch(mode, (next) => {
    saveTabMode(next)
    if (tabInitialized.value) loadCurrent()
  })

  watch(privateReadTick, () => {
    const userId = lastPrivateReadUserId.value
    if (!userId) return
    const u = chatUsers.value.find((x) => x.id === userId)
    if (u) u.unreadCount = 0
  })

  onMounted(() => {
    offWs = onMessageWebSocket(handleWs)
  })

  onBeforeUnmount(() => {
    offWs?.()
    offWs = null
  })

  onShow(() => {
    const run = () => {
      refresh().catch(() => {})
      silentRefreshChat()
    }
    run()
  })

  return {
    mode,
    loading,
    availableTabs,
    announceList,
    inboxList,
    chatSessions,
    canMarkAllRead,
    listPath,
    refresh,
    markAllRead,
    announceDesc,
    inboxDesc,
    chatDesc,
    formatListTime,
  }
}