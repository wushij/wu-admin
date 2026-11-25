<template>
  <view
    class="auth-glass-toast"
    :class="[
      `is-${toast.type}`,
      { 'is-visible': toast.visible && Boolean(toast.message) },
    ]"
    role="status"
    aria-live="polite"
    :aria-hidden="!toast.visible || !toast.message"
  >
    <view class="auth-glass-toast__icon" :class="`is-${toast.type}`" aria-hidden="true">
      <!-- #ifdef H5 -->
      <svg v-if="toast.type === 'success'" viewBox="0 0 24 24" class="toast-svg" fill="none">
        <circle cx="12" cy="12" r="9.5" stroke="currentColor" stroke-width="1.8" opacity="0.35" />
        <path
          d="M7.5 12.4l3.1 3.1 6-6.6"
          stroke="currentColor"
          stroke-width="2.1"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
      <svg v-else-if="toast.type === 'error'" viewBox="0 0 24 24" class="toast-svg" fill="none">
        <circle cx="12" cy="12" r="9.5" stroke="currentColor" stroke-width="1.8" opacity="0.35" />
        <path
          d="M9 9l6 6M15 9l-6 6"
          stroke="currentColor"
          stroke-width="2.1"
          stroke-linecap="round"
        />
      </svg>
      <svg v-else viewBox="0 0 24 24" class="toast-svg" fill="none">
        <circle cx="12" cy="12" r="9.5" stroke="currentColor" stroke-width="1.8" opacity="0.35" />
        <path d="M12 7.8v5.2" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" />
        <circle cx="12" cy="16.2" r="1.2" fill="currentColor" />
      </svg>
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <text class="toast-glyph">
        {{ toast.type === 'success' ? '✓' : toast.type === 'error' ? '✕' : '!' }}
      </text>
      <!-- #endif -->
    </view>
    <text class="auth-glass-toast__text">{{ toast.message }}</text>
  </view>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount } from 'vue'
import { registerAuthToastHost, useAuthToastState } from '@/composables/auth/useAuthToast'

const toast = useAuthToastState()
let unregister: (() => void) | null = null

onMounted(() => {
  unregister = registerAuthToastHost()
})

onBeforeUnmount(() => {
  unregister?.()
  unregister = null
})
</script>

<style lang="scss" scoped>
.auth-glass-toast {
  position: fixed;
  top: calc(36rpx + env(safe-area-inset-top));
  left: 50%;
  z-index: 9999;
  display: inline-flex;
  align-items: center;
  gap: 14rpx;
  max-width: calc(100vw - 80rpx);
  padding: 18rpx 32rpx;
  border-radius: 9999rpx;
  background: rgba(15, 23, 42, 0.82);
  backdrop-filter: blur(18px) saturate(150%);
  -webkit-backdrop-filter: blur(18px) saturate(150%);
  border: 1px solid rgba(165, 180, 252, 0.34);
  box-shadow:
    0 24rpx 64rpx rgba(0, 0, 0, 0.52),
    0 0 36rpx rgba(99, 102, 241, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  pointer-events: none;
  visibility: hidden;
  opacity: 0;
  transform: translate(-50%, -28rpx) scale(0.96);
  transition:
    opacity 0.22s ease,
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    visibility 0s linear 0.28s;

  &.is-visible {
    visibility: visible;
    opacity: 1;
    transform: translate(-50%, 0) scale(1);
    transition:
      opacity 0.22s ease,
      transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
      visibility 0s;
  }

  &.is-warning {
    border-color: rgba(251, 191, 36, 0.38);
    box-shadow:
      0 24rpx 64rpx rgba(0, 0, 0, 0.52),
      0 0 32rpx rgba(245, 158, 11, 0.14),
      inset 0 1px 0 rgba(255, 255, 255, 0.14);
  }

  &.is-error {
    border-color: rgba(248, 113, 113, 0.42);
    box-shadow:
      0 24rpx 64rpx rgba(0, 0, 0, 0.52),
      0 0 32rpx rgba(239, 68, 68, 0.16),
      inset 0 1px 0 rgba(255, 255, 255, 0.14);
  }
}

.auth-glass-toast__icon {
  width: 34rpx;
  height: 34rpx;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #a5b4fc;

  &.is-warning {
    color: #fbbf24;
  }

  &.is-error {
    color: #fca5a5;
  }
}

.toast-svg {
  width: 34rpx;
  height: 34rpx;
  display: block;
}

.toast-glyph {
  font-size: 26rpx;
  font-weight: 700;
  line-height: 1;
}

.auth-glass-toast__text {
  color: #f8fafc;
  font-size: 27rpx;
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: 0.02em;
  word-break: break-word;
}
</style>
