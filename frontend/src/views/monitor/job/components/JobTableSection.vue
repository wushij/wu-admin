<template>
  <el-card shadow="never" class="table-card">
    <template #header>
      <div class="card-header-row">
        <span>任务列表</span>
        <div class="header-actions">
          <RecycleCenterLink tab="job" />
          <el-button link type="primary" @click="$emit('refresh')">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </div>
    </template>

    <el-form :model="queryParams" inline class="module-search-form">
      <el-form-item label="任务名称">
        <el-input
          v-model="queryParams.jobName"
          placeholder="模糊搜索"
          clearable
          style="width: 150px"
          @keyup.enter="$emit('search')"
        />
      </el-form-item>
      <el-form-item label="任务组">
        <el-select v-model="queryParams.jobGroup" placeholder="全部" clearable style="width: 130px">
          <el-option label="SYSTEM" value="SYSTEM" />
          <el-option label="DEFAULT" value="DEFAULT" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 110px">
          <el-option label="运行中" :value="1" />
          <el-option label="已暂停" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="$emit('search')">搜索</el-button>
        <el-button :icon="Refresh" @click="$emit('reset')">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-toolbar">
      <el-button v-permission="'monitor:job:add'" type="primary" @click="$emit('add')">
        <el-icon><Plus /></el-icon>自定义任务
      </el-button>
      <el-button @click="$emit('open-all-logs')">
        <el-icon><List /></el-icon>调度日志
      </el-button>
      <el-button v-permission="'monitor:job:delete'" type="danger" plain @click="$emit('clean-logs')">清空全部日志</el-button>
    </div>

    <el-table
      :data="tableData"
      v-loading="loading"
      border
      stripe
      class="job-table"
      :header-cell-style="{ textAlign: 'center' }"
      :cell-style="{ textAlign: 'center' }"
    >
      <el-table-column prop="jobName" label="任务" min-width="150" align="center">
        <template #default="{ row }">
          <div class="job-name-cell">
            <span class="job-name">{{ row.jobName }}</span>
            <el-tag size="small" :type="groupTagType(row.jobGroup)">{{ row.jobGroup }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="invokeTarget" label="调用目标" min-width="220" align="center" show-overflow-tooltip />
      <el-table-column label="调度策略" min-width="160" align="center">
        <template #default="{ row }">
          <div class="cron-cell">
            <code class="cron-code">{{ row.cronExpression }}</code>
            <span v-if="row.cronHint" class="cron-hint">{{ row.cronHint }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="下次执行" min-width="170" align="center">
        <template #default="{ row }">
          <span v-if="row.status === 1 && row.nextFireTime" class="next-time">{{ row.nextFireTime }}</span>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-switch
            v-permission="'monitor:job:edit'"
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            inline-prompt
            active-text="开"
            inactive-text="停"
            @change="(v: string | number | boolean) => $emit('status-change', row, Number(v))"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button type="primary" size="small" @click="$emit('open-logs', row)">日志</el-button>
            <el-button v-permission="'monitor:job:edit'" type="primary" size="small" @click="$emit('run', row)">执行</el-button>
            <el-button v-permission="'monitor:job:edit'" type="primary" size="small" @click="$emit('edit', row)">编辑</el-button>
            <el-button v-permission="'monitor:job:delete'" type="danger" size="small" @click="$emit('delete', row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="$emit('load-page')"
        @current-change="$emit('load-page')"
      />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { Plus, List, Refresh, Search } from '@element-plus/icons-vue'
import RecycleCenterLink from '@/components/RecycleCenterLink.vue'
import type { SysJob } from '@/api/monitor/job'
import { groupTagType } from '../constants/cronPresets'

defineProps<{
  queryParams: { pageNo: number; pageSize: number; jobName: string; jobGroup: string; status: number | null }
  tableData: SysJob[]
  loading: boolean
  total: number
}>()

defineEmits<{
  refresh: []
  search: []
  reset: []
  add: []
  'open-all-logs': []
  'clean-logs': []
  'status-change': [row: SysJob, status: number]
  'open-logs': [row: SysJob]
  run: [row: SysJob]
  edit: [row: SysJob]
  delete: [row: SysJob]
  'load-page': []
}>()
</script>

<style scoped>
.table-card {
  margin-bottom: 16px;
}
.table-card :deep(.el-card__body) {
  padding: 16px 20px 20px;
}
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.module-search-form {
  margin-bottom: 12px;
}
.table-toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.job-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}
.job-name {
  font-weight: 600;
}
.cron-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: center;
}
.cron-code {
  font-size: 12px;
  color: #374151;
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
}
.cron-hint {
  font-size: 11px;
  color: #909399;
}
.next-time {
  font-size: 12px;
  color: #52c41a;
}
.text-muted {
  color: #c0c4cc;
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
