<template>
  <el-table
    :key="tableKey"
    ref="tableRef"
    :data="menuList"
    v-loading="loading"
    row-key="id"
    border
    stripe
    :default-expand-all="expandAll"
    :tree-props="{ children: 'children' }"
    class="menu-tree-table"
    @row-click="onRowClick"
  >
    <el-table-column
      prop="name"
      label="菜单名称"
      min-width="180"
      show-overflow-tooltip
      header-align="center"
      align="left"
      class-name="menu-name-col"
    >
      <template #default="{ row }">
        <span
          class="menu-name-text"
          :class="{ 'is-expandable': hasChildren(row) }"
          @click.stop="toggleExpand(row)"
        >
          {{ row.name }}
        </span>
      </template>
    </el-table-column>
    <el-table-column prop="type" label="类型" width="76" header-align="center" align="center">
      <template #default="{ row }">
        <el-tag :type="menuTypeMeta(row.type)?.tag" size="small" effect="light">
          {{ menuTypeMeta(row.type)?.label }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="图标" width="120" header-align="center" align="center">
      <template #default="{ row }">
        <span v-if="row.type === 3 || !row.icon" class="text-muted">-</span>
        <span v-else class="icon-cell">
          <el-icon :size="18"><component :is="resolveMenuIcon(row.icon)" /></el-icon>
          <span class="icon-name">{{ row.icon }}</span>
        </span>
      </template>
    </el-table-column>
    <el-table-column
      prop="path"
      label="路由地址"
      min-width="120"
      show-overflow-tooltip
      header-align="center"
      align="center"
    >
      <template #default="{ row }">
        <template v-if="row.type !== 3">
          <el-tag v-if="isExternalRow(row)" type="warning" size="small" class="ext-tag">外链</el-tag>
          {{ row.path || '-' }}
        </template>
        <span v-else class="text-muted">-</span>
      </template>
    </el-table-column>
    <el-table-column
      prop="component"
      label="组件/外链"
      min-width="130"
      show-overflow-tooltip
      header-align="center"
      align="center"
    >
      <template #default="{ row }">
        {{ row.component || '-' }}
      </template>
    </el-table-column>
    <el-table-column prop="sort" label="排序" width="64" header-align="center" align="center" />
    <el-table-column prop="status" label="状态" width="76" header-align="center" align="center">
      <template #default="{ row }">
        <el-switch
          v-model="row.status"
          :active-value="1"
          :inactive-value="0"
          v-permission="'system:menu:update'"
          @click.stop
          @change="$emit('status-change', row)"
        />
      </template>
    </el-table-column>
    <el-table-column label="操作" width="200" fixed="right" header-align="center" align="center">
      <template #default="{ row }">
        <div class="action-buttons" @click.stop>
          <el-button
            v-if="row.type !== 3"
            v-permission="'system:menu:create'"
            type="primary"
            size="small"
            @click="$emit('add', row)"
          >
            新增
          </el-button>
          <el-button
            v-permission="'system:menu:update'"
            type="primary"
            size="small"
            @click="$emit('edit', row)"
          >
            编辑
          </el-button>
          <el-button
            v-permission="'system:menu:delete'"
            type="danger"
            size="small"
            @click="$emit('delete', row)"
          >
            删除
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { TableInstance } from 'element-plus'
import type { MenuVO } from '@/api/system/menu'
import { resolveMenuIcon } from '@/utils/menu-icon'
import { menuNodeHasChildren } from '@/utils/menu-tree'
import { menuTypeMeta } from '../constants/menuMeta'

defineProps<{
  menuList: MenuVO[]
  loading: boolean
  expandAll: boolean
  tableKey: number
  isExternalRow: (row: MenuVO) => boolean
}>()

defineEmits<{
  add: [row?: MenuVO]
  edit: [row: MenuVO]
  delete: [row: MenuVO]
  'status-change': [row: MenuVO]
}>()

const tableRef = ref<TableInstance | null>(null)

const hasChildren = (row: MenuVO) => menuNodeHasChildren(row)

function toggleExpand(row: MenuVO) {
  if (!hasChildren(row)) return
  tableRef.value?.toggleRowExpansion(row)
}

function onRowClick(row: MenuVO, column: { property?: string }) {
  if (column?.property === 'name') {
    toggleExpand(row)
  }
}

defineExpose({ tableRef })
</script>

<style scoped>
.menu-tree-table {
  width: 100%;
}

.menu-tree-table :deep(.el-table__header th .cell) {
  text-align: center;
}

.menu-tree-table :deep(.el-table__body td:not(.menu-name-col) .cell) {
  text-align: center;
}

.menu-tree-table :deep(.el-table__body td.menu-name-col .cell) {
  display: flex;
  align-items: center;
  text-align: left;
}

.menu-tree-table :deep(.el-table__expand-icon) {
  width: 22px;
  height: 22px;
  margin-right: 2px;
  cursor: pointer;
}

.menu-tree-table :deep(.el-table__row) {
  cursor: default;
}

.menu-name-text.is-expandable {
  cursor: pointer;
  user-select: none;
}

.menu-name-text.is-expandable:hover {
  color: var(--el-color-primary);
}

.icon-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.icon-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.text-muted {
  color: var(--el-text-color-placeholder);
}

.ext-tag {
  margin-right: 4px;
  vertical-align: middle;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  margin: 0;
}
</style>
