/** 聊天联系人 */
export interface ChatUser {
  id: number
  username?: string
  nickname?: string
  online?: boolean
  isBlocked?: boolean
  unreadCount?: number
  lastMessage?: string
  lastMessageTime?: string
}

/** 群聊摘要 */
export interface ChatGroup {
  id: number
  name: string
  ownerId?: number
  memberCount?: number
  announcement?: string
  lastMessage?: string
  lastMessageTime?: string
  unreadCount?: number
}

/** 聊天消息 */
export interface ChatMessage {
  id: number
  senderId?: number
  senderName?: string
  receiverId?: number
  groupId?: number
  content?: string
  msgType?: number
  sendTime?: string
}

/** 群成员 */
export interface GroupMember {
  id?: number
  userId: number
  nickname?: string
  userNickname?: string
  username?: string
  role?: number
  muted?: boolean
}

/** 群聊操作日志 */
export interface ChatGroupLogItem {
  id: number
  actionType?: string
  content?: string
  operatorId?: number
  operatorName?: string
  targetUserId?: number
  targetUserName?: string
  detail?: string
  createTime?: string
}

/** 系统通知（用户侧列表项） */
export interface AnnounceItem {
  id: number
  title: string
  content: string
  isRead?: number
  createTime?: string
}
