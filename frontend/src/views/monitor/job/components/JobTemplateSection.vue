<template>
  <el-card shadow="never" class="template-card">
    <template #header>
      <div class="card-header-row">
        <span>快捷模板</span>
        <span class="header-tip">点击卡片一键创建任务（默认暂停，确认后可启用）</span>
      </div>
    </template>
    <el-row :gutter="12">
      <el-col v-for="tpl in templates" :key="tpl.key" :xs="24" :sm="12" :lg="8" :xl="6">
        <div class="template-item" @click="$emit('create-from-template', tpl)">
          <div class="template-icon">{{ tplIcon(tpl.icon) }}</div>
          <div class="template-body">
            <div class="template-name">{{ tpl.name }}</div>
            <div class="template-desc">{{ tpl.description }}</div>
            <div class="template-meta">
              <el-tag size="small" type="info">{{ tpl.relatedModule }}</el-tag>
              <span class="template-cron">{{ tpl.cronExpression }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup lang="ts">
import type { JobTemplate } from '@/api/monitor/job'
import { tplIcon } from '../constants/cronPresets'

defineProps<{ templates: JobTemplate[] }>()
defineEmits<{ 'create-from-template': [tpl: JobTemplate] }>()
</script>

<style scoped>
.template-card {
  margin-bottom: 16px;
}
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.header-tip {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}
.template-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}
.template-item:hover {
  border-color: var(--theme-primary, #111827);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}
.template-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
}
.template-name {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}
.template-desc {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  margin-bottom: 8px;
}
.template-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.template-cron {
  font-size: 11px;
  color: #909399;
  font-family: monospace;
}
</style>
