<template>
  <transition name="aiwu-history">
    <div v-if="visible" ref="panelRef" class="aiwu-history-panel">
      <div class="aiwu-history-head">
        <span class="aiwu-history-title">
          <el-icon :size="14"><Clock /></el-icon>
          历史对话
          <span v-if="sessions.length" class="history-count">共 {{ sessions.length }} 条</span>
        </span>
        <div class="aiwu-history-actions">
          <button
            v-if="sessions.length"
            class="aiwu-history-clear-btn"
            type="button"
            @click="emit('clear-all')"
          >
            清空全部
          </button>
          <button class="aiwu-history-new-btn" type="button" @click="emit('new-session')">
            新会话
          </button>
        </div>
      </div>

      <div v-if="loading" class="aiwu-history-empty">正在加载历史会话…</div>
      <div v-else-if="sessions.length" class="aiwu-history-list">
        <div
          v-for="item in sessions"
          :key="item.conversationId"
          class="aiwu-history-item"
          :class="{ 'is-current': item.conversationId === currentId }"
          role="button"
          tabindex="0"
          @click="emit('select', item)"
          @keydown.enter.prevent="emit('select', item)"
        >
          <div class="aiwu-history-item-content">
            <span class="aiwu-history-item-title" :title="item.title">{{ item.title || '未命名会话' }}</span>
            <span class="aiwu-history-item-time">
              {{ formatTime(item.lastTime) }}
              <template v-if="item.messageCount"> · {{ item.messageCount }} 轮对话</template>
            </span>
          </div>
          <el-tooltip content="删除会话" placement="top" :show-after="1000">
            <button
              class="aiwu-history-delete"
              type="button"
              aria-label="删除会话"
              @click.stop="emit('delete', item)"
            >
              <el-icon :size="13"><Delete /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>
      <div v-else class="aiwu-history-empty">还没有历史对话，和 AI wu助手聊聊吧</div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Clock, Delete } from '@element-plus/icons-vue'
import type { AiConversationVO } from '@/api/ai'

const props = defineProps<{
  visible: boolean
  loading: boolean
  sessions: AiConversationVO[]
  currentId: string
}>()

const emit = defineEmits<{
  (e: 'select', item: AiConversationVO): void
  (e: 'delete', item: AiConversationVO): void
  (e: 'clear-all'): void
  (e: 'new-session'): void
  (e: 'close'): void
}>()

const panelRef = ref<HTMLElement>()

function formatTime(raw?: string): string {
  if (!raw) return ''
  const date = new Date(raw.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return raw
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const sameDay = (a: Date, b: Date) =>
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  if (sameDay(date, now)) return `今天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (sameDay(date, yesterday)) return `昨天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** 点击面板外部收起历史浮层（Esc 由根组件统一处理，避免同时关闭抽屉） */
function handlePointerDown(event: PointerEvent) {
  if (!props.visible) return
  const target = event.target as HTMLElement | null
  if (!target) return
  if (panelRef.value?.contains(target)) return
  if (target.closest('[data-aiwu-history-trigger]')) return
  if (target.closest('.el-overlay, .el-message-box, .el-popper')) return
  emit('close')
}

onMounted(() => {
  document.addEventListener('pointerdown', handlePointerDown, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handlePointerDown, true)
})
</script>
