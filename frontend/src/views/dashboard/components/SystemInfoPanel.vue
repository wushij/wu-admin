<template>
  <!-- 单点克制使用 Border Beam 强调系统运行概览卡 -->
  <section class="section-card motion-border-beam">
    <div class="section-header">
      <div class="section-title-wrapper">
        <div class="section-icon" aria-hidden="true">
          <el-icon :size="18"><InfoFilled /></el-icon>
        </div>
        <h2 class="section-title">系统信息</h2>
      </div>
      <el-tag type="success" size="small" effect="plain">
        <span class="status-tag-inner">
          <el-icon><CircleCheck /></el-icon>
          <span>运行正常</span>
        </span>
      </el-tag>
    </div>
    <p v-if="platformSubtitle" class="sys-subtitle">{{ platformSubtitle }}</p>
    <ul class="sys-meta-list">
      <li v-for="item in metaList" :key="item.label" class="sys-meta-item">
        <span class="sys-meta-label">{{ item.label }}</span>
        <span class="sys-meta-value motion-tabular">
          <el-tag
            v-if="item.tag"
            :type="item.tag as 'success' | 'primary' | 'warning' | 'info' | 'danger'"
            size="small"
            effect="plain"
          >
            {{ item.value }}
          </el-tag>
          <template v-else>{{ item.value }}</template>
        </span>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { InfoFilled, CircleCheck } from '@element-plus/icons-vue'

defineProps<{
  platformSubtitle: string
  metaList: Array<{ label: string; value: string; tag?: string }>
}>()
</script>

<style scoped lang="scss">
.section-card {
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border-subtle);
}

.section-title-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.section-icon {
  width: 32px;
  height: 32px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
}

.section-title {
  margin: 0;
  font-size: var(--text-h3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.status-tag-inner {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.sys-subtitle {
  margin: var(--space-3) var(--space-5) 0;
  padding-bottom: var(--space-3);
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  border-bottom: 1px solid var(--color-border-subtle);
}

.sys-meta-list {
  list-style: none;
  margin: 0;
  padding: var(--space-2) var(--space-5) var(--space-4);
}

.sys-meta-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: 10px 0;
  font-size: 13.5px;
  line-height: 1.5;
  border-bottom: 1px solid var(--color-border-subtle);
}

.sys-meta-item:last-child {
  border-bottom: none;
}

.sys-meta-label {
  flex-shrink: 0;
  color: var(--color-text-secondary);
}

.sys-meta-value {
  text-align: right;
  font-weight: 600;
  color: var(--color-text-primary);
  word-break: break-word;
}
</style>
