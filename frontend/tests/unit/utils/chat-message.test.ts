import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CHAT_MSG_TYPE } from '@/constants/chat'
import {
  canRecallMessage,
  formatFilePayload,
  groupMemberDisplayName,
  markMessageRecalled,
  parseFilePayload,
  previewMessageText,
  recallNoticeText,
  renderMentionHtml,
  resolveRecallMessageId,
} from '@/utils/chat-message'

describe('groupMemberDisplayName', () => {
  it('prefers user nickname, then group nickname, then username', () => {
    expect(groupMemberDisplayName({ userNickname: '张三', nickname: '群昵称', username: 'zhang' })).toBe('张三')
    expect(groupMemberDisplayName({ nickname: '群昵称', username: 'zhang' })).toBe('群昵称')
    expect(groupMemberDisplayName({ username: 'zhang' })).toBe('zhang')
  })
})

describe('parseFilePayload / formatFilePayload', () => {
  it('parses valid JSON file payload', () => {
    const json = JSON.stringify({ url: '/f/1', name: 'doc.pdf', size: 1024 })
    expect(parseFilePayload(json)).toEqual({ url: '/f/1', name: 'doc.pdf', size: 1024 })
  })

  it('returns null for invalid content', () => {
    expect(parseFilePayload('')).toBeNull()
    expect(parseFilePayload('not-json')).toBeNull()
    expect(parseFilePayload(JSON.stringify({ url: '/f/1' }))).toBeNull()
  })

  it('formats file payload with fallback name', () => {
    const raw = formatFilePayload({ url: '/f/2', fileName: 'a.txt', fileSize: 10, id: 5 })
    expect(JSON.parse(raw)).toEqual({ url: '/f/2', name: 'a.txt', size: 10, fileId: 5 })
  })
})

describe('previewMessageText', () => {
  it('renders type-specific preview labels', () => {
    expect(previewMessageText({ msgType: CHAT_MSG_TYPE.IMAGE, content: 'x' })).toBe('[图片]')
    expect(previewMessageText({
      msgType: CHAT_MSG_TYPE.FILE,
      content: JSON.stringify({ url: '/f/1', name: 'readme.md' }),
    })).toBe('[文件] readme.md')
    expect(previewMessageText({ msgType: CHAT_MSG_TYPE.RECALLED, content: '' })).toBe('[撤回了一条消息]')
    expect(previewMessageText({ msgType: CHAT_MSG_TYPE.TEXT, content: 'hello' })).toBe('hello')
  })
})

describe('canRecallMessage', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-08T12:00:00'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('allows sender within recall window', () => {
    expect(canRecallMessage(
      { senderId: 1, sendTime: '2026-06-08T11:59:00' },
      1,
      2,
    )).toBe(true)
  })

  it('rejects non-sender or expired message', () => {
    expect(canRecallMessage({ senderId: 1, sendTime: '2026-06-08T11:57:00' }, 1, 2)).toBe(false)
    expect(canRecallMessage({ senderId: 1, sendTime: '2026-06-08T11:59:00' }, 2, 2)).toBe(false)
  })
})

describe('markMessageRecalled', () => {
  it('returns new array with recalled message', () => {
    const list = [
      { id: 1, msgType: CHAT_MSG_TYPE.TEXT, content: 'a', senderId: 1 },
      { id: 2, msgType: CHAT_MSG_TYPE.TEXT, content: 'b', senderId: 2 },
    ]
    const next = markMessageRecalled(list, 2)
    expect(next).not.toBe(list)
    expect(next[1].msgType).toBe(CHAT_MSG_TYPE.RECALLED)
    expect(next[1].content).toBe('')
  })

  it('returns same array when id not found', () => {
    const list = [{ id: 1, msgType: CHAT_MSG_TYPE.TEXT, content: 'a', senderId: 1 }]
    expect(markMessageRecalled(list, 99)).toBe(list)
  })
})

describe('resolveRecallMessageId', () => {
  it('prefers messageId over id', () => {
    expect(resolveRecallMessageId({ messageId: 10, id: 20 })).toBe(10)
    expect(resolveRecallMessageId({ id: 20 })).toBe(20)
    expect(resolveRecallMessageId({})).toBe(0)
  })
})

describe('recallNoticeText', () => {
  it('formats private and group recall notices', () => {
    expect(recallNoticeText({ senderId: 1, senderName: 'Alice' }, 1, 'private')).toBe('你撤回了一条消息')
    expect(recallNoticeText({ senderId: 2, senderName: 'Alice' }, 1, 'private')).toBe('对方撤回了一条消息')
    expect(recallNoticeText({ senderId: 2, senderName: 'Alice' }, 1, 'group')).toBe('Alice撤回了一条消息')
  })
})

describe('renderMentionHtml', () => {
  it('escapes html and wraps mentions', () => {
    expect(renderMentionHtml('hi @bob')).toBe('hi <span class="msg-mention">@bob</span>')
    expect(renderMentionHtml('<script>@x</script>')).toContain('&lt;script&gt;')
  })
})
