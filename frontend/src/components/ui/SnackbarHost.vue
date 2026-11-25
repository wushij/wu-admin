<template>
  <Teleport to="body">
    <div
      class="ui-snackbar-region"
      aria-live="polite"
      aria-relevant="additions text"
    >
      <TransitionGroup name="snackbar-spring" tag="div" class="ui-snackbar-stack">
        <div
          v-for="item in items"
          :key="item.id"
          class="ui-snackbar"
          :class="`is-${item.variant}`"
          role="status"
          @mouseenter="pauseSnackbarTimer(item.id)"
          @mouseleave="resumeSnackbarTimer(item.id)"
          @focusin="pauseSnackbarTimer(item.id)"
          @focusout="resumeSnackbarTimer(item.id)"
        >
          <span class="ui-snackbar-icon" aria-hidden="true">
            <el-icon :size="18">
              <CircleCheckFilled v-if="item.variant === 'success'" />
              <WarningFilled v-else-if="item.variant === 'warning' || item.variant === 'danger'" />
              <InfoFilled v-else />
            </el-icon>
          </span>

          <div class="ui-snackbar-text">
            <span class="ui-snackbar-msg">{{ item.message }}</span>
            <span v-if="item.description" class="ui-snackbar-desc">{{ item.description }}</span>
          </div>

          <div class="ui-snackbar-actions">
            <button
              v-if="item.actionLabel"
              type="button"
              class="ui-snackbar-action-btn"
              :disabled="item.acting"
              @click="triggerSnackbarAction(item.id)"
            >
              {{ item.acting ? '处理中…' : item.actionLabel }}
            </button>
            <button
              type="button"
              class="ui-snackbar-close"
              aria-label="关闭提示"
              @click="dismissSnackbar(item.id)"
            >
              <el-icon :size="14"><Close /></el-icon>
            </button>
          </div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { CircleCheckFilled, WarningFilled, InfoFilled, Close } from '@element-plus/icons-vue'
import { useSnackbar } from '@/composables/ui/useSnackbar'

const {
  items,
  dismissSnackbar,
  triggerSnackbarAction,
  pauseSnackbarTimer,
  resumeSnackbarTimer,
} = useSnackbar()
</script>

<style scoped lang="scss">
.ui-snackbar-region {
  position: fixed;
  left: 50%;
  bottom: calc(var(--space-5) + env(safe-area-inset-bottom, 0px));
  transform: translateX(-50%);
  z-index: var(--z-snackbar, 2400);
  pointer-events: none;
  width: min(520px, calc(100vw - 32px));
}

.ui-snackbar-stack {
  display: flex;
  flex-direction: column-reverse;
  align-items: center;
  gap: var(--space-2);
}

.ui-snackbar {
  pointer-events: auto;
  width: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 10px 12px 10px 16px;
  border-radius: var(--radius-pill);
  background: var(--color-bg-elevated);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-float);
  backdrop-filter: blur(12px);
}

.ui-snackbar-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-info);
}

.ui-snackbar.is-success .ui-snackbar-icon {
  color: var(--color-success);
}

.ui-snackbar.is-warning .ui-snackbar-icon {
  color: var(--color-warning);
}

.ui-snackbar.is-danger .ui-snackbar-icon {
  color: var(--color-danger);
}

.ui-snackbar-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.ui-snackbar-msg {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  line-height: 1.4;
}

.ui-snackbar-desc {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
  line-height: 1.35;
}

.ui-snackbar-actions {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  flex-shrink: 0;
}

.ui-snackbar-action-btn {
  border: none;
  border-radius: var(--radius-pill);
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  transition:
    background-color var(--duration-fast) var(--ease-out);

  &:hover:not(:disabled) {
    background: var(--el-color-primary-light-8);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.ui-snackbar-close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius-circle);
  background: transparent;
  color: var(--color-text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
  }
}

.snackbar-spring-enter-active,
.snackbar-spring-leave-active {
  transition:
    transform var(--duration-normal) var(--ease-spring),
    opacity var(--duration-fast) var(--ease-out);
}

.snackbar-spring-enter-from,
.snackbar-spring-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}
</style>
