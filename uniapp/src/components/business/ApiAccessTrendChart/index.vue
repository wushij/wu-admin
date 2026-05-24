<template>
  <view v-if="points.length" class="trend-chart card--elevated">
    <text class="trend-chart__title">近 {{ points.length }} 日请求趋势</text>
    <view class="trend-chart__body">
      <view v-for="item in points" :key="item.key" class="trend-chart__col">
        <view class="trend-chart__bar-wrap">
          <view class="trend-chart__bar" :style="{ height: item.height }">
            <view v-if="item.failHeight" class="trend-chart__bar-fail" :style="{ height: item.failHeight }" />
          </view>
        </view>
        <text class="trend-chart__count">{{ item.total }}</text>
        <text class="trend-chart__label">{{ item.label }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ApiAccessDailyStat } from '@/types/system'

const props = defineProps<{
  dailyStats?: Record<string, ApiAccessDailyStat>
  maxDays?: number
}>()

const points = computed(() => {
  const map = props.dailyStats || {}
  const keys = Object.keys(map).sort().slice(-(props.maxDays ?? 7))
  if (!keys.length) return []
  const maxTotal = Math.max(...keys.map((k) => map[k]?.total ?? 0), 1)
  return keys.map((key) => {
    const stat = map[key] || {}
    const total = stat.total ?? 0
    const fail = stat.fail ?? 0
    const heightPct = `${Math.max(8, (total / maxTotal) * 100)}%`
    const failPct = total > 0 ? `${(fail / total) * 100}%` : '0%'
    const label = key.length >= 10 ? key.slice(5) : key
    return {
      key,
      label,
      total,
      height: heightPct,
      failHeight: fail > 0 ? failPct : '',
    }
  })
})
</script>

<style lang="scss" scoped>

.trend-chart {
  padding: 28rpx 32rpx;
  margin-bottom: $card-gap;
}

.trend-chart__title {
  display: block;
  margin-bottom: 24rpx;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.trend-chart__body {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8rpx;
  height: 220rpx;
}

.trend-chart__col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
}

.trend-chart__bar-wrap {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.trend-chart__bar {
  position: relative;
  width: 36rpx;
  min-height: 8rpx;
  border-radius: 8rpx 8rpx 4rpx 4rpx;
  background: linear-gradient(180deg, #6366f1, #818cf8);
  transition: height 0.35s ease;
  overflow: hidden;
}

.trend-chart__bar-fail {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(245, 108, 108, 0.85);
}

.trend-chart__count {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: $color-text-secondary;
}

.trend-chart__label {
  margin-top: 4rpx;
  font-size: 20rpx;
  color: $color-text-placeholder;
  transform: scale(0.92);
}
</style>
