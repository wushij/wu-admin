<template>
  <!-- 悬浮球（未展开且登录后可见） -->
  <view
    v-if="showFloatBall && !aiWuStore.panelVisible"
    class="aiwu-ball"
    :class="{ 'is-streaming': aiWuStore.streaming }"
    @click="aiWuStore.openPanel()"
  >
    <view class="aurora-pulse-ring" />
    <AiCompassIcon :size="28" :dark="true" :spin="true" class="ball-compass" />
    <view v-if="aiWuStore.streaming" class="streaming-ping" />
  </view>

  <!-- 对话抽屉 -->
  <view
    v-if="aiWuStore.panelVisible"
    class="aiwu-mask"
    @click="handleMaskClick"
    @touchmove.stop.prevent
  />
  <view
    class="aiwu-drawer"
    :class="{ 'is-open': aiWuStore.panelVisible }"
    @click="handleBlankClick"
    @touchmove.stop
  >
    <AiWuHeader
      :history-visible="historyVisible"
      @toggle-history="toggleHistory"
      @new-session="handleNewSession"
      @close="aiWuStore.closePanel()"
    />

    <AiWuHistoryPanel
      :visible="historyVisible"
      :loading="historyLoading"
      :sessions="conversations"
      :current-id="aiWuStore.conversationId"
      @select="handleRestore"
      @delete="handleDelete"
      @clear-all="handleClearAll"
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
      :placeholder="
        aiWuStore.modelsLoaded && aiWuStore.models.length === 0
          ? '暂无可用模型，请联系管理员配置'
          : '输入问题…'
      "
      :web-search-available="aiWuStore.webSearchAvailable"
      :web-search-enabled="aiWuStore.webSearchEnabled"
      @send="handleSend"
      @stop="aiWuStore.stop()"
      @toggle-web-search="aiWuStore.toggleWebSearch()"
    />
  </view>

  <!-- 全局绝对置顶二次确认弹窗 (z-index: 99999，完全置于抽屉与遮罩之上) -->
  <view
    v-if="confirmState.visible"
    class="aiwu-confirm-mask"
    @click="confirmState.visible = false"
    @touchmove.stop.prevent
  >
    <view class="aiwu-confirm-card" @click.stop>
      <view class="aiwu-confirm-icon">
        <text class="warn-icon">!</text>
      </view>
      <view class="aiwu-confirm-title">{{ confirmState.title }}</view>
      <view class="aiwu-confirm-desc">{{ confirmState.content }}</view>
      <view class="aiwu-confirm-actions">
        <button class="aiwu-confirm-btn aiwu-confirm-btn--cancel" @click="confirmState.visible = false">
          取消
        </button>
        <button
          class="aiwu-confirm-btn"
          :class="confirmState.isDanger ? 'aiwu-confirm-btn--danger' : 'aiwu-confirm-btn--primary'"
          @click="onConfirmAction"
        >
          {{ confirmState.confirmText }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { useAiWuStore } from '@/store/aiWu'
import type { AiWuAttachment } from '@/store/aiWu'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { hasToken } from '@/core/auth'
import { listConversations, deleteConversation, clearAllConversations, type AiConversationVO } from '@/api/ai'
import AiCompassIcon from './AiCompassIcon.vue'
import AiWuHeader from './AiWuHeader.vue'
import AiWuHistoryPanel from './AiWuHistoryPanel.vue'
import AiWuMessageList from './AiWuMessageList.vue'
import AiWuComposer from './AiWuComposer.vue'

const aiWuStore = useAiWuStore()
const userStore = useUserStore()
const appStore = useAppStore()

const showFloatBall = computed(
  () => (hasToken() || !!userStore.userInfo?.userId) && appStore.aiAssistantEnabled,
)

const userAvatar = computed(() => userStore.userInfo?.avatar || '')
const userFallback = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '我',
)

const inputText = ref('')
const messageListRef = ref<InstanceType<typeof AiWuMessageList>>()
const composerRef = ref<InstanceType<typeof AiWuComposer>>()
const historyVisible = ref(false)
let savedScrollY = 0

function handleMaskClick() {
  aiWuStore.closePanel()
}

function handleBlankClick() {
  // 保持空白区域点击
}

function scrollToBottom() {
  messageListRef.value?.scrollToBottom(true)
}

function handleSend(payload: { text: string; attachments: AiWuAttachment[] }) {
  if (aiWuStore.streaming) return
  if (!payload.text && payload.attachments.length === 0) return
  void aiWuStore.send(payload.text, payload.attachments)
  nextTick(scrollToBottom)
}

function handleQuickSend(text: string) {
  if (aiWuStore.streaming) return
  if (aiWuStore.modelsLoaded && aiWuStore.models.length === 0) return
  void aiWuStore.send(text)
  nextTick(scrollToBottom)
}

function copyMessage(content: string) {
  uni.setClipboardData({
    data: content,
    showToast: false,
    success: () => uni.showToast({ title: '已复制', icon: 'none' }),
    fail: () => uni.showToast({ title: '复制失败', icon: 'none' }),
  })
}

function handleRegenerate() {
  if (aiWuStore.streaming) return
  void aiWuStore.regenerate()
  nextTick(scrollToBottom)
}

watch(
  () => aiWuStore.panelVisible,
  (visible) => {
    if (!visible) {
      historyVisible.value = false
      composerRef.value?.closeUploadMenu()
    }
    // 抽屉打开时强力锁定外层 Body 滚动，彻底防止 H5 下流式渲染与输入导致的页面位移
    // #ifdef H5
    if (visible) {
      savedScrollY = window.scrollY || document.documentElement.scrollTop || 0
      document.documentElement.style.overflow = 'hidden'
      document.body.style.overflow = 'hidden'
      document.body.style.position = 'fixed'
      document.body.style.top = `-${savedScrollY}px`
      document.body.style.width = '100%'
      document.body.style.touchAction = 'none'
    } else {
      document.documentElement.style.overflow = ''
      document.body.style.overflow = ''
      document.body.style.position = ''
      document.body.style.top = ''
      document.body.style.width = ''
      document.body.style.touchAction = ''
      window.scrollTo(0, savedScrollY)
    }
    // #endif
    if (visible) {
      nextTick(scrollToBottom)
    }
  },
  { immediate: true },
)

// 面板开着时组件被卸载（如切页）需解除页面滚动锁定
onUnmounted(() => {
  // #ifdef H5
  document.documentElement.style.overflow = ''
  document.body.style.overflow = ''
  document.body.style.position = ''
  document.body.style.top = ''
  document.body.style.width = ''
  document.body.style.touchAction = ''
  // #endif
})

// ---------- 历史对话 ----------
const historyLoading = ref(false)
const conversations = ref<AiConversationVO[]>([])

async function toggleHistory() {
  historyVisible.value = !historyVisible.value
  if (!historyVisible.value) return
  historyLoading.value = true
  try {
    const res = await listConversations()
    conversations.value = res.data || []
  } catch {
    uni.showToast({ title: '历史会话加载失败', icon: 'none' })
  } finally {
    historyLoading.value = false
  }
}

// ---------- 二次确认置顶弹窗 ----------
const confirmState = ref<{
  visible: boolean
  title: string
  content: string
  confirmText: string
  isDanger: boolean
  onConfirm: () => Promise<void> | void
}>({
  visible: false,
  title: '',
  content: '',
  confirmText: '确定',
  isDanger: true,
  onConfirm: () => {},
})

function openConfirm(options: {
  title: string
  content: string
  confirmText?: string
  isDanger?: boolean
  onConfirm: () => Promise<void> | void
}) {
  confirmState.value = {
    visible: true,
    title: options.title,
    content: options.content,
    confirmText: options.confirmText || '确定',
    isDanger: options.isDanger ?? true,
    onConfirm: options.onConfirm,
  }
}

async function onConfirmAction() {
  const cb = confirmState.value.onConfirm
  confirmState.value.visible = false
  if (cb) {
    await cb()
  }
}

async function handleDelete(conv: AiConversationVO) {
  if (aiWuStore.streaming && conv.conversationId === aiWuStore.conversationId) {
    uni.showToast({ title: '回答生成中，请稍后再操作', icon: 'none' })
    return
  }
  const brief = (conv.title || '此会话').trim().slice(0, 18)
  openConfirm({
    title: '删除会话确认',
    content: `确定删除历史对话「${brief}」吗？删除后将无法恢复。`,
    confirmText: '删除',
    isDanger: true,
    onConfirm: async () => {
      try {
        await deleteConversation(conv.conversationId)
        conversations.value = conversations.value.filter(
          (item) => item.conversationId !== conv.conversationId,
        )
        uni.showToast({ title: '已删除该历史对话', icon: 'success' })
        if (conv.conversationId === aiWuStore.conversationId) {
          aiWuStore.clear()
        }
      } catch {
        uni.showToast({ title: '删除会话失败，请重试', icon: 'none' })
      }
    },
  })
}

async function handleClearAll() {
  if (aiWuStore.streaming) {
    uni.showToast({ title: '回答生成中，请稍后再操作', icon: 'none' })
    return
  }
  openConfirm({
    title: '清空历史确认',
    content: '确定清空全部历史对话吗？清空后所有会话记录都将永久删除。',
    confirmText: '清空全部',
    isDanger: true,
    onConfirm: async () => {
      try {
        await clearAllConversations()
        conversations.value = []
        uni.showToast({ title: '已清空所有历史对话', icon: 'success' })
        aiWuStore.clear()
      } catch {
        uni.showToast({ title: '清空失败，请重试', icon: 'none' })
      }
    },
  })
}

async function handleRestore(conv: AiConversationVO) {
  if (aiWuStore.streaming) {
    uni.showToast({ title: '回答生成中，请稍后切换', icon: 'none' })
    return
  }
  if (conv.conversationId === aiWuStore.conversationId) {
    historyVisible.value = false
    return
  }
  const ok = await aiWuStore.restoreConversation(conv.conversationId)
  if (ok) {
    historyVisible.value = false
    nextTick(scrollToBottom)
  } else {
    uni.showToast({ title: '该会话暂无可恢复的记录', icon: 'none' })
  }
}

function handleNewSession() {
  aiWuStore.clear()
  historyVisible.value = false
  inputText.value = ''
}

async function handleDeleteMessage(id: number) {
  if (aiWuStore.streaming) {
    uni.showToast({ title: '回答生成中，请稍后再操作', icon: 'none' })
    return
  }
  const message = aiWuStore.messages.find((item) => item.id === id)
  const isUser = message?.role === 'user'
  openConfirm({
    title: '删除本轮对话',
    content: `将同时删除${isUser ? '该提问及其对应回答' : '该回答及其对应提问'}，并从当前上下文与历史记录中永久移除；删除后模型不会再引用这部分内容。`,
    confirmText: '删除本轮',
    isDanger: true,
    onConfirm: async () => {
      const deleted = await aiWuStore.deleteMessage(id)
      if (deleted) {
        uni.showToast({ title: '已删除本轮对话', icon: 'success' })
      }
    },
  })
}
</script>
