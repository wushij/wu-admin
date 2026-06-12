<template>
  <view class="metric-bar">
    <view class="metric-bar__head">
      <text class="metric-bar__label">{{ label }}</text>
      <text class="metric-bar__value" :class="levelClass">{{ display }}</text>
    </view>
    <view class="metric-bar__track">
      <view class="metric-bar__fill" :class="levelClass" :style="{ width: `${percent}%` }" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  percent?: number | null
  display?: string
}>()

const percent = computed(() => {
  const v = props.percent ?? 0
  return Math.min(100, Math.max(0, v))
})

const levelClass = computed(() => {
  const p = percent.value
  if (p >= 85) return 'metric-bar--danger'
  if (p >= 60) return 'metric-bar--warning'
  return 'metric-bar--success'
})

const display = computed(() => props.display ?? `${percent.value.toFixed(1)}%`)
</script>

<style lang="scss" scoped>
.metric-bar {
  margin-bottom: 24rpx;
}

.metric-bar__head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.metric-bar__label {
  font-size: 28rpx;
  color: #1e293b;
}

.metric-bar__value {
  font-size: 28rpx;
  font-weight: 600;
}

.metric-bar__track {
  height: 16rpx;
  border-radius: 999rpx;
  background: #f0f0f0;
  overflow: hidden;
}

.metric-bar__fill {
  height: 100%;
  border-radius: 999rpx;
  transition: width 0.3s;
}

.metric-bar--success {
  color: #67c23a;
  background: #67c23a;
}

.metric-bar--warning {
  color: #e6a23c;
  background: #e6a23c;
}

.metric-bar--danger {
  color: #f56c6c;
  background: #f56c6c;
}
</style>
