<template>
  <div class="section-card">
    <div class="section-header">
      <div class="section-title-wrapper">
        <div class="section-icon">
          <el-icon :size="18"><InfoFilled /></el-icon>
        </div>
        <span class="section-title">系统信息</span>
      </div>
      <el-tag type="success" size="small" effect="plain">
        <el-icon style="margin-right: 4px;"><CircleCheck /></el-icon>
        运行正常
      </el-tag>
    </div>
    <p v-if="platformSubtitle" class="sys-subtitle">{{ platformSubtitle }}</p>
    <ul class="sys-meta-list">
      <li v-for="item in metaList" :key="item.label" class="sys-meta-item">
        <span class="sys-meta-label">{{ item.label }}</span>
        <span class="sys-meta-value">
          <el-tag
            v-if="item.tag"
            :type="item.tag as 'success' | 'primary' | 'warning' | 'info' | 'danger'"
            size="small"
            effect="plain"
          >{{ item.value }}</el-tag>
          <template v-else>{{ item.value }}</template>
        </span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { InfoFilled, CircleCheck } from '@element-plus/icons-vue'

defineProps<{
  platformSubtitle: string
  metaList: Array<{ label: string; value: string; tag?: string }>
}>()
</script>

<style scoped>
.section-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  overflow: hidden;
  height: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, var(--theme-primary-hover, #374151) 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--theme-text-base, #1F2937);
}

.sys-subtitle {
  margin: 0 24px 4px;
  padding-bottom: 12px;
  font-size: 13px;
  color: var(--theme-text-secondary, #6b7280);
  line-height: 1.5;
  border-bottom: 1px solid #f3f4f6;
}

.sys-meta-list {
  list-style: none;
  margin: 0;
  padding: 8px 24px 24px;
}

.sys-meta-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 11px 0;
  font-size: 14px;
  line-height: 1.5;
  border-bottom: 1px solid #f3f4f6;
}

.sys-meta-item:last-child { border-bottom: none; }

.sys-meta-label {
  flex-shrink: 0;
  color: var(--theme-text-secondary, #6b7280);
}

.sys-meta-label::after { content: '：'; }

.sys-meta-value {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  color: var(--theme-text-base, #1f2937);
  word-break: break-word;
}
</style>
