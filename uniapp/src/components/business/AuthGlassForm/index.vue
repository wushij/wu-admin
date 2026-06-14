<template>
  <view class="auth-glass-form" :class="[{ 'auth-glass-form--animated': animated }, customClass]">
    <!-- #ifdef H5 -->
    <div class="auth-glass-form__backdrop" />
    <!-- #endif -->
    <!-- #ifndef H5 -->
    <view class="auth-glass-form__backdrop" />
    <!-- #endif -->
    <view class="auth-glass-form__body" :class="bodyClass">
      <slot />
    </view>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    animated?: boolean
    customClass?: string
    bodyClass?: string
  }>(),
  {
    animated: true,
    customClass: '',
    bodyClass: '',
  },
)
</script>

<style lang="scss" scoped>

.auth-glass-form {
  position: relative;
  width: 100%;
  max-width: 640rpx;
  border-radius: 40rpx;
  @include auth-glass-form-shell;
}

.auth-glass-form--animated {
  animation: authGlassIn 0.85s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

.auth-glass-form__backdrop {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  z-index: 0;
  @include auth-glass-form-backdrop;
}

.auth-glass-form__body {
  position: relative;
  z-index: 1;
  padding: 40rpx;
}

@keyframes authGlassIn {
  from {
    opacity: 0;
    transform: translate3d(0, 28rpx, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}
</style>
