<template>
  <view class="line-chart">
    <view v-if="title" class="line-chart__head">
      <text class="line-chart__title">{{ title }}</text>
      <text v-if="latestText" class="line-chart__latest">{{ latestText }}</text>
    </view>
    <view class="line-chart__body">
      <view class="line-chart__y-axis">
        <text>{{ yMax }}{{ unit }}</text>
        <text>{{ Math.round(yMax / 2) }}{{ unit }}</text>
        <text>0</text>
      </view>
      <view class="line-chart__plot">
        <view v-if="!plotPoints.length" class="line-chart__empty">
          <text>采样中…</text>
        </view>
        <svg
          v-else
          class="line-chart__svg"
          viewBox="0 0 300 100"
          preserveAspectRatio="none"
        >
          <defs>
            <linearGradient :id="gradientId" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" :stop-color="color" stop-opacity="0.28" />
              <stop offset="100%" :stop-color="color" stop-opacity="0.02" />
            </linearGradient>
          </defs>
          <polygon v-if="areaPoints" :points="areaPoints" :fill="`url(#${gradientId})`" />
          <polyline
            v-if="linePoints"
            :points="linePoints"
            fill="none"
            :stroke="color"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </view>
    </view>
    <view v-if="xLabels.length" class="line-chart__x-axis">
      <text v-for="(label, idx) in xLabels" :key="idx" class="line-chart__x-label">{{ label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    title?: string
    labels?: string[]
    values: (number | null)[]
    color?: string
    unit?: string
    yMax?: number
    latestText?: string
  }>(),
  {
    labels: () => [],
    color: '#409eff',
    unit: '%',
    yMax: 100,
  },
)

const gradientId = `chart-grad-${Math.random().toString(36).slice(2, 9)}`

const plotPoints = computed(() => {
  const vals = props.values
  if (!vals.length) return [] as Array<{ x: number; y: number }>
  const max = props.yMax || 100
  const lastIndex = Math.max(vals.length - 1, 1)
  return vals
    .map((value, index) => {
      if (value == null || Number.isNaN(value)) return null
      const x = (index / lastIndex) * 300
      const y = 100 - (Math.min(max, Math.max(0, value)) / max) * 100
      return { x, y }
    })
    .filter(Boolean) as Array<{ x: number; y: number }>
})

const linePoints = computed(() => plotPoints.value.map((p) => `${p.x},${p.y}`).join(' '))

const areaPoints = computed(() => {
  if (!plotPoints.value.length) return ''
  const base = plotPoints.value.map((p) => `${p.x},${p.y}`).join(' ')
  const first = plotPoints.value[0]
  const last = plotPoints.value[plotPoints.value.length - 1]
  return `${first.x},100 ${base} ${last.x},100`
})

const xLabels = computed(() => {
  const labels = props.labels
  if (!labels.length) return [] as string[]
  if (labels.length <= 3) return labels
  return [labels[0], labels[Math.floor(labels.length / 2)], labels[labels.length - 1]]
})
</script>

<style lang="scss" scoped>

.line-chart {
  margin-bottom: 24rpx;
}

.line-chart__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.line-chart__title {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.line-chart__latest {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.line-chart__body {
  display: flex;
  gap: 12rpx;
  height: 220rpx;
}

.line-chart__y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex-shrink: 0;
  width: 56rpx;
  padding: 4rpx 0;
  font-size: 20rpx;
  color: $color-text-placeholder;
  text-align: right;
}

.line-chart__plot {
  flex: 1;
  min-width: 0;
  border-radius: $radius-md;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.9) 0%, rgba(255, 255, 255, 0.6) 100%);
  border: 1px solid $color-border-light;
  overflow: hidden;
}

.line-chart__svg {
  width: 100%;
  height: 100%;
  display: block;
}

.line-chart__empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.line-chart__x-axis {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
  padding-left: 68rpx;
}

.line-chart__x-label {
  font-size: 20rpx;
  color: $color-text-placeholder;
}
</style>
