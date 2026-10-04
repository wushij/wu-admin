<template>
  <div class="ai-wu-panel">
    <!-- 头部 -->
    <div class="panel-header">
      <div class="header-left">
        <div class="ai-avatar">
          <AiCompassIcon :size="24" :dark="true" :spin="true" />
        </div>
        <div class="header-title">
          <div class="title">AI wu助手</div>
          <div class="subtitle">系统专属智能副驾 · 贴心指引答疑</div>
        </div>
      </div>
      <div class="header-actions">
        <div class="header-toolbar" role="toolbar" aria-label="操作">
          <el-tooltip :content="historyVisible ? '收起历史会话' : '历史对话记录'" placement="bottom">
            <button
              class="header-btn"
              :class="{ 'is-active': historyVisible }"
              type="button"
              aria-label="历史对话记录"
              @click="toggleHistory"
            >
              <el-icon :size="15"><Clock /></el-icon>
            </button>
          </el-tooltip>

          <span class="header-toolbar-sep" aria-hidden="true" />

          <el-tooltip content="开启新会话" placement="bottom">
            <button
              class="header-btn"
              type="button"
              aria-label="新对话"
              @click="handleClear"
            >
              <el-icon :size="15"><Plus /></el-icon>
            </button>
          </el-tooltip>

          <span class="header-toolbar-sep" aria-hidden="true" />

          <el-tooltip content="收起面板" placement="bottom">
            <button
              class="header-btn header-btn--close"
              type="button"
              aria-label="收起"
              @click="aiWuStore.closePanel()"
            >
              <el-icon :size="15"><Close /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>
    </div>

    <!-- 历史对话浮层 -->
    <transition name="history-slide">
      <div v-if="historyVisible" class="history-layer">
        <div class="history-head">
          <div class="history-head-title">
            <el-icon :size="15"><Clock /></el-icon>
            <span>历史对话</span>
            <span v-if="conversations.length" class="history-count">{{ conversations.length }}</span>
          </div>
          <div class="history-head-tools">
            <el-tooltip v-if="conversations.length" content="清空所有记录" placement="bottom" :show-after="300">
              <button
                class="history-tool-btn is-danger"
                type="button"
                aria-label="清空所有记录"
                @click="handleClearAll"
              >
                <el-icon :size="14"><Delete /></el-icon>
              </button>
            </el-tooltip>
            <button class="history-tool-btn" type="button" title="关闭" aria-label="关闭" @click="historyVisible = false">
              <el-icon :size="14"><Close /></el-icon>
            </button>
          </div>
        </div>
        <div v-loading="historyLoading" class="history-body">
          <template v-if="conversations.length">
            <div
              v-for="conv in conversations"
              :key="conv.conversationId"
              class="history-item"
              :class="{ 'is-active': conv.conversationId === aiWuStore.conversationId }"
              role="button"
              tabindex="0"
              @click="handleRestore(conv.conversationId)"
              @keydown.enter.prevent="handleRestore(conv.conversationId)"
            >
              <div class="history-item-icon">
                <el-icon :size="15"><ChatDotRound /></el-icon>
              </div>
              <div class="history-item-main">
                <div class="history-item-title">{{ conv.title }}</div>
                <div class="history-item-meta">
                  <span>{{ formatConvTime(conv.lastTime) }}</span>
                  <span class="meta-dot">·</span>
                  <span>{{ conv.messageCount || 0 }} 轮对话</span>
                </div>
              </div>
              <div class="history-item-tail" @click.stop>
                <span v-if="conv.conversationId === aiWuStore.conversationId" class="history-item-tag">当前</span>
                <div class="history-action-slot">
                  <el-icon
                    v-if="conv.conversationId !== aiWuStore.conversationId"
                    class="history-item-arrow"
                    :size="13"
                  >
                    <ArrowRight />
                  </el-icon>
                  <el-tooltip content="删除此对话" placement="top" :show-after="300">
                    <button
                      class="history-item-del-btn"
                      type="button"
                      aria-label="删除此对话"
                      @click.stop="handleDelete(conv)"
                    >
                      <el-icon :size="13"><Delete /></el-icon>
                    </button>
                  </el-tooltip>
                </div>
              </div>
            </div>
          </template>
          <div v-else-if="!historyLoading" class="history-empty">
            <el-icon :size="34"><ChatDotRound /></el-icon>
            <p>还没有历史对话</p>
            <span>和 AI wu助手聊聊，记录会自动保存在这里</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- 消息区 -->
    <div ref="msgListRef" class="panel-body">
      <!-- 欢迎屏 -->
      <div v-if="aiWuStore.messages.length === 0" class="welcome">
        <div class="welcome-hero">
          <div class="welcome-avatar-glow" />
          <div class="welcome-avatar">
            <AiCompassIcon :size="38" :dark="true" :spin="true" />
          </div>
        </div>
        <div class="welcome-title">你好，我是 AI wu助手</div>
        <div class="welcome-desc">
          {{ aiWuStore.modelsLoaded && aiWuStore.models.length === 0 ? '暂无可用模型，请联系管理员配置' : '我是本系统的智能助手，熟悉各功能模块与操作路径，有问题尽管问我～' }}
        </div>
        <div class="quick-section">
          <div class="quick-header">
            <el-icon :size="14"><Opportunity /></el-icon>
            <span>常见问题快速提问</span>
          </div>
          <div class="quick-list">
            <button
              v-for="(q, idx) in QUICK_QUESTIONS"
              :key="idx"
              class="quick-card"
              type="button"
              :disabled="aiWuStore.models.length === 0"
              @click="sendQuick(q.text)"
            >
              <div class="quick-card-icon">
                <component :is="q.icon" />
              </div>
              <div class="quick-card-content">
                <div class="quick-card-title">{{ q.text }}</div>
                <div class="quick-card-desc">{{ q.desc }}</div>
              </div>
              <el-icon class="quick-card-arrow" :size="14"><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
      </div>

      <!-- 消息气泡 -->
      <div
        v-for="msg in aiWuStore.messages"
        :key="msg.id"
        class="msg-row"
        :class="msg.role === 'user' ? 'is-user' : 'is-ai'"
      >
        <div v-if="msg.role === 'assistant'" class="msg-avatar ai">
          <AiCompassIcon :size="18" :dark="true" :spin="false" />
        </div>
        <div class="bubble" :class="{ 'is-error': msg.error }">
          <!-- 等待首字节：三点跳动 -->
          <span v-if="msg.streaming && !msg.content" class="typing-dots">
            <i /><i /><i />
          </span>
          <!-- AI 回答：Markdown 排版渲染 -->
          <AiWuMarkdown
            v-else-if="msg.role === 'assistant' && !msg.error"
            :content="msg.content"
            :streaming="msg.streaming"
          />
          <template v-else>
            <span class="bubble-text">{{ msg.content }}</span>
            <span v-if="msg.streaming" class="cursor-blink" />
          </template>
          <!-- AI 回答完成后悬浮显示复制按钮 -->
          <button
            v-if="msg.role === 'assistant' && !msg.streaming && !msg.error && msg.content"
            class="copy-chip"
            type="button"
            :title="copiedId === msg.id ? '已复制' : '复制回答'"
            @click="copyMessage(msg.content, msg.id)"
          >
            <el-icon :size="13"><Check v-if="copiedId === msg.id" /><CopyDocument v-else /></el-icon>
            <span>{{ copiedId === msg.id ? '已复制' : '复制' }}</span>
          </button>
        </div>
        <div v-if="msg.role === 'user'" class="msg-avatar user">
          <img v-if="userAvatar" :src="userAvatar" alt="" />
          <el-icon v-else :size="14"><User /></el-icon>
        </div>
      </div>

      <!-- L3 工具调用状态提示 -->
      <div v-if="aiWuStore.streaming && aiWuStore.statusHint" class="tool-status">
        <span class="tool-status-dot" />
        <span>{{ aiWuStore.statusHint }}</span>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="panel-footer">
      <div class="input-wrap" :class="{ 'is-multiline': isMultiLine }">
        <textarea
          ref="inputRef"
          v-model="inputText"
          class="chat-input"
          rows="1"
          maxlength="4000"
          :placeholder="aiWuStore.models.length === 0 && aiWuStore.modelsLoaded ? '暂无可用模型' : '输入问题，Enter 发送，Shift+Enter 换行'"
          :disabled="aiWuStore.modelsLoaded && aiWuStore.models.length === 0"
          @keydown.enter.exact.prevent="handleSend"
          @input="autoResize"
        />
        <button
          v-if="aiWuStore.streaming"
          class="send-btn stop"
          type="button"
          title="停止生成"
          @click="aiWuStore.stop()"
        >
          <span class="stop-square" />
        </button>
        <button
          v-else
          class="send-btn"
          type="button"
          title="发送"
          :disabled="!inputText.trim() || (aiWuStore.modelsLoaded && aiWuStore.models.length === 0)"
          @click="handleSend"
        >
          <el-icon :size="15"><Promotion /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted } from 'vue'
import {
  Plus,
  User,
  Promotion,
  CopyDocument,
  Check,
  Opportunity,
  Document,
  Timer,
  ChatDotRound,
  ArrowRight,
  Clock,
  Close,
  Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAiWuStore } from '@/store/aiWu'
import { useUserStore } from '@/store/user'
import { listConversations, deleteConversation, clearAllConversations, type AiConversationVO } from '@/api/ai'
import AiWuMarkdown from './AiWuMarkdown.vue'
import AiCompassIcon from './AiCompassIcon.vue'

const QUICK_QUESTIONS = [
  {
    icon: ChatDotRound,
    text: '这个系统有哪些功能模块？',
    desc: '按你的权限梳理可用功能与入口'
  },
  {
    icon: Document,
    text: '怎么修改登录密码？',
    desc: '获取本系统真实操作路径指引'
  },
  {
    icon: Timer,
    text: '我是什么角色，属于哪个部门？',
    desc: '查看当前账号的角色与组织信息'
  }
]

const aiWuStore = useAiWuStore()
const userStore = useUserStore()
const userAvatar = computed(() => userStore.userInfo.avatar || '')

const inputText = ref('')
const msgListRef = ref<HTMLElement>()
const inputRef = ref<HTMLTextAreaElement>()
const copiedId = ref<string | number | null>(null)
const isMultiLine = ref(false)

function scrollToBottom(force = true) {
  nextTick(() => {
    const el = msgListRef.value
    if (!el) return
    // 用户已往上滚阅时（距底 ≥100px）流式增量不强制拉回底部，保持用户可自由浏览
    const isAtBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 100
    if (force || isAtBottom) {
      el.scrollTop = el.scrollHeight
    }
  })
}

// 新消息入列：强制滚底
watch(
  () => aiWuStore.messages.length,
  () => scrollToBottom(),
)

// 流式增量：仅在用户未离开底部时跟随滚动
watch(
  () => aiWuStore.messages[aiWuStore.messages.length - 1]?.content,
  () => {
    if (aiWuStore.streaming) scrollToBottom(false)
  },
)

// 流式结束：滚到完整回答底部
watch(
  () => aiWuStore.streaming,
  (val) => {
    if (!val) scrollToBottom()
  },
)

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  const newHeight = Math.min(el.scrollHeight, 96)
  el.style.height = `${newHeight}px`
  isMultiLine.value = newHeight > 32 || inputText.value.includes('\n')
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || aiWuStore.streaming) return
  inputText.value = ''
  isMultiLine.value = false
  nextTick(() => autoResize())
  scrollToBottom(true)
  await aiWuStore.send(text)
  scrollToBottom(true)
}

function sendQuick(q: string) {
  if (aiWuStore.streaming) return
  scrollToBottom(true)
  aiWuStore.send(q)
  scrollToBottom(true)
}

function handleClear() {
  aiWuStore.clear()
}

// ---------- 历史对话 ----------
const historyVisible = ref(false)
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

async function handleRestore(conversationId: string) {
  if (aiWuStore.streaming) {
    ElMessage.warning('回答生成中，请稍后切换会话')
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
    ElMessage.warning('该会话暂无可恢复的记录')
  }
}

async function handleDelete(conv: AiConversationVO) {
  if (aiWuStore.streaming && conv.conversationId === aiWuStore.conversationId) {
    ElMessage.warning('回答生成中，请稍后再操作')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除历史对话「${conv.title || '此会话'}」吗？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    await deleteConversation(conv.conversationId)
    conversations.value = conversations.value.filter((item) => item.conversationId !== conv.conversationId)
    ElMessage.success('已删除该历史对话')
    // 若删除的恰好是当前正在显示的会话，清空面板并重置会话
    if (conv.conversationId === aiWuStore.conversationId) {
      aiWuStore.clear()
    }
  } catch {
    ElMessage.error('删除会话失败，请重试')
  }
}

async function handleClearAll() {
  if (aiWuStore.streaming) {
    ElMessage.warning('回答生成中，请稍后再操作')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确定清空全部历史对话吗？清空后所有会话记录都将永久删除。',
      '清空确认',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    await clearAllConversations()
    conversations.value = []
    ElMessage.success('已清空所有历史对话')
    aiWuStore.clear()
  } catch {
    ElMessage.error('清空历史对话失败，请重试')
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

async function copyMessage(content: string, id: string | number) {
  try {
    await navigator.clipboard.writeText(content)
    copiedId.value = id
    ElMessage.success('已复制')
    setTimeout(() => {
      if (copiedId.value === id) {
        copiedId.value = null
      }
    }, 2000)
  } catch {
    ElMessage.error('复制失败')
  }
}

onMounted(() => {
  if (!aiWuStore.modelsLoaded) {
    aiWuStore.loadModels()
  }
  scrollToBottom()
  inputRef.value?.focus()
})
</script>

<style scoped lang="scss">
.ai-wu-panel {
  position: fixed;
  right: 32px;
  bottom: 104px;
  z-index: 2000;
  width: 390px;
  height: 580px;
  max-height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 24px 60px -12px rgba(var(--theme-primary-rgb, 99, 102, 241), 0.2), 0 12px 24px -8px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

/* ---------- 头部 ---------- */
.panel-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(145deg, #101c38 0%, #0c1222 100%);
  border-bottom: 1px solid rgba(22, 119, 255, 0.2);
  color: #fff;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.25);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.ai-avatar {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1.5px solid rgba(22, 119, 255, 0.45);
  background: linear-gradient(145deg, #101c38, #0c1222);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3), 0 0 10px rgba(22, 119, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.25s ease;

  &:hover {
    transform: scale(1.06);
    border-color: rgba(64, 150, 255, 0.85);
    box-shadow: 0 0 16px rgba(22, 119, 255, 0.4);
  }
}

/* ---------- 历史对话浮层 ---------- */
.history-layer {
  position: absolute;
  top: 64px;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(16px);
  border-radius: 0 0 20px 20px;
}

.history-slide-enter-active,
.history-slide-leave-active {
  transition: opacity 0.22s ease, transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.history-slide-enter-from,
.history-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.history-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
}

.history-head-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;

  .history-count {
    min-width: 20px;
    height: 18px;
    padding: 0 6px;
    border-radius: 9px;
    background: var(--theme-primary-muted, rgba(99, 102, 241, 0.12));
    color: var(--theme-primary, #6366f1);
    font-size: 11px;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}

.history-layer {
  position: absolute;
  top: 64px;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  border-radius: 0 0 20px 20px;
}

.history-slide-enter-active,
.history-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.history-slide-enter-from,
.history-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.history-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #ffffff;
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
}

.history-head-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;

  .history-count {
    min-width: 20px;
    height: 18px;
    padding: 0 6px;
    border-radius: 9px;
    background: rgba(148, 163, 184, 0.16);
    color: #475569;
    font-size: 11px;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}

.history-head-tools {
  display: flex;
  align-items: center;
  gap: 4px;
}

.history-tool-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.18s ease;

  &:hover {
    background: rgba(148, 163, 184, 0.15);
    color: #334155;
  }

  &.is-danger:hover {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
  }
}

.history-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #f8fafc;

  &::-webkit-scrollbar {
    width: 5px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(148, 163, 184, 0.3);
    border-radius: 10px;
  }
}

.history-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
  cursor: pointer;
  text-align: left;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background: #ffffff;
    border-color: rgba(203, 213, 225, 0.95);
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
    transform: translateY(-1px);

    .history-item-arrow {
      opacity: 0;
      transform: scale(0.7);
    }

    .history-item-del-btn {
      opacity: 1;
      pointer-events: auto;
    }
  }

  &.is-active {
    border-color: var(--theme-primary, #6366f1);
    background: #ffffff;
    box-shadow: 0 0 0 1px var(--theme-primary, #6366f1), 0 3px 10px rgba(99, 102, 241, 0.08);

    .history-item-icon {
      background: var(--theme-primary, #6366f1);
      color: #ffffff;
    }
  }
}

.history-item-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.history-item-main {
  flex: 1;
  min-width: 0;
}

.history-item-title {
  font-size: 13.5px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-meta {
  margin-top: 3px;
  font-size: 11px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 4px;

  .meta-dot {
    opacity: 0.5;
  }
}

.history-item-tail {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.history-item-tag {
  padding: 2px 7px;
  border-radius: 6px;
  background: var(--theme-primary, #6366f1);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.history-action-slot {
  position: relative;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-item-arrow {
  position: absolute;
  color: #cbd5e1;
  transition: opacity 0.18s ease, transform 0.18s ease;
  pointer-events: none;
}

.history-item-del-btn {
  position: absolute;
  inset: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  pointer-events: none;
  transition: all 0.18s ease;

  &:hover {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
    transform: scale(1.08);
  }
}

.history-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #cbd5e1;
  padding-bottom: 30px;

  p {
    margin: 6px 0 0;
    font-size: 13px;
    font-weight: 600;
    color: #94a3b8;
  }

  span {
    font-size: 11px;
    color: #cbd5e1;
  }
}

.header-title {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;

  .title {
    font-size: 15px;
    font-weight: 700;
    line-height: 1.3;
    letter-spacing: 0.3px;
    color: #f8fafc;
    white-space: nowrap;
  }

  .subtitle {
    font-size: 11px;
    color: #94a3b8;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 200px;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* 顶部操作工具栏胶囊 */
.header-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 5px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(59, 130, 246, 0.35);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 2px 8px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.header-toolbar-sep {
  width: 1px;
  height: 14px;
  margin: 0 1px;
  background: rgba(255, 255, 255, 0.15);
  flex-shrink: 0;
}

.header-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: transparent;
  border: none;
  color: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background: rgba(59, 130, 246, 0.3);
    color: #ffffff;
    box-shadow: 0 0 10px rgba(59, 130, 246, 0.45);
    transform: scale(1.08);
  }

  &:active {
    transform: scale(0.94);
  }

  &.is-active {
    background: linear-gradient(135deg, #1677ff 0%, #3b82f6 100%);
    color: #ffffff;
    box-shadow: 0 0 12px rgba(37, 99, 235, 0.6);
  }

  &--close:hover {
    background: rgba(239, 68, 68, 0.25);
    color: #fca5a5;
    box-shadow: 0 0 10px rgba(239, 68, 68, 0.4);
  }
}

/* ---------- 消息区 ---------- */
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.5) 0%, rgba(241, 245, 249, 0.3) 100%);

  &::-webkit-scrollbar {
    width: 5px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(148, 163, 184, 0.3);
    border-radius: 10px;

    &:hover {
      background: rgba(148, 163, 184, 0.5);
    }
  }
}

/* ---------- 欢迎屏 ---------- */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px 8px 10px;
}

.welcome-hero {
  position: relative;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-avatar-glow {
  position: absolute;
  inset: -8px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.45) 0%, rgba(114, 46, 209, 0.25) 55%, transparent 75%);
  filter: blur(12px);
  animation: pulse-glow 3s infinite alternate ease-in-out;
}

@keyframes pulse-glow {
  0% {
    transform: scale(0.92);
    opacity: 0.35;
  }
  100% {
    transform: scale(1.15);
    opacity: 0.65;
  }
}

.welcome-avatar {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(145deg, #101c38, #0c1222);
  border: 1.6px solid rgba(22, 119, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.35), 0 0 16px rgba(114, 46, 209, 0.25);
  transition: transform 0.3s ease;

  &:hover {
    transform: scale(1.05);
    border-color: rgba(64, 150, 255, 0.85);
  }
}

.welcome-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.2px;
}

.welcome-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  max-width: 280px;
}

.quick-section {
  margin-top: 20px;
  width: 100%;
}

.quick-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--theme-primary, #6366f1);
  margin-bottom: 10px;
  padding-left: 4px;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
  width: 100%;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-radius: 14px;
  padding: 10px 12px;
  cursor: pointer;
  text-align: left;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);

  &:hover:not(:disabled) {
    background: #ffffff;
    border-color: var(--theme-primary-muted-strong, rgba(99, 102, 241, 0.35));
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);

    .quick-card-arrow {
      opacity: 1;
      color: var(--theme-primary, #6366f1);
    }
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.quick-card-icon {
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  color: var(--theme-primary, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.quick-card-content {
  flex: 1;
  min-width: 0;
}

.quick-card-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.3;
}

.quick-card-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-card-arrow {
  color: #cbd5e1;
  opacity: 0.6;
  flex-shrink: 0;
  transition: all 0.2s;
}

/* ---------- 消息气泡 ---------- */
.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;

  &.is-user {
    justify-content: flex-end;
  }
}

.msg-avatar {
  width: 28px;
  height: 28px;
  margin-top: 2px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);

  &.ai {
    background: linear-gradient(145deg, #101c38, #0c1222);
    border: 1px solid rgba(22, 119, 255, 0.45);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
    color: #fff;
  }

  &.user {
    background: #e2e8f0;
    color: #64748b;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 50%;
  }
}

.bubble {
  position: relative;
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.65;
  word-break: break-word;
  transition: all 0.2s;

  .is-ai & {
    max-width: 86%;
    background: #ffffff;
    color: #1e293b;
    border: 1px solid rgba(226, 232, 240, 0.9);
    border-top-left-radius: 4px;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  }

  .is-user & {
    background: linear-gradient(135deg, var(--theme-primary, #6366f1) 0%, var(--theme-logo-end, var(--theme-primary, #8b5cf6)) 100%);
    color: #fff;
    border-top-right-radius: 4px;
    box-shadow: 0 4px 14px rgba(var(--theme-primary-rgb, 99, 102, 241), 0.25);
  }

  &.is-error {
    background: #fef2f2 !important;
    color: #dc2626 !important;
    border-color: #fecaca !important;
  }
}

.bubble-text {
  white-space: pre-wrap;
}

/* AI 回答复制按钮：悬浮气泡时优雅出场 */
.copy-chip {
  position: absolute;
  right: 8px;
  bottom: 8px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(4px);
  color: #64748b;
  font-size: 11px;
  cursor: pointer;
  opacity: 0;
  transform: translateY(4px) scale(0.95);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);

  &:hover {
    color: var(--theme-primary, #6366f1);
    border-color: var(--theme-primary-muted-strong, rgba(99, 102, 241, 0.4));
    background: #ffffff;
  }
}

.msg-row:hover .copy-chip {
  opacity: 1;
  transform: translateY(0) scale(1);
}

/* 流式输出光标 */
.cursor-blink {
  display: inline-block;
  width: 2px;
  height: 14px;
  margin-left: 2px;
  vertical-align: -2px;
  background: currentColor;
  animation: ai-cursor 0.9s steps(2) infinite;
}

@keyframes ai-cursor {
  50% {
    opacity: 0;
  }
}

/* 三点跳动 */
.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 18px;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--theme-primary, #6366f1);
    animation: ai-bounce 1.2s ease-in-out infinite;

    &:nth-child(2) {
      animation-delay: 0.15s;
    }

    &:nth-child(3) {
      animation-delay: 0.3s;
    }
  }
}

/* L3 工具调用状态提示 */
.tool-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  margin-left: 36px;
  padding: 5px 12px;
  border-radius: 10px;
  background: var(--theme-primary-muted, rgba(99, 102, 241, 0.1));
  color: var(--theme-primary, #6366f1);
  font-size: 12px;
  font-weight: 500;
}

.tool-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: ai-bounce 1s ease-in-out infinite;
}

@keyframes ai-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

/* ---------- 输入区 ---------- */
.panel-footer {
  flex-shrink: 0;
  padding: 12px 14px 10px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(226, 232, 240, 0.8);
}

.input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 16px;
  padding: 6px 6px 6px 12px;
  min-height: 48px;
  box-sizing: border-box;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.02);

  &.is-multiline {
    align-items: flex-end;
  }

  &:focus-within {
    border-color: var(--theme-primary, #6366f1);
    box-shadow: 0 0 0 3.5px var(--theme-primary-muted, rgba(99, 102, 241, 0.15));
  }
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  font-size: 13px;
  line-height: 22px;
  padding: 4px 0;
  margin: 0;
  color: #1e293b;
  max-height: 96px;
  font-family: inherit;
  box-sizing: border-box;
  display: block;

  &::placeholder {
    color: #94a3b8;
  }

  &:disabled {
    cursor: not-allowed;
  }
}

.send-btn {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: none;
  border-radius: 11px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--theme-primary, #6366f1) 0%, var(--theme-logo-end, var(--theme-primary, #8b5cf6)) 100%);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(var(--theme-primary-rgb, 99, 102, 241), 0.3);

  &:hover:not(:disabled) {
    transform: scale(1.06);
    box-shadow: 0 6px 16px rgba(var(--theme-primary-rgb, 99, 102, 241), 0.4);
  }

  &:active:not(:disabled) {
    transform: scale(0.96);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    box-shadow: none;
  }

  &.stop {
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
  }
}

.stop-square {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  background: #fff;
}

.footer-tip {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 11px;
  color: #94a3b8;
}
</style>

