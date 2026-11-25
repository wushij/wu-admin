<template>
  <div v-if="siteStore.aiAssistantEnabled" class="ai-wu-trigger-root">
    <button
      id="tour-ai-float-btn"
      class="ai-wu-trigger"
      :class="{ 'is-active': aiWuStore.panelVisible }"
      type="button"
      :aria-label="aiWuStore.panelVisible ? '收起 AI wu助手' : '打开 AI wu助手'"
      @click="toggleDrawer"
      @mouseenter="hovered = true"
      @mouseleave="hovered = false"
    >
      <transition name="aiwu-tip-fade">
        <span v-if="hovered && !aiWuStore.panelVisible" class="ai-wu-trigger-tip">
          <el-icon class="tip-star"><StarFilled /></el-icon>
          <span class="tip-title">AI wu助手 · 系统智能副驾</span>
          <span class="tip-kbd">Alt+C</span>
        </span>
      </transition>

      <span class="ai-wu-trigger-ball">
        <span class="aiwu-aurora-ring" aria-hidden="true" />
        <AiCompassIcon :size="34" :dark="true" :spin="true" />
        <span v-if="aiWuStore.streaming" class="aiwu-streaming-dot" aria-hidden="true" />
      </span>
    </button>

    <AiWuDrawer
      :visible="aiWuStore.panelVisible"
      :wide="wideMode"
      @close="aiWuStore.closePanel()"
    >
      <AiWuHeader
        :wide="wideMode"
        :history-open="historyVisible"
        :has-messages="aiWuStore.messages.length > 0"
        @toggle-wide="wideMode = !wideMode"
        @toggle-history="toggleHistory"
        @new-session="handleNewSession"
        @clear="handleClear"
        @close="aiWuStore.closePanel()"
      />

      <AiWuHistoryPanel
        :visible="historyVisible"
        :loading="historyLoading"
        :sessions="conversations"
        :current-id="aiWuStore.conversationId"
        @select="handleSelectSession"
        @delete="handleDeleteSession"
        @clear-all="handleClearAllSessions"
        @new-session="handleNewSession"
        @close="historyVisible = false"
      />

      <AiWuMessageList
        ref="messageListRef"
        :messages="aiWuStore.messages"
        :streaming="aiWuStore.streaming"
        :models-loaded="aiWuStore.modelsLoaded"
        :has-models="aiWuStore.models.length > 0"
        :user-avatar="userAvatar"
        :user-fallback="userFallback"
        @send="handleQuickSend"
        @copy="copyMessage"
        @regenerate="handleRegenerate"
        @delete="handleDeleteMessage"
      />

      <AiWuComposer
        ref="composerRef"
        v-model="inputText"
        :streaming="aiWuStore.streaming"
        :disabled="aiWuStore.modelsLoaded && aiWuStore.models.length === 0"
        :web-search-available="aiWuStore.webSearchAvailable"
        :web-search-enabled="aiWuStore.webSearchEnabled"
        :web-search-engine-summary="aiWuStore.webSearchEngineSummary"
        :placeholder="
          aiWuStore.modelsLoaded && aiWuStore.models.length === 0
            ? '暂无可用模型，请联系管理员配置'
            : '请输入你的问题，Enter 发送，Shift+Enter 换行'
        "
        @send="handleSend"
        @stop="aiWuStore.stop()"
        @toggle-web-search="aiWuStore.toggleWebSearch()"
      />
    </AiWuDrawer>

    <AiWuMermaidViewer />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import { useAiWuStore } from '@/store/aiWu'
import type { AiWuAttachment } from '@/store/aiWu'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'
import {
  listConversations,
  deleteConversation,
  clearAllConversations,
  type AiConversationVO,
} from '@/api/ai'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import AiCompassIcon from './AiCompassIcon.vue'
import AiWuDrawer from './AiWuDrawer.vue'
import AiWuHeader from './AiWuHeader.vue'
import AiWuHistoryPanel from './AiWuHistoryPanel.vue'
import AiWuMessageList from './AiWuMessageList.vue'
import AiWuComposer from './AiWuComposer.vue'
import AiWuMermaidViewer from './AiWuMermaidViewer.vue'

const aiWuStore = useAiWuStore()
const siteStore = useSiteStore()
const userStore = useUserStore()

const wideMode = ref(false)
const hovered = ref(false)
const inputText = ref('')
const historyVisible = ref(false)
const historyLoading = ref(false)
const conversations = ref<AiConversationVO[]>([])
const messageListRef = ref<InstanceType<typeof AiWuMessageList>>()
const composerRef = ref<InstanceType<typeof AiWuComposer>>()

const userAvatar = computed(() => userStore.userInfo.avatar || '')
const userFallback = computed(
  () => userStore.userInfo.nickname || userStore.userInfo.username || '我',
)

let previousBodyOverflow = ''

function toggleDrawer() {
  aiWuStore.togglePanel()
}

function focusComposer() {
  nextTick(() => composerRef.value?.focus())
}

function handleSend(payload: { text: string; attachments: AiWuAttachment[] }) {
  if (aiWuStore.streaming) return
  if (!payload.text && payload.attachments.length === 0) return
  void aiWuStore.send(payload.text, payload.attachments)
  nextTick(() => messageListRef.value?.scrollToBottom(true))
}

function handleQuickSend(text: string) {
  if (aiWuStore.streaming) return
  void aiWuStore.send(text)
  nextTick(() => messageListRef.value?.scrollToBottom(true))
}

async function copyMessage(content: string) {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败，请手动选择文本复制')
  }
}

function handleRegenerate() {
  if (aiWuStore.streaming) return
  void aiWuStore.regenerate()
  nextTick(() => messageListRef.value?.scrollToBottom(true))
}

async function handleDeleteMessage(id: number) {
  if (aiWuStore.streaming) {
    ElMessage.warning('回答生成中，请稍后再操作')
    return
  }
  const message = aiWuStore.messages.find((item) => item.id === id)
  const isUser = message?.role === 'user'
  await openAlertDialog({
    title: '删除本轮对话',
    targetName: isUser ? '该提问及其对应回答' : '该回答及其对应提问',
    message:
      '将同时删除本轮一问一答，并从当前上下文与历史记录中永久移除；删除后模型不会再引用这部分内容。',
    severity: 'irreversible',
    confirmText: '删除本轮',
    onConfirm: async () => {
      const deleted = await aiWuStore.deleteMessage(id)
      if (deleted) {
        ElMessage.success('已删除本轮对话')
      }
    },
  })
}

function handleNewSession() {
  aiWuStore.clear()
  historyVisible.value = false
  focusComposer()
}

async function handleClear() {
  if (!aiWuStore.messages.length) return
  await openAlertDialog({
    title: '清空当前对话',
    targetName: `共 ${aiWuStore.messages.length} 条消息`,
    message: '将清空当前面板中的全部消息并开启新会话，已归档的历史对话不受影响。',
    severity: 'irreversible',
    confirmText: '清空对话',
    onConfirm: () => {
      aiWuStore.clear()
      focusComposer()
    },
  })
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await listConversations()
    conversations.value = res.data || []
  } catch {
    ElMessage.error('历史会话加载失败')
  } finally {
    historyLoading.value = false
  }
}

function toggleHistory() {
  historyVisible.value = !historyVisible.value
  if (historyVisible.value) {
    void loadHistory()
  }
}

async function handleSelectSession(item: AiConversationVO) {
  if (aiWuStore.streaming) {
    ElMessage.warning('回答生成中，请稍后切换会话')
    return
  }
  if (item.conversationId === aiWuStore.conversationId) {
    historyVisible.value = false
    return
  }
  const restored = await aiWuStore.restoreConversation(item.conversationId)
  if (restored) {
    historyVisible.value = false
    nextTick(() => messageListRef.value?.scrollToBottom(true))
  } else {
    ElMessage.warning('该会话暂无可恢复的记录')
  }
}

async function handleDeleteSession(item: AiConversationVO) {
  if (aiWuStore.streaming && item.conversationId === aiWuStore.conversationId) {
    ElMessage.warning('回答生成中，请稍后再操作')
    return
  }
  await openAlertDialog({
    title: '永久删除历史对话',
    targetName: item.title || '未命名会话',
    message: '删除后该对话的上下文记录将被永久移除且不可恢复。',
    severity: 'irreversible',
    confirmText: '永久删除',
    onConfirm: async () => {
      await deleteConversation(item.conversationId)
      conversations.value = conversations.value.filter(
        (session) => session.conversationId !== item.conversationId,
      )
      ElMessage.success('已永久删除该历史对话')
      if (item.conversationId === aiWuStore.conversationId) {
        aiWuStore.clear()
      }
    },
  })
}

async function handleClearAllSessions() {
  if (aiWuStore.streaming) {
    ElMessage.warning('回答生成中，请稍后再操作')
    return
  }
  await openAlertDialog({
    title: '清空全部历史对话',
    targetName: `共 ${conversations.value.length} 条会话记录`,
    message: '此操作将永久清空当前账号下的所有 AI 对话记录，清空后无法找回。',
    severity: 'irreversible',
    confirmText: '清空全部对话',
    onConfirm: async () => {
      await clearAllConversations()
      conversations.value = []
      ElMessage.success('已清空所有历史对话')
      aiWuStore.clear()
    },
  })
}

watch(
  () => aiWuStore.panelVisible,
  (visible) => {
    if (visible) {
      previousBodyOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      nextTick(() => {
        focusComposer()
        messageListRef.value?.scrollToBottom(true)
      })
    } else {
      historyVisible.value = false
      document.body.style.overflow = previousBodyOverflow
    }
  },
)

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (historyVisible.value) {
      historyVisible.value = false
      return
    }
    if (aiWuStore.panelVisible) {
      aiWuStore.closePanel()
    }
    return
  }

  const target = event.target as HTMLElement | null
  if (
    target &&
    (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)
  ) {
    return
  }

  const isAltC = event.altKey && (event.key === 'c' || event.key === 'C')
  const isCtrlShiftK =
    (event.ctrlKey || event.metaKey) && event.shiftKey && (event.key === 'k' || event.key === 'K')
  if (isAltC || isCtrlShiftK) {
    event.preventDefault()
    event.stopPropagation()
    toggleDrawer()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown)
  if (!aiWuStore.modelsLoaded) {
    void aiWuStore.loadModels()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
  document.body.style.overflow = previousBodyOverflow
})
</script>

<style lang="scss">
@use './ai-wu-assistant.scss';
@use './ai-wu-markdown.scss';
</style>
