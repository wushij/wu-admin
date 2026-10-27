<template>
  <div
    class="stat-card"
    :class="[variant, { clickable, compact }]"
    @click="handleClick"
  >
    <template v-if="variant === 'biz'">
      <div class="stat-card-body inline-biz">
        <span class="biz-label">{{ title }}</span>
        <span class="biz-value" :class="bizValueClass">{{ displayValue }}</span>
      </div>
    </template>
    <template v-else>
      <div class="stat-card-header">
        <div class="stat-icon-box" :class="iconTheme">
          <el-icon :size="compact ? 18 : 24"><component :is="icon" /></el-icon>
        </div>
        <div
          v-if="variant === 'core' && trend !== undefined && !compact"
          class="stat-trend-badge"
          :class="trend >= 0 ? 'up' : 'down'"
        >
          <el-icon><Top v-if="trend >= 0" /><Bottom v-else /></el-icon>
          <span>{{ Math.abs(trend) }}%</span>
        </div>
      </div>
      <div class="stat-card-body">
        <div class="stat-number">{{ displayValue }}</div>
        <div class="stat-title">{{ title }}</div>
      </div>
      <div v-if="footer" class="stat-card-footer"><span>{{ footer }}</span></div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { useRouter } from 'vue-router'
import { Top, Bottom } from '@element-plus/icons-vue'

const props = defineProps<{
  variant: 'core' | 'ops' | 'biz'
  title: string
  value?: number | string
  trend?: number
  icon?: Component
  iconTheme?: string
  footer?: string
  clickable?: boolean
  compact?: boolean
  to?: RouteLocationRaw
  bizValueClass?: 'warn' | 'danger' | ''
}>()

const router = useRouter()
const displayValue = computed(() => props.value ?? 0)

function handleClick() {
  if (!props.clickable || !props.to) return
  router.push(props.to)
}
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  min-height: 180px;
  display: flex;
  flex-direction: column;
}

.stat-card.ops {
  min-height: 160px;
}

.stat-card.compact {
  min-height: 0;
  padding: 14px 16px;
  border-radius: 12px;
}

.stat-card.compact .stat-card-header {
  margin-bottom: 10px;
}

.stat-card.compact .stat-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 10px;
}

.stat-card.compact .stat-trend-badge {
  padding: 2px 6px;
  font-size: 11px;
}

.stat-card.compact .stat-number {
  font-size: 24px;
  margin-bottom: 4px;
}

.stat-card.compact .stat-title {
  font-size: 12px;
}

.stat-card.compact .stat-card-body {
  margin-bottom: 8px;
}

.stat-card.compact .stat-card-footer {
  padding-top: 8px;
  font-size: 11px;
}

.stat-card.compact:hover {
  transform: translateY(-2px);
}

.stat-card.biz {
  min-height: auto;
  padding: 16px 20px;
}

.stat-card.biz .inline-biz {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
}

.biz-label {
  font-size: 14px;
  color: #6b7280;
}

.biz-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--theme-primary, #111827);
}

.biz-value.warn { color: #d97706; }
.biz-value.danger { color: #dc2626; }

.stat-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.stat-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  color: #475569;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 浅色底 + 同色系图标，保持图标图形不变 */
.stat-icon-box.user { background: #eef2ff; color: #4f46e5; border-color: #e0e7ff; }
.stat-icon-box.role { background: #fff1f2; color: #e11d48; border-color: #ffe4e6; }
.stat-icon-box.menu { background: #eff6ff; color: #2563eb; border-color: #dbeafe; }
.stat-icon-box.dept { background: #ecfdf5; color: #059669; border-color: #d1fae5; }
.stat-icon-box.pending-user { background: #fffbeb; color: #d97706; border-color: #fef3c7; }
.stat-icon-box.login-success { background: #f0fdf4; color: #16a34a; border-color: #dcfce7; }
.stat-icon-box.file-store { background: #f5f3ff; color: #7c3aed; border-color: #ede9fe; }
.stat-icon-box.post { background: #ecfeff; color: #0891b2; border-color: #cffafe; }
.stat-icon-box.online { background: #eef2ff; color: #6366f1; border-color: #e0e7ff; }
.stat-icon-box.visit { background: #f0f9ff; color: #0284c7; border-color: #e0f2fe; }
.stat-icon-box.approval { background: #faf5ff; color: #9333ea; border-color: #f3e8ff; }
.stat-icon-box.ticket { background: #fff7ed; color: #ea580c; border-color: #ffedd5; }
.stat-icon-box.job { background: #f0fdfa; color: #0d9488; border-color: #ccfbf1; }

.stat-trend-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.stat-trend-badge.up { background: #f0fdf4; color: #16a34a; }
.stat-trend-badge.down { background: #fef2f2; color: #dc2626; }

.stat-card-body {
  flex: 1;
  margin-bottom: 12px;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: var(--theme-text-base, #1F2937);
  line-height: 1;
  margin-bottom: 8px;
}

.stat-title {
  font-size: 14px;
  color: var(--theme-text-secondary, #6B7280);
  font-weight: 500;
}

.stat-card-footer {
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: var(--theme-text-secondary, #6B7280);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: var(--theme-primary, #111827);
}

.stat-card:hover .stat-icon-box {
  transform: scale(1.06);
}
</style>
