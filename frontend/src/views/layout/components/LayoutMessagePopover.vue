<template>
  <el-popover v-model:visible="popoverVisible" trigger="click" placement="bottom-end" :width="340" @show="$emit('show')">
    <template #reference>
      <el-badge :value="messageStore.totalUnread" :hidden="!messageStore.totalUnread" class="notice-badge">
        <el-icon class="notice-icon" :size="20"><component :is="ElementPlusIconsVue.Bell" /></el-icon>
      </el-badge>
    </template>
    <el-tabs v-model="messageTab" class="message-tabs">
      <el-tab-pane name="inbox">
        <template #label>
          <span>业务消息</span>
          <el-badge v-if="messageStore.inboxCount" :value="messageStore.inboxCount" class="tab-badge" />
        </template>
        <div class="notice-panel-header">
          <span>工单 / 审批等业务提醒</span>
          <el-button link type="primary" @click="$emit('readAllInbox')">全部已读</el-button>
        </div>
        <div v-if="inboxList.length" class="notice-list">
          <div
            v-for="item in inboxList"
            :key="item.id"
            class="notice-item"
            :class="{ unread: item.readStatus === 0 }"
            @click="$emit('readInbox', item)"
          >
            <div class="notice-title">{{ item.title }}</div>
            <div class="notice-content">{{ item.content }}</div>
            <div class="notice-time">{{ item.createTime }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无业务消息" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane name="announce">
        <template #label>
          <span>系统通知</span>
          <el-badge v-if="messageStore.announceCount" :value="messageStore.announceCount" class="tab-badge" />
        </template>
        <div class="notice-panel-header">
          <span>平台公告与通知</span>
          <el-button link type="primary" @click="$emit('readAllAnnounce')">全部已读</el-button>
        </div>
        <div v-if="announceList.length" class="notice-list">
          <div
            v-for="item in announceList"
            :key="item.id"
            class="notice-item"
            :class="{ unread: !item.isRead }"
            @click="$emit('readAnnounce', item)"
          >
            <div class="notice-title">{{ item.title }}</div>
            <div class="notice-content">{{ item.content }}</div>
            <div class="notice-time">{{ item.createTime }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无系统通知" :image-size="60" />
        <div class="panel-footer">
          <el-button link type="primary" @click="router.push('/message/notice')">管理通知</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane name="chat">
        <template #label>
          <span>企业IM</span>
          <el-badge v-if="messageStore.chatCount" :value="messageStore.chatCount" class="tab-badge" />
        </template>
        <div class="chat-tab-body">
          <p class="chat-hint">企业IM 未读 {{ messageStore.chatCount }} 条</p>
          <el-button type="primary" @click="goChat">进入企业IM</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { useMessageStore, type InboxNoticeItem } from '@/store/message'
import type { AnnounceMyVO } from '@/types/message'

defineProps<{
  inboxList: InboxNoticeItem[]
  announceList: AnnounceMyVO[]
}>()

const messageTab = defineModel<string>('messageTab', { default: 'inbox' })

defineEmits<{
  show: []
  readInbox: [item: InboxNoticeItem]
  readAnnounce: [item: AnnounceMyVO]
  readAllInbox: []
  readAllAnnounce: []
}>()

const router = useRouter()
const messageStore = useMessageStore()
const popoverVisible = ref(false)

function goChat() {
  popoverVisible.value = false
  router.push('/message/chat')
}
</script>

<style scoped>
.notice-badge { cursor: pointer; }
.notice-icon { color: var(--theme-text-base, #1F2937); }
.message-tabs :deep(.el-tabs__header) { margin-bottom: 8px; }
.tab-badge { margin-left: 6px; }
.panel-footer { text-align: center; padding-top: 8px; }
.chat-tab-body { text-align: center; padding: 24px 12px; }
.chat-hint { color: #666; margin-bottom: 12px; }
.notice-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 600;
}
.notice-list { max-height: 360px; overflow: auto; }
.notice-item {
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #f8f9fb;
  cursor: pointer;
}
.notice-item.unread {
  background: var(--theme-primary-muted, rgba(37, 99, 235, 0.08));
  border: 1px solid var(--theme-primary-muted-strong, rgba(37, 99, 235, 0.12));
}
.notice-title { font-weight: 600; margin-bottom: 4px; }
.notice-content { font-size: 13px; color: #606266; }
.notice-time { margin-top: 4px; font-size: 12px; color: #909399; }
</style>
