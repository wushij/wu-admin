<template>
  <el-drawer
    :model-value="visible"
    :title="title"
    size="56%"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
    @closed="$emit('closed')"
  >
    <div class="log-drawer-body">
      <el-radio-group
        :model-value="statusFilter"
        class="log-filter"
        @update:model-value="onFilterChange"
      >
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="0">成功</el-radio-button>
        <el-radio-button value="1">失败</el-radio-button>
      </el-radio-group>
      <el-table
        :data="logData"
        v-loading="logLoading"
        border
        stripe
        size="small"
        class="log-table"
        style="width: 100%"
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="jobName" label="任务" min-width="140" align="center" show-overflow-tooltip />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">{{ formatDuration(row) }}</template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始" min-width="170" align="center" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="$emit('show-detail', row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="logQuery.pageNo"
          v-model:page-size="logQuery.pageSize"
          :total="logTotal"
          layout="total, prev, pager, next"
          small
          background
          @current-change="$emit('load')"
        />
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import type { SysJobLog } from '@/api/monitor/job'

defineProps<{
  visible: boolean
  title: string
  statusFilter: 'all' | '0' | '1'
  logData: SysJobLog[]
  logLoading: boolean
  logTotal: number
  logQuery: { pageNo: number; pageSize: number }
  formatDuration: (row: SysJobLog) => string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'update:statusFilter': [value: 'all' | '0' | '1']
  closed: []
  load: []
  'show-detail': [row: SysJobLog]
}>()

function onFilterChange(val: string | number | boolean | undefined) {
  const next = String(val ?? 'all') as 'all' | '0' | '1'
  emit('update:statusFilter', next)
  emit('load')
}
</script>

<style scoped>
.log-filter {
  margin-bottom: 12px;
}
.log-drawer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}
:deep(.el-drawer__body) {
  display: flex;
  flex-direction: column;
  padding-top: 0;
}
.log-table {
  flex: 1;
  width: 100%;
}
.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
