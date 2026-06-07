export type WsMessageType = 'notice' | 'chat' | 'groupChat' | 'groupAnnouncement' | 'typing' | 'presence' | 'ping' | 'pong' | string

export interface WsPushMessage {
  type: WsMessageType
  title?: string
  content?: string
  time?: number | string
  senderId?: number
  senderName?: string
  senderAvatar?: string
  groupId?: number
  groupName?: string
  /** 群公告全文（groupAnnouncement 推送） */
  announcement?: string
  msgType?: number
  fromUserId?: number
  /** typing 事件：false 表示对方已停止输入（如已发送消息） */
  active?: boolean
  userId?: number
  online?: boolean
  atMe?: boolean
  mentionIds?: number[]
  recall?: boolean
  messageId?: number
  id?: number
  /** 系统通知 ID（发布推送时携带，用于已读与详情） */
  announceId?: number
}

type WsHandler = (msg: WsPushMessage) => void

let ws: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let reconnectAttempts = 0
const handlers = new Set<WsHandler>()

function getWsUrl(): string {
  const token = localStorage.getItem('token') || ''
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  return `${proto}://${location.host}/api/ws/message?token=${encodeURIComponent(token)}`
}

export function connectMessageWebSocket(): void {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return
  }
  const token = localStorage.getItem('token')
  if (!token) return

  ws = new WebSocket(getWsUrl())

  ws.onopen = () => {
    reconnectAttempts = 0
    startHeartbeat()
  }

  ws.onmessage = (ev: MessageEvent) => {
    try {
      const data = JSON.parse(ev.data as string) as WsPushMessage
      handlers.forEach((fn) => fn(data))
    } catch {
      /* ignore malformed payload */
    }
  }

  ws.onclose = () => {
    stopHeartbeat()
    scheduleReconnect()
  }

  ws.onerror = () => {
    ws?.close()
  }
}

export function disconnectMessageWebSocket(): void {
  stopHeartbeat()
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  reconnectAttempts = 99
  ws?.close()
  ws = null
}

export function onMessageWebSocket(handler: WsHandler): () => void {
  handlers.add(handler)
  return () => handlers.delete(handler)
}

/** 客户端通过 WS 发送（如正在输入） */
export function sendMessageWebSocket(payload: Record<string, unknown>): void {
  if (ws?.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(payload))
  }
}

function startHeartbeat(): void {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'ping' }))
    }
  }, 25000)
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
