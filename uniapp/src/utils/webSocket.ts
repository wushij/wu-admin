import { getToken } from '@/utils/auth'

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

function getWsUrl(): string {
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = getToken()

  // #ifdef H5
  if (base.startsWith('/')) {
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${proto}//${window.location.host}${base}/ws/message`
  }
  // #endif

  const url =
    base.replace(/^https?:/, (m) => (m === 'https' ? 'wss' : 'ws')) + '/ws/message'

  // #ifdef MP-WEIXIN
  if (token) return `${url}?Authorization=${encodeURIComponent(token)}`
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
  socketTask.send({ data: JSON.stringify({ type: 'ping' }) })
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
