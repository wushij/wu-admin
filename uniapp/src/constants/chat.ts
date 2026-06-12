/** 聊天消息类型 */
export const CHAT_MSG_TYPE = {
  TEXT: 1,
  IMAGE: 2,
  FILE: 3,
  SYSTEM: 4,
  RECALLED: 5,
} as const

export const CHAT_PAGE_SIZE = 50

/** 消息时间分隔阈值（5 分钟） */
export const CHAT_TIME_GAP_MS = 5 * 60 * 1000

export interface ChatFilePayload {
  url: string
  name: string
  size?: number
  fileId?: number
}
