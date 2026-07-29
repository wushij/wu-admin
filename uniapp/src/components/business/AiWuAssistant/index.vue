<template>
  <!-- 悬浮球（未展开且登录后可见） -->
  <view
    v-if="showFloatBall && !aiWuStore.panelVisible"
    class="aiwu-ball"
    :class="{ 'is-streaming': aiWuStore.streaming }"
    @click="aiWuStore.openPanel()"
  >
    <view class="aiwu-ball__halo" />
    <view class="aiwu-ball__icon-inner">
      <view class="aiwu-ball__wand">
        <svg viewBox="0 0 1024 1024" width="22" height="22" fill="#ffffff">
          <path fill="#ffffff" d="M512 64h64v192h-64zm0 576h64v192h-64zM160 480v-64h192v64zm576 0v-64h192v64zM249.856 199.04l45.248-45.184L430.848 289.6 385.6 334.848 249.856 199.104zM657.152 606.4l45.248-45.248 135.744 135.744-45.248 45.248zM114.048 923.2 68.8 877.952l316.8-316.8 45.248 45.248zM702.4 334.848 657.152 289.6l135.744-135.744 45.248 45.248z"/>
        </svg>
      </view>
    </view>
  </view>

  <!-- 对话抽屉 -->
  <view v-if="aiWuStore.panelVisible" class="aiwu-mask" @click="aiWuStore.closePanel()" @touchmove.stop.prevent />
  <view class="aiwu-drawer" :class="{ 'is-open': aiWuStore.panelVisible }">
    <!-- 头部 -->
    <view class="aiwu-header">
      <view class="aiwu-header__left">
        <view class="aiwu-header__avatar" @click.stop="toggleHistory">
          <svg viewBox="0 0 1024 1024" width="18" height="18" fill="#ffffff">
            <path fill="#ffffff" d="M512 64h64v192h-64zm0 576h64v192h-64zM160 480v-64h192v64zm576 0v-64h192v64zM249.856 199.04l45.248-45.184L430.848 289.6 385.6 334.848 249.856 199.104zM657.152 606.4l45.248-45.248 135.744 135.744-45.248 45.248zM114.048 923.2 68.8 877.952l316.8-316.8 45.248 45.248zM702.4 334.848 657.152 289.6l135.744-135.744 45.248 45.248z"/>
          </svg>
          <view class="aiwu-header__avatar-badge">
            <IconFont name="clock-o" :size="20" color="#0f172a" />
          </view>
        </view>
        <view class="aiwu-header__title">
          <text class="title">AI wu助手</text>
          <text class="subtitle">{{ aiWuStore.currentModel ? (aiWuStore.currentModel.modelName || aiWuStore.currentModel.name) : '智能问答' }}</text>
        </view>
      </view>
      <view class="aiwu-header__actions">
        <view class="model-picker-wrap">
          <view class="aiwu-header__btn model" @click.stop="toggleModelMenu">
            <text class="model-name">{{ aiWuStore.currentModel ? (aiWuStore.currentModel.modelName || aiWuStore.currentModel.name) : '选择模型' }}</text>
            <text class="arrow" :class="{ 'is-open': modelMenuVisible }">▾</text>
          </view>
          <!-- 内嵌模型选择浮框 (留在当前界面切换，不触发全屏 mask 关闭) -->
          <view v-if="modelMenuVisible" class="model-dropdown-popover" @click.stop>
            <view
              v-for="m in aiWuStore.models"
              :key="m.id"
              class="popover-item"
              :class="{ 'is-active': m.id === aiWuStore.selectedModelId }"
              @click.stop="selectModel(m.id)"
            >
              <text class="popover-name">{{ m.modelName || m.name }}</text>
              <text v-if="m.id === aiWuStore.selectedModelId" class="popover-check">✓</text>
            </view>
          </view>
        </view>
        <view class="aiwu-header__btn plus" title="新对话" @click="handleClear">
          <text class="plus-txt">+</text>
        </view>
        <view class="aiwu-header__btn close" title="关闭" @click="aiWuStore.closePanel()">
          <text>✕</text>
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
        <view class="aiwu-history__close" @click="historyVisible = false">
          <text>✕</text>
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
          <text v-if="conv.conversationId === aiWuStore.conversationId" class="item-tag">当前</text>
          <text v-else class="item-arrow">›</text>
        </view>
        <view v-if="!historyLoading && conversations.length === 0" class="aiwu-history__empty">
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
      :scroll-into-view="scrollIntoId"
      scroll-with-animation
      @scroll="onBodyScroll"
    >
      <!-- 欢迎屏（与 PC 100% 对齐） -->
      <view v-if="aiWuStore.messages.length === 0" class="welcome-box">
        <view class="welcome-hero">
          <view class="welcome-avatar-glow" />
          <view class="welcome-avatar">
            <svg viewBox="0 0 1024 1024" width="28" height="28" fill="#ffffff">
              <path fill="#ffffff" d="M512 64h64v192h-64zm0 576h64v192h-64zM160 480v-64h192v64zm576 0v-64h192v64zM249.856 199.04l45.248-45.184L430.848 289.6 385.6 334.848 249.856 199.104zM657.152 606.4l45.248-45.248 135.744 135.744-45.248 45.248zM114.048 923.2 68.8 877.952l316.8-316.8 45.248 45.248zM702.4 334.848 657.152 289.6l135.744-135.744 45.248 45.248z"/>
            </svg>
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
          <svg viewBox="0 0 1024 1024" width="14" height="14" fill="#ffffff">
            <path fill="#ffffff" d="M512 64h64v192h-64zm0 576h64v192h-64zM160 480v-64h192v64zm576 0v-64h192v64zM249.856 199.04l45.248-45.184L430.848 289.6 385.6 334.848 249.856 199.104zM657.152 606.4l45.248-45.248 135.744 135.744-45.248 45.248zM114.048 923.2 68.8 877.952l316.8-316.8 45.248 45.248zM702.4 334.848 657.152 289.6l135.744-135.744 45.248 45.248z"/>
          </svg>
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
    <view class="aiwu-footer">
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
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, onUnmounted } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import { useAiWuStore } from '@/store/aiWu'
import { useUserStore } from '@/store/user'
import { renderMarkdown } from '@/utils/chat-markdown'
import { hasToken } from '@/utils/auth'
import { listConversations, type AiConversationVO } from '@/api/ai'

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

const showFloatBall = computed(() => hasToken() || !!userStore.userInfo?.userId)

const userAvatar = computed(() => userStore.userInfo?.avatar || '')
const userInitial = computed(() => (userStore.userInfo?.nickname || 'U').slice(0, 1))

const inputText = ref('')
const scrollIntoId = ref('')

const modelMenuVisible = ref(false)

function toggleModelMenu() {
  if (aiWuStore.streaming) return
  if (!aiWuStore.modelsLoaded) {
    aiWuStore.loadModels()
  }
  if (!aiWuStore.models || aiWuStore.models.length === 0) {
    uni.showToast({ title: '暂无可切换模型', icon: 'none' })
    return
  }
  modelMenuVisible.value = !modelMenuVisible.value
}

function selectModel(id: number) {
  aiWuStore.selectedModelId = id
  modelMenuVisible.value = false
  const model = aiWuStore.models.find((m) => m.id === id)
  if (model) {
    uni.showToast({ title: `已切换至 ${model.modelName || model.name}`, icon: 'none' })
  }
}

watch(
  () => aiWuStore.panelVisible,
  (visible) => {
    if (!visible) {
      modelMenuVisible.value = false
      historyVisible.value = false
    }
    // 抽屉打开时锁定页面滚动，防止内层滚到边界后穿透滚动外层页面
    // #ifdef H5
    document.documentElement.style.overflow = visible ? 'hidden' : ''
    document.body.style.overflow = visible ? 'hidden' : ''
    // #endif
  },
)

// 面板开着时组件被卸载（如切页）需解除页面滚动锁定
onUnmounted(() => {
  // #ifdef H5
  document.documentElement.style.overflow = ''
  document.body.style.overflow = ''
  // #endif
})

function scrollToBottom() {
  scrollIntoId.value = ''
  nextTick(() => {
    scrollIntoId.value = 'aiwu-bottom-anchor'
  })
}

// 用户是否贴近底部（scroll-view 无法直接量高，靠 @scroll 事件实时计算）
const nearBottom = ref(true)

function onBodyScroll(e: { detail: { scrollTop: number; scrollHeight: number } }) {
  const { scrollTop, scrollHeight } = e.detail
  // 视口高度约占屏 60%，用 rpx 换算不可靠，直接用窗口高度估算
  const viewH = uni.getWindowInfo().windowHeight * 0.6
  nearBottom.value = scrollHeight - scrollTop - viewH < 120
}

// 新消息入列：强制滚底，并重新进入跟随模式
watch(
  () => aiWuStore.messages.length,
  () => {
    nearBottom.value = true
    scrollToBottom()
  },
)

// 流式增量：用户往上滚阅后不再强制拉回底部
watch(
  () => aiWuStore.messages[aiWuStore.messages.length - 1]?.content,
  () => {
    if (aiWuStore.streaming && nearBottom.value) scrollToBottom()
  },
)

// 流式结束：若仍在跟随模式则滚到完整回答底部
watch(
  () => aiWuStore.streaming,
  (val) => {
    if (!val && nearBottom.value) scrollToBottom()
  },
)

watch(
  () => aiWuStore.panelVisible,
  (visible) => {
    if (visible) scrollToBottom()
  },
)

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || aiWuStore.streaming) return
  inputText.value = ''
  await aiWuStore.send(text)
}

function sendQuick(q: string) {
  if (aiWuStore.streaming || (aiWuStore.modelsLoaded && aiWuStore.models.length === 0)) return
  aiWuStore.send(q)
}

function handleClear() {
  if (aiWuStore.messages.length === 0) return
  aiWuStore.clear()
}

// ---------- 历史对话 ----------
const historyVisible = ref(false)
const historyLoading = ref(false)
const conversations = ref<AiConversationVO[]>([])

async function toggleHistory() {
  historyVisible.value = !historyVisible.value
  if (!historyVisible.value) return
  modelMenuVisible.value = false
  historyLoading.value = true
  try {
    const res = await listConversations()
    conversations.value = res.data || []
  } finally {
    historyLoading.value = false
  }
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

<!-- v-html 产物不带 data-v 属性，uni-app 会给组件内所有 style 块强制加 scoped，
     Markdown 正文样式统一挂在 App.vue 引入的 chat-markdown-global.scss 全局样式中 -->
<style lang="scss" scoped>
/* ---------- 悬浮球 ---------- */
.aiwu-ball {
  position: fixed;
  right: 32rpx;
  bottom: calc(180rpx + env(safe-area-inset-bottom));
  z-index: 9999;
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  border: 2rpx solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.5);
  transition: transform 0.25s, background-color 0.25s;

  &__icon-inner {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__close {
    font-size: 36rpx;
    color: #ffffff;
    font-weight: 700;
  }

  &__wand {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &.is-open {
    background: linear-gradient(135deg, #334155 0%, #0f172a 100%);
    box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.4);
  }

  &__halo {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: inherit;
    animation: aiwu-pulse 2.6s ease-out infinite;
    pointer-events: none;
  }

  &.is-open &__halo {
    animation: none;
  }

  &.is-streaming &__halo {
    animation-duration: 1.2s;
  }
}

@keyframes aiwu-pulse {
  0% {
    transform: scale(1);
    opacity: 0.55;
  }
  70%,
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

/* ---------- 抽屉 ---------- */
.aiwu-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(15, 23, 42, 0.45);
}

.aiwu-drawer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1001;
  height: 86vh;
  display: flex;
  flex-direction: column;
  border-radius: 32rpx 32rpx 0 0;
  background: #f8fafc;
  transform: translateY(105%);
  transition: transform 0.3s cubic-bezier(0.32, 0.72, 0, 1);
  overflow: hidden;

  &.is-open {
    transform: translateY(0);
  }
}

/* ---------- 头部 ---------- */
/* ---------- 头部 (石墨黑 100% 对齐 PC 图 2) ---------- */
.aiwu-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  padding: 24rpx 28rpx;
  background: #0f172a;

  &__left {
    display: flex;
    align-items: center;
    gap: 16rpx;
    min-width: 0;
  }

  &__avatar {
    position: relative;
    width: 64rpx;
    height: 64rpx;
    border-radius: 20rpx;
    background: rgba(255, 255, 255, 0.12);
    border: 2rpx solid rgba(255, 255, 255, 0.15);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32rpx;
    flex-shrink: 0;

    &:active {
      background: rgba(255, 255, 255, 0.22);
    }
  }

  /* 头像右下角小时钟角标：提示可点击查看历史对话 */
  &__avatar-badge {
    position: absolute;
    right: -8rpx;
    bottom: -8rpx;
    width: 30rpx;
    height: 30rpx;
    border-radius: 50%;
    background: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.25);
  }

  &__title {
    display: flex;
    flex-direction: column;
    min-width: 0;

    .title {
      font-size: 28rpx;
      font-weight: 600;
      color: #fff;
    }

    .subtitle {
      font-size: 20rpx;
      color: rgba(255, 255, 255, 0.8);
      max-width: 220rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

.model-picker-wrap {
  position: relative;
}

.model-dropdown-popover {
  position: absolute;
  top: calc(100% + 12rpx);
  right: 0;
  z-index: 1010;
  min-width: 280rpx;
  max-width: 400rpx;
  background: #1e293b;
  border: 2rpx solid rgba(255, 255, 255, 0.16);
  border-radius: 20rpx;
  box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.6);
  padding: 10rpx 0;
  overflow: hidden;

  .popover-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
    padding: 18rpx 24rpx;
    font-size: 24rpx;
    color: #cbd5e1;
    transition: background 0.2s;

    &:active {
      background: rgba(255, 255, 255, 0.1);
    }

    &.is-active {
      color: #6366f1;
      font-weight: 600;
      background: rgba(99, 102, 241, 0.15);
    }

    .popover-name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .popover-check {
      font-size: 24rpx;
      color: #6366f1;
      font-weight: 700;
    }
  }
}

  &__actions {
    display: flex;
    align-items: center;
    gap: 12rpx;
    flex-shrink: 0;
  }

  &__btn {
    height: 52rpx;
    padding: 0 18rpx;
    border-radius: 14rpx;
    background: rgba(255, 255, 255, 0.12);
    border: 2rpx solid rgba(255, 255, 255, 0.15);
    display: flex;
    align-items: center;
    gap: 6rpx;
    font-size: 22rpx;
    color: #fff;

    &.close {
      width: 52rpx;
      padding: 0;
      justify-content: center;
    }

    &.plus {
      width: 52rpx;
      padding: 0;
      justify-content: center;
      .plus-txt {
        font-size: 32rpx;
        line-height: 1;
        font-weight: 500;
      }
    }

    &.model {
      max-width: 380rpx;
    }

    .model-name {
      max-width: 320rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .arrow {
      opacity: 0.8;
    }
  }
}

/* ---------- 消息区 ---------- */
.aiwu-body {
  flex: 1;
  min-height: 0;
  /* 滚到边界时不将滚动链传递给外层页面 */
  overscroll-behavior: contain;
  padding: 24rpx;
  box-sizing: border-box;
}

/* ---------- 历史对话浮层 ---------- */
.aiwu-history {
  position: absolute;
  top: 112rpx;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  display: flex;
  flex-direction: column;
  background: #f8fafc;

  &__head {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 28rpx 32rpx 20rpx;
    border-bottom: 2rpx solid rgba(226, 232, 240, 0.8);
  }

  &__head-title {
    display: flex;
    align-items: center;
    gap: 12rpx;

    text {
      font-size: 30rpx;
      font-weight: 700;
      color: #0f172a;
    }

    .count {
      min-width: 36rpx;
      height: 34rpx;
      padding: 0 12rpx;
      border-radius: 17rpx;
      background: rgba(99, 102, 241, 0.12);
      color: #6366f1;
      font-size: 22rpx;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  &__close {
    width: 52rpx;
    height: 52rpx;
    border-radius: 14rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #94a3b8;
    font-size: 30rpx;

    &:active {
      background: rgba(148, 163, 184, 0.15);
    }
  }

  &__body {
    flex: 1;
    min-height: 0;
    padding: 16rpx 24rpx 32rpx;
    box-sizing: border-box;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 20rpx;
    padding: 22rpx 24rpx;
    margin-bottom: 14rpx;
    border: 2rpx solid transparent;
    border-radius: 20rpx;
    background: #ffffff;
    box-shadow: 0 4rpx 14rpx rgba(15, 23, 42, 0.03);

    &:active {
      background: #f1f5f9;
    }

    &.is-active {
      background: rgba(99, 102, 241, 0.08);
      border-color: rgba(99, 102, 241, 0.3);
    }

    .item-icon {
      width: 60rpx;
      height: 60rpx;
      border-radius: 18rpx;
      background: rgba(99, 102, 241, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .item-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 6rpx;
      min-width: 0;
    }

    .item-title {
      font-size: 27rpx;
      font-weight: 600;
      color: #1e293b;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-meta {
      font-size: 21rpx;
      color: #94a3b8;
    }

    .item-tag {
      flex-shrink: 0;
      padding: 4rpx 14rpx;
      border-radius: 12rpx;
      background: #6366f1;
      color: #fff;
      font-size: 20rpx;
      font-weight: 600;
    }

    .item-arrow {
      flex-shrink: 0;
      font-size: 40rpx;
      color: #cbd5e1;
      font-weight: 300;
    }
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 120rpx 40rpx;
    gap: 12rpx;

    .empty-title {
      margin-top: 12rpx;
      font-size: 27rpx;
      font-weight: 600;
      color: #94a3b8;
    }

    .empty-desc {
      font-size: 22rpx;
      color: #cbd5e1;
      text-align: center;
    }
  }
}

.aiwu-anchor {
  height: 2rpx;
}

/* ---------- 欢迎屏与快捷卡片 (100% 对齐 PC) ---------- */
.welcome-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 40rpx 16rpx 20rpx;
}

.welcome-hero {
  position: relative;
  margin-bottom: 24rpx;
}

.welcome-avatar-glow {
  position: absolute;
  inset: -12rpx;
  border-radius: 40rpx;
  background: #0f172a;
  opacity: 0.45;
  filter: blur(16rpx);
}

.welcome-avatar {
  position: relative;
  z-index: 1;
  width: 104rpx;
  height: 104rpx;
  border-radius: 32rpx;
  background: #0f172a;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.4);
}

.welcome-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.welcome-desc {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #64748b;
  line-height: 1.5;
}

.quick-section {
  width: 100%;
  margin-top: 40rpx;
  text-align: left;
}

.quick-header {
  display: flex;
  align-items: center;
  gap: 10rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 20rpx;

  &-text {
    font-size: 24rpx;
    color: #0f172a;
    font-weight: 700;
  }
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  width: 100%;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 28rpx;
  background: #ffffff;
  border: 2rpx solid #e2e8f0;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 14rpx rgba(15, 23, 42, 0.03);
  transition: all 0.2s ease;

  &:active {
    background: #f1f5f9;
    border-color: #0f172a;
    transform: scale(0.99);
  }

  &-icon {
    width: 64rpx;
    height: 64rpx;
    border-radius: 20rpx;
    background: #f8fafc;
    border: 2rpx solid #e2e8f0;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4rpx;
    min-width: 0;
  }

  &-title {
    font-size: 26rpx;
    font-weight: 600;
    color: #0f172a;
  }

  &-desc {
    font-size: 20rpx;
    color: #64748b;
  }

  &-arrow {
    font-size: 36rpx;
    color: #94a3b8;
    font-weight: 300;
  }
}

.aiwu-row {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  margin-bottom: 22rpx;

  &.is-user {
    justify-content: flex-end;
  }
}

.aiwu-avatar {
  width: 52rpx;
  height: 52rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  overflow: hidden;

  &.ai {
    background: #0f172a;
  }

  &.user {
    background: #e2e8f0;
    color: #475569;
    font-weight: 600;
  }

  &.user-img {
    border-radius: 16rpx;
  }
}

.aiwu-bubble {
  max-width: 76%;
  padding: 16rpx 22rpx;
  border-radius: 24rpx;
  font-size: 26rpx;
  line-height: 1.65;
  word-break: break-word;

  .is-ai & {
    background: #fff;
    color: #1e293b;
    border: 2rpx solid rgba(226, 232, 240, 0.9);
    border-top-left-radius: 8rpx;
    box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.04);
  }

  .is-user & {
    background: #0f172a;
    color: #fff;
    border-top-right-radius: 8rpx;
  }

  &.is-error {
    background: #fef2f2 !important;
    color: #dc2626 !important;
    border-color: #fecaca !important;
  }

  &__text {
    white-space: pre-wrap;
  }
}

/* AI 回答复制按钮（移动端无 hover，回答完成后常驻右下） */
.aiwu-copy-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 10rpx;
}

.aiwu-copy-chip {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 16rpx;
  border: 2rpx solid #e2e8f0;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.08);
  font-size: 21rpx;
  color: #64748b;
  line-height: 1;

  &:active {
    color: #6366f1;
    border-color: rgba(99, 102, 241, 0.4);
    background: #ffffff;
  }

  &.is-copied {
    color: #16a34a;
    border-color: rgba(22, 163, 74, 0.35);
  }
}

/* L3 工具调用状态提示 */
.aiwu-tool-status {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  margin: 4rpx 0 8rpx 66rpx;
  padding: 8rpx 20rpx;
  border-radius: 14rpx;
  background: rgba(99, 102, 241, 0.1);

  text {
    font-size: 22rpx;
    color: #6366f1;
    font-weight: 500;
  }
}

.aiwu-typing {
  display: flex;
  align-items: center;
  gap: 8rpx;
  height: 30rpx;

  .dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background: #94a3b8;
    animation: aiwu-bounce 1.2s ease-in-out infinite;

    &:nth-child(2) {
      animation-delay: 0.15s;
    }

    &:nth-child(3) {
      animation-delay: 0.3s;
    }
  }
}

@keyframes aiwu-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-8rpx);
    opacity: 1;
  }
}

/* ---------- 输入区 ---------- */
.aiwu-footer {
  flex-shrink: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: rgba(248, 250, 252, 0.96);
  border-top: 2rpx solid rgba(226, 232, 240, 0.8);
}

.aiwu-input-wrap {
  display: flex;
  align-items: center;
  gap: 14rpx;
  background: #fff;
  border: 2rpx solid #e2e8f0;
  border-radius: 24rpx;
  padding: 10rpx 12rpx 10rpx 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.02);
}

.aiwu-input {
  flex: 1;
  font-size: 26rpx;
  line-height: 1.5;
  color: #1e293b;
  min-height: 48rpx;
  max-height: 180rpx;
  padding: 8rpx 0;
  margin: 0;
  box-sizing: border-box;
}

:deep(.aiwu-input-placeholder),
.aiwu-input-placeholder {
  color: #94a3b8;
}

.aiwu-send {
  width: 64rpx;
  height: 64rpx;
  flex-shrink: 0;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #0f172a;
  box-shadow: 0 4rpx 14rpx rgba(15, 23, 42, 0.2);
  transition: all 0.2s ease;

  &.disabled {
    opacity: 0.35;
    box-shadow: none;
  }

  &.stop {
    background: #ef4444;
  }

  .stop-square {
    width: 20rpx;
    height: 20rpx;
    border-radius: 4rpx;
    background: #fff;
  }
}
</style>
