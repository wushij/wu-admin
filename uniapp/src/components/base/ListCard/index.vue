<template>
  <view class="list-card card--elevated" @click="emit('click')">
    <slot />
  </view>
</template>

<script setup lang="ts">
const emit = defineEmits<{ click: [] }>()
</script>

<style lang="scss" scoped>

.list-card {
  padding: 32rpx;
  margin-bottom: $card-gap;
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.list-card:active {
  transform: scale(0.98);
  opacity: 0.92;
}
</style>
