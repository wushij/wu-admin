/**
 * AI wu助手对话 API（移动端）
 *
 * 流式对话在 H5 下使用原生 fetch 增量读取 SSE；
 * 会话凭证走 Authorization 头（与 luch-request 拦截器一致），并手动补齐防重放签名头。
 */
import http, { get } from '@/utils/request'
import { getToken } from '@/utils/auth'
import { generateNonce, getTimestamp, signHmacSm3 } from '@/utils/crypto'
import { getClientId, getSecurityConfig, requestSessionSignKey } from '@/utils/security-config'

export interface AiChatModelVO {
  id: number
  name: string
  provider: string
  modelName: string
  isDefault?: number
}

export interface AiChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AiChatStreamBody {
  conversationId: string
  modelId?: number | null
  source: 'pc' | 'mobile'
  messages: AiChatMessage[]
}

export interface AiChatUsage {
  promptTokens: number
  completionTokens: number
  totalTokens: number
  durationMs: number
}

export interface AiChatStreamCallbacks {
  onDelta: (text: string) => void
  onDone: (usage: AiChatUsage | null) => void
  onError: (message: string) => void
  /** 工具调用状态提示（L3 Function Calling，可选） */
  onStatus?: (message: string) => void
}

/** 启用中的模型列表（对话窗模型选择器） */
export function listChatModels() {
  return get<AiChatModelVO[]>('/ai/chat/models')
}

/** 历史会话摘要 */
export interface AiConversationVO {
  conversationId: string
  title: string
  lastTime?: string
  messageCount?: number
}

/** 历史会话单轮问答 */
export interface AiChatHistoryItemVO {
  question: string
  answer: string
  createTime?: string
}

/** 我的历史会话列表（仅本人，最近优先） */
export function listConversations() {
  return get<AiConversationVO[]>('/ai/chat/conversations')
}

/** 指定会话的问答序列（恢复续聊） */
export function getConversationHistory(conversationId: string) {
  return get<AiChatHistoryItemVO[]>('/ai/chat/history', { conversationId })
}

/**
 * SSE 流式对话（事件: delta/done/error），仅 H5 支持增量渲染
 * 通过 AbortSignal 支持中途停止；abort 时静默返回，不触发 onError
 */
export async function streamChat(
  body: AiChatStreamBody,
  callbacks: AiChatStreamCallbacks,
  signal?: AbortSignal,
): Promise<void> {
  let handled = false
  // #ifdef H5
  handled = true
  await streamChatH5(body, callbacks, signal)
  // #endif
  if (!handled) {
    callbacks.onError('当前平台暂不支持 AI 流式对话，请使用网页版')
  }
}

/** H5 实现：原生 fetch 增量读取 SSE */
async function streamChatH5(
  body: AiChatStreamBody,
  callbacks: AiChatStreamCallbacks,
  signal?: AbortSignal,
): Promise<void> {
  // 与请求拦截器一致：签名密钥未就绪时先初始化会话密钥
  const current = getSecurityConfig()
  if (!current.sm3SignKey && !current.sm4Key) {
    try {
      await requestSessionSignKey(http)
    } catch {
      /* 忽略：密钥服务不可用时按未开启签名处理 */
    }
  }

  const timestamp = getTimestamp()
  const nonce = generateNonce()
  const bodyStr = JSON.stringify(body)
  const headers: Record<string, string> = {
    'Content-Type': 'application/json;charset=UTF-8',
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Client-Id': getClientId(),
  }
  const token = getToken()
  if (token) {
    headers.Authorization = token
  }

  const secConfig = getSecurityConfig()
  const isSignEnabled = (secConfig.sm3SignEnabled || secConfig.sm2SignEnabled) && secConfig.sm3SignKey
  if (isSignEnabled) {
    // 签名串格式与后端 ApiSecurityFilter 严格对齐：METHOD\n路径\ntimestamp\nnonce\nbody
    const signContent = `POST\n/ai/chat/stream\n${timestamp}\n${nonce}\n${bodyStr}`
    const signature = signHmacSm3(signContent, secConfig.sm3SignKey || '')
    if (signature) {
      headers['X-Signature'] = signature
    }
  }

  let response: Response
  try {
    response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      credentials: 'include',
      headers,
      body: bodyStr,
      signal,
    })
  } catch (err) {
    if ((err as Error)?.name === 'AbortError') return
    callbacks.onError('网络异常，请检查网络连接')
    return
  }

  if (response.status === 401) {
    callbacks.onError('登录已过期，请重新登录后使用 AI wu助手')
    return
  }
  const respBody = response.body
  if (!response.ok || !respBody) {
    callbacks.onError(`AI 服务请求失败 (HTTP ${response.status})`)
    return
  }

  const reader = respBody.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let eventName = ''
  let dataLines: string[] = []
  let finished = false

  const dispatch = () => {
    if (dataLines.length === 0 && !eventName) return
    const data = dataLines.join('\n')
    if (eventName === 'delta') {
      callbacks.onDelta(data)
    } else if (eventName === 'status') {
      if (callbacks.onStatus) {
        try {
          callbacks.onStatus((JSON.parse(data) as { message?: string }).message || '')
        } catch {
          /* 忽略非法状态数据 */
        }
      }
    } else if (eventName === 'done') {
      finished = true
      try {
        callbacks.onDone(JSON.parse(data) as AiChatUsage)
      } catch {
        callbacks.onDone(null)
      }
    } else if (eventName === 'error') {
      finished = true
      callbacks.onError(data || 'AI 服务异常，请稍后再试')
    }
    eventName = ''
    dataLines = []
  }

  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let idx: number
      while ((idx = buffer.indexOf('\n')) >= 0) {
        let line = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 1)
        if (line.endsWith('\r')) line = line.slice(0, -1)
        if (line === '') {
          dispatch()
          continue
        }
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).replace(/^ /, ''))
        }
      }
    }
    dispatch()
    // 流被服务端直接关闭且未发 done/error 事件时，按完成兜底
    if (!finished) {
      callbacks.onDone(null)
    }
  } catch (err) {
    if ((err as Error)?.name === 'AbortError') return
    if (!finished) {
      callbacks.onError('连接中断，请稍后再试')
    }
  }
}
