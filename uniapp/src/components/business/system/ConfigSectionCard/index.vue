<template>
  <view class="config-card">
    <view v-if="title" class="config-card__head">
      <text class="config-card__title">{{ title }}</text>
      <slot name="extra" />
    </view>
    <view class="config-card__body" :class="{ 'config-card__body--plain': !title }">
      <slot />
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{ title?: string }>()
</script>

<style lang="scss" scoped>

.config-card {
  margin-bottom: 20rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  overflow: hidden;
}

.config-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx;
  background: $color-bg-muted;
  border-bottom: 1px solid $color-border-light;
}

.config-card__title {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.config-card__body--plain {
  border-radius: $radius-lg;
}
</style>
