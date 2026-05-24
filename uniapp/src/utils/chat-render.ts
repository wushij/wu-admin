import { CHAT_MSG_TYPE, CHAT_TIME_GAP_MS } from '@/constants/chat'
import { isRecalledMessage, recallNoticeText } from '@/utils/chat-message'
import type { ChatMessage } from '@/types/message'

export type ChatRenderItem =
  | { kind: 'time'; id: string; text: string }
  | { kind: 'system'; id: string; text: string }
  | { kind: 'recall'; id: string; text: string }
  | { kind: 'message'; id: string; message: ChatMessage }

function parseMsgTimestamp(time: string | number | undefined) {
  if (time == null || time === '') return 0
  const ts = new Date(time).getTime()
  return Number.isNaN(ts) ? 0 : ts
}

function shouldSplitMessageTime(prev: ChatMessage, curr: ChatMessage) {
  const ta = parseMsgTimestamp(prev.sendTime)
  const tb = parseMsgTimestamp(curr.sendTime)
  if (!ta || !tb) return true
  if (new Date(ta).toDateString() !== new Date(tb).toDateString()) return true
  return tb - ta > CHAT_TIME_GAP_MS
}

function formatTimeDivider(time: string | number | undefined) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const target = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  const hm = d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (target.getTime() === today.getTime()) return hm
  if (target.getTime() === yesterday.getTime()) return `昨天 ${hm}`
  if (d.getFullYear() === now.getFullYear()) return `${d.getMonth() + 1}月${d.getDate()}日 ${hm}`
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function buildChatRenderItems(
  list: ChatMessage[],
  currentUserId: number,
  withSystem = false,
): ChatRenderItem[] {
  const recallMode: 'private' | 'group' = withSystem ? 'group' : 'private'
  const items: ChatRenderItem[] = []
  let prev: ChatMessage | undefined
  for (const message of list) {
    if (withSystem && message.msgType === CHAT_MSG_TYPE.SYSTEM) {
      items.push({ kind: 'system', id: `sys-${message.id}`, text: message.content || '' })
      prev = message
      continue
    }
    if (isRecalledMessage(message)) {
      if (!prev || shouldSplitMessageTime(prev, message)) {
        items.push({ kind: 'time', id: `time-${message.id}`, text: formatTimeDivider(message.sendTime) })
      }
      items.push({
        kind: 'recall',
        id: `recall-${message.id}`,
        text: recallNoticeText(message, currentUserId, recallMode),
      })
      prev = message
      continue
    }
    if (
      !prev ||
      (withSystem && prev.msgType === CHAT_MSG_TYPE.SYSTEM) ||
      isRecalledMessage(prev) ||
      shouldSplitMessageTime(prev, message)
    ) {
      items.push({ kind: 'time', id: `time-${message.id}`, text: formatTimeDivider(message.sendTime) })
    }
    items.push({ kind: 'message', id: `msg-${message.id}`, message })
    prev = message
  }
  return items
}
