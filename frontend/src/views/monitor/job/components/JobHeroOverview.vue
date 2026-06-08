<template>
  <div class="job-hero">
    <div class="hero-left">
      <h2 class="hero-title">定时任务调度中心</h2>
      <p class="hero-desc">基于 Quartz 管理维护任务：日志归档、聊天消息清理、通知与回收站清理等</p>
      <div class="hero-tags">
        <el-tag type="info" effect="plain">任务 {{ overview.totalJobs ?? 0 }}</el-tag>
        <el-tag type="success" effect="plain">运行中 {{ overview.runningJobs ?? 0 }}</el-tag>
        <el-tag type="warning" effect="plain">已暂停 {{ overview.pausedJobs ?? 0 }}</el-tag>
      </div>
    </div>
    <el-row :gutter="12" class="hero-stats">
      <el-col :span="6" :xs="12">
        <div class="hero-stat">
          <div class="hero-stat-value">{{ overview.totalCount ?? 0 }}</div>
          <div class="hero-stat-label">累计执行</div>
        </div>
      </el-col>
      <el-col :span="6" :xs="12">
        <div class="hero-stat success">
          <div class="hero-stat-value">{{ overview.successCount ?? 0 }}</div>
          <div class="hero-stat-label">成功</div>
        </div>
      </el-col>
      <el-col :span="6" :xs="12">
        <div class="hero-stat fail">
          <div class="hero-stat-value">{{ overview.failCount ?? 0 }}</div>
          <div class="hero-stat-label">失败</div>
        </div>
      </el-col>
      <el-col :span="6" :xs="12">
        <div class="hero-stat rate">
          <div class="hero-stat-value">{{ overview.successRate ?? 100 }}%</div>
          <div class="hero-stat-label">成功率</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import type { JobOverview } from '@/api/monitor/job'

defineProps<{ overview: JobOverview }>()
</script>

<style scoped>
.job-hero {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px 28px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, #374151 100%);
  color: #fff;
}
.hero-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
}
.hero-desc {
  margin: 0 0 12px;
  font-size: 13px;
  opacity: 0.85;
  max-width: 480px;
}
.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.hero-stats {
  flex: 1;
  min-width: 280px;
  max-width: 520px;
}
.hero-stat {
  text-align: center;
  padding: 12px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
}
.hero-stat-value {
  font-size: 24px;
  font-weight: 700;
}
.hero-stat-label {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 4px;
}
.hero-stat.success .hero-stat-value { color: #95de64; }
.hero-stat.fail .hero-stat-value { color: #ff9c9c; }
.hero-stat.rate .hero-stat-value { color: #91d5ff; }
</style>
