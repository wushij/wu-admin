/** 聊天联系人 */
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

/** 群聊摘要 */
export interface ChatGroup {
  id: number
  name: string
  ownerId?: number
  memberCount?: number
  announcement?: string
  /** 群信息/公告更新时间 */
  updateTime?: string
  /** 当前用户在该群的角色 0成员 1管理员 2群主 */
  myRole?: number
  /** 免打扰：仅 @ 我时提醒 */
  notifyMuted?: boolean
  /** 是否有未读群公告（需置顶展示） */
  announcementUnread?: boolean
  /** 最近更新群公告的操作人 */
  announcementPublisherId?: number
  announcementPublisherName?: string
  announcementPublisherAvatar?: string
  announcementPublishTime?: string
  lastMessage?: string
  lastMessageTime?: string
  unreadCount?: number
}

/** 聊天消息 */
export interface ChatMessage {
  id: number
  senderId?: number
  senderName?: string
  senderAvatar?: string
  receiverId?: number
  groupId?: number
  content?: string
  msgType?: number
  mentionIds?: number[] | string
  sendTime?: string
}

/** 群成员 */
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

/** 系统通知（用户侧列表项，与后端 AnnounceMyVO 对齐） */
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

/** 公告管理列表（与后端 AnnounceDO 展示字段对齐） */
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
  createBy?: number
}
