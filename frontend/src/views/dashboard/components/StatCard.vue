<template>
  <div
    class="stat-card"
    :class="[variant, { clickable }]"
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
          <el-icon :size="24"><component :is="icon" /></el-icon>
        </div>
        <div v-if="variant === 'core' && trend !== undefined" class="stat-trend-badge" :class="trend >= 0 ? 'up' : 'down'">
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
  color: #fff;
}

.stat-icon-box.user { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-icon-box.role { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.stat-icon-box.menu { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.stat-icon-box.dept { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.stat-icon-box.pending-user { background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%); }
.stat-icon-box.login-success { background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%); }
.stat-icon-box.file-store { background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); }
.stat-icon-box.post { background: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%); }

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
</style>
