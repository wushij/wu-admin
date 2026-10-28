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
        <!-- 空模型状态：有模型管理权限的用户提供直达入口 -->
        <el-button
          v-if="aiWuStore.modelsLoaded && aiWuStore.models.length === 0 && canManageModel"
          class="go-model-btn"
          type="primary"
          size="small"
          round
          :icon="Position"
          @click="goModelConfig"
        >
          去配置模型
        </el-button>
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
import { useRouter } from 'vue-router'
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
  Delete,
  Position
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hasPermission } from '@/directives/permission'
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
const router = useRouter()
const userAvatar = computed(() => userStore.userInfo.avatar || '')

/** 是否具备模型管理权限（决定空模型态是否展示直达入口，避免普通用户点击后被路由守卫拦截） */
const canManageModel = computed(() => hasPermission('system:ai-model:list'))

function goModelConfig() {
  router.push('/ai/model')
}

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

<style scoped lang="scss" src="./ai-wu-chat-panel.scss"></style>