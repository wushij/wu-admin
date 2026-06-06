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
    />
    <el-tree
      v-if="activeTab === 'dept'"
      ref="deptTreeRef"
      :data="deptDisplayTree"
      node-key="id"
      :props="{ label: 'name', children: 'children' }"
      :filter-node-method="filterTreeNode"
      highlight-current
      draggable
      :allow-drop="allowDeptDrop"
      @node-click="$emit('dept-node-click', $event)"
      @node-drop="(...args) => $emit('dept-drop', ...args)"
    >
      <template #default="{ data }">
        <span class="tree-node-label">{{ data.name }}</span>
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
      :allow-drop="allowPostDrop"
      :default-expanded-keys="postDefaultExpandedKeys"
      @node-click="$emit('post-node-click', $event)"
      @node-drop="(...args) => $emit('post-drop', ...args)"
    >
      <template #default="{ data }">
        <span class="tree-node-label">{{ data.postName }}</span>
      </template>
    </el-tree>
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
import type { DeptVO } from '@/api/system/dept'
import type { PostVO } from '@/api/system/post'

defineProps<{
  activeTab: string
  treeSearch: string
  deptDisplayTree: DeptVO[]
  postTree: PostVO[]
  postDefaultExpandedKeys: number[]
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
  width: 220px;
  max-width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    flex: 1;
    padding-bottom: 12px;
  }
}
.org-tree-title {
  font-weight: 600;
}
.org-tree-search {
  margin-bottom: 10px;
}
.org-tree-card :deep(.el-tree) {
  flex: 1;
  overflow: auto;
  max-height: calc(100vh - 320px);
}
.org-add-root {
  width: 100%;
  margin-top: 12px;
}
.tree-node-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
