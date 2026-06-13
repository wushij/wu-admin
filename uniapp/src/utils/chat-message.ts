import { CHAT_MSG_TYPE, type ChatFilePayload } from '@/constants/chat'
import { fileDisplayUrl } from '@/api/system/file/index'
import type { ChatMessage, GroupMember } from '@/types/message'

/** 群成员展示名 */
export function groupMemberDisplayName(
  m: Pick<GroupMember, 'userNickname' | 'nickname' | 'username'>,
): string {
  return (m.userNickname || m.nickname || m.username || '').trim()
}

export function parseFilePayload(content?: string): ChatFilePayload | null {
  if (!content?.trim()) return null
  try {
    const data = JSON.parse(content) as ChatFilePayload
    if (data?.url && data?.name) return data
  } catch {
    /* plain url fallback for image */
  }
  return null
}

const IMAGE_EXT_RE = /\.(jpe?g|png|gif|webp|bmp|svg)$/i

function isImageContent(content: string): boolean {
  const trimmed = content.trim()
  if (!trimmed || trimmed.startsWith('{')) return false
  return IMAGE_EXT_RE.test(trimmed.split('?')[0])
}

/** 兼容 msgType 缺失时按 content 推断（如历史消息或接口未返回类型） */
export function resolveEffectiveMsgType(msg?: Pick<ChatMessage, 'msgType' | 'content'>): number {
  const type = msg?.msgType ?? CHAT_MSG_TYPE.TEXT
  const content = msg?.content?.trim() || ''
  if (!content) return type

  const file = parseFilePayload(content)
  if (file?.url) {
    if (isImageContent(file.url) || isImageFileMeta(file.name)) return CHAT_MSG_TYPE.IMAGE
    return CHAT_MSG_TYPE.FILE
  }

  if (type !== CHAT_MSG_TYPE.TEXT) return type
  if (isImageContent(content)) return CHAT_MSG_TYPE.IMAGE
  return type
}

export function resolveMediaUrl(msg?: Pick<ChatMessage, 'msgType' | 'content'>): string {
  if (!msg?.content) return ''
  const type = resolveEffectiveMsgType(msg)
  if (type === CHAT_MSG_TYPE.IMAGE) {
    const file = parseFilePayload(msg.content)
    return fileDisplayUrl(file?.url || msg.content)
  }
  if (type === CHAT_MSG_TYPE.FILE) {
    const f = parseFilePayload(msg.content)
    return f ? fileDisplayUrl(f.url) : ''
  }
  return ''
}

export function isImageFileMeta(name?: string, mime?: string): boolean {
  if (mime?.startsWith('image/')) return true
  if (!name?.trim()) return false
  return IMAGE_EXT_RE.test(name.trim().split('?')[0])
}

export function getFileInfo(msg?: Pick<ChatMessage, 'msgType' | 'content'>): ChatFilePayload | null {
  if (!msg || resolveEffectiveMsgType(msg) !== CHAT_MSG_TYPE.FILE) return null
  return parseFilePayload(msg.content)
}

export function formatFilePayload(file: {
  url: string
  originalName?: string
  fileName?: string
  fileSize?: number
  id?: number
}): string {
  return JSON.stringify({
    url: file.url,
    name: file.originalName || file.fileName || '文件',
    size: file.fileSize,
    fileId: file.id,
  } satisfies ChatFilePayload)
}

export function previewMessageText(msg?: Pick<ChatMessage, 'msgType' | 'content'>): string {
  if (!msg) return ''
  const type = resolveEffectiveMsgType(msg)
  if (type === CHAT_MSG_TYPE.IMAGE) return '[图片]'
  if (type === CHAT_MSG_TYPE.FILE) {
    const f = parseFilePayload(msg.content)
    return f ? `[文件] ${f.name}` : '[文件]'
  }
  if (type === CHAT_MSG_TYPE.RECALLED) return '[撤回了一条消息]'
  return msg.content || ''
}

export function isRecalledMessage(msg?: Pick<ChatMessage, 'msgType'>): boolean {
  return msg?.msgType === CHAT_MSG_TYPE.RECALLED
}

export function markMessageRecalled(list: ChatMessage[], messageId: number | string): ChatMessage[] {
  const targetId = Number(messageId)
  if (!targetId) return list
  let changed = false
  const next = list.map((m) => {
    if (Number(m.id) !== targetId) return m
    changed = true
    return { ...m, msgType: CHAT_MSG_TYPE.RECALLED, content: '' }
  })
  return changed ? next : list
}

export function resolveRecallMessageId(data: { messageId?: number | string; id?: number | string }): number {
  return Number(data.messageId ?? data.id ?? 0)
}

/** 撤回提示文案（私聊像微信：对方/你；群聊显示昵称） */
export function recallNoticeText(
  msg: Pick<ChatMessage, 'senderId' | 'senderName'>,
  currentUserId: number | undefined,
  mode: 'private' | 'group',
): string {
  const isSelf = currentUserId != null && msg.senderId === currentUserId
  if (isSelf) return '你撤回了一条消息'
  if (mode === 'private') return '对方撤回了一条消息'
  const name = msg.senderName?.trim() || '成员'
  return `${name}撤回了一条消息`
}

export function canRecallMessage(
  msg: Pick<ChatMessage, 'senderId' | 'sendTime' | 'msgType'>,
  currentUserId?: number,
  windowMinutes = 2,
): boolean {
  if (msg.msgType === CHAT_MSG_TYPE.RECALLED) return false
  if (!currentUserId || msg.senderId !== currentUserId) return false
  if (!msg.sendTime) return false
  const sent = new Date(msg.sendTime).getTime()
  if (Number.isNaN(sent)) return false
  return Date.now() - sent <= windowMinutes * 60 * 1000
}
