<template>
  <div class="ai-wu-panel">
    <!-- 头部 -->
    <div class="panel-header">
      <div class="header-left">
        <div class="ai-avatar">
          <el-icon :size="18"><MagicStick /></el-icon>
        </div>
        <div class="header-title">
          <div class="title">AI wu助手</div>
          <div class="subtitle">{{ aiWuStore.currentModel ? aiWuStore.currentModel.name : '智能问答' }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-select
          v-if="aiWuStore.models.length > 1"
          v-model="aiWuStore.selectedModelId"
          size="small"
          class="model-select"
          :disabled="aiWuStore.streaming"
          placeholder="选择模型"
        >
          <el-option v-for="m in aiWuStore.models" :key="m.id" :label="m.modelName || m.name" :value="m.id" />
        </el-select>
        <el-tooltip content="新对话" placement="bottom">
          <button class="icon-btn" type="button" title="新对话" @click="handleClear">
            <el-icon :size="15"><Plus /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>

    <!-- 消息区 -->
    <div ref="msgListRef" class="panel-body">
      <!-- 欢迎屏 -->
      <div v-if="aiWuStore.messages.length === 0" class="welcome">
        <div class="welcome-hero">
          <div class="welcome-avatar-glow" />
          <div class="welcome-avatar">
            <el-icon :size="28"><MagicStick /></el-icon>
          </div>
        </div>
        <div class="welcome-title">你好，我是 AI wu助手</div>
        <div class="welcome-desc">
          {{ aiWuStore.modelsLoaded && aiWuStore.models.length === 0 ? '暂无可用模型，请联系管理员配置' : '我是你的全能 AI 伙伴，有什么可以帮你的吗？' }}
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
          <el-icon :size="14"><MagicStick /></el-icon>
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
  MagicStick,
  Plus,
  User,
  Promotion,
  CopyDocument,
  Check,
  Opportunity,
  Document,
  Timer,
  ChatDotRound,
  ArrowRight
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAiWuStore } from '@/store/aiWu'
import { useUserStore } from '@/store/user'
import AiWuMarkdown from './AiWuMarkdown.vue'

const QUICK_QUESTIONS = [
  {
    icon: ChatDotRound,
    text: '介绍一下这个管理系统',
    desc: '快速了解系统架构与核心功能模块'
  },
  {
    icon: Document,
    text: '帮我写一段周报总结',
    desc: '梳理本周工作要点与项目进展现状'
  },
  {
    icon: Timer,
    text: '如何提高工作效率？',
    desc: '获取数字化办公与时间管理实用建议'
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
  await aiWuStore.send(text)
}

function sendQuick(q: string) {
  if (aiWuStore.streaming) return
  aiWuStore.send(q)
}

function handleClear() {
  aiWuStore.clear()
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
  gap: 8px;
  padding: 14px 16px;
  background: linear-gradient(135deg, var(--theme-primary, #6366f1) 0%, var(--theme-logo-end, var(--theme-primary, #8b5cf6)) 100%);
  color: #fff;
  box-shadow: 0 4px 16px rgba(var(--theme-primary-rgb, 99, 102, 241), 0.15);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.header-title {
  min-width: 0;

  .title {
    font-size: 14px;
    font-weight: 700;
    line-height: 1.3;
    letter-spacing: 0.3px;
  }

  .subtitle {
    font-size: 11px;
    opacity: 0.85;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 120px;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.model-select {
  width: 165px;

  :deep(.el-select__wrapper) {
    background: rgba(255, 255, 255, 0.2);
    box-shadow: none !important;
    border-radius: 8px;
    backdrop-filter: blur(4px);
  }

  :deep(.el-select__selected-item),
  :deep(.el-select__caret) {
    color: #fff;
    font-size: 12px;
    font-weight: 500;
  }
}

.icon-btn {
  width: 30px;
  height: 30px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background: rgba(255, 255, 255, 0.3);
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
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
  margin-bottom: 14px;
}

.welcome-avatar-glow {
  position: absolute;
  inset: -6px;
  border-radius: 22px;
  background: linear-gradient(135deg, var(--theme-primary, #6366f1) 0%, var(--theme-logo-end, var(--theme-primary, #8b5cf6)) 100%);
  opacity: 0.35;
  filter: blur(10px);
  animation: pulse-glow 3s infinite alternate ease-in-out;
}

@keyframes pulse-glow {
  0% {
    transform: scale(0.95);
    opacity: 0.25;
  }
  100% {
    transform: scale(1.1);
    opacity: 0.45;
  }
}

.welcome-avatar {
  position: relative;
  width: 58px;
  height: 58px;
  border-radius: 20px;
  background: linear-gradient(135deg, var(--theme-primary, #6366f1) 0%, var(--theme-logo-end, var(--theme-primary, #8b5cf6)) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(var(--theme-primary-rgb, 99, 102, 241), 0.35);
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
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--theme-primary-muted, rgba(99, 102, 241, 0.1));
  color: var(--theme-primary, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
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
  border-radius: 10px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);

  &.ai {
    background: linear-gradient(135deg, var(--theme-primary, #6366f1) 0%, var(--theme-logo-end, var(--theme-primary, #8b5cf6)) 100%);
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

