<template>
  <el-table
    ref="tableRef"
    :data="menuList"
    v-loading="loading"
    row-key="id"
    border
    stripe
    :default-expand-all="expandAll"
    :tree-props="{ children: 'children' }"
    class="menu-tree-table"
  >
    <el-table-column prop="name" label="菜单名称" min-width="200" show-overflow-tooltip />
    <el-table-column prop="type" label="类型" width="90" align="center">
      <template #default="{ row }">
        <el-tag :type="menuTypeMeta(row.type)?.tag" size="small" effect="light">
          {{ menuTypeMeta(row.type)?.label }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="图标" width="150" align="center">
      <template #default="{ row }">
        <span v-if="row.type === 3 || !row.icon" class="text-muted">-</span>
        <span v-else class="icon-cell">
          <el-icon :size="18"><component :is="resolveMenuIcon(row.icon)" /></el-icon>
          <span class="icon-name">{{ row.icon }}</span>
        </span>
      </template>
    </el-table-column>
    <el-table-column prop="path" label="路由地址" min-width="160" show-overflow-tooltip>
      <template #default="{ row }">
        <template v-if="row.type !== 3">
          <el-tag v-if="isExternalRow(row)" type="warning" size="small" class="ext-tag">外链</el-tag>
          {{ row.path || '-' }}
        </template>
        <span v-else class="text-muted">-</span>
      </template>
    </el-table-column>
    <el-table-column prop="component" label="组件/外链" min-width="180" show-overflow-tooltip>
      <template #default="{ row }">
        {{ row.component || '-' }}
      </template>
    </el-table-column>
    <el-table-column prop="sort" label="排序" width="70" align="center" />
    <el-table-column prop="status" label="状态" width="88" align="center">
      <template #default="{ row }">
        <el-switch
          v-model="row.status"
          :active-value="1"
          :inactive-value="0"
          v-permission="'system:menu:update'"
          @change="$emit('status-change', row)"
        />
      </template>
    </el-table-column>
    <el-table-column label="操作" width="220" fixed="right" align="center">
      <template #default="{ row }">
        <div class="action-buttons">
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
import { menuTypeMeta } from '../constants/menuMeta'

defineProps<{
  menuList: MenuVO[]
  loading: boolean
  expandAll: boolean
  isExternalRow: (row: MenuVO) => boolean
}>()

defineEmits<{
  add: [row?: MenuVO]
  edit: [row: MenuVO]
  delete: [row: MenuVO]
  'status-change': [row: MenuVO]
}>()

const tableRef = ref<TableInstance | null>(null)
defineExpose({ tableRef })
</script>

<style scoped>
.menu-tree-table {
  width: 100%;
}

.icon-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.icon-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  max-width: 90px;
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
