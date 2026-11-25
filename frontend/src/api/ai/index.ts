/**
 * AI wu助手对话 API
 *
 * 流式对话无法走 axios（不支持增量读取响应体），使用原生 fetch：
 * - 会话凭证：Sa-Token 走 httpOnly Cookie，fetch 显式 credentials: 'include'
 * - 防重放头：复用 crypto/security-config 工具函数手动补齐 X-Timestamp/X-Nonce/X-Client-Id/X-Signature
 */
import service, { get, del, post } from '@/utils/request'
import { generateNonce, getTimestamp, signHmacSm3 } from '@/utils/crypto'
import { getClientId, getSecurityConfig, requestSessionSignKey } from '@/utils/security-config'
import type { AiModelVO } from '@/api/system/ai-model'

/** 对话附件展示元数据（与后端 AiChatAttachmentVO 对齐） */
export interface AiChatAttachment {
  attachmentId?: string
  fileName: string
  fileSize?: number
  fileSizeText?: string
  fileType?: string
  isImage?: boolean
  /** 图片内联地址：本轮上传即时预览使用，历史回放时为空 */
  imageUrl?: string
  /** 持久化访问地址：历史回放与下载使用 */
  fileUrl?: string
  previewText?: string
  parseStatus?: string
  errorMessage?: string
}

export interface AiChatMessage {
  role: 'user' | 'assistant'
  content: string
  /** 本轮附件ID（仅用户消息，服务端据此读取解析正文） */
  attachmentIds?: string[]
  /** 附件元数据（随对话落库，用于历史回放） */
  attachments?: Array<{
    attachmentId?: string
    fileName: string
    fileSize?: number
    fileType?: string
    fileUrl?: string
    isImage?: boolean
  }>
}

export interface WebSearchCitation {
  title: string
  url: string
  snippet?: string
  source?: string
}

export interface AiChatCapabilities {
  webSearchEnabled: boolean
  engineSummary: string
}

export interface AiChatStreamBody {
  conversationId: string
  modelId?: number | null
  source: 'pc' | 'mobile'
  webSearch?: boolean
  messages: AiChatMessage[]
}

export interface AiChatUsage {
  promptTokens: number
  completionTokens: number
  totalTokens: number
  durationMs: number
  /** 本轮对话落库记录ID（用于成对删除同步移除上下文） */
  logId?: number | null
}

export interface AiChatStreamCallbacks {
  /** 增量文本 */
  onDelta: (text: string) => void
  /** 推理模型思维链增量（如 DeepSeek reasoner 的 reasoning_content） */
  onReasoning?: (text: string) => void
  /** 联网检索外链参考资料 */
  onCitations?: (citations: WebSearchCitation[]) => void
  /** 正常结束（usage 可能为 null） */
  onDone: (usage: AiChatUsage | null) => void
  /** 服务端/网络错误 */
  onError: (message: string) => void
  /** 工具调用状态提示（L3 Function Calling，可选） */
  onStatus?: (message: string) => void
}

/** 校验并规范化联网搜索参考外链列表（仅允许 http/https 安全外链） */
export function normalizeWebSearchCitations(raw: unknown): WebSearchCitation[] {
  if (!Array.isArray(raw)) return []
  const result: WebSearchCitation[] = []
  for (const item of raw) {
    if (!item || typeof item !== 'object') continue
    const record = item as Record<string, unknown>
    const title = String(record.title || '').trim()
    const url = String(record.url || '').trim()
    if (!title || (!url.startsWith('https://') && !url.startsWith('http://'))) {
      continue
    }
    result.push({
      title,
      url,
      snippet: record.snippet ? String(record.snippet) : undefined,
      source: record.source ? String(record.source) : '联网检索',
    })
  }
  return result
}

/** 查询当前系统联网能力状态与生效引擎摘要 */
export function getChatCapabilities() {
  return get<AiChatCapabilities>('/ai/chat/capabilities')
}

/** 启用中的模型列表（悬浮窗模型选择器） */
export function listChatModels() {
  return get<AiModelVO[]>('/ai/chat/models')
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
  /** 对话记录ID（按轮删除使用） */
  id?: number
  question: string
  answer: string
  /** 深度思考过程正文 */
  reasoning?: string
  /** 深度思考耗时（毫秒） */
  thinkingDurationMs?: number | null
  /** 工具/联网执行轨迹摘要 */
  toolMessages?: string[]
  /** 本轮用户附件元数据 */
  attachments?: AiChatAttachment[]
  /** 本轮附件正文快照（续聊上下文回放） */
  attachmentContext?: string
  /** 本轮联网检索参考来源 */
  citations?: WebSearchCitation[]
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

/** 删除指定历史会话（仅本人） */
export function deleteConversation(conversationId: string) {
  return del<boolean>(`/ai/chat/conversations/${encodeURIComponent(conversationId)}`)
}

/** 清空我的所有历史会话（仅本人） */
export function clearAllConversations() {
  return del<boolean>('/ai/chat/conversations/clean')
}

/** 上传并解析对话附件（图片/文档） */
export function uploadChatAttachment(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<AiChatAttachment>('/ai/chat/attachment', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}

/** 删除本人的单轮问答记录（同步移除该轮上下文） */
export function deleteChatMessage(logId: number) {
  return del<{ deleted: boolean; deletedIds: number[] }>(`/ai/chat/messages/${logId}`)
}

/** 删除指定会话最后一条记录（中断/失败轮次尚未拿到记录ID时兜底） */
export function deleteLastChatRound(conversationId: string) {
  return del<boolean>(`/ai/chat/conversations/${encodeURIComponent(conversationId)}/last-round`)
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
  let hasReceivedContent = false

  const dispatch = () => {
    if (dataLines.length === 0 && !eventName) return
    const data = dataLines.join('\n')
    if (eventName === 'delta') {
      if (data) {
        hasReceivedContent = true
      }
      callbacks.onDelta(data)
    } else if (eventName === 'reasoning') {
      // 推理模型思维链增量，前端据此渲染分段式深度思考
      if (data) {
        callbacks.onReasoning?.(data)
      }
    } else if (eventName === 'status') {
      // L3 工具调用与联网检索状态提示，解析 message 字段；失败则忽略
      if (callbacks.onStatus) {
        try {
          callbacks.onStatus((JSON.parse(data) as { message?: string }).message || '')
        } catch {
          /* 忽略非法状态数据 */
        }
      }
    } else if (eventName === 'citations') {
      if (callbacks.onCitations) {
        try {
          const list = normalizeWebSearchCitations(JSON.parse(data))
          if (list.length > 0) {
            callbacks.onCitations(list)
          }
        } catch {
          /* 忽略非法引用数据 */
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
          // Spring SseEmitter 的格式为 `data:` + 原始数据（不额外追加分隔空格），
          // 必须完整保留数据自身的前导空格，否则英文思维链的 token 会全部粘连成单词
          dataLines.push(line.slice(5))
        }
      }
    }
    dispatch()
    // 流被服务端直接关闭且未发 done/error 事件时，按完成兜底
    if (!finished) {
      finished = true
      callbacks.onDone(null)
    }
  } catch (err) {
    if ((err as Error)?.name === 'AbortError') return
    // 尝试解析 buffer 里残留的数据
    if (buffer) {
      const remaining = buffer.split('\n')
      for (let raw of remaining) {
        let line = raw.endsWith('\r') ? raw.slice(0, -1) : raw
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          // Spring SseEmitter 的格式为 `data:` + 原始数据（不额外追加分隔空格），
          // 必须完整保留数据自身的前导空格，否则英文思维链的 token 会全部粘连成单词
          dataLines.push(line.slice(5))
        }
      }
      dispatch()
    }
    if (finished) return
    // 核心修复：若已经正常接收到了 AI 的回答内容，连接断开属于流末尾的自然关闭，直接按完成处理，绝不污染正文
    if (hasReceivedContent) {
      finished = true
      callbacks.onDone(null)
      return
    }
    callbacks.onError('连接中断，请稍后再试')
  }
}
