<template>
  <view class="data-card" @click="emit('click')">
    <view class="data-card__icon" :class="`grad-${theme}`">
      <IconFont :name="icon" :size="36" color="#ffffff" />
    </view>
    <view class="data-card__body">
      <text class="data-card__value">{{ value }}</text>
      <text class="data-card__title">{{ title }}</text>
      <text v-if="footer" class="data-card__footer">{{ footer }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/common/IconFont/index.vue'
import type { IconName } from '@/constants/iconfont'

defineProps<{
  title: string
  value: string | number
  icon: IconName
  theme: string
  footer?: string
}>()

const emit = defineEmits<{ click: [] }>()
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.data-card {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 24rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  box-shadow: $shadow-card;
  transition: transform 0.15s ease;
}

.data-card:active {
  transform: scale(0.98);
}

.data-card__icon {
  flex-shrink: 0;
  width: 64rpx;
  height: 64rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.data-card__body {
  flex: 1;
  min-width: 0;
}

.data-card__value {
  display: block;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.2;
}

.data-card__title {
  display: block;
  margin-top: 4rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.data-card__footer {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: $color-text-placeholder;
}
</style>
