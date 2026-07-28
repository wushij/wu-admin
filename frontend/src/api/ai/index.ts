/**
 * AI wu助手对话 API
 *
 * 流式对话无法走 axios（不支持增量读取响应体），使用原生 fetch：
 * - 会话凭证：Sa-Token 走 httpOnly Cookie，fetch 显式 credentials: 'include'
 * - 防重放头：复用 crypto/security-config 工具函数手动补齐 X-Timestamp/X-Nonce/X-Client-Id/X-Signature
 */
import service, { get } from '@/utils/request'
import { generateNonce, getTimestamp, signHmacSm3 } from '@/utils/crypto'
import { getClientId, getSecurityConfig, requestSessionSignKey } from '@/utils/security-config'
import type { AiModelVO } from '@/api/system/ai-model'

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
  /** 增量文本 */
  onDelta: (text: string) => void
  /** 正常结束（usage 可能为 null） */
  onDone: (usage: AiChatUsage | null) => void
  /** 服务端/网络错误 */
  onError: (message: string) => void
}

/** 启用中的模型列表（悬浮窗模型选择器） */
export function listChatModels() {
  return get<AiModelVO[]>('/ai/chat/models')
}

/**
 * SSE 流式对话（事件: delta/done/error）
 * 通过 AbortSignal 支持中途停止；abort 时静默返回，不触发 onError
 */
export async function streamChat(
  body: AiChatStreamBody,
  callbacks: AiChatStreamCallbacks,
  signal?: AbortSignal,
): Promise<void> {
  // 与 axios 拦截器一致：签名密钥未就绪时先初始化会话密钥
  const current = getSecurityConfig()
  if (!current.sm3SignKey && !current.sm4Key) {
    try {
      await requestSessionSignKey(service)
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
  if (!response.ok || !response.body) {
    callbacks.onError(`AI 服务请求失败 (HTTP ${response.status})`)
    return
  }

  const reader = response.body.getReader()
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
