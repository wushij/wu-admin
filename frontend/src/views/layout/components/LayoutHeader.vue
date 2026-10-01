<template>
  <div class="header">
    <div class="header-left">
      <el-icon class="collapse-btn" @click="$emit('toggleCollapse')">
        <component :is="isCollapse ? ElementPlusIconsVue.Expand : ElementPlusIconsVue.Fold" />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentPageTitle && currentPageTitle !== '工作台'">
          {{ currentPageTitle }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <div class="header-action-group">
        <LayoutMessagePopover
          v-model:message-tab="messageTab"
          :inbox-list="inboxList"
          :announce-list="announceList"
          @show="$emit('loadMessages')"
          @read-inbox="$emit('readInbox', $event)"
          @read-announce="$emit('readAnnounce', $event)"
          @read-all-inbox="$emit('readAllInbox')"
          @read-all-announce="$emit('readAllAnnounce')"
        />
        <LayoutThemePicker
          :current-color="currentColor"
          :active-preset-id="activePresetId"
          @preset-select="$emit('presetSelect', $event)"
          @color-change="$emit('colorChange', $event)"
        />
      </div>

      <div class="header-divider" />

      <el-dropdown @command="$emit('userCommand', $event)">
        <div class="user-info">
          <el-avatar :size="30" :src="headerAvatarSrc" class="header-avatar" fit="cover">
            <el-icon v-if="!headerAvatarSrc"><component :is="ElementPlusIconsVue.UserFilled" /></el-icon>
          </el-avatar>
          <span class="username">{{ nickname }}</span>
          <el-icon class="user-arrow"><component :is="ElementPlusIconsVue.ArrowDown" /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import type { InboxNoticeItem } from '@/store/message'
import type { AnnounceMyVO } from '@/types/message'
import LayoutMessagePopover from './LayoutMessagePopover.vue'
import LayoutThemePicker from './LayoutThemePicker.vue'

defineProps<{
  isCollapse: boolean
  currentPageTitle: string
  currentColor: string
  activePresetId: string | undefined
  headerAvatarSrc: string | undefined
  nickname: string
  inboxList: InboxNoticeItem[]
  announceList: AnnounceMyVO[]
}>()

const messageTab = defineModel<string>('messageTab', { default: 'inbox' })

defineEmits<{
  toggleCollapse: []
  loadMessages: []
  readInbox: [item: InboxNoticeItem]
  readAnnounce: [item: AnnounceMyVO]
  readAllInbox: []
  readAllAnnounce: []
  presetSelect: [presetId: string]
  colorChange: [color: string | null]
  userCommand: [command: string]
}>()
</script>

<style scoped>
.header {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  margin-right: 20px;
  color: var(--theme-text-base, #1F2937);
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: var(--theme-primary, #3b82f6);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-action-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-divider {
  width: 1px;
  height: 18px;
  background: var(--theme-border, #e2e8f0);
  margin: 0 2px;
}

.user-info {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 12px 3px 4px;
  border-radius: 9999px;
  border: 1px solid var(--theme-border, #e2e8f0);
  background: #ffffff;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.user-info:hover {
  border-color: var(--theme-primary, #3b82f6);
  background: var(--theme-primary-muted, rgba(59, 130, 246, 0.04));
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.header-avatar {
  flex-shrink: 0;
  border: none;
}

.username {
  font-size: 13px;
  font-weight: 500;
  color: var(--theme-text-base, #1e293b);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  font-size: 12px;
  color: #94a3b8;
  transition: transform 0.25s ease, color 0.2s;
}

.user-info:hover .user-arrow {
  color: var(--theme-primary, #3b82f6);
  transform: rotate(180deg);
}
</style>
