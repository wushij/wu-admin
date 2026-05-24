<template>
  <Transition name="slide-up">
    <div v-if="messageStore.showNotification && messageStore.currentNotification" class="notification-popup">
      <div class="notification-header">
        <el-icon class="notification-icon" :size="18">
          <Bell v-if="messageStore.currentNotification.type === 'notice'" />
          <ChatDotRound v-else />
        </el-icon>
        <span class="notification-title">{{ messageStore.currentNotification.title }}</span>
        <el-button link @click="messageStore.closeNotification()">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="notification-content">{{ messageStore.currentNotification.content }}</div>
      <div class="notification-footer">
        <span class="notification-time">{{ formatTime(messageStore.currentNotification.time) }}</span>
        <el-button link type="primary" size="small" @click.stop="handleView">查看详情</el-button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Bell, ChatDotRound, Close } from '@element-plus/icons-vue'
import { useMessageStore } from '@/store/message'

const router = useRouter()
const messageStore = useMessageStore()

function formatTime(timestamp: string | number) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  if (isNaN(date.getTime())) return ''
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function handleView() {
  const n = messageStore.currentNotification
  if (!n) return
  messageStore.closeNotification()

  if (n.type === 'notice') {
    await router.push('/message/notice')
    return
  }

  const query: Record<string, string> = {}
  if (n.groupId != null) {
    query.groupId = String(n.groupId)
  } else if (n.senderId != null) {
    query.userId = String(n.senderId)
  }

  await router.push({ path: '/message/chat', query })
}
</script>

<style scoped>
.notification-popup {
  position: fixed;
  left: 20px;
  bottom: 20px;
  width: 320px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  overflow: hidden;
}

.notification-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  gap: 8px;
}

.notification-icon {
  color: var(--theme-primary, #111827);
}

.notification-title {
  flex: 1;
  font-weight: 500;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-content {
  padding: 12px 16px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  max-height: 60px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px 12px;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>

<style>
.emoji-popover.el-popover {
  overflow: hidden;
}
.emoji-popover .emoji-grid {
  overflow-x: hidden !important;
  overflow-y: auto !important;
}
</style>
