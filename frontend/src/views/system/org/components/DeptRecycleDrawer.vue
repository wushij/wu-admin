<template>
  <el-drawer :model-value="visible" title="部门回收站" size="720px" @update:model-value="$emit('update:visible', $event)">
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="name" label="部门名称" />
      <el-table-column label="操作" width="180" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="$emit('restore', row)">恢复</el-button>
          <el-button type="danger" size="small" @click="$emit('permanent-delete', row)">彻底删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.pageNo"
      v-model:page-size="query.pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="recycle-pagination"
      @current-change="$emit('load')"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import type { DeptVO, DeptRecycleQuery } from '@/api/system/dept'

defineProps<{
  visible: boolean
  list: DeptVO[]
  loading: boolean
  total: number
  query: DeptRecycleQuery
}>()

defineEmits<{
  'update:visible': [value: boolean]
  load: []
  restore: [row: DeptVO]
  'permanent-delete': [row: DeptVO]
}>()
</script>

<style scoped>
.recycle-pagination {
  margin-top: 12px;
}
</style>
