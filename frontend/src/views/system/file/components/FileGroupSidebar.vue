<template>
  <el-card class="group-card" shadow="never">
    <template #header>
      <div class="card-header">文件分组</div>
    </template>

    <div class="type-tabs">
      <div
        v-for="tab in typeTabs"
        :key="tab.value"
        :class="['type-tab', { active: activeType === tab.value }]"
        @click="$emit('switch-type', tab.value)"
      >
        {{ tab.label }}
      </div>
    </div>

    <div class="group-list-wrapper">
      <div class="group-list">
        <div
          :class="['group-item', { active: activeGroupId === -1 }]"
          @click="$emit('select-group', -1)"
        >
          <el-icon><Folder /></el-icon>
          <span class="group-name">全部</span>
        </div>
        <div
          :class="['group-item', { active: activeGroupId === null }]"
          @click="$emit('select-group', null)"
        >
          <el-icon><Folder /></el-icon>
          <span class="group-name">未分组</span>
          <span v-if="ungroupedCount > 0" class="group-count">{{ ungroupedCount }}</span>
        </div>
        <div
          v-for="group in groups"
          :key="group.id"
          :class="['group-item', { active: activeGroupId === group.id }]"
          @click="$emit('select-group', group.id)"
        >
          <el-icon><Folder /></el-icon>
          <span class="group-name">{{ group.name }}</span>
          <span v-if="(group.fileCount ?? 0) > 0" class="group-count">{{ group.fileCount }}</span>
          <el-dropdown trigger="click" @command="(cmd) => $emit('group-cmd', cmd, group)">
            <el-icon class="group-more" @click.stop><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <el-button class="add-group-btn" dashed block @click="$emit('open-group-dialog')">
      <el-icon><Plus /></el-icon>
      新增分组
    </el-button>
  </el-card>
</template>

<script setup lang="ts">
import { Folder, MoreFilled, Plus } from '@element-plus/icons-vue'
import type { FileGroupVO } from '@/api/system/file'
import { typeTabs } from '../constants/typeTabs'

defineProps<{
  activeType: string
  activeGroupId: number | null
  groups: FileGroupVO[]
  ungroupedCount: number
}>()

defineEmits<{
  'switch-type': [type: string]
  'select-group': [groupId: number | null]
  'group-cmd': [cmd: string, group: FileGroupVO]
  'open-group-dialog': []
}>()
</script>

<style scoped lang="scss">
.group-card {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;

  :deep(.el-card__body) {
    padding: 0 0 12px;
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: hidden;
  }
}

.card-header {
  font-size: 14px;
  font-weight: 600;
}

.type-tabs {
  display: flex;
  padding: 8px;
  gap: 4px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.type-tab {
  flex: 1;
  padding: 5px 4px;
  text-align: center;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  color: var(--color-text-secondary);
  border: 1.5px solid transparent;
  border-radius: var(--radius-pill);
  transition:
    background-color var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);

  &:hover:not(.active) {
    color: var(--capsule-pearl-ice-text);
    background: var(--color-bg-hover);
  }

  &.active {
    color: var(--capsule-pearl-ice-text);
    font-weight: 600;
    background: var(--capsule-pearl-ice-bg);
    border-color: var(--capsule-pearl-ice-border);
    box-shadow: var(--capsule-pearl-ice-shadow);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
  }
}

.group-list-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  gap: 8px;
  font-size: 14px;
  transition: background 0.2s;

  &:hover {
    background: var(--el-fill-color-light);
    .group-more {
      opacity: 1;
    }
  }

  &.active {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
  }
}

.group-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.group-more {
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
}

.add-group-btn {
  margin: 8px 12px 0;
  width: calc(100% - 24px);
}
</style>
