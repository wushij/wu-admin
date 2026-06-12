<template>
  <el-card class="dept-tree-card" shadow="never">
    <template #header>
      <span class="dept-tree-title">部门</span>
    </template>
    <el-input
      v-model="treeSearch"
      placeholder="搜索部门"
      clearable
      class="dept-tree-search"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <div class="dept-tree-body">
      <div
        class="dept-tree-all"
        :class="{ 'dept-tree-all--active': !currentDeptId }"
        @click="selectAll"
      >
        全部用户
      </div>
      <el-tree
        ref="treeRef"
        :data="deptOptions"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        highlight-current
        :indent="20"
        :filter-node-method="filterNode"
        @node-click="onNodeClick"
        @current-change="onCurrentChange"
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
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ElTree } from 'element-plus'
import { Search, OfficeBuilding, Folder, FolderOpened } from '@element-plus/icons-vue'
import type { DeptVO } from '@/api/system/dept'

const props = defineProps<{ deptOptions: DeptVO[] }>()
const emit = defineEmits<{ 'dept-click': [data: DeptVO | null] }>()

const treeRef = ref<InstanceType<typeof ElTree> | null>(null)
const treeSearch = ref('')
const currentDeptId = ref<number | null>(null)

function filterNode(value: string, data: unknown) {
  const node = data as DeptVO
  if (!value) return true
  return String(node.name ?? '').includes(value)
}

function onNodeClick(data: DeptVO) {
  currentDeptId.value = data.id
  emit('dept-click', data)
}

function onCurrentChange(data: DeptVO | undefined) {
  currentDeptId.value = data?.id ?? null
}

function selectAll() {
  currentDeptId.value = null
  treeRef.value?.setCurrentKey(undefined as unknown as string)
  emit('dept-click', null)
}

watch(treeSearch, (val) => {
  treeRef.value?.filter(val)
})

defineExpose({
  treeRef,
  setCurrentDeptId(id: number | null) {
    currentDeptId.value = id
    if (id) treeRef.value?.setCurrentKey(id)
    else treeRef.value?.setCurrentKey(undefined as unknown as string)
  },
})
</script>

<style scoped lang="scss">
.dept-tree-card {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-height: calc(100vh - 200px);
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
    min-height: 0;
    padding: 0 12px 12px;
  }
}

.dept-tree-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.dept-tree-search {
  margin-bottom: 12px;

  :deep(.el-input__wrapper) {
    border-radius: 8px;
    box-shadow: 0 0 0 1px var(--el-border-color-lighter) inset;
  }
}

.dept-tree-body {
  flex: 1;
  min-height: 0;
  padding: 8px 4px;
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fafc 0%, #f3f5f8 100%);
  border: 1px solid var(--el-border-color-extra-light);
  overflow: auto;

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

.dept-tree-all {
  height: 36px;
  margin: 0 2px 6px;
  padding: 0 10px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 36px;
  color: var(--el-text-color-primary);
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:hover {
    background: rgba(1, 7, 16, 0.06);
  }
}

.dept-tree-all--active {
  background: rgba(1, 7, 16, 0.08);
  font-weight: 600;
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
</style>
