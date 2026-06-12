<template>
  <view class="fade-in" :class="{ 'fade-in--show': show }">
    <slot />
  </view>
</template>

<script setup lang="ts">
defineProps<{
  show?: boolean
}>()
</script>

<style lang="scss" scoped>
.fade-in {
  opacity: 0;
  transform: translateY(8rpx);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-in--show {
  opacity: 1;
  transform: translateY(0);
}
</style>
