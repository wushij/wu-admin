<template>
  <view v-if="visible" class="aiwu-history" @click.stop>
    <view class="aiwu-history__head">
      <view class="aiwu-history__head-title">
        <IconFont name="clock-o" :size="30" color="#0f172a" />
        <text>历史对话</text>
        <text v-if="sessions.length" class="count">{{ sessions.length }}</text>
      </view>
      <view class="aiwu-history__head-tools">
        <view
          v-if="sessions.length"
          class="aiwu-history__clear-btn"
          @click.stop="emit('clear-all')"
        >
          <text>清空</text>
        </view>
        <view class="aiwu-history__close" @click.stop="emit('close')">
          <text>✕</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="aiwu-history__body">
      <view
        v-for="conv in sessions"
        :key="conv.conversationId"
        class="aiwu-history__item"
        :class="{ 'is-active': conv.conversationId === currentId }"
        @click="emit('select', conv)"
      >
        <view class="item-icon">
          <IconFont name="chat-o" :size="28" color="#6366f1" />
        </view>
        <view class="item-main">
          <text class="item-title">{{ conv.title }}</text>
          <text class="item-meta">
            {{ formatConvTime(conv.lastTime) }} · {{ conv.messageCount || 0 }} 轮对话
          </text>
        </view>
        <view class="item-tail">
          <text v-if="conv.conversationId === currentId" class="item-tag">当前</text>
          <view class="item-del-btn" @click.stop="emit('delete', conv)">
            <text class="item-del-text">删除</text>
          </view>
          <text v-if="conv.conversationId !== currentId" class="item-arrow">›</text>
        </view>
      </view>

      <view v-if="loading && sessions.length === 0" class="aiwu-history__empty">
        <text class="empty-title">加载中…</text>
        <text class="empty-desc">正在获取历史对话记录</text>
      </view>
      <view v-else-if="!loading && sessions.length === 0" class="aiwu-history__empty">
        <IconFont name="chat-o" :size="64" color="#cbd5e1" />
        <text class="empty-title">还没有历史对话</text>
        <text class="empty-desc">和 AI wu助手聊聊，记录会自动保存在这里</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/base/IconFont/index.vue'
import type { AiConversationVO } from '@/api/ai'

defineProps<{
  visible: boolean
  loading: boolean
  sessions: AiConversationVO[]
  currentId: string
}>()

const emit = defineEmits<{
  (e: 'select', item: AiConversationVO): void
  (e: 'delete', item: AiConversationVO): void
  (e: 'clear-all'): void
  (e: 'close'): void
}>()

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
</script>
