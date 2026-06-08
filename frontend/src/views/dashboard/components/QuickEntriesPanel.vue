<template>
  <div class="section-card">
    <div class="section-header">
      <div class="section-title-wrapper">
        <div class="section-icon">
          <el-icon :size="18"><Grid /></el-icon>
        </div>
        <span class="section-title">快捷入口</span>
      </div>
      <el-tag type="info" size="small" effect="plain">常用功能</el-tag>
    </div>
    <div class="quick-grid">
      <div
        v-for="item in quickEntries"
        :key="item.key"
        v-permission="item.permission"
        class="quick-item"
        @click="goQuick(item)"
      >
        <div class="quick-icon-wrapper" :class="item.theme">
          <el-icon :size="26"><component :is="item.icon" /></el-icon>
        </div>
        <div class="quick-info">
          <div class="quick-name">{{ item.name }}</div>
          <div class="quick-desc">{{ item.desc }}</div>
        </div>
        <el-icon class="quick-arrow"><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Grid, ArrowRight } from '@element-plus/icons-vue'
import { quickEntries, type QuickEntry } from '../constants/quickEntries'

const router = useRouter()

function goQuick(item: QuickEntry) {
  if (item.query) {
    router.push({ path: item.path, query: item.query })
  } else {
    router.push(item.path)
  }
}
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

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 24px;
}

.quick-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
  gap: 14px;
}

.quick-item:hover {
  background: #fff;
  border-color: var(--theme-primary, #111827);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.quick-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  transition: all 0.3s;
}

.quick-item:hover .quick-icon-wrapper { transform: scale(1.05); }

.quick-icon-wrapper.user { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.quick-icon-wrapper.menu { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.quick-icon-wrapper.dict { background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%); }
.quick-icon-wrapper.config { background: linear-gradient(135deg, #64748b 0%, #475569 100%); }
.quick-icon-wrapper.approval { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); }
.quick-icon-wrapper.chat { background: linear-gradient(135deg, #f97316 0%, #fb923c 100%); }
.quick-icon-wrapper.notice { background: linear-gradient(135deg, #ec4899 0%, #f472b6 100%); }
.quick-icon-wrapper.job { background: linear-gradient(135deg, #14b8a6 0%, #2dd4bf 100%); }
.quick-icon-wrapper.monitor { background: linear-gradient(135deg, #0ea5e9 0%, #38bdf8 100%); }
.quick-icon-wrapper.server { background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%); }
.quick-icon-wrapper.cache { background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%); }
.quick-icon-wrapper.log { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }

.quick-info { flex: 1; min-width: 0; }

.quick-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--theme-text-base, #1F2937);
  margin-bottom: 4px;
}

.quick-desc {
  font-size: 12px;
  color: var(--theme-text-secondary, #6B7280);
}

.quick-arrow {
  color: #d0d5dd;
  transition: all 0.3s;
  flex-shrink: 0;
}

.quick-item:hover .quick-arrow {
  color: var(--theme-primary, #111827);
  transform: translateX(4px);
}

@media (max-width: 1200px) {
  .quick-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .quick-grid { grid-template-columns: repeat(2, 1fr); padding: 16px; }
}

@media (max-width: 640px) {
  .quick-grid { grid-template-columns: 1fr; }
}
</style>
