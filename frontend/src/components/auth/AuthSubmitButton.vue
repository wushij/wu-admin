<template>
  <button
    :type="nativeType"
    class="auth-cta-btn"
    :class="{
      'is-loading': loading && !success,
      'is-success': success,
    }"
    :disabled="disabled || loading || success"
    :aria-busy="loading"
    :aria-label="accessibleLabel"
    @click="handleClick"
  >
    <!-- 默认与加载态主胶囊表面：通过 clip-path 与合成层透明度实现零重排收拢 -->
    <span class="auth-cta-btn__surface" aria-hidden="true">
      <span class="auth-cta-btn__shimmer" />
    </span>

    <!-- 文本与加载指示层 -->
    <span class="auth-cta-btn__content">
      <span class="auth-cta-btn__label" :class="{ 'is-hidden': loading || success }">
        <slot>{{ label }}</slot>
      </span>
      <span class="auth-cta-btn__loading" :class="{ 'is-visible': loading && !success }" aria-hidden="true">
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
            class="auth-cta-btn__spinner-arc"
            d="M12 3a9 9 0 0 1 9 9"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
          />
        </svg>
        <span v-if="loadingText" class="auth-cta-btn__loading-text">{{ loadingText }}</span>
      </span>
    </span>

    <!-- 成功态居中正圆徽章与一笔勾勒 SVG 对勾 -->
    <span class="auth-cta-btn__success-stage" aria-hidden="true">
      <span class="auth-cta-btn__pulse-ring" />
      <span class="auth-cta-btn__success-circle">
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
      </span>
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * 认证表单三态主提交按钮组件
 * 支持常态（胶囊按钮）、加载态（矢量旋转环）与成功态（零重排向心收拢为正圆 + 一笔勾勒对勾）。
 */
const props = withDefaults(
  defineProps<{
    /** 常态按钮文案 */
    label?: string
    /** 加载中文案 */
    loadingText?: string
    /** 成功态无障碍播报文案 */
    successLabel?: string
    /** 是否处于请求加载中 */
    loading?: boolean
    /** 是否已成功（触发变成对勾的动效） */
    success?: boolean
    /** 是否禁用 */
    disabled?: boolean
    /** 原生 button type 属性 */
    nativeType?: 'button' | 'submit' | 'reset'
  }>(),
  {
    label: '登 录',
    loadingText: '登录中…',
    successLabel: '登录成功',
    loading: false,
    success: false,
    disabled: false,
    nativeType: 'button',
  },
)

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const accessibleLabel = computed(() => {
  if (props.success) return props.successLabel
  if (props.loading) return props.loadingText
  return props.label
})

function handleClick(event: MouseEvent) {
  if (props.disabled || props.loading || props.success) return
  emit('click', event)
}
</script>

<style scoped>
.auth-cta-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 48px;
  padding: 0 24px;
  border: 0;
  border-radius: var(--radius-pill);
  background: transparent;
  color: #4338ca;
  font-family: inherit;
  font-size: 15.5px;
  font-weight: 600;
  letter-spacing: 3px;
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  outline: none;
  isolation: isolate;
}

.auth-cta-btn:disabled {
  cursor: default;
}

.auth-cta-btn:focus-visible {
  outline: 2px solid rgba(165, 180, 252, 0.9);
  outline-offset: 2px;
}

/* ── 常态胶囊视觉底座（独立合成层，收拢时通过 clip-path 零重排收敛） ── */
.auth-cta-btn__surface {
  position: absolute;
  inset: 0;
  border-radius: var(--radius-pill);
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(238, 242, 255, 0.96) 52%,
    rgba(243, 232, 255, 0.96) 100%
  );
  border: 1.5px solid rgba(99, 102, 241, 0.42);
  box-shadow:
    0 6px 18px rgba(99, 102, 241, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  overflow: hidden;
  clip-path: inset(0 0 0 0 round 9999px);
  transform: translateY(0) scale(1);
  opacity: 1;
  transition:
    transform var(--duration-fast) var(--ease-out),
    opacity 180ms var(--ease-out),
    clip-path 240ms cubic-bezier(0.22, 1, 0.36, 1),
    background var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
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

@media (hover: hover) and (pointer: fine) {
  .auth-cta-btn:hover:not(:disabled) .auth-cta-btn__surface {
    background: linear-gradient(135deg, #ffffff 0%, #e0e7ff 52%, #ede9fe 100%);
    border-color: #6366f1;
    box-shadow:
      0 8px 24px rgba(99, 102, 241, 0.34),
      inset 0 1px 0 #ffffff;
  }

  .auth-cta-btn:hover:not(:disabled) .auth-cta-btn__shimmer {
    opacity: 1;
    transform: translateX(110%);
    transition:
      transform 520ms var(--ease-out),
      opacity 120ms ease;
  }
}

.auth-cta-btn:active:not(:disabled) .auth-cta-btn__surface {
  box-shadow: 0 3px 10px rgba(99, 102, 241, 0.22);
}

.auth-cta-btn.is-loading .auth-cta-btn__surface {
  opacity: 0.92;
}

/* 成功态：长圆胶囊零重排平滑向中心 48px 正圆收敛并淡出 */
.auth-cta-btn.is-success .auth-cta-btn__surface {
  clip-path: inset(0 calc(50% - 24px) 0 calc(50% - 24px) round 9999px);
  opacity: 0;
  transform: scale(0.94);
  transition:
    clip-path 220ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 180ms var(--ease-out) 80ms,
    transform 220ms var(--ease-out);
}

/* ── 文案与加载态容器 ─────────────────────────────────── */
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
    opacity var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);
}

.auth-cta-btn__label.is-hidden {
  opacity: 0;
  transform: translateY(-6px) scale(0.94);
}

.auth-cta-btn__loading {
  position: absolute;
  inset: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  letter-spacing: 1.5px;
  font-size: 14.5px;
  color: #4338ca;
  opacity: 0;
  transform: translateY(6px) scale(0.94);
  transition:
    opacity var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);
}

.auth-cta-btn__loading.is-visible {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.auth-cta-btn__spinner {
  width: 18px;
  height: 18px;
  animation: auth-cta-spin 0.8s linear infinite;
}

.auth-cta-btn__spinner-track {
  opacity: 0.22;
}

@keyframes auth-cta-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── 成功态正圆徽章与一笔勾勒 SVG 对勾 ─────────────────── */
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
  width: 48px;
  height: 48px;
  border-radius: var(--radius-circle);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #4338ca;
  background: linear-gradient(
    135deg,
    #ffffff 0%,
    #eef2ff 52%,
    #f3e8ff 100%
  );
  border: 1.5px solid rgba(99, 102, 241, 0.56);
  box-shadow:
    0 8px 26px rgba(99, 102, 241, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.98);
  opacity: 0;
  transform: scale(0.68);
  transition:
    opacity 180ms var(--ease-out),
    transform 280ms var(--ease-spring);
}

.auth-cta-btn.is-success .auth-cta-btn__success-circle {
  opacity: 1;
  transform: scale(1);
  transition-delay: 90ms;
}

.auth-cta-btn__pulse-ring {
  position: absolute;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-circle);
  border: 1.5px solid rgba(129, 140, 248, 0.58);
  opacity: 0;
  transform: scale(0.9);
}

.auth-cta-btn.is-success .auth-cta-btn__pulse-ring {
  animation: auth-cta-pulse 520ms var(--ease-out) 160ms forwards;
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
  width: 24px;
  height: 24px;
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

/* ── 无障碍动效降级（prefers-reduced-motion） ──────────── */
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
  .auth-cta-btn.is-success .auth-cta-btn__pulse-ring {
    animation: none !important;
  }

  .auth-cta-btn.is-success .auth-cta-btn__pulse-ring {
    opacity: 0 !important;
  }
}
</style>
