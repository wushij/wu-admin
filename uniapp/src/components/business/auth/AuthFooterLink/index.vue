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
  gap: 10rpx;
  margin-top: 32rpx;
  padding-top: 28rpx;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  line-height: 1.5;
  text-align: center;
}

.auth-footer__text,
.auth-footer__link {
  font-size: 27rpx;
  line-height: 1.5;
}

.auth-footer__text {
  color: rgba(203, 213, 225, 0.72);
}

.auth-footer__link {
  color: rgba(226, 232, 240, 0.92);
  font-weight: 500;
}
</style>
