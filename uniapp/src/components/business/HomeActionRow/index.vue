<template>
  <view class="action-row" @click="emit('click')">
    <ModuleIcon :icon="icon" :theme="theme" size="md" />
    <view class="action-row__body">
      <text class="action-row__title">{{ title }}</text>
      <text v-if="subtitle" class="action-row__sub">{{ subtitle }}</text>
    </view>
    <view v-if="badge > 0" class="action-row__badge">{{ badge > 99 ? '99+' : badge }}</view>
    <IconFont name="arrow" :size="24" color="#c0c4cc" />
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/common/IconFont/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import type { IconName } from '@/constants/iconfont'

withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    icon: IconName
    theme: string
    badge?: number
  }>(),
  { badge: 0 },
)

const emit = defineEmits<{ click: [] }>()
</script>

<style lang="scss" scoped>

.action-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  min-height: 96rpx;
  padding: 20rpx 0;
  border-top: 1px solid $color-border-light;
}

.action-row:first-child {
  border-top: none;
}

.action-row:active {
  opacity: 0.85;
}

.action-row__body {
  flex: 1;
  min-width: 0;
}

.action-row__title {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.action-row__sub {
  display: block;
  margin-top: 4rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.action-row__badge {
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 12rpx;
  border-radius: $radius-full;
  background: $color-chat-unread;
  color: #fff;
  font-size: 20rpx;
  line-height: 36rpx;
  text-align: center;
}
</style>
