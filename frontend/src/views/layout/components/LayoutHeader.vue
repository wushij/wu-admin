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
      <el-dropdown @command="$emit('userCommand', $event)">
        <span class="user-info">
          <el-avatar :size="32" :src="headerAvatarSrc" class="header-avatar">
            <el-icon v-if="!headerAvatarSrc"><component :is="ElementPlusIconsVue.UserFilled" /></el-icon>
          </el-avatar>
          <span class="username">{{ nickname }}</span>
          <el-icon><component :is="ElementPlusIconsVue.ArrowDown" /></el-icon>
        </span>
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

.header-left { display: flex; align-items: center; }

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  margin-right: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username { margin: 0 8px; }
</style>
