<template>
  <div
    class="ui-inline-alert"
    :class="[`is-${type}`, { 'is-compact': compact }]"
    :role="type === 'error' || type === 'warning' ? 'alert' : 'status'"
    aria-live="polite"
  >
    <span class="ui-inline-alert__icon" aria-hidden="true">
      <el-icon :size="compact ? 16 : 18">
        <CircleCloseFilled v-if="type === 'error'" />
        <WarningFilled v-else-if="type === 'warning'" />
        <CircleCheckFilled v-else-if="type === 'success'" />
        <InfoFilled v-else />
      </el-icon>
    </span>
    <div class="ui-inline-alert__body">
      <div class="ui-inline-alert__title">{{ title }}</div>
      <p v-if="description" class="ui-inline-alert__desc">{{ description }}</p>
      <p v-if="suggestion" class="ui-inline-alert__suggestion">{{ suggestion }}</p>
      <slot />
    </div>
    <button
      v-if="closable"
      type="button"
      class="ui-inline-alert__close"
      aria-label="关闭提示"
      @click="$emit('close')"
    >
      <el-icon :size="14"><Close /></el-icon>
    </button>
  </div>
</template>

<script setup lang="ts">
import {
  CircleCloseFilled,
  WarningFilled,
  CircleCheckFilled,
  InfoFilled,
  Close,
} from '@element-plus/icons-vue'

withDefaults(
  defineProps<{
    type?: 'error' | 'warning' | 'info' | 'success'
    title: string
    description?: string
    suggestion?: string
    compact?: boolean
    closable?: boolean
  }>(),
  {
    type: 'info',
    description: '',
    suggestion: '',
    compact: false,
    closable: false,
  },
)

defineEmits<{
  close: []
}>()
</script>

<style scoped lang="scss">
.ui-inline-alert {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-info-border);
  background: var(--color-info-soft);
  color: var(--color-text-primary);
  font-size: 13px;
  line-height: 1.5;
}

.ui-inline-alert.is-compact {
  padding: var(--space-2) var(--space-3);
  gap: var(--space-2);
}

.ui-inline-alert.is-error {
  border-color: var(--color-danger-border);
  background: var(--color-danger-soft);

  .ui-inline-alert__icon {
    color: var(--color-danger);
  }
}

.ui-inline-alert.is-warning {
  border-color: var(--color-warning-border);
  background: var(--color-warning-soft);

  .ui-inline-alert__icon {
    color: var(--color-warning);
  }
}

.ui-inline-alert.is-success {
  border-color: var(--color-success-border);
  background: var(--color-success-soft);

  .ui-inline-alert__icon {
    color: var(--color-success);
  }
}

.ui-inline-alert.is-info .ui-inline-alert__icon {
  color: var(--color-info);
}

.ui-inline-alert__icon {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  margin-top: 1px;
}

.ui-inline-alert__body {
  flex: 1;
  min-width: 0;
}

.ui-inline-alert__title {
  font-weight: 600;
  color: var(--color-text-primary);
}

.ui-inline-alert__desc {
  margin: 2px 0 0;
  color: var(--color-text-secondary);
}

.ui-inline-alert__suggestion {
  margin: 4px 0 0;
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.ui-inline-alert__close {
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  width: 24px;
  height: 24px;
  border-radius: var(--radius-circle);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
  }
}
</style>
