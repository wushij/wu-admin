import type { WsPushMessage } from '@/utils/messageWebSocket'

export interface ActiveChatTarget {
  type: 'user' | 'group'
  id: number
}

export function resolvePushTitle(msg: WsPushMessage): string {
  if (msg.type === 'notice') return msg.title || '系统通知'
  if (msg.type === 'groupAnnouncement') {
    return msg.title || `${msg.groupName || '群聊'} 发布了新公告`
  }
  if (msg.type === 'groupChat') {
    if (msg.atMe) return '[有人@你] ' + (msg.senderName ? `${msg.senderName}(群消息)` : '群消息')
    return msg.senderName ? `${msg.senderName}(群消息)` : '群消息'
  }
  return msg.senderName || '新消息'
}

function sameGroupId(a: number | undefined, b: number | undefined) {
  if (a == null || b == null) return false
  return Number(a) === Number(b)
}

export function isGroupNotifyMuted(
  groupId: number | undefined,
  groupNotifyMutedById: Record<number, boolean>,
) {
  if (groupId == null) return false
  return !!groupNotifyMutedById[groupId] || !!groupNotifyMutedById[Number(groupId)]
}

/** 是否 @ 到当前用户 */
export function isAtMeMessage(msg: WsPushMessage) {
  return !!msg.atMe
}

/** 当前正在查看的会话与推送目标一致时不弹窗 */
export function shouldNotifyChat(active: ActiveChatTarget | null, msg: WsPushMessage): boolean {
  if (!active) return true
  if (
    (msg.type === 'groupChat' || msg.type === 'groupAnnouncement')
    && active.type === 'group'
    && sameGroupId(active.id, msg.groupId)
  ) {
    return false
  }
  if (msg.type === 'chat' && active.type === 'user' && active.id === msg.senderId) {
    return false
  }
  return true
}

/** 群聊免打扰：普通消息不提醒；@ 我、群公告始终提醒 */
export function shouldNotifyGroupChat(
  msg: WsPushMessage,
  groupNotifyMutedById: Record<number, boolean>,
): boolean {
  if (msg.type === 'groupAnnouncement') return true
  if (msg.type !== 'groupChat' || msg.groupId == null) return true
  if (!isGroupNotifyMuted(msg.groupId, groupNotifyMutedById)) return true
  return isAtMeMessage(msg)
}

/** 免打扰群：仅 @ 我、群公告计入未读角标 */
export function shouldCountGroupUnread(
  msg: WsPushMessage,
  groupNotifyMutedById: Record<number, boolean>,
): boolean {
  if (msg.type === 'groupAnnouncement') return true
  if (msg.type !== 'groupChat' || msg.groupId == null) return true
  if (!isGroupNotifyMuted(msg.groupId, groupNotifyMutedById)) return true
  return isAtMeMessage(msg)
}
