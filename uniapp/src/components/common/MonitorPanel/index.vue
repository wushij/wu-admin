<template>
  <view class="monitor-panel card--elevated">
    <view v-if="title" class="monitor-panel__head">
      <text class="monitor-panel__title">{{ title }}</text>
      <slot name="extra" />
    </view>
    <slot />
  </view>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
}>()
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/mixins.scss';

.monitor-panel {
  padding: 28rpx 32rpx;
  margin-bottom: $card-gap;
}

.monitor-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.monitor-panel__title {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}
</style>
