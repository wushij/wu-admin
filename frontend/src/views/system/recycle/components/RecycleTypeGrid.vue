<template>
  <div class="type-cards">
    <button
      v-for="type in visibleTypes"
      :key="type.key"
      type="button"
      class="type-card motion-hover-lift"
      :class="[
        `accent-${type.accent}`,
        { active: activeType === type.key, 'has-pending': summary[type.key] > 0 },
      ]"
      @click="$emit('select', type.key)"
    >
      <span class="type-icon-wrap" aria-hidden="true">
        <el-icon class="type-icon"><component :is="type.icon" /></el-icon>
      </span>
      <span class="type-body">
        <span class="type-label">{{ type.label }}</span>
        <span class="type-count motion-tabular">{{ summary[type.key] || 0 }} 条</span>
      </span>
      <span v-if="summary[type.key] > 0" class="type-dot" aria-hidden="true" />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { RecycleSummary } from '@/api/system/recycle'
import type { RecycleTypeConfig, RecycleTypeKey } from '../recycle-config'

defineProps<{
  visibleTypes: RecycleTypeConfig[]
  activeType: RecycleTypeKey
  summary: RecycleSummary
}>()

defineEmits<{
  select: [key: RecycleTypeKey]
}>()
</script>

<style scoped lang="scss">
.type-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(156px, 1fr));
  gap: var(--space-3);
}

.type-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 12px 14px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  cursor: pointer;
  text-align: left;

  &.active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);

    .type-label {
      color: var(--el-color-primary);
      font-weight: 600;
    }
  }

  &.has-pending:not(.active) {
    border-color: var(--color-border-strong);
  }
}

.type-icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--accent-soft, rgba(99, 102, 241, 0.12));
  color: var(--accent-ink, #6366f1);
  border: 1px solid var(--accent-line, rgba(99, 102, 241, 0.25));
}

.type-icon {
  font-size: 18px;
}

.type-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.type-label {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-count {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.type-dot {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 7px;
  height: 7px;
  border-radius: var(--radius-circle);
  background: var(--accent-ink, var(--el-color-primary));
}

.accent-user { --accent-soft: rgba(99, 102, 241, 0.12); --accent-ink: #6366f1; --accent-line: rgba(99, 102, 241, 0.24); }
.accent-role { --accent-soft: rgba(244, 63, 94, 0.12); --accent-ink: #f43f5e; --accent-line: rgba(244, 63, 94, 0.24); }
.accent-menu { --accent-soft: rgba(59, 130, 246, 0.12); --accent-ink: #3b82f6; --accent-line: rgba(59, 130, 246, 0.24); }
.accent-dept { --accent-soft: rgba(16, 185, 129, 0.12); --accent-ink: #10b981; --accent-line: rgba(16, 185, 129, 0.24); }
.accent-post { --accent-soft: rgba(6, 182, 212, 0.12); --accent-ink: #06b6d4; --accent-line: rgba(6, 182, 212, 0.24); }
.accent-ticket { --accent-soft: rgba(249, 115, 22, 0.12); --accent-ink: #f97316; --accent-line: rgba(249, 115, 22, 0.24); }
.accent-approval { --accent-soft: rgba(168, 85, 247, 0.12); --accent-ink: #a855f7; --accent-line: rgba(168, 85, 247, 0.24); }
.accent-dict { --accent-soft: rgba(99, 102, 241, 0.12); --accent-ink: #6366f1; --accent-line: rgba(99, 102, 241, 0.24); }
.accent-dictData { --accent-soft: rgba(139, 92, 246, 0.12); --accent-ink: #8b5cf6; --accent-line: rgba(139, 92, 246, 0.24); }
.accent-announce { --accent-soft: rgba(236, 72, 153, 0.12); --accent-ink: #ec4899; --accent-line: rgba(236, 72, 153, 0.24); }
.accent-job { --accent-soft: rgba(20, 184, 166, 0.12); --accent-ink: #14b8a6; --accent-line: rgba(20, 184, 166, 0.24); }
.accent-file { --accent-soft: rgba(124, 58, 237, 0.12); --accent-ink: #8b5cf6; --accent-line: rgba(124, 58, 237, 0.24); }
.accent-gen { --accent-soft: rgba(59, 130, 246, 0.12); --accent-ink: #3b82f6; --accent-line: rgba(59, 130, 246, 0.24); }
</style>
