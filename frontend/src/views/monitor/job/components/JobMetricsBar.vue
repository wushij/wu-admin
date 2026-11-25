<template>
  <div class="job-metrics-wrap">
    <div class="job-metrics-wrap__title">执行统计</div>
    <el-row :gutter="12" class="job-metrics-row">
    <el-col v-for="item in items" :key="item.key" :xs="12" :sm="6">
      <div class="job-metric-card" :class="item.tone ? `job-metric-card--${item.tone}` : ''">
        <div class="job-metric-card__value">{{ item.value }}</div>
        <div class="job-metric-card__label">{{ item.label }}</div>
      </div>
    </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { JobOverview } from '@/api/monitor/job'

const props = defineProps<{ overview: JobOverview }>()

const items = computed(() => [
  { key: 'total', label: '累计执行', value: props.overview.totalCount ?? 0 },
  { key: 'success', label: '成功', value: props.overview.successCount ?? 0, tone: 'ok' },
  { key: 'fail', label: '失败', value: props.overview.failCount ?? 0, tone: 'fail' },
  { key: 'rate', label: '成功率', value: `${props.overview.successRate ?? 100}%`, tone: 'rate' },
])
</script>

<style scoped>
.job-metrics-wrap {
  margin-bottom: 16px;
}

.job-metrics-wrap__title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.job-metrics-row {
  margin-bottom: 0;
}

.job-metric-card {
  padding: 18px 12px;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
  box-shadow: var(--shadow-xs);
  text-align: center;
}

.job-metric-card__value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}

.job-metric-card__label {
  margin-top: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.job-metric-card--ok .job-metric-card__value {
  color: var(--el-color-success);
}

.job-metric-card--fail .job-metric-card__value {
  color: var(--el-color-danger);
}

.job-metric-card--rate .job-metric-card__value {
  color: #0d9488;
}
</style>
