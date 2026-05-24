<template>
  <view class="auth-back" @click="emit('click')">
    <view class="auth-back__pill">
      <text class="auth-back__icon">‹</text>
      <text class="auth-back__text">{{ text }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    text?: string
  }>(),
  {
    text: '返回',
  },
)

const emit = defineEmits<{ click: [] }>()
</script>

<style lang="scss" scoped>
.auth-back {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  padding: calc(16rpx + env(safe-area-inset-top)) 24rpx 12rpx;
  pointer-events: none;
}

.auth-back__pill {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  padding: 12rpx 24rpx 12rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid transparent;
  backdrop-filter: blur(12px);
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.18);
  pointer-events: auto;
}

.auth-back__pill:active {
  background: rgba(255, 255, 255, 0.18);
}

.auth-back__icon {
  color: rgba(255, 255, 255, 0.95);
  font-size: 34rpx;
  line-height: 1;
  transform: translateY(-1rpx);
}

.auth-back__text {
  color: rgba(255, 255, 255, 0.92);
  font-size: 28rpx;
  line-height: 1;
  text-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.35);
}
</style>
