<template>
  <view class="stat-grid" :class="`stat-grid--cols-${columns}`">
    <view
      v-for="item in items"
      :key="item.label"
      class="stat-grid__item"
      :class="item.tone ? `stat-grid__item--${item.tone}` : ''"
    >
      <text class="stat-grid__value">{{ item.value }}</text>
      <text class="stat-grid__label">{{ item.label }}</text>
      <text v-if="item.hint" class="stat-grid__hint">{{ item.hint }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
export interface StatItem {
  label: string
  value: string | number
  hint?: string
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

withDefaults(
  defineProps<{
    items: StatItem[]
    columns?: 2 | 3 | 4
  }>(),
  { columns: 2 },
)
</script>

<style lang="scss" scoped>

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: $card-gap;
}

.stat-grid--cols-2 {
  grid-template-columns: repeat(2, 1fr);
}

.stat-grid--cols-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
}

.stat-grid--cols-3 .stat-grid__item {
  padding: 16rpx 8rpx;
  border-radius: 14rpx;
  text-align: center;
  border-color: rgba(226, 232, 240, 0.7);
}

.stat-grid--cols-3 .stat-grid__value {
  font-size: 32rpx;
  line-height: 1.15;
}

.stat-grid--cols-3 .stat-grid__label {
  margin-top: 6rpx;
  font-size: 21rpx;
  white-space: nowrap;
}

.stat-grid--cols-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12rpx;
}

.stat-grid--cols-4 .stat-grid__item {
  padding: 18rpx 10rpx;
  text-align: center;
}

.stat-grid--cols-4 .stat-grid__value {
  font-size: 32rpx;
}

.stat-grid--cols-4 .stat-grid__label {
  margin-top: 4rpx;
  font-size: 20rpx;
  white-space: nowrap;
}

.stat-grid__item {
  padding: 24rpx 20rpx;
  border-radius: $radius-md;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.03);
}

.stat-grid__value {
  display: block;
  font-size: 40rpx;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.2;
}

.stat-grid__label {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.stat-grid__hint {
  display: block;
  margin-top: 4rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.stat-grid__item--primary .stat-grid__value { color: $color-primary; }
.stat-grid__item--success .stat-grid__value { color: $color-success; }
.stat-grid__item--warning .stat-grid__value { color: $color-warning; }
.stat-grid__item--danger .stat-grid__value { color: $color-danger; }
.stat-grid__item--info .stat-grid__value { color: #6366f1; }
</style>
