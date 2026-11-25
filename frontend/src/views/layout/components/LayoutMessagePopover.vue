<template>
  <el-popover v-model:visible="popoverVisible" trigger="click" placement="bottom-end" :width="348" @show="$emit('show')">
    <template #reference>
      <el-badge :value="messageStore.totalUnread" :hidden="!messageStore.totalUnread" class="notice-badge">
        <el-tooltip content="消息中心" placement="bottom" :show-after="1000" :disabled="popoverVisible">
          <button
            id="tour-message-btn"
            type="button"
            class="dock-segment-btn"
            :class="{ 'is-active': isActive }"
            aria-label="消息中心"
            @click="$emit('activate')"
          >
            <el-icon :size="15"><component :is="ElementPlusIconsVue.Bell" /></el-icon>
          </button>
        </el-tooltip>
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
        <EmptyState v-else title="暂无业务消息" description="新的审批与工单通知会显示在这里" />
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
        <EmptyState v-else title="暂无系统通知" />
        <div class="panel-footer">
          <el-button link type="primary" @click="goNoticeManage">管理通知</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane name="chat">
        <template #label>
          <span>企业IM</span>
          <el-badge v-if="messageStore.chatCount" :value="messageStore.chatCount" class="tab-badge" />
        </template>
        <div class="chat-tab-body">
          <p class="chat-hint">企业 IM 未读 {{ messageStore.chatCount }} 条</p>
          <el-button type="primary" @click="goChat">进入企业 IM</el-button>
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
import EmptyState from '@/components/ui/EmptyState.vue'

defineProps<{
  inboxList: InboxNoticeItem[]
  announceList: AnnounceMyVO[]
  isActive?: boolean
}>()

const messageTab = defineModel<string>('messageTab', { default: 'inbox' })

defineEmits<{
  activate: []
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

function goNoticeManage() {
  popoverVisible.value = false
  router.push('/message/notice')
}
</script>

<style scoped lang="scss">
.notice-badge {
  box-sizing: border-box;
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--dock-slot, 28px);
  height: var(--dock-slot, 28px);
  flex-shrink: 0;
}

.notice-badge :deep(.el-badge__content.is-fixed) {
  top: 2px;
  right: 2px;
  transform: translateY(-50%) translateX(50%) scale(0.84);
}

.message-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.tab-badge {
  margin-left: 6px;
}

.panel-footer {
  text-align: center;
  padding-top: 8px;
  border-top: 1px solid var(--color-border-subtle);
}

.chat-tab-body {
  text-align: center;
  padding: 24px 12px;
}

.chat-hint {
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.notice-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.notice-list {
  max-height: 320px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item {
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  cursor: pointer;
  transition: border-color var(--duration-fast) var(--ease-out);

  &:hover {
    border-color: var(--color-border-strong);
  }
}

.notice-item.unread {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
}

.notice-title {
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.notice-content {
  font-size: 12.5px;
  color: var(--color-text-secondary);
}

.notice-time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}
</style>
