import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  listChatModels,
  streamChat,
  getConversationHistory,
  getChatCapabilities,
  deleteChatMessage,
  deleteLastChatRound,
  normalizeWebSearchCitations,
  type AiChatAttachment,
  type AiChatMessage,
  type AiChatModelVO,
  type WebSearchCitation,
} from '@/api/ai'

export interface AiToolTrace {
  id: string
  message: string
}

/** 对话附件（含前端回放用上下文块） */
export interface AiWuAttachment extends AiChatAttachment {
  /** 附件正文上下文块：本轮上传由服务端返回，后续轮次发送时回放 */
  contextText?: string
}

export interface AiWuMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  /** assistant 消息流式输出中 */
  streaming?: boolean
  /** 出错的 assistant 消息 */
  error?: boolean
  time: number
  /** 推理模型思维链原文 */
  reasoning?: string
  /** 思维链是否仍在生成 */
  reasoningStreaming?: boolean
  /** 本轮思考开始时间（发送即计时） */
  thinkingStartedAt?: number
  /** 本轮思考结束时间 */
  thinkingFinishedAt?: number
  /** L3 工具调用轨迹 */
  tools?: AiToolTrace[]
  /** 联网搜索参考来源外链 */
  citations?: WebSearchCitation[]
  /** 本轮用户附件（仅用户消息） */
  attachments?: AiWuAttachment[]
  /** 本轮附件正文快照（参与后续轮次上下文） */
  attachmentContext?: string
  /** 已落库轮次ID：用户提问与对应回答共享，用于成对删除 */
  roundLogId?: number
}

/** 发送给后端的上下文条数上限（与后端 MAX_CONTEXT_MESSAGES 对齐） */
const MAX_CONTEXT = 20
/** 历史轮次附件正文回放上限：最多 3 轮、每轮 6000 字，控制上下文体积 */
const MAX_REPLAY_ATTACHMENT_ROUNDS = 3
const MAX_REPLAY_ATTACHMENT_CHARS = 6000
const WEB_SEARCH_PREF_KEY = 'wu_ai_web_search'

let msgSeq = 0
let toolSeq = 0

function genConversationId(): string {
  return `mobile-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

/**
 * AI wu助手全局状态（移动端）：对话抽屉开关、模型列表、消息流与流式发送
 */
export const useAiWuStore = defineStore('aiWu', () => {
  const panelVisible = ref(false)
  const messages = ref<AiWuMessage[]>([])
  const conversationId = ref(genConversationId())
  const models = ref<AiChatModelVO[]>([])
  const selectedModelId = ref<number | null>(null)
  const streaming = ref(false)
  const modelsLoaded = ref(false)
  /** L3 工具调用状态提示（空串表示无） */
  const statusHint = ref('')
  /** 系统后台联网搜索是否可用 */
  const webSearchAvailable = ref(false)
  /** 当前启用的联网搜索引擎摘要 */
  const webSearchEngineSummary = ref('')
  /** 用户当前是否开启联网搜索 */
  const webSearchEnabled = ref(uni.getStorageSync(WEB_SEARCH_PREF_KEY) === '1')
  let abortController: AbortController | null = null

  const currentModel = computed(
    () => models.value.find((m) => m.id === selectedModelId.value) || null,
  )

  async function loadCapabilities() {
    try {
      const res = await getChatCapabilities()
      webSearchAvailable.value = Boolean(res.data?.webSearchEnabled)
      webSearchEngineSummary.value = res.data?.engineSummary || ''
      if (!webSearchAvailable.value) {
        webSearchEnabled.value = false
      }
    } catch {
      webSearchAvailable.value = false
    }
  }

  function toggleWebSearch() {
    if (!webSearchAvailable.value) {
      uni.showToast({
        title: '管理员尚未在「系统配置 → 第三方配置」开启联网搜索引擎',
        icon: 'none',
      })
      return
    }
    webSearchEnabled.value = !webSearchEnabled.value
    uni.setStorageSync(WEB_SEARCH_PREF_KEY, webSearchEnabled.value ? '1' : '0')
  }

  async function loadModels() {
    try {
      const res = await listChatModels()
      models.value = res.data || []
      modelsLoaded.value = true
      // 始终优先选用后台设定的默认模型 isDefault === 1，若无则取首个
      const def = models.value.find((m) => m.isDefault === 1) || models.value[0]
      selectedModelId.value = def ? def.id : null
    } catch {
      modelsLoaded.value = true
    }
  }

  function openPanel() {
    panelVisible.value = true
    loadModels()
    loadCapabilities()
  }

  function closePanel() {
    panelVisible.value = false
  }

  /** 悬浮球点击：开 ↔ 关 */
  function togglePanel() {
    if (panelVisible.value) {
      closePanel()
    } else {
      openPanel()
    }
  }

  /**
   * 组装发送给后端的上下文（截取最近 N 条，不含流式中/出错消息）。
   * 历史轮次的附件正文通过 attachmentContext 回放；最新一轮仅传附件ID，由服务端注入正文与图片。
   */
  function buildContext(): AiChatMessage[] {
    const source = messages.value
      .filter(
        (m) => !m.streaming && !m.error && (Boolean(m.content) || Boolean(m.attachments?.length)),
      )
      .slice(-MAX_CONTEXT)

    let lastUserIndex = -1
    for (let i = source.length - 1; i >= 0; i--) {
      if (source[i].role === 'user') {
        lastUserIndex = i
        break
      }
    }

    // 仅回放最近 3 轮历史附件正文，避免多轮附件把上下文撑满
    const replayIndexes = new Set<number>()
    for (let i = source.length - 1; i >= 0; i--) {
      if (i === lastUserIndex || replayIndexes.size >= MAX_REPLAY_ATTACHMENT_ROUNDS) {
        continue
      }
      const message = source[i]
      if (message.role === 'user' && message.attachmentContext) {
        replayIndexes.add(i)
      }
    }

    return source.map((message, index) => {
      let content = message.content
      if (message.role === 'user' && replayIndexes.has(index) && message.attachmentContext) {
        const context = message.attachmentContext.slice(0, MAX_REPLAY_ATTACHMENT_CHARS)
        content = content ? `${content}\n\n${context}` : context
      }
      const item: AiChatMessage = { role: message.role, content }
      if (message.role === 'user' && index === lastUserIndex && message.attachments?.length) {
        const ids = message.attachments
          .map((attachment) => attachment.attachmentId)
          .filter((id): id is string => Boolean(id))
        if (ids.length > 0) {
          item.attachmentIds = ids
        }
        item.attachments = message.attachments.map((attachment) => ({
          attachmentId: attachment.attachmentId,
          fileName: attachment.fileName,
          fileSize: attachment.fileSize,
          fileType: attachment.fileType,
          fileUrl: attachment.fileUrl,
          isImage: attachment.isImage,
        }))
      }
      return item
    })
  }

  async function send(text: string, attachments: AiWuAttachment[] = []) {
    const content = text.trim()
    const readyAttachments = attachments.filter((attachment) => Boolean(attachment.attachmentId))
    if ((!content && readyAttachments.length === 0) || streaming.value) return
    await runStream(content, true, readyAttachments)
  }

  /**
   * 内部流式执行：
   * - pushUser 为 false 时用于「重新生成」避免重复插入用户消息
   * - contextOverride 用于重新生成时沿用原轮附件上下文
   */
  async function runStream(
    content: string,
    pushUser = true,
    attachments: AiWuAttachment[] = [],
    contextOverride = '',
  ) {
    let userMsg: AiWuMessage | undefined
    if (pushUser) {
      const attachmentContext =
        contextOverride ||
        attachments
          .map((attachment) => attachment.contextText)
          .filter((text): text is string => Boolean(text))
          .join('\n\n')
      userMsg = {
        id: ++msgSeq,
        role: 'user',
        content,
        attachments: attachments.length > 0 ? attachments : undefined,
        attachmentContext: attachmentContext || undefined,
        time: Date.now(),
      }
      messages.value.push(userMsg)
    }
    const startedAt = Date.now()
    messages.value.push({
      id: ++msgSeq,
      role: 'assistant',
      content: '',
      streaming: true,
      reasoning: '',
      reasoningStreaming: false,
      thinkingStartedAt: startedAt,
      time: startedAt,
      tools: [],
    })
    // 必须取数组中的响应式代理再增量写入，直接改 push 前的原始对象 Vue 无法感知（流式不刷新）
    const assistantMsg = messages.value[messages.value.length - 1]
    streaming.value = true
    abortController = typeof AbortController !== 'undefined' ? new AbortController() : null

    const finish = () => {
      assistantMsg.streaming = false
      assistantMsg.reasoningStreaming = false
      if (!assistantMsg.thinkingFinishedAt) {
        assistantMsg.thinkingFinishedAt = Date.now()
      }
      streaming.value = false
      statusHint.value = ''
      abortController = null
    }

    await streamChat(
      {
        conversationId: conversationId.value,
        modelId: selectedModelId.value,
        source: 'mobile',
        webSearch: webSearchAvailable.value && webSearchEnabled.value,
        messages: buildContext(),
      },
      {
        onDelta: (delta) => {
          statusHint.value = ''
          if (delta && !assistantMsg.content) {
            // 首个正文分片到达即标志思考阶段结束，立即锁定思考用时并收束思考态
            assistantMsg.reasoningStreaming = false
            if (!assistantMsg.thinkingFinishedAt) {
              assistantMsg.thinkingFinishedAt = Date.now()
            }
          }
          assistantMsg.content += delta
        },
        onReasoning: (delta) => {
          assistantMsg.reasoning = (assistantMsg.reasoning || '') + delta
          assistantMsg.reasoningStreaming = true
          if (!assistantMsg.content) {
            assistantMsg.thinkingFinishedAt = undefined
          }
          if (!assistantMsg.thinkingStartedAt) {
            assistantMsg.thinkingStartedAt = Date.now()
          }
        },
        onCitations: (citations) => {
          assistantMsg.citations = citations
        },
        onDone: (usage) => {
          const logId = usage?.logId
          if (logId) {
            assistantMsg.roundLogId = logId
            if (userMsg) {
              userMsg.roundLogId = logId
            }
          }
          finish()
        },
        onStatus: (message) => {
          const clean = (message || '').trim()
          if (!clean) return
          statusHint.value = clean
          const tools = assistantMsg.tools || []
          const last = tools[tools.length - 1]
          // 连续重复的工具提示只保留一条，避免轨迹刷屏
          if (!last || last.message !== clean) {
            tools.push({ id: `tool-${++toolSeq}`, message: clean })
            assistantMsg.tools = tools
          }
        },
        onError: (message) => {
          if (!assistantMsg.content) {
            assistantMsg.content = message
            assistantMsg.error = true
          } else {
            uni.showToast({ title: message, icon: 'none' })
          }
          finish()
        },
      },
      abortController?.signal,
    )
    // abort 时 streamChat 静默返回，这里兜底复位
    if (streaming.value) {
      if (assistantMsg.streaming && !assistantMsg.content) {
        assistantMsg.content = '（已停止）'
      }
      finish()
    }
  }

  /** 用户点击停止：中断流式请求，保留已生成内容 */
  function stop() {
    abortController?.abort()
  }

  /** 新对话：清空消息并更换会话 ID */
  function clear() {
    stop()
    messages.value = []
    conversationId.value = genConversationId()
  }

  /**
   * 删除单条消息：用户消息与其对应回答（或回答与其对应提问）成对删除。
   * 已落库轮次先调用后端按轮删除，确保后续上下文与历史恢复都不再生效。
   */
  async function deleteMessage(id: number): Promise<boolean> {
    if (streaming.value && messages.value.some((m) => m.id === id && m.streaming)) {
      return false
    }
    const index = messages.value.findIndex((message) => message.id === id)
    if (index < 0) {
      return false
    }
    const target = messages.value[index]
    const idsToDelete = new Set<number>([target.id])
    if (target.role === 'user') {
      const next = messages.value[index + 1]
      if (next?.role === 'assistant') {
        idsToDelete.add(next.id)
        if (!target.roundLogId) {
          target.roundLogId = next.roundLogId
        }
      }
    } else {
      const prev = messages.value[index - 1]
      if (prev?.role === 'user') {
        idsToDelete.add(prev.id)
        if (!target.roundLogId) {
          target.roundLogId = prev.roundLogId
        }
      }
    }

    const roundIds = new Set<number>()
    if (target.roundLogId) {
      roundIds.add(target.roundLogId)
    }
    for (const message of messages.value) {
      if (idsToDelete.has(message.id) && message.roundLogId) {
        roundIds.add(message.roundLogId)
      }
    }

    if (roundIds.size > 0) {
      try {
        await Promise.all(Array.from(roundIds).map((logId) => deleteChatMessage(logId)))
      } catch {
        uni.showToast({ title: '删除失败，请稍后重试', icon: 'none' })
        return false
      }
    } else if (
      conversationId.value &&
      (index === messages.value.length - 1 || index === messages.value.length - 2)
    ) {
      // 中断/失败轮次前端未拿到落库ID：按会话最后一轮回退删除，避免历史恢复后残留
      try {
        await new Promise((resolve) => setTimeout(resolve, 200))
        await deleteLastChatRound(conversationId.value)
      } catch {
        // 兜底删除失败不阻断本地移除
      }
    }

    messages.value = messages.value.filter((message) => !idsToDelete.has(message.id))
    return true
  }

  /** 重新生成：撤回该回答与对应提问后携带原附件重新发起 */
  async function regenerate() {
    if (streaming.value) return
    const lastIndex = messages.value.length - 1
    if (lastIndex < 0) return
    let assistantIndex = lastIndex
    while (assistantIndex >= 0 && messages.value[assistantIndex].role !== 'assistant') {
      assistantIndex--
    }
    if (assistantIndex < 0) return
    const assistant = messages.value[assistantIndex]
    if (assistant.streaming || assistant.error) return

    let userIndex = assistantIndex - 1
    while (userIndex >= 0 && messages.value[userIndex].role !== 'user') {
      userIndex--
    }
    if (userIndex < 0) return
    const userMsg = messages.value[userIndex]
    const question = userMsg.content
    const attachments = userMsg.attachments || []
    const attachmentContext = userMsg.attachmentContext || ''
    if (userMsg.roundLogId) {
      try {
        await deleteChatMessage(userMsg.roundLogId)
      } catch {
        uni.showToast({
          title: '原回答记录清理失败，重新生成后可能出现重复历史记录',
          icon: 'none',
        })
      }
    }

    messages.value = messages.value.slice(0, userIndex)
    await runStream(question, true, attachments, attachmentContext)
  }

  /** 恢复历史会话：拉取问答序列重建消息流（含附件与上下文快照），后续发送自动带上下文续聊 */
  async function restoreConversation(targetConversationId: string): Promise<boolean> {
    if (streaming.value || !targetConversationId) return false
    const res = await getConversationHistory(targetConversationId)
    const rounds = res.data || []
    if (rounds.length === 0) return false
    const restored: AiWuMessage[] = []
    for (const round of rounds) {
      const attachments = (round.attachments || []).map((attachment) => ({
        ...attachment,
        fileName: attachment.fileName || '未命名附件',
      }))
      const roundLogId = round.id
      if (round.question || attachments.length > 0) {
        restored.push({
          id: ++msgSeq,
          role: 'user',
          content: round.question || '',
          attachments: attachments.length > 0 ? attachments : undefined,
          attachmentContext: round.attachmentContext || undefined,
          roundLogId,
          time: Date.now(),
        })
      }
      if (round.answer || round.reasoning || (round.toolMessages && round.toolMessages.length > 0)) {
        const citations = normalizeWebSearchCitations(round.citations)
        const tools: AiToolTrace[] | undefined =
          round.toolMessages && round.toolMessages.length > 0
            ? round.toolMessages.map((message, index) => ({
                id: `hist-tool-${roundLogId ?? restored.length}-${index}`,
                message,
              }))
            : undefined
        const now = Date.now()
        const thinkingDurationMs =
          typeof round.thinkingDurationMs === 'number' && round.thinkingDurationMs > 0
            ? round.thinkingDurationMs
            : 0
        restored.push({
          id: ++msgSeq,
          role: 'assistant',
          content: round.answer || '',
          reasoning: round.reasoning || undefined,
          thinkingStartedAt: thinkingDurationMs > 0 ? now - thinkingDurationMs : undefined,
          thinkingFinishedAt: thinkingDurationMs > 0 ? now : undefined,
          tools,
          citations: citations.length > 0 ? citations : undefined,
          roundLogId,
          time: now,
        })
      }
    }
    messages.value = restored
    conversationId.value = targetConversationId
    return true
  }

  return {
    panelVisible,
    messages,
    conversationId,
    models,
    selectedModelId,
    streaming,
    modelsLoaded,
    statusHint,
    webSearchAvailable,
    webSearchEngineSummary,
    webSearchEnabled,
    currentModel,
    loadModels,
    loadCapabilities,
    toggleWebSearch,
    openPanel,
    closePanel,
    togglePanel,
    send,
    stop,
    clear,
    deleteMessage,
    regenerate,
    restoreConversation,
  }
})
