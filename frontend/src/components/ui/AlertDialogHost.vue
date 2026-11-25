<template>
  <Teleport to="body">
    <Transition name="alert-dialog-fade">
      <div
        v-if="alertDialogState.visible && alertDialogState.options"
        class="ui-alert-backdrop"
        role="presentation"
        @click.self="onBackdropClick"
      >
        <div
          ref="dialogRef"
          class="ui-alert-dialog"
          :class="`is-${alertDialogState.options.severity}`"
          role="alertdialog"
          aria-modal="true"
          :aria-labelledby="titleId"
          :aria-describedby="descId"
          tabindex="-1"
          @keydown="onKeyDown"
        >
          <div class="ui-alert-head">
            <div class="ui-alert-icon-wrap" aria-hidden="true">
              <el-icon :size="22">
                <WarningFilled />
              </el-icon>
            </div>
            <div class="ui-alert-title-col">
              <h3 :id="titleId" class="ui-alert-title">
                {{ alertDialogState.options.title }}
              </h3>
              <span
                v-if="alertDialogState.options.severity === 'irreversible'"
                class="ui-alert-badge"
              >
                不可恢复操作
              </span>
            </div>
          </div>

          <div :id="descId" class="ui-alert-body">
            <div v-if="alertDialogState.options.targetName" class="ui-alert-target">
              <span class="ui-alert-target-label">操作对象</span>
              <span class="ui-alert-target-value">{{ alertDialogState.options.targetName }}</span>
            </div>
            <p class="ui-alert-message">{{ alertDialogState.options.message }}</p>

            <div v-if="alertDialogState.options.requireConfirmInput" class="ui-alert-confirm-input">
              <label class="ui-alert-input-label" for="ui-alert-input-field">
                请输入 <strong>{{ alertDialogState.options.requireConfirmInput }}</strong> 以确认执行：
              </label>
              <el-input
                id="ui-alert-input-field"
                v-model="alertDialogState.inputValue"
                :placeholder="alertDialogState.options.inputPlaceholder || `请输入 ${alertDialogState.options.requireConfirmInput}`"
                clearable
                @input="alertDialogState.errorText = ''"
                @keyup.enter="confirmAlertDialog"
              />
              <InlineAlert
                v-if="alertDialogState.errorText"
                type="error"
                :title="alertDialogState.errorText"
                compact
              />
            </div>
          </div>

          <div class="ui-alert-footer">
            <el-button
              ref="cancelBtnRef"
              :disabled="alertDialogState.loading"
              @click="cancelAlertDialog"
            >
              {{ alertDialogState.options.cancelText || '取消' }}
            </el-button>
            <el-button
              :type="alertDialogState.options.severity === 'warning' ? 'warning' : 'danger'"
              :loading="alertDialogState.loading"
              :disabled="isConfirmDisabled"
              @click="confirmAlertDialog"
            >
              {{ alertDialogState.options.confirmText }}
            </el-button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch, onUnmounted } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import {
  alertDialogState,
  cancelAlertDialog,
  confirmAlertDialog,
} from '@/composables/ui/useAlertDialog'
import InlineAlert from './InlineAlert.vue'

const titleId = 'ui-alert-dialog-title'
const descId = 'ui-alert-dialog-desc'
const dialogRef = ref<HTMLElement | null>(null)
const cancelBtnRef = ref<{ $el?: HTMLElement } | null>(null)
let prevFocusedEl: HTMLElement | null = null

const isConfirmDisabled = computed(() => {
  const required = alertDialogState.options?.requireConfirmInput
  if (!required) return false
  return alertDialogState.inputValue.trim() !== required
})

function onBackdropClick() {
  if (alertDialogState.options?.closeOnClickModal) {
    cancelAlertDialog()
  }
}

function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    if (alertDialogState.options?.closeOnEscape) {
      e.preventDefault()
      cancelAlertDialog()
    }
    return
  }
  // 焦点陷阱（Focus Trap）
  if (e.key === 'Tab' && dialogRef.value) {
    const focusables = Array.from(
      dialogRef.value.querySelectorAll<HTMLElement>(
        'button:not([disabled]), input:not([disabled]), [tabindex]:not([tabindex="-1"])',
      ),
    )
    if (!focusables.length) return
    const first = focusables[0]
    const last = focusables[focusables.length - 1]
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault()
      last.focus()
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault()
      first.focus()
    }
  }
}

watch(
  () => alertDialogState.visible,
  async (visible) => {
    if (visible) {
      prevFocusedEl = document.activeElement as HTMLElement | null
      await nextTick()
      // 默认聚焦安全的「取消」按钮或文本输入框
      const inputEl = dialogRef.value?.querySelector<HTMLInputElement>('#ui-alert-input-field')
      if (inputEl) {
        inputEl.focus()
      } else {
        cancelBtnRef.value?.$el?.focus()
      }
    } else if (prevFocusedEl) {
      prevFocusedEl.focus?.()
      prevFocusedEl = null
    }
  },
)

onUnmounted(() => {
  prevFocusedEl = null
})
</script>

<style scoped lang="scss">
.ui-alert-backdrop {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal, 2100);
  background: var(--color-backdrop);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
}

.ui-alert-dialog {
  width: 100%;
  max-width: 448px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-float);
  padding: var(--space-5);
  color: var(--color-text-primary);
  outline: none;
}

.ui-alert-head {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.ui-alert-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-circle);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--color-danger-soft);
  color: var(--color-danger);
  border: 1px solid var(--color-danger-border);
}

.ui-alert-dialog.is-warning .ui-alert-icon-wrap {
  background: var(--color-warning-soft);
  color: var(--color-warning);
  border-color: var(--color-warning-border);
}

.ui-alert-title-col {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  flex-wrap: wrap;
  padding-top: 2px;
}

.ui-alert-title {
  margin: 0;
  font-size: var(--text-h3);
  font-weight: 600;
  line-height: var(--leading-h3);
  color: var(--color-text-primary);
}

.ui-alert-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--text-caption);
  font-weight: 600;
  background: var(--color-danger-soft);
  color: var(--color-danger);
  border: 1px solid var(--color-danger-border);
}

.ui-alert-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.ui-alert-target {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  font-size: 13px;
}

.ui-alert-target-label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.ui-alert-target-value {
  color: var(--color-text-primary);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ui-alert-message {
  margin: 0;
  font-size: var(--text-body);
  line-height: var(--leading-body);
  color: var(--color-text-secondary);
}

.ui-alert-confirm-input {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.ui-alert-input-label {
  font-size: 13px;
  color: var(--color-text-secondary);

  strong {
    color: var(--color-danger);
    user-select: all;
  }
}

.ui-alert-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}

.alert-dialog-fade-enter-active,
.alert-dialog-fade-leave-active {
  transition: opacity var(--duration-fast) var(--ease-out);

  .ui-alert-dialog {
    transition: transform var(--duration-normal) var(--ease-spring), opacity var(--duration-fast) var(--ease-out);
  }
}

.alert-dialog-fade-enter-from,
.alert-dialog-fade-leave-to {
  opacity: 0;

  .ui-alert-dialog {
    opacity: 0;
    transform: scale(0.96) translateY(8px);
  }
}
</style>
