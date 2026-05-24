<template>
  <view class="module-icon" :class="[`module-icon--${size}`, `grad-${theme}`]">
    <view class="module-icon__shine" />
    <IconFont :name="icon" :size="resolvedIconSize" color="#ffffff" />
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import type { IconName } from '@/constants/iconfont'

const ICON_SIZE: Record<string, number> = {
  xs: 22,
  sm: 26,
  md: 32,
  ml: 36,
  lg: 36,
}

const props = withDefaults(
  defineProps<{
    icon: IconName
    theme: string
    size?: 'xs' | 'sm' | 'md' | 'ml' | 'lg'
    iconSize?: number
  }>(),
  { size: 'md' },
)

const resolvedIconSize = computed(() => props.iconSize ?? ICON_SIZE[props.size])
</script>

<style lang="scss" scoped>
.module-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(79, 70, 229, 0.18);
}

.module-icon--xs {
  width: 44rpx;
  height: 44rpx;
  border-radius: 14rpx;
  box-shadow: 0 4rpx 12rpx rgba(79, 70, 229, 0.14);
}

.module-icon--sm {
  width: 52rpx;
  height: 52rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(79, 70, 229, 0.14);
}

.module-icon--md {
  width: 64rpx;
  height: 64rpx;
  border-radius: 20rpx;
}

.module-icon--ml {
  width: 72rpx;
  height: 72rpx;
  border-radius: 22rpx;
}

.module-icon--lg {
  width: 88rpx;
  height: 88rpx;
  border-radius: 26rpx;
}

.module-icon__shine {
  position: absolute;
  top: -20%;
  left: -10%;
  width: 60%;
  height: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.35) 0%, transparent 100%);
  border-radius: 50%;
  pointer-events: none;
}
</style>
