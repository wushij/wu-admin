import { CHAT_MSG_TYPE, CHAT_TIME_GAP_MS } from '@/constants/chat'
import {
  previewMessageText, isRecalledMessage, renderMentionHtml,
  recallNoticeText, parseFilePayload,
} from '@/utils/chat-message'
import type { ChatMessage } from '@/types/message'

export type ChatRenderItem =
  | { kind: 'time'; id: string; text: string }
  | { kind: 'system'; id: string; text: string }
  | { kind: 'recall'; id: string; text: string }
  | { kind: 'message'; id: string; message: ChatMessage }

const GROUP_AVATAR_COLORS = ['#576b95', '#10aeff', '#07c160', '#fa9d3b', '#6467f0', '#354b70']

export function groupAvatarStyle(name?: string) {
  const s = name || 'G'
  let hash = 0
  for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash)
  return { background: GROUP_AVATAR_COLORS[Math.abs(hash) % GROUP_AVATAR_COLORS.length] }
}

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
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export function buildChatRenderItems(list: ChatMessage[], currentUserId: number, withSystem = false): ChatRenderItem[] {
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
      items.push({ kind: 'recall', id: `recall-${message.id}`, text: recallNoticeText(message, currentUserId, recallMode) })
      prev = message
      continue
    }
    if (!prev || (withSystem && prev.msgType === CHAT_MSG_TYPE.SYSTEM) || isRecalledMessage(prev) || shouldSplitMessageTime(prev, message)) {
      items.push({ kind: 'time', id: `time-${message.id}`, text: formatTimeDivider(message.sendTime) })
    }
    items.push({ kind: 'message', id: `msg-${message.id}`, message })
    prev = message
  }
  return items
}

export function formatTime(time: string | number | undefined) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export function formatListTime(time: string | number | undefined) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const target = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  if (target.getTime() === today.getTime()) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return `${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getDate().toString().padStart(2, '0')}`
}

export function formatSearchPreview(content: string | undefined, msgType?: number) {
  const text = previewMessageText({ content, msgType })
  return text.length > 40 ? `${text.slice(0, 40)}…` : text
}

export function renderTextContent(text?: string) {
  return renderMentionHtml(text || '')
}

export function fileDisplayName(content?: string) {
  return parseFilePayload(content)?.name || '[文件]'
}

export function openImagePreview(url: string | undefined, setPreviewUrl: (url: string) => void, setPreviewVisible: (v: boolean) => void) {
  setPreviewUrl(url || '')
  setPreviewVisible(true)
}

export async function openChatFile(content?: string) {
  const file = parseFilePayload(content)
  if (!file) return
  if (file.fileId) {
    try {
      const { fetchFileBlob } = await import('@/api/system/file/index')
      const blob = await fetchFileBlob(`/system/file/download/${file.fileId}`)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = file.name || 'download'
      document.body.appendChild(a); a.click(); document.body.removeChild(a)
      URL.revokeObjectURL(url)
      return
    } catch { /* fallback */ }
  }
  let url = file.url || ''
  if (url && !url.startsWith('http') && !url.startsWith('/api')) url = url.startsWith('/') ? `/api${url}` : `/api/${url}`
  if (!url) return
  const params = new URLSearchParams({ disposition: 'attachment' })
  if (file.name) params.set('filename', file.name)
  const sep = url.includes('?') ? '&' : '?'
  window.open(`${url}${sep}${params}`, '_blank', 'noopener')
}

export function scrollToMessage(msgId: number, container: HTMLElement | null) {
  const msgElement = container?.querySelector(`[data-msg-id="${msgId}"]`)
  if (msgElement) {
    msgElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    msgElement.classList.add('message-highlight')
    setTimeout(() => msgElement.classList.remove('message-highlight'), 2000)
  }
}

export function scrollBottom(refEl: { value: HTMLElement | null }) {
  const el = refEl.value
  if (el) el.scrollTop = el.scrollHeight
}

export { previewMessageText, isRecalledMessage, parseFilePayload }
