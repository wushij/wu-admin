<template>
  <el-card class="org-tree-card" shadow="never">
    <template #header>
      <span class="org-tree-title">{{ activeTab === 'dept' ? '部门体系' : '岗位体系' }}</span>
    </template>
    <el-input
      :model-value="treeSearch"
      placeholder="搜索名称"
      clearable
      class="org-tree-search"
      @update:model-value="$emit('update:treeSearch', $event)"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <div class="org-tree-body">
      <el-tree
        v-if="activeTab === 'dept'"
        ref="deptTreeRef"
        :data="deptDisplayTree"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        :filter-node-method="filterTreeNode"
        highlight-current
        draggable
        :indent="20"
        :allow-drop="allowDeptDrop"
        @node-click="$emit('dept-node-click', $event)"
        @node-drop="(...args) => $emit('dept-drop', ...args)"
      >
        <template #default="{ data, node }">
          <div class="tree-node">
            <el-icon class="tree-node__icon" :class="{ 'tree-node__icon--leaf': node.isLeaf }">
              <OfficeBuilding v-if="node.isLeaf" />
              <FolderOpened v-else-if="node.expanded" />
              <Folder v-else />
            </el-icon>
            <span class="tree-node__name">{{ data.name }}</span>
            <span v-if="data.userCount" class="tree-node__badge">{{ data.userCount }}</span>
          </div>
        </template>
      </el-tree>
      <el-tree
        v-else
        ref="postTreeRef"
        :data="postTree"
        node-key="id"
        :props="{ label: 'postName', children: 'children' }"
        :filter-node-method="filterPostTreeNode"
        highlight-current
        draggable
        :indent="20"
        :allow-drop="allowPostDrop"
        @node-click="$emit('post-node-click', $event)"
        @node-drop="(...args) => $emit('post-drop', ...args)"
      >
        <template #default="{ data, node }">
          <div class="tree-node">
            <el-icon class="tree-node__icon" :class="{ 'tree-node__icon--leaf': node.isLeaf }">
              <Briefcase v-if="node.isLeaf" />
              <FolderOpened v-else-if="node.expanded" />
              <Folder v-else />
            </el-icon>
            <span class="tree-node__name">{{ data.postName }}</span>
          </div>
        </template>
      </el-tree>
    </div>

    <el-button
      v-permission="activeTab === 'dept' ? 'system:dept:create' : 'system:post:create'"
      class="org-add-root"
      type="primary"
      plain
      @click="$emit('add-root')"
    >
      {{ activeTab === 'dept' ? '新增一级部门' : '新增顶级岗位' }}
    </el-button>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ElTree } from 'element-plus'
import { Search, OfficeBuilding, Folder, FolderOpened, Briefcase } from '@element-plus/icons-vue'
import type { DeptVO } from '@/api/system/dept'
import type { PostVO } from '@/api/system/post'

defineProps<{
  activeTab: string
  treeSearch: string
  deptDisplayTree: DeptVO[]
  postTree: PostVO[]
  filterTreeNode: (value: string, data: unknown) => boolean
  filterPostTreeNode: (value: string, data: unknown) => boolean
  allowDeptDrop: (dragging: unknown, drop: unknown, type: string) => boolean
  allowPostDrop: (dragging: unknown, drop: unknown, type: string) => boolean
}>()

defineEmits<{
  'update:treeSearch': [value: string]
  'dept-node-click': [data: DeptVO]
  'post-node-click': [data: PostVO]
  'dept-drop': [dragging: unknown, drop: unknown, dropType: 'before' | 'after' | 'inner', evt?: DragEvent]
  'post-drop': [dragging: unknown, drop: unknown, dropType: 'before' | 'after' | 'inner', evt?: DragEvent]
  'add-root': []
}>()

const deptTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
const postTreeRef = ref<InstanceType<typeof ElTree> | null>(null)

defineExpose({ deptTreeRef, postTreeRef })
</script>

<style scoped lang="scss">
.org-tree-card {
  width: 280px;
  max-width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);

  :deep(.el-card__header) {
    padding: 14px 16px 10px;
    border-bottom: none;
  }

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    flex: 1;
    padding: 0 12px 12px;
  }
}

.org-tree-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.org-tree-search {
  margin-bottom: 12px;

  :deep(.el-input__wrapper) {
    border-radius: 8px;
    box-shadow: 0 0 0 1px var(--el-border-color-lighter) inset;
  }
}

.org-tree-body {
  flex: 1;
  min-height: 0;
  padding: 8px 4px;
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fafc 0%, #f3f5f8 100%);
  border: 1px solid var(--el-border-color-extra-light);
  overflow: auto;
  max-height: calc(100vh - 320px);

  :deep(.el-tree) {
    background: transparent;
    --el-tree-node-hover-bg-color: rgba(1, 7, 16, 0.06);
  }

  :deep(.el-tree-node__content) {
    height: 36px;
    margin: 2px 0;
    border-radius: 8px;
    transition: background-color 0.15s ease;
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background: rgba(1, 7, 16, 0.08);
    font-weight: 600;
  }

  :deep(.el-tree-node__expand-icon) {
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }

  :deep(.el-tree-node__expand-icon.is-leaf) {
    color: transparent;
  }
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
  padding-right: 6px;
}

.tree-node__icon {
  flex-shrink: 0;
  font-size: 15px;
  color: #6366f1;
}

.tree-node__icon--leaf {
  color: #64748b;
}

.tree-node__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.tree-node__badge {
  flex-shrink: 0;
  min-width: 20px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.12);
  color: #4f46e5;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.org-add-root {
  width: 100%;
  margin-top: 12px;
  border-radius: 8px;
}
</style>
