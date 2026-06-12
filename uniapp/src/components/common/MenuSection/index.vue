<template>
  <view class="menu-section">
    <text v-if="title" class="menu-section__title">{{ title }}</text>
    <view class="menu-section__card card--elevated">
      <slot />
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
}>()
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.menu-section {
  margin-bottom: $section-gap;
}

.menu-section__title {
  display: block;
  margin-bottom: 16rpx;
  padding: 0 8rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  letter-spacing: 0.04em;
}

.menu-section__card {
  overflow: hidden;
}
</style>
