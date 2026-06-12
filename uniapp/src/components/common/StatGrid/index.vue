<template>
  <view class="stat-grid" :class="{ 'stat-grid--cols-2': columns === 2 }">
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
    columns?: 2 | 4
  }>(),
  { columns: 2 },
)
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: $card-gap;
}

.stat-grid--cols-2 {
  grid-template-columns: repeat(2, 1fr);
}

.stat-grid__item {
  padding: 24rpx 20rpx;
  border-radius: $radius-md;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(244, 246, 250, 0.96));
  border: 1px solid $color-border-light;
  box-shadow: $shadow-card;
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
