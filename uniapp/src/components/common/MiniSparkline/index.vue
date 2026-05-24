<template>
  <view v-if="points.length" class="sparkline">
    <text v-if="title" class="sparkline__title">{{ title }}</text>
    <view class="sparkline__body">
      <view v-for="(p, i) in points" :key="i" class="sparkline__col">
        <view class="sparkline__bar-wrap">
          <view class="sparkline__bar" :style="{ height: p.height, background: barColor(p.value) }" />
        </view>
      </view>
    </view>
    <view v-if="latestLabel" class="sparkline__foot">
      <text class="sparkline__latest">{{ latestLabel }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  title?: string
  values: number[]
  max?: number
  scaleMax?: number
  latestLabel?: string
}>()

const points = computed(() => {
  const vals = props.values.filter((v) => !Number.isNaN(v))
  if (!vals.length) return []
  const max = props.scaleMax ?? props.max ?? Math.max(...vals, 1)
  return vals.map((v) => ({
    value: v,
    height: `${Math.max(6, (v / max) * 100)}%`,
  }))
})

function barColor(v: number) {
  if (v >= 85) return 'linear-gradient(180deg, #f87171, #ef4444)'
  if (v >= 60) return 'linear-gradient(180deg, #fbbf24, #f59e0b)'
  return 'linear-gradient(180deg, #818cf8, #6366f1)'
}
</script>

<style lang="scss" scoped>

.sparkline {
  margin-top: 8rpx;
}

.sparkline__title {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.sparkline__body {
  display: flex;
  align-items: flex-end;
  gap: 6rpx;
  height: 120rpx;
}

.sparkline__col {
  flex: 1;
  height: 100%;
}

.sparkline__bar-wrap {
  height: 100%;
  display: flex;
  align-items: flex-end;
}

.sparkline__bar {
  width: 100%;
  min-height: 6rpx;
  border-radius: 6rpx 6rpx 2rpx 2rpx;
  transition: height 0.35s ease;
}

.sparkline__foot {
  margin-top: 8rpx;
}

.sparkline__latest {
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}
</style>
