<template>
  <el-card shadow="never" class="template-card">
    <template #header>
      <div class="card-header-row">
        <div class="header-left">
          <el-icon class="header-icon"><Opportunity /></el-icon>
          <span>快捷模板</span>
        </div>
        <span class="header-tip">点击卡片一键创建任务（默认暂停，确认后可启用）</span>
      </div>
    </template>
    <el-row :gutter="12">
      <el-col v-for="tpl in templates" :key="tpl.key" :xs="24" :sm="12" :lg="8" :xl="6">
        <div class="template-item" @click="$emit('create-from-template', tpl)">
          <div
            class="template-icon-wrap"
            :style="{
              color: getJobTemplateVisual(tpl).color,
              background: getJobTemplateVisual(tpl).bgColor,
              borderColor: getJobTemplateVisual(tpl).borderColor,
            }"
          >
            <el-icon :size="22">
              <component :is="getJobTemplateVisual(tpl).component" />
            </el-icon>
          </div>
          <div class="template-body">
            <div class="template-title-row">
              <div class="template-name">{{ tpl.name }}</div>
              <el-icon class="template-arrow"><Right /></el-icon>
            </div>
            <div class="template-desc" :title="tpl.description">{{ tpl.description }}</div>
            <div class="template-meta">
              <el-tag size="small" :type="getJobTemplateVisual(tpl).tagType || 'info'" effect="light">
                {{ tpl.relatedModule }}
              </el-tag>
              <span class="template-cron">
                <el-icon :size="12" class="cron-clock-icon"><Clock /></el-icon>
                <code>{{ tpl.cronExpression }}</code>
              </span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup lang="ts">
import { Opportunity, Right, Clock } from '@element-plus/icons-vue'
import type { JobTemplate } from '@/api/monitor/job'
import { getJobTemplateVisual } from '../constants/cronPresets'

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
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: var(--el-text-color-primary);
}
.header-icon {
  color: var(--theme-primary, #2563eb);
  font-size: 18px;
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
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition:
    border-color var(--duration-normal) var(--ease-out),
    box-shadow var(--duration-normal) var(--ease-out),
    transform var(--duration-normal) var(--ease-out);
  background: var(--color-bg-surface);
  position: relative;
  overflow: hidden;
}
.template-item:hover {
  border-color: var(--theme-primary, #2563eb);
  box-shadow: 0 8px 24px -4px rgba(15, 23, 42, 0.08), 0 2px 6px -1px rgba(15, 23, 42, 0.04);
}
.template-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  border: 1px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.template-body {
  flex: 1;
  min-width: 0;
}
.template-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}
.template-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  transition: color 0.2s ease;
}
.template-item:hover .template-name {
  color: var(--theme-primary, #2563eb);
}
.template-arrow {
  font-size: 13px;
  color: #94a3b8;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.2s ease;
}
.template-item:hover .template-arrow {
  opacity: 1;
  transform: translateX(0);
  color: var(--theme-primary, #2563eb);
}
.template-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  margin-bottom: 8px;
  min-height: 36px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}
.template-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.template-cron {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--color-text-secondary);
  background: var(--el-fill-color-light, var(--color-bg-subtle));
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-extra-light, var(--color-border-subtle));
}
.cron-clock-icon {
  color: var(--color-text-muted);
}
.template-cron code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>
