<template>
  <view
    class="auth-cta-btn"
    :class="[
      `is-${variant}`,
      customClass,
      {
        'is-loading': loading && !success,
        'is-success': success,
        'is-disabled': disabled,
      },
    ]"
    role="button"
    :aria-disabled="disabled || loading || success"
    @click="handleClick"
  >
    <!-- 默认与加载态明珠胶囊表面（成功态脱离文档流向中心 96rpx 正圆收缩，避免 <button> 默认灰底遮挡） -->
    <view class="auth-cta-btn__surface">
      <view class="auth-cta-btn__shimmer" />
    </view>

    <!-- 常态文本与加载中旋转环 -->
    <view class="auth-cta-btn__content">
      <view class="auth-cta-btn__label" :class="{ 'is-hidden': loading || success }">
        <slot>{{ label }}</slot>
      </view>
      <view class="auth-cta-btn__loading" :class="{ 'is-visible': loading && !success }">
        <!-- #ifdef H5 -->
        <svg class="auth-cta-btn__spinner" viewBox="0 0 24 24" fill="none">
          <circle
            class="auth-cta-btn__spinner-track"
            cx="12"
            cy="12"
            r="9"
            stroke="currentColor"
            stroke-width="2.2"
          />
          <path
            d="M12 3a9 9 0 0 1 9 9"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
          />
        </svg>
        <!-- #endif -->
        <!-- #ifndef H5 -->
        <view class="auth-cta-btn__spinner-css" />
        <!-- #endif -->
        <text v-if="loadingText" class="auth-cta-btn__loading-text">{{ loadingText }}</text>
      </view>
    </view>

    <!-- 成功态居中正圆徽章、脉冲扩散光环与一笔勾勒对勾 -->
    <view class="auth-cta-btn__success-stage">
      <view class="auth-cta-btn__pulse-ring" />
      <view class="auth-cta-btn__success-circle">
        <!-- #ifdef H5 -->
        <svg class="auth-cta-btn__check" viewBox="0 0 24 24" fill="none">
          <path
            class="auth-cta-btn__check-path"
            d="M6.2 12.6l4.1 4.1 7.7-8.4"
            pathLength="1"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <!-- #endif -->
        <!-- #ifndef H5 -->
        <text class="auth-cta-btn__check-glyph">✓</text>
        <!-- #endif -->
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * 移动端认证三态主提交按钮（1:1 复刻 PC 端 AuthSubmitButton）
 * 支持：常态明珠胶囊 + 扫光、加载旋转环、成功向心收拢正圆 + 脉冲环 + 一笔勾勒对勾动效。
 */
const props = withDefaults(
  defineProps<{
    label?: string
    loadingText?: string
    loading?: boolean
    success?: boolean
    disabled?: boolean
    /** 视觉风格：primary（登录浅柔紫）/ ice（注册浅冰蓝） */
    variant?: 'primary' | 'ice'
    customClass?: string
  }>(),
  {
    label: '登 录',
    loadingText: '登录中…',
    loading: false,
    success: false,
    disabled: false,
    variant: 'primary',
    customClass: '',
  },
)

const emit = defineEmits<{
  click: []
}>()

function handleClick() {
  if (props.disabled || props.loading || props.success) return
  emit('click')
}
</script>

<style lang="scss" scoped>
.auth-cta-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 96rpx;
  padding: 0 40rpx;
  margin-top: 8rpx;
  box-sizing: border-box;
  border: 0;
  border-radius: 9999rpx;
  background: transparent;
  color: #4338ca;
  font-size: 31rpx;
  font-weight: 600;
  letter-spacing: 6rpx;
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  overflow: visible;
  isolation: isolate;

  &.is-ice {
    color: #0369a1;
  }

  &.is-disabled {
    opacity: 0.52;
    cursor: default;
  }

  &.is-loading,
  &.is-success {
    cursor: default;
  }
}

.auth-cta-btn__surface {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 100%;
  box-sizing: border-box;
  border-radius: 9999rpx;
  overflow: hidden;
  transform: translateX(-50%) scale(1);
  opacity: 1;
  @include auth-pearl-primary-btn;
  transition:
    width 240ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 160ms ease 110ms,
    transform 220ms ease,
    box-shadow 0.2s ease;
}

.auth-cta-btn.is-ice .auth-cta-btn__surface {
  @include auth-pearl-ice-btn;
  border-width: 3rpx;
}

.auth-cta-btn__shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    110deg,
    transparent 25%,
    rgba(255, 255, 255, 0.65) 50%,
    transparent 75%
  );
  transform: translateX(-110%);
  opacity: 0;
  pointer-events: none;
}

/* #ifdef H5 */
@media (hover: hover) and (pointer: fine) {
  .auth-cta-btn:hover:not(.is-disabled):not(.is-loading):not(.is-success) .auth-cta-btn__shimmer {
    opacity: 1;
    transform: translateX(110%);
    transition: transform 520ms ease-out, opacity 120ms ease;
  }
}
/* #endif */

.auth-cta-btn:active:not(.is-disabled):not(.is-loading):not(.is-success) .auth-cta-btn__surface {
  transform: translateX(-50%) scale(0.985);
}

.auth-cta-btn.is-loading .auth-cta-btn__surface {
  opacity: 0.92;
}

.auth-cta-btn.is-success .auth-cta-btn__surface {
  width: 96rpx;
  opacity: 0;
  transform: translateX(-50%) scale(0.96);
}

.auth-cta-btn__content {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.auth-cta-btn__label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  opacity: 1;
  transform: translateY(0) scale(1);
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;

  &.is-hidden {
    opacity: 0;
    transform: translateY(-10rpx) scale(0.94);
  }
}

.auth-cta-btn__loading {
  position: absolute;
  inset: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  letter-spacing: 3rpx;
  font-size: 29rpx;
  color: inherit;
  opacity: 0;
  transform: translateY(10rpx) scale(0.94);
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;

  &.is-visible {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.auth-cta-btn__spinner {
  width: 36rpx;
  height: 36rpx;
  animation: auth-cta-spin 0.8s linear infinite;
}

.auth-cta-btn__spinner-track {
  opacity: 0.22;
}

.auth-cta-btn__spinner-css {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(99, 102, 241, 0.22);
  border-top-color: currentColor;
  animation: auth-cta-spin 0.8s linear infinite;
}

@keyframes auth-cta-spin {
  to {
    transform: rotate(360deg);
  }
}

.auth-cta-btn__success-stage {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.auth-cta-btn__success-circle {
  position: relative;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #4338ca;
  background: linear-gradient(135deg, #ffffff 0%, #eef2ff 52%, #f3e8ff 100%);
  border: 3rpx solid rgba(99, 102, 241, 0.56);
  box-shadow:
    0 16rpx 52rpx rgba(99, 102, 241, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.98);
  opacity: 0;
  transform: scale(0.68);
  transition:
    opacity 180ms ease,
    transform 280ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

.auth-cta-btn.is-ice .auth-cta-btn__success-circle {
  color: #0369a1;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 55%, #ecfdf5 100%);
  border-color: rgba(14, 165, 233, 0.56);
  box-shadow:
    0 16rpx 52rpx rgba(14, 165, 233, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.98);
}

.auth-cta-btn.is-success .auth-cta-btn__success-circle {
  opacity: 1;
  transform: scale(1);
  transition-delay: 90ms;
}

.auth-cta-btn__pulse-ring {
  position: absolute;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  border: 3rpx solid rgba(129, 140, 248, 0.58);
  opacity: 0;
  transform: scale(0.9);
}

.auth-cta-btn.is-ice .auth-cta-btn__pulse-ring {
  border-color: rgba(56, 189, 248, 0.56);
}

.auth-cta-btn.is-success .auth-cta-btn__pulse-ring {
  animation: auth-cta-pulse 520ms ease-out 160ms forwards;
}

@keyframes auth-cta-pulse {
  0% {
    opacity: 0.65;
    transform: scale(0.95);
  }
  100% {
    opacity: 0;
    transform: scale(1.48);
  }
}

.auth-cta-btn__check {
  width: 48rpx;
  height: 48rpx;
  display: block;
}

.auth-cta-btn__check-path {
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  transition: stroke-dashoffset 260ms cubic-bezier(0.22, 1, 0.36, 1);
}

.auth-cta-btn.is-success .auth-cta-btn__check-path {
  stroke-dashoffset: 0;
  transition-delay: 190ms;
}

.auth-cta-btn__check-glyph {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1;
}

@media (prefers-reduced-motion: reduce) {
  .auth-cta-btn__surface,
  .auth-cta-btn__shimmer,
  .auth-cta-btn__label,
  .auth-cta-btn__loading,
  .auth-cta-btn__success-circle,
  .auth-cta-btn__check-path {
    transition-duration: 0.01ms !important;
    transition-delay: 0ms !important;
  }

  .auth-cta-btn__spinner,
  .auth-cta-btn__spinner-css,
  .auth-cta-btn.is-success .auth-cta-btn__pulse-ring {
    animation: none !important;
  }
}
</style>
