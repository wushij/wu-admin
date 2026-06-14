<template>
  <view class="ring-progress">
    <view class="ring-progress__ring" :style="ringStyle">
      <view class="ring-progress__inner">
        <text class="ring-progress__value">{{ displayValue }}</text>
        <text class="ring-progress__label">{{ label }}</text>
      </view>
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

const clamped = computed(() => {
  const v = props.percent ?? 0
  return Math.min(100, Math.max(0, v))
})

const levelColor = computed(() => {
  const p = clamped.value
  if (p >= 85) return '#f56c6c'
  if (p >= 60) return '#e6a23c'
  return '#67c23a'
})

const displayValue = computed(() => props.display ?? `${clamped.value.toFixed(0)}%`)

const ringStyle = computed(() => {
  const p = clamped.value
  const color = levelColor.value
  return {
    background: `conic-gradient(${color} 0% ${p}%, #eef0f3 ${p}% 100%)`,
  }
})
</script>

<style lang="scss" scoped>

.ring-progress {
  display: flex;
  justify-content: center;
}

.ring-progress__ring {
  width: 176rpx;
  height: 176rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.4s ease;
}

.ring-progress__inner {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: $color-bg-card;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px $color-border-light;
}

.ring-progress__value {
  font-size: 32rpx;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.2;
}

.ring-progress__label {
  margin-top: 4rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  text-align: center;
  padding: 0 8rpx;
}
</style>
