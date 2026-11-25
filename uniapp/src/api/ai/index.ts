/**
 * AI wu助手对话 API（移动端）
 *
 * 流式对话：H5 使用原生 fetch 增量读取 SSE；微信小程序使用 uni.request 的
 * enableChunked 分块接收；其余平台（App 等）暂不支持流式，给出明确提示。
 * 会话凭证走 Authorization 头（与 luch-request 拦截器一致），并手动补齐防重放签名头。
 */
import http, { get, del } from '@/core/http'
import { getToken } from '@/core/auth'
import { resolveApiBaseUrl } from '@/core/api-base'
import { generateNonce, getTimestamp, signHmacSm3 } from '@/utils/security/crypto'
import { getClientId, getSecurityConfig, requestSessionSignKey } from '@/utils/security/security-config'
import { uploadFile } from '@/utils/upload'

export interface AiChatModelVO {
  id: number
  name: string
  provider: string
  modelName: string
  isDefault?: number
}

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
  /** 附件正文上下文块：本轮上传由服务端返回，后续轮次发送时回放 */
  contextText?: string
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
  /** 是否开启联网实时检索 */
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
  /** 增量正文 */
  onDelta: (text: string) => void
  /** 推理模型思维链增量 */
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

/**
 * 上传并解析对话附件（图片/文档）。
 * 后端对 multipart 请求豁免签名校验，仅需登录令牌。
 */
export function uploadChatAttachment(filePath: string) {
  return uploadFile<AiChatAttachment>({
    url: '/ai/chat/attachment',
    filePath,
    name: 'file',
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

/** 组装防重放请求头（签名串格式与后端 ApiSecurityFilter 严格对齐） */
async function buildStreamHeaders(bodyStr: string): Promise<Record<string, string>> {
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
  const isSignEnabled =
    (secConfig.sm3SignEnabled || secConfig.sm2SignEnabled) && secConfig.sm3SignKey
  if (isSignEnabled) {
    // 签名串格式：METHOD\n路径\ntimestamp\nnonce\nbody
    const signContent = `POST\n/ai/chat/stream\n${timestamp}\n${nonce}\n${bodyStr}`
    const signature = signHmacSm3(signContent, secConfig.sm3SignKey || '')
    if (signature) {
      headers['X-Signature'] = signature
    }
  }
  return headers
}

/**
 * SSE 解析引擎：H5 的 fetch 与小程序的分块接收共用同一套事件解析逻辑。
 * 支持 delta / reasoning / status / citations / done / error 六类事件。
 */
function createSseEngine(callbacks: AiChatStreamCallbacks) {
  let buffer = ''
  let eventName = ''
  let dataLines: string[] = []
  let finished = false
  let hasReceivedContent = false

  const dispatch = () => {
    if (dataLines.length === 0 && !eventName) return
    const data = dataLines.join('\n')
    if (eventName === 'delta') {
      if (data) hasReceivedContent = true
      callbacks.onDelta(data)
    } else if (eventName === 'reasoning') {
      if (data) callbacks.onReasoning?.(data)
    } else if (eventName === 'status') {
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
          if (list.length > 0) callbacks.onCitations(list)
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

  const consumeLine = (raw: string) => {
    let line = raw
    if (line.endsWith('\r')) line = line.slice(0, -1)
    if (line === '') {
      dispatch()
      return
    }
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      // Spring SseEmitter 的格式为 `data:` + 原始数据（不额外追加分隔空格），
      // 必须完整保留数据自身的前导空格，否则英文思维链的 token 会全部粘连成单词
      dataLines.push(line.slice(5))
    }
  }

  return {
    /** 追加一段已解码文本并逐行消费 */
    feed(text: string) {
      if (!text) return
      buffer += text
      let idx: number
      while ((idx = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 1)
        consumeLine(line)
      }
    },
    /** 流结束时冲刷残留数据并兜底完成事件 */
    end() {
      if (buffer) {
        consumeLine(buffer)
        buffer = ''
        dispatch()
      }
      if (!finished) {
        finished = true
        callbacks.onDone(null)
      }
    },
    /** 流异常中断：已收到正文时按正常结束处理，避免污染内容 */
    fail() {
      if (finished) return
      if (hasReceivedContent) {
        finished = true
        callbacks.onDone(null)
        return
      }
      callbacks.onError('连接中断，请稍后再试')
    },
    isFinished: () => finished,
  }
}

/**
 * SSE 流式对话（H5 + 微信小程序）
 * 通过 AbortSignal 支持中途停止；abort 时静默返回，不触发 onError
 */
export async function streamChat(
  body: AiChatStreamBody,
  callbacks: AiChatStreamCallbacks,
  signal?: AbortSignal,
): Promise<void> {
  // #ifdef H5
  await streamChatH5(body, callbacks, signal)
  // #endif

  // #ifdef MP-WEIXIN
  await streamChatMp(body, callbacks, signal)
  // #endif

  // #ifndef H5
  // #ifndef MP-WEIXIN
  void body
  void signal
  callbacks.onError('当前平台暂不支持 AI 流式对话，请使用网页版')
  // #endif
  // #endif
}

/** H5 实现：原生 fetch 增量读取 SSE */
async function streamChatH5(
  body: AiChatStreamBody,
  callbacks: AiChatStreamCallbacks,
  signal?: AbortSignal,
): Promise<void> {
  const bodyStr = JSON.stringify(body)
  const headers = await buildStreamHeaders(bodyStr)

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
  const engine = createSseEngine(callbacks)

  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      engine.feed(decoder.decode(value, { stream: true }))
    }
    engine.feed(decoder.decode())
    engine.end()
  } catch (err) {
    if ((err as Error)?.name === 'AbortError') return
    engine.fail()
  }
}

/** 微信小程序实现：uni.request 分块接收 + 增量 UTF-8 解码 */
function streamChatMp(
  body: AiChatStreamBody,
  callbacks: AiChatStreamCallbacks,
  signal?: AbortSignal,
): Promise<void> {
  return new Promise<void>((resolve) => {
    let settled = false
    const finish = () => {
      if (settled) return
      settled = true
      resolve()
    }

    const decoder = createIncrementalUtf8Decoder()
    const engine = createSseEngine({
      ...callbacks,
      onDone: (usage) => {
        callbacks.onDone(usage)
        finish()
      },
      onError: (message) => {
        callbacks.onError(message)
        finish()
      },
    })

    buildStreamHeaders(JSON.stringify(body)).then((headers) => {
      /** 微信小程序扩展参数：enableChunked 与 onChunkReceived 不在通用类型定义中 */
      type ChunkedRequestTask = {
        abort: () => void
        onChunkReceived?: (callback: (res: { data: ArrayBuffer }) => void) => void
      }
      const requestOptions = {
        url: `${resolveApiBaseUrl()}/ai/chat/stream`,
        method: 'POST',
        header: headers,
        data: JSON.stringify(body),
        enableChunked: true,
        responseType: 'arraybuffer',
        success: (res: { statusCode?: number }) => {
          if (res.statusCode === 401) {
            callbacks.onError('登录已过期，请重新登录后使用 AI wu助手')
            finish()
            return
          }
          if (res.statusCode && res.statusCode >= 400) {
            callbacks.onError(`AI 服务请求失败 (HTTP ${res.statusCode})`)
            finish()
            return
          }
          engine.feed(decoder.flush())
          engine.end()
          finish()
        },
        fail: (err: { errMsg?: string }) => {
          const message = String(err?.errMsg || '')
          if (message.includes('abort')) {
            finish()
            return
          }
          engine.fail()
          finish()
        },
      } as unknown as UniApp.RequestOptions
      const task = uni.request(requestOptions) as unknown as ChunkedRequestTask

      // 分块到达：解码后交给 SSE 引擎
      task.onChunkReceived?.((res) => {
        engine.feed(decoder.decode(res.data))
      })

      if (signal) {
        if (signal.aborted) {
          task.abort()
          finish()
          return
        }
        signal.addEventListener('abort', () => {
          task.abort()
          finish()
        })
      }
    }).catch(() => {
      callbacks.onError('网络异常，请检查网络连接')
      finish()
    })
  })
}

/**
 * 增量 UTF-8 解码器：分块边界可能截断多字节字符，
 * 需要保留不完整尾部字节，等下一块到达后再拼接解码。
 */
function createIncrementalUtf8Decoder() {
  const hasTextDecoder = typeof TextDecoder !== 'undefined'
  const textDecoder = hasTextDecoder ? new TextDecoder('utf-8') : null
  let pending = new Uint8Array(0)

  return {
    decode(chunk: ArrayBuffer | ArrayBufferView | string): string {
      if (typeof chunk === 'string') return chunk
      const bytes =
        chunk instanceof ArrayBuffer
          ? new Uint8Array(chunk)
          : new Uint8Array(chunk.buffer, chunk.byteOffset, chunk.byteLength)

      const merged = new Uint8Array(pending.length + bytes.length)
      merged.set(pending, 0)
      merged.set(bytes, pending.length)

      if (textDecoder) {
        // TextDecoder 的 stream 模式会自动保留不完整字节
        return textDecoder.decode(merged, { stream: true })
      }

      const { completeLength } = findUtf8Boundary(merged)
      pending = merged.slice(completeLength)
      return decodeUtf8Complete(merged.subarray(0, completeLength))
    },
    flush(): string {
      if (textDecoder) {
        const rest = textDecoder.decode()
        pending = new Uint8Array(0)
        return rest
      }
      const rest = decodeUtf8Complete(pending)
      pending = new Uint8Array(0)
      return rest
    },
  }
}

/** 找到最后一个完整 UTF-8 字符的结束位置（用于无 TextDecoder 时的兜底） */
function findUtf8Boundary(bytes: Uint8Array): { completeLength: number } {
  let i = bytes.length - 1
  let continuation = 0
  while (i >= 0 && (bytes[i] & 0xc0) === 0x80) {
    continuation++
    i--
  }
  if (i < 0) return { completeLength: 0 }
  const lead = bytes[i]
  const need = lead >= 0xf0 ? 4 : lead >= 0xe0 ? 3 : lead >= 0xc0 ? 2 : 1
  const available = continuation + 1
  return { completeLength: available >= need ? bytes.length : i }
}

/** 手动 UTF-8 解码（仅在小程序运行时不支持 TextDecoder 时使用） */
function decodeUtf8Complete(bytes: Uint8Array): string {
  let out = ''
  let i = 0
  while (i < bytes.length) {
    const first = bytes[i]
    let codePoint = 0xfffd
    let need = 0
    if (first < 0x80) {
      codePoint = first
      need = 0
    } else if ((first & 0xe0) === 0xc0) {
      codePoint = first & 0x1f
      need = 1
    } else if ((first & 0xf0) === 0xe0) {
      codePoint = first & 0x0f
      need = 2
    } else if ((first & 0xf8) === 0xf0) {
      codePoint = first & 0x07
      need = 3
    }
    let valid = i + need < bytes.length || need === 0
    let offset = 1
    while (offset <= need && valid) {
      const next = bytes[i + offset]
      if ((next & 0xc0) !== 0x80) {
        valid = false
        break
      }
      codePoint = (codePoint << 6) | (next & 0x3f)
      offset++
    }
    if (!valid) {
      out += '\ufffd'
      i += 1
      continue
    }
    out += String.fromCodePoint(codePoint)
    i += offset
  }
  return out
}
