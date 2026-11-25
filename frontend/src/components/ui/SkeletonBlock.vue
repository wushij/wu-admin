<template>
  <div class="ui-skeleton-wrap" :class="`mode-${mode}`" aria-busy="true" aria-label="加载中">
    <template v-if="mode === 'cards'">
      <div v-for="n in count" :key="n" class="ui-skeleton-card">
        <div class="ui-skeleton-row">
          <div class="motion-shimmer sk-avatar" />
          <div class="sk-lines">
            <div class="motion-shimmer sk-line short" />
            <div class="motion-shimmer sk-line medium" />
          </div>
        </div>
        <div class="motion-shimmer sk-line full" />
        <div class="motion-shimmer sk-line wide" />
      </div>
    </template>
    <template v-else>
      <div v-for="n in count" :key="n" class="ui-skeleton-table-row">
        <div class="motion-shimmer sk-cell sm" />
        <div class="motion-shimmer sk-cell md" />
        <div class="motion-shimmer sk-cell lg" />
        <div class="motion-shimmer sk-cell sm" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    mode?: 'rows' | 'cards'
    count?: number
  }>(),
  {
    mode: 'rows',
    count: 4,
  },
)
</script>

<style scoped lang="scss">
.ui-skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.ui-skeleton-wrap.mode-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--space-4);
}

.ui-skeleton-card {
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-subtle);
  background: var(--color-bg-surface);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.ui-skeleton-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.sk-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.sk-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.sk-line {
  height: 12px;
  border-radius: var(--radius-pill);
  &.short { width: 42%; }
  &.medium { width: 68%; }
  &.wide { width: 82%; }
  &.full { width: 100%; }
}

.ui-skeleton-table-row {
  display: grid;
  grid-template-columns: 1fr 2fr 3fr 1fr;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
}

.sk-cell {
  height: 14px;
  border-radius: var(--radius-pill);
}
</style>
