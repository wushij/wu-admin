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
/* #ifdef H5 */
@property --auth-beam-angle {
  syntax: '<angle>';
  initial-value: 0deg;
  inherits: false;
}
/* #endif */

.auth-glass-form {
  position: relative;
  width: 100%;
  max-width: 680rpx;
  border-radius: 48rpx;
  isolation: isolate;
  @include auth-glass-form-shell;
}

/* #ifdef H5 */
/* 卡片外框流光（Border Beam）：沿圆角边框缓缓流动的淡靛紫与冰蓝微光，1:1 复刻 PC 端 */
.auth-glass-form::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  padding: 1.25px;
  background: conic-gradient(
    from var(--auth-beam-angle, 0deg),
    transparent 0deg 210deg,
    rgba(129, 140, 248, 0.62) 258deg,
    rgba(255, 255, 255, 0.88) 298deg,
    rgba(56, 189, 248, 0.65) 336deg,
    transparent 360deg
  );
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  mask-composite: exclude;
  pointer-events: none;
  z-index: 2;
  opacity: 0.9;
  animation: auth-beam-spin 7.5s linear infinite;
}

@keyframes auth-beam-spin {
  to {
    --auth-beam-angle: 360deg;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-glass-form::before {
    animation: none !important;
    opacity: 0.45;
  }
}
/* #endif */

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
  padding: 48rpx 40rpx 44rpx;
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
