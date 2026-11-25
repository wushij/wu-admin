<template>
  <div class="ui-empty-state">
    <div class="ui-empty-icon" aria-hidden="true">
      <el-icon :size="28"><FolderOpened /></el-icon>
    </div>
    <h4 class="ui-empty-title">{{ title }}</h4>
    <p v-if="description" class="ui-empty-desc">{{ description }}</p>
    <div v-if="$slots.actions" class="ui-empty-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { FolderOpened } from '@element-plus/icons-vue'

withDefaults(
  defineProps<{
    title?: string
    description?: string
  }>(),
  {
    title: '暂无数据',
    description: '',
  },
)
</script>

<style scoped lang="scss">
.ui-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--space-7) var(--space-5);
  color: var(--color-text-secondary);
}

.ui-empty-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-circle);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-3);
}

.ui-empty-title {
  margin: 0;
  font-size: var(--text-h3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.ui-empty-desc {
  margin: var(--space-1) 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  max-width: 340px;
}

.ui-empty-actions {
  margin-top: var(--space-4);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
</style>
