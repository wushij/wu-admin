<template>
  <view class="empty-state">
    <view class="empty-state__illus">
      <IconFont v-if="icon" :name="icon" :size="80" color="#c0c4cc" />
    </view>
    <text class="empty-state__title">{{ title }}</text>
    <text v-if="description" class="empty-state__desc">{{ description }}</text>
    <slot />
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/common/IconFont/index.vue'
import type { IconName } from '@/constants/iconfont'

withDefaults(
  defineProps<{
    title?: string
    description?: string
    icon?: IconName
  }>(),
  {
    title: '暂无数据',
  },
)
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 48rpx;
}

.empty-state__illus {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 24rpx;
  border-radius: 50%;
  background: linear-gradient(180deg, $color-bg-muted 0%, $color-bg-page 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state__title {
  font-size: $font-size-base;
  color: $color-text-secondary;
}

.empty-state__desc {
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-placeholder;
  text-align: center;
  line-height: 1.5;
}
</style>
