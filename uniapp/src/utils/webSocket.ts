import { getToken } from '@/utils/auth'
import { resolveApiBaseUrl } from '@/utils/api-base'

export type WsMessageType =
  | 'notice'
  | 'chat'
  | 'groupChat'
  | 'groupAnnouncement'
  | 'typing'
  | 'presence'
  | 'ping'
  | 'pong'
  | string

export interface WsPushMessage {
  type: WsMessageType
  title?: string
  content?: string
  time?: number | string
  senderId?: number
  senderName?: string
  senderAvatar?: string
  receiverId?: number
  groupId?: number
  groupName?: string
  announcement?: string
  msgType?: number
  fromUserId?: number
  active?: boolean
  userId?: number
  online?: boolean
  atMe?: boolean
  recall?: boolean
  messageId?: number
  id?: number
  announceId?: number
}

type WsHandler = (msg: WsPushMessage) => void

let socketTask: UniApp.SocketTask | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let reconnectAttempts = 0
const handlers = new Set<WsHandler>()

function appendToken(url: string, token: string): string {
  if (!token) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}Authorization=${encodeURIComponent(token)}`
}

function getWsUrl(): string {
  const base = resolveApiBaseUrl()
  const token = getToken()
  let url: string

  // #ifdef H5
  if (base.startsWith('/')) {
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    url = `${proto}//${window.location.host}${base}/ws/message`
  } else {
    url = base.replace(/^https?:/, (m) => (m === 'https' ? 'wss' : 'ws')) + '/ws/message'
  }
  // H5 浏览器 WebSocket 无法自定义 Header，须通过 URL 传 Token
  return appendToken(url, token)
  // #endif

  url = base.replace(/^https?:/, (m) => (m === 'https' ? 'wss' : 'ws')) + '/ws/message'
  // #ifdef MP-WEIXIN
  return appendToken(url, token)
  // #endif
  return url
}

export function connectMessageWebSocket(): void {
  if (socketTask) return
  const token = getToken()
  if (!token) return

  socketTask = uni.connectSocket({
    url: getWsUrl(),
    header: { Authorization: token },
    complete: () => {},
  })

  socketTask.onOpen(() => {
    reconnectAttempts = 0
    startHeartbeat()
  })

  socketTask.onMessage((res) => {
    try {
      const data = JSON.parse(res.data as string) as WsPushMessage
      if (data.type === 'pong') return
      handlers.forEach((fn) => fn(data))
    } catch {
      /* ignore malformed payload */
    }
  })

  socketTask.onClose(() => {
    stopHeartbeat()
    socketTask = null
    scheduleReconnect()
  })

  socketTask.onError(() => {
    socketTask?.close({})
    socketTask = null
  })
}

export function disconnectMessageWebSocket(): void {
  stopHeartbeat()
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  reconnectAttempts = 99
  socketTask?.close({})
  socketTask = null
}

export function onMessageWebSocket(handler: WsHandler): () => void {
  handlers.add(handler)
  return () => handlers.delete(handler)
}

export function sendMessageWebSocket(payload: Record<string, unknown>): void {
  socketTask?.send({ data: JSON.stringify(payload) })
}

function sendPing(): void {
  if (!socketTask) return
  try {
    socketTask.send({ data: JSON.stringify({ type: 'ping' }) })
  } catch {
    // 连接已死但 onClose/onError 未被 OS 触发，强制清理并重建
    forceReconnect()
  }
}

function startHeartbeat(): void {
  stopHeartbeat()
  heartbeatTimer = setInterval(sendPing, 25000)
}

function stopHeartbeat(): void {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function scheduleReconnect(): void {
  if (reconnectAttempts >= 5) return
  reconnectAttempts += 1
  reconnectTimer = setTimeout(() => connectMessageWebSocket(), 3000)
}

/** 强制关闭当前连接（可能已死）并立即重建，跳过退避限制 */
function forceReconnect(): void {
  stopHeartbeat()
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  const dead = socketTask
  socketTask = null
  if (dead) {
    try { dead.close({}) } catch { /* already dead */ }
  }
  reconnectAttempts = 0
  connectMessageWebSocket()
}

/** 前台恢复或网络恢复时主动重连（重置退避计数，并探测连接是否真正存活） */
export function ensureMessageWebSocketConnected(): void {
  reconnectAttempts = 0
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (socketTask) {
    // socketTask 存在不代表连接仍存活（移动端 OS 可能在后台静默杀掉 TCP），
    // 尝试发送探测帧；若失败 forceReconnect 会清理并重建
    try {
      socketTask.send({ data: JSON.stringify({ type: 'ping' }) })
    } catch {
      forceReconnect()
    }
    return
  }
  connectMessageWebSocket()
}
