import { CHAT_MSG_TYPE, type ChatFilePayload } from '@/constants/chat'
import type { ChatMessage } from '@/types/message'

export function parseFilePayload(content?: string): ChatFilePayload | null {
  if (!content?.trim()) return null
  try {
    const data = JSON.parse(content) as ChatFilePayload
    if (data?.url && data?.name) return data
  } catch {
    /* plain url fallback */
  }
  return null
}

export function formatFilePayload(file: { url: string; originalName?: string; fileName?: string; fileSize?: number; id?: number }): string {
  return JSON.stringify({
    url: file.url,
    name: file.originalName || file.fileName || '文件',
    size: file.fileSize,
    fileId: file.id,
  } satisfies ChatFilePayload)
}

export function previewMessageText(msg?: Pick<ChatMessage, 'msgType' | 'content'>): string {
  if (!msg) return ''
  const type = msg.msgType ?? CHAT_MSG_TYPE.TEXT
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

export function canRecallMessage(msg: Pick<ChatMessage, 'senderId' | 'sendTime'>, currentUserId?: number, windowMinutes = 2): boolean {
  if (!currentUserId || msg.senderId !== currentUserId) return false
  if (!msg.sendTime) return false
  const sent = new Date(msg.sendTime).getTime()
  if (Number.isNaN(sent)) return false
  return Date.now() - sent <= windowMinutes * 60 * 1000
}

export function renderMentionHtml(text: string): string {
  if (!text) return ''
  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return escaped.replace(
    /@([^\s@]+)/g,
    '<span class="msg-mention">@$1</span>',
  )
}

/** 将列表中指定消息标记为已撤回（返回新数组，确保 Vue 响应式更新） */
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
