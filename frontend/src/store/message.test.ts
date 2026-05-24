import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useMessageStore } from '@/store/message'
import { getMessageSummary } from '@/api/message'
import { getMyNoticeList } from '@/api/system/notice'

vi.mock('@/api/message', () => ({
  getMessageSummary: vi.fn(),
}))

vi.mock('@/api/system/notice', () => ({
  getMyNoticeList: vi.fn(),
}))

vi.mock('@/utils/messageWebSocket', () => ({
  connectMessageWebSocket: vi.fn(),
  disconnectMessageWebSocket: vi.fn(),
  onMessageWebSocket: vi.fn(() => vi.fn()),
}))

describe('useMessageStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  it('aggregates group unread counts', () => {
    const store = useMessageStore()
    store.incrementGroupUnread(1)
    store.incrementGroupUnread(1)
    store.incrementGroupUnread(2)
    expect(store.getGroupUnread(1)).toBe(2)
    expect(store.groupChatUnread).toBe(3)
    store.clearGroupUnread(1)
    expect(store.getGroupUnread(1)).toBe(0)
    expect(store.groupChatUnread).toBe(1)
  })

  it('refreshSummary updates counters from API', async () => {
    vi.mocked(getMessageSummary).mockResolvedValue({
      code: 200,
      data: { inboxCount: 2, announceCount: 3, chatCount: 4 },
    })
    const store = useMessageStore()
    await store.refreshSummary()
    expect(store.inboxCount).toBe(2)
    expect(store.announceCount).toBe(3)
    expect(store.privateChatCount).toBe(4)
    expect(store.totalUnread).toBe(9)
  })

  it('loadInbox fills notice list', async () => {
    vi.mocked(getMyNoticeList).mockResolvedValue({
      code: 200,
      data: [{ id: 1, title: 't', content: 'c' }],
    })
    const store = useMessageStore()
    await store.loadInbox()
    expect(store.inboxList).toHaveLength(1)
  })

  it('handleWsMessage increments group unread when not in active group', () => {
    const store = useMessageStore()
    store.setActiveChatTarget({ type: 'user', id: 99 })
    store.handleWsMessage({ type: 'groupChat', groupId: 7, content: 'hi' })
    expect(store.getGroupUnread(7)).toBe(1)
    expect(store.showNotification).toBe(true)
  })

  it('handleWsMessage skips popup for active group chat', () => {
    const store = useMessageStore()
    store.setActiveChatTarget({ type: 'group', id: 7 })
    store.handleWsMessage({ type: 'groupChat', groupId: 7, content: 'hi' })
    expect(store.getGroupUnread(7)).toBe(0)
    expect(store.showNotification).toBe(false)
  })

  it('auto closes notification after timeout', () => {
    const store = useMessageStore()
    store.showPushNotification({ type: 'notice', title: '公告', content: '内容' })
    expect(store.showNotification).toBe(true)
    vi.advanceTimersByTime(5000)
    expect(store.showNotification).toBe(false)
  })
})
