<template>
  <el-dialog
    :model-value="visible"
    title="菜单回收站"
    width="980px"
    :lock-scroll="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-table
      :data="list"
      v-loading="loading"
      border
      stripe
      :header-cell-style="{ textAlign: 'center' }"
      :cell-style="{ textAlign: 'center' }"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="菜单名称" width="180" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="menuTypeMeta(row.type)?.tag" size="small">
            {{ menuTypeMeta(row.type)?.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由" width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <DictTag :value="row.status" dict-type="sys_normal_disable" />
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="删除时间" width="180" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="$emit('restore', row)">恢复</el-button>
          <el-button size="small" type="danger" @click="$emit('permanent-delete', row)">清除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.pageNo"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="recycle-pagination"
      @size-change="$emit('load')"
      @current-change="$emit('load')"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import type { MenuVO, MenuRecycleQuery } from '@/api/system/menu'
import { menuTypeMeta } from '../constants/menuMeta'

defineProps<{
  visible: boolean
  list: MenuVO[]
  loading: boolean
  total: number
  query: MenuRecycleQuery
}>()

defineEmits<{
  'update:visible': [value: boolean]
  load: []
  restore: [row: MenuVO]
  'permanent-delete': [row: MenuVO]
}>()
</script>

<style scoped>
.recycle-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
