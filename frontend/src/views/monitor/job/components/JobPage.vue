<template>
  <div class="app-container job-page">
    <JobHeroOverview :overview="overview" />

    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span>执行结果分布</span></template>
          <div ref="pieChartRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span>近 7 日执行趋势</span></template>
          <div ref="barChartRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <JobTemplateSection :templates="templates" @create-from-template="quickCreateFromTemplate" />

    <JobTableSection
      :query-params="queryParams"
      :table-data="tableData"
      :loading="loading"
      :total="total"
      @refresh="refreshAll(true)"
      @search="handleSearch"
      @reset="handleReset"
      @add="handleAdd"
      @open-all-logs="openAllLogs"
      @clean-logs="handleCleanLogs"
      @status-change="handleStatusChange"
      @open-logs="openJobLogs"
      @run="handleRun"
      @edit="handleEdit"
      @delete="handleDelete"
      @load-page="loadPage"
    />

    <JobFormDialog
      ref="formDialogRef"
      v-model:visible="formVisible"
      :title="formTitle"
      :form-data="formData"
      :form-rules="formRules"
      :cron-preset="cronPreset"
      :cron-preview="cronPreview"
      :submitting="submitting"
      @closed="resetForm"
      @validate-cron="validateCron"
      @apply-cron-preset="applyCronPreset"
      @submit="submitForm"
    />

    <JobLogDrawer
      v-model:visible="logVisible"
      v-model:status-filter="logStatusFilter"
      :title="logTitle"
      :log-data="logData"
      :log-loading="logLoading"
      :log-total="logTotal"
      :log-query="logQuery"
      :format-duration="formatDuration"
      @closed="onLogClosed"
      @load="loadLogs"
      @show-detail="showLogDetail"
    />

    <JobLogDetailDialog v-model:visible="logDetailVisible" :detail="logDetail" />
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import JobHeroOverview from './JobHeroOverview.vue'
import JobTemplateSection from './JobTemplateSection.vue'
import JobTableSection from './JobTableSection.vue'
import JobFormDialog from './JobFormDialog.vue'
import JobLogDrawer from './JobLogDrawer.vue'
import JobLogDetailDialog from './JobLogDetailDialog.vue'
import { useJobPage } from '../composables/useJobPage'

const formDialogRef = ref<InstanceType<typeof JobFormDialog> | null>(null)
const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()

const {
  overview,
  templates,
  loading,
  tableData,
  total,
  queryParams,
  formVisible,
  formTitle,
  submitting,
  formRef,
  cronPreset,
  cronPreview,
  formData,
  formRules,
  logVisible,
  logLoading,
  logData,
  logTotal,
  logStatusFilter,
  logQuery,
  logDetailVisible,
  logDetail,
  logTitle,
  applyCronPreset,
  validateCron,
  refreshAll,
  handleSearch,
  handleReset,
  handleAdd,
  handleEdit,
  quickCreateFromTemplate,
  submitForm,
  handleDelete,
  handleStatusChange,
  handleRun,
  openAllLogs,
  openJobLogs,
  loadLogs,
  loadPage,
  showLogDetail,
  formatDuration,
  onLogClosed,
  handleCleanLogs,
  resetForm,
} = useJobPage({ pieChartRef, barChartRef })

watchEffect(() => {
  formRef.value = formDialogRef.value?.formRef
})
</script>

<style scoped>
.job-page {
  padding-bottom: 24px;
}
.charts-row {
  margin-bottom: 16px;
}
.chart-card :deep(.el-card__header) {
  padding: 12px 16px;
  font-weight: 600;
}
.chart-box {
  height: 240px;
}
</style>
