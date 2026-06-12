import { ref, computed, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
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

export function useMessageTab() {
  const messageStore = useMessageStore()
  const { hasPerm } = usePermission()
  const mode = ref<MessageTabKey>('inbox')
  const tabInitialized = ref(false)
  const loading = ref(false)
  const announceList = ref<AnnounceMyVO[]>([])
  const inboxList = ref<NoticeVO[]>([])
  const chatSessions = ref<ChatSessionPreview[]>([])

  const availableTabs = computed(() =>
    MESSAGE_CHANNELS.filter((ch) => !ch.permission || hasPerm(ch.permission)).map((ch) => {
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

  function buildChatSessions(users: ChatUser[], groups: ChatGroup[]): ChatSessionPreview[] {
    const sessions: ChatSessionPreview[] = [
      ...users.map((u) => ({
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
      ...groups.map((g) => ({
        key: `g-${g.id}`,
        type: 'group' as const,
        id: g.id,
        title: g.name,
        desc: g.lastMessage || `${g.memberCount || 0} 人`,
        time: g.lastMessageTime,
        badge: g.unreadCount || 0,
      })),
    ]
    return sessions
      .sort((a, b) => {
        const ta = a.time ? new Date(a.time).getTime() : 0
        const tb = b.time ? new Date(b.time).getTime() : 0
        return tb - ta
      })
      .slice(0, MESSAGE_PREVIEW_LIMIT)
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
    chatSessions.value = buildChatSessions(userRes.data || [], groupRes.data || [])
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
    await messageStore.refreshSummary()
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
    if (item.type === 'group' && item.desc.includes(':')) return item.desc
    return previewMessageText({ content: item.desc })
  }

  function pickDefaultTab(tabs: { key: string }[]): MessageTabKey {
    const saved = readSavedTabMode()
    if (saved && tabs.some((t) => t.key === saved)) return saved
    const prefer: MessageTabKey[] = ['inbox', 'chat', 'announce']
    const hit = prefer.find((key) => tabs.some((t) => t.key === key))
    return (hit || tabs[0]?.key || 'inbox') as MessageTabKey
  }

  watch(
    availableTabs,
    (tabs) => {
      if (!tabs.length) return
      if (!tabInitialized.value) {
        tabInitialized.value = true
        mode.value = pickDefaultTab(tabs)
        return
      }
      if (!tabs.some((t) => t.key === mode.value)) {
        mode.value = pickDefaultTab(tabs)
        saveTabMode(mode.value)
      }
    },
    { immediate: true },
  )

  watch(mode, (next) => {
    saveTabMode(next)
    if (tabInitialized.value) loadCurrent()
  })

  onShow(() => {
    refresh()
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
