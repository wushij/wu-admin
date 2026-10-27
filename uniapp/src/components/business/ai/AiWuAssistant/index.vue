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
  <view v-if="aiWuStore.panelVisible" class="aiwu-mask" @click="handleMaskClick" @touchmove.stop.prevent />
  <view class="aiwu-drawer" :class="{ 'is-open': aiWuStore.panelVisible }" @click="handleBlankClick" @touchmove.stop>
    <!-- 头部 -->
    <view class="aiwu-header" @touchmove.stop.prevent>
      <view class="aiwu-header__left">
        <view class="aiwu-header__avatar">
          <AiCompassIcon :size="24" :dark="true" :spin="true" />
        </view>
        <view class="aiwu-header__title">
          <text class="title">AI wu助手</text>
          <text class="subtitle">系统专属智能副驾 · 贴心指引答疑</text>
        </view>
      </view>
      <view class="aiwu-header__actions">
        <view class="header-toolbar" role="toolbar">
          <view
            class="header-btn"
            :class="{ 'is-active': historyVisible }"
            title="历史对话"
            @click.stop="toggleHistory"
          >
            <IconFont name="clock-o" :size="26" color="#f1f5f9" />
          </view>
          <view class="header-toolbar-sep" />
          <view
            class="header-btn"
            title="新对话"
            @click.stop="handleClear"
          >
            <svg viewBox="0 0 1024 1024" width="13" height="13" fill="#f1f5f9">
              <path d="M480 480V208a32 32 0 1 1 64 0v272h272a32 32 0 1 1 0 64H544v272a32 32 0 1 1-64 0V544H208a32 32 0 1 1 0-64h272z" />
            </svg>
          </view>
          <view class="header-toolbar-sep" />
          <view
            class="header-btn header-btn--close"
            title="关闭"
            @click.stop="aiWuStore.closePanel()"
          >
            <svg viewBox="0 0 1024 1024" width="12" height="12" fill="#f1f5f9">
              <path d="M292.7 236.1a32 32 0 0 0-45.3 45.3L466.7 512 247.4 731.3a32 32 0 0 0 45.3 45.3L512 557.3l219.3 219.3a32 32 0 0 0 45.3-45.3L557.3 512l219.3-219.3a32 32 0 0 0-45.3-45.3L512 466.7 292.7 236.1z" />
            </svg>
          </view>
        </view>
      </view>
    </view>

    <!-- 历史对话浮层 -->
    <view v-if="historyVisible" class="aiwu-history" @click.stop>
      <view class="aiwu-history__head">
        <view class="aiwu-history__head-title">
          <IconFont name="clock-o" :size="30" color="#0f172a" />
          <text>历史对话</text>
          <text v-if="conversations.length" class="count">{{ conversations.length }}</text>
        </view>
        <view class="aiwu-history__head-tools">
          <view
            v-if="conversations.length"
            class="aiwu-history__clear-btn"
            hover-class="aiwu-history__clear-btn--hover"
            :hover-stop-propagation="true"
            @click.stop="handleClearAll"
          >
            <text>清空</text>
          </view>
          <view class="aiwu-history__close" @click="historyVisible = false">
            <text>✕</text>
          </view>
        </view>
      </view>
      <scroll-view scroll-y class="aiwu-history__body">
        <view
          v-for="conv in conversations"
          :key="conv.conversationId"
          class="aiwu-history__item"
          :class="{ 'is-active': conv.conversationId === aiWuStore.conversationId }"
          @click="handleRestore(conv.conversationId)"
        >
          <view class="item-icon">
            <IconFont name="chat-o" :size="28" color="#6366f1" />
          </view>
          <view class="item-main">
            <text class="item-title">{{ conv.title }}</text>
            <text class="item-meta">{{ formatConvTime(conv.lastTime) }} · {{ conv.messageCount || 0 }} 轮对话</text>
          </view>
          <view class="item-tail">
            <text v-if="conv.conversationId === aiWuStore.conversationId" class="item-tag">当前</text>
            <view
              class="item-del-btn"
              hover-class="item-del-btn--hover"
              :hover-stop-propagation="true"
              @click.stop="handleDelete(conv)"
            >
              <text class="item-del-text">删除</text>
            </view>
            <text v-if="conv.conversationId !== aiWuStore.conversationId" class="item-arrow">›</text>
          </view>
        </view>
        <view v-if="historyLoading && conversations.length === 0" class="aiwu-history__empty">
          <text class="empty-title">加载中…</text>
          <text class="empty-desc">正在获取历史对话记录</text>
        </view>
        <view v-else-if="!historyLoading && conversations.length === 0" class="aiwu-history__empty">
          <IconFont name="chat-o" :size="64" color="#cbd5e1" />
          <text class="empty-title">还没有历史对话</text>
          <text class="empty-desc">和 AI wu助手聊聊，记录会自动保存在这里</text>
        </view>
      </scroll-view>
    </view>

    <!-- 消息区 -->
    <scroll-view
      class="aiwu-body"
      scroll-y
      :scroll-top="scrollTop"
      :scroll-into-view="scrollIntoViewId"
      @scroll="onBodyScroll"
    >
      <!-- 欢迎屏（与 PC 100% 对齐） -->
      <view v-if="aiWuStore.messages.length === 0" class="welcome-box">
        <view class="welcome-hero">
          <view class="welcome-avatar-glow" />
          <view class="welcome-avatar">
            <AiCompassIcon :size="38" :dark="true" :spin="true" />
          </view>
        </view>
        <text class="welcome-title">你好，我是 AI wu助手</text>
        <text class="welcome-desc">
          {{ aiWuStore.modelsLoaded && aiWuStore.models.length === 0 ? '暂无可用模型，请联系管理员配置' : '我是本系统的智能助手，熟悉各功能模块与操作路径，有问题尽管问我～' }}
        </text>

        <view class="quick-section">
          <view class="quick-header">
            <text class="quick-header-bulb">💡</text>
            <text class="quick-header-text">常见问题快速提问</text>
          </view>
          <view class="quick-list">
            <view
              v-for="(q, idx) in QUICK_QUESTIONS"
              :key="idx"
              class="quick-card"
              @click="sendQuick(q.text)"
            >
              <view class="quick-card-icon">
                <IconFont :name="q.icon" :size="32" color="#0f172a" />
              </view>
              <view class="quick-card-content">
                <text class="quick-card-title">{{ q.text }}</text>
                <text class="quick-card-desc">{{ q.desc }}</text>
              </view>
              <text class="quick-card-arrow">›</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 消息气泡 -->
      <view
        v-for="msg in aiWuStore.messages"
        :id="`aiwu-msg-${msg.id}`"
        :key="msg.id"
        class="aiwu-row"
        :class="msg.role === 'user' ? 'is-user' : 'is-ai'"
      >
        <view v-if="msg.role === 'assistant'" class="aiwu-avatar ai">
          <AiCompassIcon :size="18" :dark="true" :spin="false" />
        </view>
        <view class="aiwu-bubble" :class="{ 'is-error': msg.error }">
          <view v-if="msg.streaming && !msg.content" class="aiwu-typing">
            <view class="dot" />
            <view class="dot" />
            <view class="dot" />
          </view>
          <view
            v-else-if="isH5 && msg.role === 'assistant' && !msg.error"
            class="aiwu-markdown"
            :class="{ 'is-streaming': msg.streaming }"
            v-html="renderMarkdown(msg.content)"
          />
          <text v-else class="aiwu-bubble__text">{{ msg.content }}</text>
          <!-- AI 回答完成后显示复制按钮（移动端无 hover，常驻展示） -->
          <view
            v-if="msg.role === 'assistant' && !msg.streaming && !msg.error && msg.content"
            class="aiwu-copy-row"
          >
            <view class="aiwu-copy-chip" :class="{ 'is-copied': copiedId === msg.id }" @click="copyMessage(msg.content, msg.id)">
              <!-- PC 端同款 Element Plus Check / CopyDocument 图标 -->
              <svg v-if="copiedId === msg.id" viewBox="0 0 1024 1024" width="13" height="13" fill="currentColor">
                <path d="M406.656 706.944 195.84 496.256a32 32 0 1 0-45.248 45.248l256 256 512-512a32 32 0 0 0-45.248-45.248L406.592 706.944z" />
              </svg>
              <svg v-else viewBox="0 0 1024 1024" width="13" height="13" fill="currentColor">
                <path d="M768 832a128 128 0 0 1-128 128H192A128 128 0 0 1 64 832V384a128 128 0 0 1 128-128v64a64 64 0 0 0-64 64v448a64 64 0 0 0 64 64h448a64 64 0 0 0 64-64h64z" />
                <path d="M384 128a64 64 0 0 0-64 64v448a64 64 0 0 0 64 64h448a64 64 0 0 0 64-64V192a64 64 0 0 0-64-64H384zm0-64h448a128 128 0 0 1 128 128v448a128 128 0 0 1-128 128H384a128 128 0 0 1-128-128V192A128 128 0 0 1 384 64z" />
              </svg>
              <text>{{ copiedId === msg.id ? '已复制' : '复制' }}</text>
            </view>
          </view>
        </view>
        <image
          v-if="msg.role === 'user' && userAvatar"
          class="aiwu-avatar user-img"
          :src="userAvatar"
          mode="aspectFill"
        />
        <view v-else-if="msg.role === 'user'" class="aiwu-avatar user">
          <text>{{ userInitial }}</text>
        </view>
      </view>
      <!-- L3 工具调用状态提示 -->
      <view v-if="aiWuStore.streaming && aiWuStore.statusHint" class="aiwu-tool-status">
        <text>{{ aiWuStore.statusHint }}</text>
      </view>

      <view id="aiwu-bottom-anchor" class="aiwu-anchor" />
    </scroll-view>

    <!-- 输入区 -->
    <view class="aiwu-footer" @touchmove.stop.prevent>
      <view class="aiwu-input-wrap">
        <textarea
          v-model="inputText"
          class="aiwu-input"
          :maxlength="4000"
          :auto-height="true"
          :show-confirm-bar="false"
          :cursor-spacing="16"
          confirm-type="send"
          :disabled="aiWuStore.modelsLoaded && aiWuStore.models.length === 0"
          :placeholder="aiWuStore.modelsLoaded && aiWuStore.models.length === 0 ? '暂无可用模型' : '输入问题…'"
          placeholder-class="aiwu-input-placeholder"
          @confirm="handleSend"
        />
        <view
          v-if="aiWuStore.streaming"
          class="aiwu-send stop"
          @click="aiWuStore.stop()"
        >
          <view class="stop-square" />
        </view>
        <view
          v-else
          class="aiwu-send"
          :class="{ disabled: !inputText.trim() }"
          @click="handleSend"
        >
          <svg viewBox="0 0 1024 1024" width="18" height="18" fill="#ffffff">
            <path fill="#ffffff" d="m64 448 832-320-128 704-446.08-243.328L832 192 242.816 545.472zm256 512V657.024L512 768z"/>
          </svg>
        </view>
      </view>
    </view>
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
import { ref, computed, nextTick, watch, onUnmounted } from 'vue'
import IconFont from '@/components/base/IconFont/index.vue'
import { useAiWuStore } from '@/store/aiWu'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { renderMarkdown } from '@/utils/display/chat-markdown'
import { hasToken } from '@/core/auth'
import { listConversations, deleteConversation, clearAllConversations, type AiConversationVO } from '@/api/ai'
import AiCompassIcon from './AiCompassIcon.vue'

const QUICK_QUESTIONS = [
  {
    icon: 'chat-o',
    text: '这个系统有哪些功能模块？',
    desc: '按你的权限梳理可用功能与入口',
  },
  {
    icon: 'notes-o',
    text: '怎么修改登录密码？',
    desc: '获取本系统真实操作路径指引',
  },
  {
    icon: 'clock-o',
    text: '我是什么角色，属于哪个部门？',
    desc: '查看当前账号的角色与组织信息',
  },
] as const

// 仅 H5 支持 v-html 渲染 Markdown，其他平台降级纯文本
let isH5 = false
// #ifdef H5
isH5 = true
// #endif

const aiWuStore = useAiWuStore()
const userStore = useUserStore()
const appStore = useAppStore()

const showFloatBall = computed(() => (hasToken() || !!userStore.userInfo?.userId) && appStore.aiAssistantEnabled)

const userAvatar = computed(() => userStore.userInfo?.avatar || '')
const userInitial = computed(() => (userStore.userInfo?.nickname || 'U').slice(0, 1))

const inputText = ref('')
const scrollTop = ref(0)
const scrollIntoViewId = ref('')
let savedScrollY = 0

const historyVisible = ref(false)

function handleMaskClick() {
  aiWuStore.closePanel()
}

function handleBlankClick() {
  // 保持空白区域点击
}

watch(
  () => aiWuStore.panelVisible,
  (visible) => {
    if (!visible) {
      historyVisible.value = false
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

function scrollToBottom(force = true) {
  if (force) {
    nearBottom.value = true
  }
  nextTick(() => {
    scrollIntoViewId.value = ''
    nextTick(() => {
      scrollIntoViewId.value = 'aiwu-bottom-anchor'
    })
    scrollTop.value = scrollTop.value === 999999 ? 999998 : 999999

    // #ifdef H5
    const el = document.querySelector('.aiwu-body') as HTMLElement
    if (el && (force || nearBottom.value)) {
      el.scrollTop = el.scrollHeight
    }
    // #endif
  })
}

// 用户是否贴近底部（scroll-view 靠 @scroll 事件实时计算）
const nearBottom = ref(true)

function onBodyScroll(e: { detail: { scrollTop: number; scrollHeight: number } }) {
  const { scrollTop, scrollHeight } = e.detail
  // #ifdef H5
  const el = document.querySelector('.aiwu-body') as HTMLElement
  if (el) {
    nearBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 120
    return
  }
  // #endif
  const viewH = uni.getWindowInfo().windowHeight * 0.6
  nearBottom.value = scrollHeight - scrollTop - viewH < 120
}

// 新消息入列：强制滚底，并重新进入跟随模式
watch(
  () => aiWuStore.messages.length,
  () => {
    scrollToBottom(true)
  },
)

// 流式增量：仅在跟随模式下向上推动
watch(
  () => aiWuStore.messages[aiWuStore.messages.length - 1]?.content,
  () => {
    if (aiWuStore.streaming && nearBottom.value) {
      scrollToBottom(false)
    }
  },
)

// 流式结束：滚到完整回答底部
watch(
  () => aiWuStore.streaming,
  (val) => {
    if (!val) {
      scrollToBottom(true)
    }
  },
)

watch(
  () => aiWuStore.panelVisible,
  (visible) => {
    if (visible) {
      scrollToBottom(true)
    }
  },
)

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || aiWuStore.streaming) return
  inputText.value = ''
  scrollToBottom(true)
  await aiWuStore.send(text)
  scrollToBottom(true)
}

function sendQuick(q: string) {
  if (aiWuStore.streaming || (aiWuStore.modelsLoaded && aiWuStore.models.length === 0)) return
  scrollToBottom(true)
  aiWuStore.send(q)
  scrollToBottom(true)
}

function handleClear() {
  if (aiWuStore.messages.length === 0) return
  aiWuStore.clear()
}

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
        conversations.value = conversations.value.filter((item) => item.conversationId !== conv.conversationId)
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

async function handleRestore(conversationId: string) {
  if (aiWuStore.streaming) {
    uni.showToast({ title: '回答生成中，请稍后切换', icon: 'none' })
    return
  }
  if (conversationId === aiWuStore.conversationId) {
    historyVisible.value = false
    return
  }
  const ok = await aiWuStore.restoreConversation(conversationId)
  if (ok) {
    historyVisible.value = false
    scrollToBottom()
  } else {
    uni.showToast({ title: '该会话暂无可恢复的记录', icon: 'none' })
  }
}

/** 会话时间友好化：今天 HH:mm / 昨天 / MM-DD */
function formatConvTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return time
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const sameDay = (a: Date, b: Date) =>
    a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
  if (sameDay(date, now)) {
    return `今天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (sameDay(date, yesterday)) {
    return `昨天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const copiedId = ref<string | number | null>(null)

function copyMessage(content: string, id: string | number) {
  uni.setClipboardData({
    data: content,
    showToast: false,
    success: () => {
      copiedId.value = id
      uni.showToast({ title: '已复制', icon: 'none' })
      setTimeout(() => {
        if (copiedId.value === id) copiedId.value = null
      }, 2000)
    },
    fail: () => {
      uni.showToast({ title: '复制失败', icon: 'none' })
    },
  })
}
</script>

<style lang="scss" scoped>
@import './ai-wu-assistant.scss';
</style>
