<template>
  <div
    class="stat-card motion-hover-lift"
    :class="[variant, { clickable, compact }]"
    :role="clickable ? 'button' : undefined"
    :tabindex="clickable ? 0 : undefined"
    @click="handleClick"
    @keydown.enter.prevent="handleClick"
  >
    <template v-if="variant === 'biz'">
      <div class="stat-card-body inline-biz">
        <span class="biz-label">{{ title }}</span>
        <span class="biz-value motion-tabular" :class="bizValueClass">
          <CountUpNumber v-if="typeof displayValue === 'number'" :value="displayValue" />
          <template v-else>{{ displayValue }}</template>
        </span>
      </div>
    </template>
    <template v-else>
      <div class="stat-card-header">
        <div class="stat-icon-box" :class="iconTheme" aria-hidden="true">
          <el-icon :size="compact ? 18 : 22"><component :is="icon" /></el-icon>
        </div>
        <div
          v-if="variant === 'core' && trend !== undefined && !compact"
          class="stat-trend-badge"
          :class="trend >= 0 ? 'up' : 'down'"
        >
          <el-icon><Top v-if="trend >= 0" /><Bottom v-else /></el-icon>
          <span class="motion-tabular">{{ Math.abs(trend) }}%</span>
        </div>
      </div>
      <div class="stat-card-body">
        <div class="stat-number motion-tabular">
          <CountUpNumber v-if="typeof displayValue === 'number'" :value="displayValue" />
          <template v-else>{{ displayValue }}</template>
        </div>
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
import CountUpNumber from '@/components/ui/CountUpNumber.vue'

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

<style scoped lang="scss">
.stat-card {
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  min-height: 168px;
  display: flex;
  flex-direction: column;

  &.clickable {
    cursor: pointer;
  }

  &.ops {
    min-height: 152px;
  }

  &.compact {
    min-height: 0;
    padding: var(--space-4);
    border-radius: var(--radius-lg);
  }
}

.stat-card.compact .stat-card-header {
  margin-bottom: var(--space-3);
}

.stat-card.compact .stat-icon-box {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
}

.stat-card.compact .stat-number {
  font-size: 24px;
  margin-bottom: 4px;
}

.stat-card.compact .stat-title {
  font-size: 12.5px;
}

.stat-card.compact .stat-card-body {
  margin-bottom: var(--space-2);
}

.stat-card.compact .stat-card-footer {
  padding-top: var(--space-2);
  font-size: 11.5px;
}

.stat-card.biz {
  min-height: auto;
  padding: var(--space-4) var(--space-5);
}

.stat-card.biz .inline-biz {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
}

.biz-label {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.biz-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.biz-value.warn { color: var(--color-warning); }
.biz-value.danger { color: var(--color-danger); }

.stat-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-4);
}

.stat-icon-box {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-subtle);
  color: var(--el-color-primary);
  border: 1px solid var(--color-border-subtle);
  transition: transform var(--duration-fast) var(--ease-out);
}

.stat-icon-box.user { background: rgba(79, 70, 229, 0.1); color: #6366f1; border-color: rgba(99, 102, 241, 0.22); }
.stat-icon-box.role { background: rgba(225, 29, 72, 0.1); color: #f43f5e; border-color: rgba(244, 63, 94, 0.22); }
.stat-icon-box.menu { background: rgba(37, 99, 235, 0.1); color: #3b82f6; border-color: rgba(59, 130, 246, 0.22); }
.stat-icon-box.dept { background: rgba(5, 150, 105, 0.1); color: #10b981; border-color: rgba(16, 185, 129, 0.22); }
.stat-icon-box.file-store { background: rgba(124, 58, 237, 0.1); color: #8b5cf6; border-color: rgba(139, 92, 246, 0.22); }
.stat-icon-box.post { background: rgba(8, 145, 178, 0.1); color: #06b6d4; border-color: rgba(6, 182, 212, 0.22); }
.stat-icon-box.online { background: rgba(99, 102, 241, 0.1); color: #6366f1; border-color: rgba(99, 102, 241, 0.22); }
.stat-icon-box.approval { background: rgba(147, 51, 234, 0.1); color: #a855f7; border-color: rgba(168, 85, 247, 0.22); }
.stat-icon-box.ticket { background: rgba(234, 88, 12, 0.1); color: #f97316; border-color: rgba(249, 115, 22, 0.22); }
.stat-icon-box.job { background: rgba(13, 148, 136, 0.1); color: #14b8a6; border-color: rgba(20, 184, 166, 0.22); }

.stat-trend-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--text-caption);
  font-weight: 600;
}

.stat-trend-badge.up {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.stat-trend-badge.down {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.stat-card-body {
  flex: 1;
  margin-bottom: var(--space-3);
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.1;
  margin-bottom: 6px;
}

.stat-title {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  font-weight: 500;
}

.stat-card-footer {
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-border-subtle);
  font-size: 12.5px;
  color: var(--color-text-muted);
}

@media (hover: hover) and (pointer: fine) {
  .stat-card:hover {
    border-color: var(--el-color-primary-light-5);
  }
}
</style>
