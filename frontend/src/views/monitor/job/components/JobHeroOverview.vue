<template>
  <header class="job-hero">
    <div class="job-hero-left">
      <div class="job-hero-head">
        <div class="job-hero-icon" aria-hidden="true">
          <ModulePageIcon :icon="MODULE_PAGE_ICON.job" :size="20" />
        </div>
        <div class="job-hero-text">
          <h1 class="job-hero-title">定时任务调度中心</h1>
          <p class="job-hero-desc">基于 Quartz 管理维护任务：日志归档、聊天消息清理、通知与回收站清理等</p>
        </div>
      </div>
      <div class="job-hero-tags">
        <el-tag type="info" effect="plain">任务 {{ overview.totalJobs ?? 0 }}</el-tag>
        <el-tag type="success" effect="plain">运行中 {{ overview.runningJobs ?? 0 }}</el-tag>
        <el-tag type="warning" effect="plain">已暂停 {{ overview.pausedJobs ?? 0 }}</el-tag>
      </div>
    </div>

    <el-row :gutter="12" class="job-hero-stats">
      <el-col :span="6" :xs="12">
        <div class="job-hero-stat">
          <div class="job-hero-stat-value">{{ overview.totalCount ?? 0 }}</div>
          <div class="job-hero-stat-label">累计执行</div>
        </div>
      </el-col>
      <el-col :span="6" :xs="12">
        <div class="job-hero-stat is-success">
          <div class="job-hero-stat-value">{{ overview.successCount ?? 0 }}</div>
          <div class="job-hero-stat-label">成功</div>
        </div>
      </el-col>
      <el-col :span="6" :xs="12">
        <div class="job-hero-stat is-fail">
          <div class="job-hero-stat-value">{{ overview.failCount ?? 0 }}</div>
          <div class="job-hero-stat-label">失败</div>
        </div>
      </el-col>
      <el-col :span="6" :xs="12">
        <div class="job-hero-stat is-rate">
          <div class="job-hero-stat-value">{{ overview.successRate ?? 100 }}%</div>
          <div class="job-hero-stat-label">成功率</div>
        </div>
      </el-col>
    </el-row>
  </header>
</template>

<script setup lang="ts">
import type { JobOverview } from '@/api/monitor/job'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'

defineProps<{ overview: JobOverview }>()
</script>

<style scoped lang="scss">
.job-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-5);
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  border-radius: var(--radius-lg);
  background-color: var(--color-bg-surface);
  background-image:
    radial-gradient(ellipse 58% 115% at 96% -12%, var(--el-color-primary-light-9) 0%, transparent 66%),
    radial-gradient(ellipse 42% 90% at 4% 112%, var(--el-color-primary-light-9) 0%, transparent 62%);
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
}

.job-hero-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
  flex: 1;
  min-width: 280px;
}

.job-hero-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.job-hero-icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border: 1px solid var(--el-color-primary-light-7);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.job-hero-text {
  min-width: 0;
}

.job-hero-title {
  margin: 0;
  font-size: var(--text-h2);
  font-weight: 600;
  line-height: var(--leading-h2);
  color: var(--color-text-primary);
}

.job-hero-desc {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.job-hero-tags {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.job-hero-stats {
  flex: 1;
  min-width: 280px;
  max-width: 520px;
}

.job-hero-stat {
  text-align: center;
  padding: var(--space-3) var(--space-2);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.job-hero-stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.25;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-primary);
}

.job-hero-stat-label {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.job-hero-stat.is-success .job-hero-stat-value {
  color: var(--color-success);
}

.job-hero-stat.is-fail .job-hero-stat-value {
  color: var(--color-danger);
}

.job-hero-stat.is-rate .job-hero-stat-value {
  color: var(--el-color-primary);
}
</style>
