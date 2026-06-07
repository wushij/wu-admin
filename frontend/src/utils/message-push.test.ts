import { describe, expect, it } from 'vitest'
import {
  resolvePushTitle,
  shouldNotifyChat,
  shouldNotifyGroupChat,
  shouldCountGroupUnread,
} from '@/utils/message-push'
import type { WsPushMessage } from '@/utils/messageWebSocket'

describe('resolvePushTitle', () => {
  it('uses notice title when type is notice', () => {
    const msg: WsPushMessage = { type: 'notice', title: '维护通知' }
    expect(resolvePushTitle(msg)).toBe('维护通知')
  })

  it('formats group chat sender name', () => {
    const msg: WsPushMessage = { type: 'groupChat', senderName: '张三' }
    expect(resolvePushTitle(msg)).toBe('张三(群消息)')
  })

  it('falls back for private chat', () => {
    expect(resolvePushTitle({ type: 'chat' })).toBe('新消息')
    expect(resolvePushTitle({ type: 'chat', senderName: '李四' })).toBe('李四')
  })
})

describe('shouldNotifyChat', () => {
  it('always notifies when no active target', () => {
    const msg: WsPushMessage = { type: 'chat', senderId: 1 }
    expect(shouldNotifyChat(null, msg)).toBe(true)
  })

  it('suppresses when viewing the same private chat', () => {
    const msg: WsPushMessage = { type: 'chat', senderId: 9 }
    expect(shouldNotifyChat({ type: 'user', id: 9 }, msg)).toBe(false)
    expect(shouldNotifyChat({ type: 'user', id: 8 }, msg)).toBe(true)
  })

  it('suppresses when viewing the same group chat', () => {
    const msg: WsPushMessage = { type: 'groupChat', groupId: 5 }
    expect(shouldNotifyChat({ type: 'group', id: 5 }, msg)).toBe(false)
    expect(shouldNotifyChat({ type: 'group', id: 6 }, msg)).toBe(true)
  })

  it('suppresses group announcement when viewing the same group', () => {
    const msg: WsPushMessage = { type: 'groupAnnouncement', groupId: 5 }
    expect(shouldNotifyChat({ type: 'group', id: 5 }, msg)).toBe(false)
    expect(shouldNotifyChat({ type: 'group', id: 6 }, msg)).toBe(true)
  })
})

describe('group notify muted', () => {
  const muted = { 5: true }

  it('blocks normal group chat when muted', () => {
    const msg: WsPushMessage = { type: 'groupChat', groupId: 5 }
    expect(shouldNotifyGroupChat(msg, muted)).toBe(false)
    expect(shouldCountGroupUnread(msg, muted)).toBe(false)
  })

  it('allows @ me when muted', () => {
    const msg: WsPushMessage = { type: 'groupChat', groupId: 5, atMe: true }
    expect(shouldNotifyGroupChat(msg, muted)).toBe(true)
    expect(shouldCountGroupUnread(msg, muted)).toBe(true)
  })

  it('always allows group announcement', () => {
    const msg: WsPushMessage = { type: 'groupAnnouncement', groupId: 5 }
    expect(shouldNotifyGroupChat(msg, muted)).toBe(true)
    expect(shouldCountGroupUnread(msg, muted)).toBe(true)
  })
})
