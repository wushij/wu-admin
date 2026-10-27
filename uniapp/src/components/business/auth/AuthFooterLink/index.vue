<template>
  <view class="auth-footer" :class="customClass">
    <text class="auth-footer__text">{{ text }}</text>
    <text class="auth-footer__link" @click="emit('click')">{{ linkText }}</text>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    text: string
    linkText: string
    customClass?: string
  }>(),
  {
    customClass: '',
  },
)

const emit = defineEmits<{ click: [] }>()
</script>

<style lang="scss" scoped>
.auth-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 32rpx;
  line-height: 1.5;
  text-align: center;
}

.auth-footer__text,
.auth-footer__link {
  font-size: 26rpx;
  line-height: 1.5;
}

.auth-footer__text {
  color: rgba(255, 255, 255, 0.65);
}

.auth-footer__link {
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.5);
  text-decoration: underline;
  text-underline-offset: 6rpx;
}
</style>
