<template>
  <el-dialog
    :model-value="visible"
    title="用户回收站"
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
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="mobile" label="手机号" width="130" />
      <el-table-column prop="deptName" label="部门" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <DictTag :value="row.status" dict-type="sys_normal_disable" />
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="删除时间" width="180" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button size="small" type="success" @click="$emit('restore', row)">恢复</el-button>
            <el-button size="small" type="danger" @click="$emit('permanent-delete', row)">清除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.pageNo"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="$emit('load')"
      @current-change="$emit('load')"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import type { UserVO, UserRecycleQuery } from '@/api/system/user'

defineProps<{
  visible: boolean
  list: UserVO[]
  loading: boolean
  total: number
  query: UserRecycleQuery
}>()

defineEmits<{
  'update:visible': [value: boolean]
  load: []
  restore: [row: UserVO]
  'permanent-delete': [row: UserVO]
}>()
</script>

<style scoped>
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
