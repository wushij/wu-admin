<template>
  <view class="fab" @click="$emit('click')">
    <text class="fab__icon">+</text>
  </view>
</template>

<script setup lang="ts">
defineEmits<{ click: [] }>()
</script>

<style lang="scss" scoped>
.fab {
  position: fixed;
  right: 32rpx;
  bottom: calc(48rpx + env(safe-area-inset-bottom));
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #010710, #1e293b);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(1, 7, 16, 0.35);
  z-index: 100;
}

.fab__icon {
  font-size: 56rpx;
  line-height: 1;
  color: #fff;
  font-weight: 300;
}
</style>
