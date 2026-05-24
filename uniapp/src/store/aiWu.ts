import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  listChatModels,
  streamChat,
  getConversationHistory,
  type AiChatMessage,
  type AiChatModelVO,
} from '@/api/ai'

export interface AiWuMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  /** assistant 消息流式输出中 */
  streaming?: boolean
  /** 出错的 assistant 消息 */
  error?: boolean
  time: number
}

/** 发送给后端的上下文条数上限（与后端 MAX_CONTEXT_MESSAGES 对齐） */
const MAX_CONTEXT = 20

let msgSeq = 0

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
  let abortController: AbortController | null = null

  const currentModel = computed(
    () => models.value.find((m) => m.id === selectedModelId.value) || null,
  )

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

  /** 组装发送给后端的上下文（截取最近 N 条，不含流式中/出错消息） */
  function buildContext(): AiChatMessage[] {
    return messages.value
      .filter((m) => !m.streaming && !m.error && m.content)
      .slice(-MAX_CONTEXT)
      .map((m) => ({ role: m.role, content: m.content }))
  }

  async function send(text: string) {
    const content = text.trim()
    if (!content || streaming.value) return

    messages.value.push({ id: ++msgSeq, role: 'user', content, time: Date.now() })
    messages.value.push({
      id: ++msgSeq,
      role: 'assistant',
      content: '',
      streaming: true,
      time: Date.now(),
    })
    // 必须取数组中的响应式代理再增量写入，直接改 push 前的原始对象 Vue 无法感知（流式不刷新）
    const assistantMsg = messages.value[messages.value.length - 1]
    streaming.value = true
    abortController = typeof AbortController !== 'undefined' ? new AbortController() : null

    const finish = () => {
      assistantMsg.streaming = false
      streaming.value = false
      statusHint.value = ''
      abortController = null
    }

    await streamChat(
      {
        conversationId: conversationId.value,
        modelId: selectedModelId.value,
        source: 'mobile',
        messages: buildContext(),
      },
      {
        onDelta: (delta) => {
          statusHint.value = ''
          assistantMsg.content += delta
        },
        onDone: () => finish(),
        onStatus: (message) => {
          statusHint.value = message
        },
        onError: (message) => {
          if (!assistantMsg.content) {
            assistantMsg.content = message
            assistantMsg.error = true
          } else {
            assistantMsg.content += `\n\n[${message}]`
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

  /** 恢复历史会话：拉取问答序列重建消息流，后续发送自动带上下文续聊 */
  async function restoreConversation(targetConversationId: string): Promise<boolean> {
    if (streaming.value || !targetConversationId) return false
    const res = await getConversationHistory(targetConversationId)
    const rounds = res.data || []
    if (rounds.length === 0) return false
    const restored: AiWuMessage[] = []
    for (const round of rounds) {
      if (round.question) {
        restored.push({ id: ++msgSeq, role: 'user', content: round.question, time: Date.now() })
      }
      if (round.answer) {
        restored.push({ id: ++msgSeq, role: 'assistant', content: round.answer, time: Date.now() })
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
    currentModel,
    loadModels,
    openPanel,
    closePanel,
    togglePanel,
    send,
    stop,
    clear,
    restoreConversation,
  }
})
