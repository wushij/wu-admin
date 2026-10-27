<template>
  <view class="metric-bar">
    <view class="metric-bar__head">
      <view class="metric-bar__label-wrap">
        <text class="metric-bar__label">{{ label }}</text>
        <text v-if="hint" class="metric-bar__hint">{{ hint }}</text>
      </view>
      <text class="metric-bar__value" :class="levelTextClass">{{ valueText }}</text>
    </view>
    <view v-if="showBar" class="metric-bar__track">
      <view class="metric-bar__fill" :class="levelFillClass" :style="{ width: `${percent}%` }" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  percent?: number | null
  /** 右侧主数值，默认显示百分比 */
  display?: string
  /** 标签下方辅助说明，如容量 */
  hint?: string
}>()

const hasPercent = computed(() => props.percent != null && !Number.isNaN(props.percent))

const percent = computed(() => {
  if (!hasPercent.value) return 0
  return Math.min(100, Math.max(0, props.percent as number))
})

const showBar = computed(() => hasPercent.value)

const level = computed(() => {
  const p = percent.value
  if (p >= 85) return 'danger'
  if (p >= 60) return 'warning'
  return 'success'
})

const levelTextClass = computed(() => `metric-bar__value--${level.value}`)
const levelFillClass = computed(() => `metric-bar__fill--${level.value}`)

const valueText = computed(() => {
  if (props.display) return props.display
  if (!hasPercent.value) return '—'
  return `${percent.value.toFixed(1)}%`
})
</script>

<style lang="scss" scoped>

.metric-bar {
  padding: 20rpx 0;

  &:not(:last-child) {
    border-bottom: 1px solid $color-border-light;
  }
}

.metric-bar__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  margin-bottom: 16rpx;
}

.metric-bar__label-wrap {
  flex: 1;
  min-width: 0;
}

.metric-bar__label {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  line-height: 1.4;
}

.metric-bar__hint {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.45;
  word-break: break-all;
}

.metric-bar__value {
  flex-shrink: 0;
  font-size: $font-size-base;
  font-weight: $font-weight-bold;
  line-height: 1.4;
}

.metric-bar__track {
  height: 20rpx;
  border-radius: 999rpx;
  background: #eef1f5;
  overflow: hidden;
}

.metric-bar__fill {
  height: 100%;
  min-width: 0;
  border-radius: 999rpx;
  transition: width 0.35s ease;
}

.metric-bar__value--success {
  color: #67c23a;
}

.metric-bar__value--warning {
  color: #e6a23c;
}

.metric-bar__value--danger {
  color: #f56c6c;
}

.metric-bar__fill--success {
  background: linear-gradient(90deg, #85ce61, #67c23a);
}

.metric-bar__fill--warning {
  background: linear-gradient(90deg, #f3c76a, #e6a23c);
}

.metric-bar__fill--danger {
  background: linear-gradient(90deg, #f78989, #f56c6c);
}
</style>
