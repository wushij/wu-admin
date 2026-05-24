import type { WsPushMessage } from '@/utils/messageWebSocket'

export interface ActiveChatTarget {
  type: 'user' | 'group'
  id: number
}

export function resolvePushTitle(msg: WsPushMessage): string {
  if (msg.type === 'notice') return msg.title || '系统通知'
  if (msg.type === 'groupChat') return msg.senderName ? `${msg.senderName}(群消息)` : '群消息'
  return msg.senderName || '新消息'
}

/** 当前正在查看的会话与推送目标一致时不弹窗 */
export function shouldNotifyChat(active: ActiveChatTarget | null, msg: WsPushMessage): boolean {
  if (!active) return true
  if (msg.type === 'groupChat' && active.type === 'group' && active.id === msg.groupId) {
    return false
  }
  if (msg.type === 'chat' && active.type === 'user' && active.id === msg.senderId) {
    return false
  }
  return true
}
