export interface AnnounceVO {
  id: number
  title: string
  content: string
  noticeType?: number
  channels?: string[]
  targetType?: number
  targetIds?: number[]
  status?: number
  createName?: string
  createTime?: string
}

export interface AnnounceSaveDTO {
  id?: number | null
  title: string
  content: string
  noticeType?: number
  channels?: string[]
  targetType?: number
  targetIds?: number[]
  status?: number
}

/** 与 backend AnnounceMyVO 对齐 */
export interface AnnounceMyVO {
  id: number
  title: string
  content: string
  noticeType?: number
  status?: number
  createName?: string
  createTime?: string
  isRead?: number
  readTime?: string
}

/** 与 backend NoticeDO 展示字段对齐 */
export interface NoticeVO {
  id: number
  title: string
  content: string
  bizType?: string
  bizId?: number
  readStatus?: number
  createTime?: string
}

export interface ChatUser {
  id: number
  username?: string
  nickname?: string
  avatar?: string
  online?: boolean
  isBlocked?: boolean
  unreadCount?: number
  lastMessage?: string
  lastMessageTime?: string
}

export interface ChatGroup {
  id: number
  name: string
  ownerId?: number
  memberCount?: number
  announcement?: string
  updateTime?: string
  myRole?: number
  notifyMuted?: boolean
  announcementUnread?: boolean
  announcementPublisherId?: number
  announcementPublisherName?: string
  announcementPublishTime?: string
  lastMessage?: string
  lastMessageTime?: string
  unreadCount?: number
}

export interface GroupMember {
  id?: number
  userId: number
  nickname?: string
  userNickname?: string
  username?: string
  avatar?: string
  role?: number
  muted?: boolean
  notifyMuted?: boolean
}

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

export interface ChatMessage {
  id: number
  senderId?: number
  senderName?: string
  senderAvatar?: string
  receiverId?: number
  groupId?: number
  content?: string
  msgType?: number
  sendTime?: string
}
