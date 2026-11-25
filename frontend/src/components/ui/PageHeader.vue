<template>
  <header class="ui-page-header">
    <div class="ui-page-header__left">
      <div v-if="icon" class="ui-page-header__icon" aria-hidden="true">
        <ModulePageIcon :icon="icon" :size="20" />
      </div>
      <div class="ui-page-header__text">
        <div class="ui-page-header__title-row">
          <h1 class="ui-page-header__title">{{ title }}</h1>
          <slot name="badge" />
        </div>
        <p v-if="description" class="ui-page-header__desc">{{ description }}</p>
      </div>
    </div>

    <div class="ui-page-header__right">
      <div v-if="stats && stats.length" class="ui-page-header__stats">
        <div v-for="stat in stats" :key="stat.label" class="ui-page-stat-chip">
          <span class="ui-page-stat-num">
            <CountUpNumber v-if="typeof stat.value === 'number'" :value="stat.value" />
            <template v-else>{{ stat.value }}</template>
          </span>
          <span class="ui-page-stat-label">{{ stat.label }}</span>
        </div>
      </div>
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import CountUpNumber from './CountUpNumber.vue'

export interface PageHeaderStat {
  label: string
  value: number | string
}

defineProps<{
  title: string
  description?: string
  icon?: string
  stats?: PageHeaderStat[]
}>()
</script>

<style scoped lang="scss">
.ui-page-header {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  border-radius: var(--radius-lg);
  background-color: var(--color-bg-surface);
  background-image:
    radial-gradient(ellipse 58% 115% at 96% -12%, var(--el-color-primary-light-9) 0%, transparent 66%),
    radial-gradient(ellipse 42% 90% at 4% 112%, var(--el-color-primary-light-9) 0%, transparent 62%);
  border: 1px solid var(--color-border);
}

.ui-page-header__left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.ui-page-header__icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border: 1px solid var(--el-color-primary-light-7);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ui-page-header__text {
  min-width: 0;
}

.ui-page-header__title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.ui-page-header__title {
  margin: 0;
  font-size: var(--text-h2);
  font-weight: 600;
  line-height: var(--leading-h2);
  color: var(--color-text-primary);
}

.ui-page-header__desc {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.ui-page-header__right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.ui-page-header__stats {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.ui-page-stat-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 55%, var(--color-bg-elevated));
  border: 1px solid color-mix(in srgb, var(--el-color-primary-light-7) 75%, var(--color-border));
  box-shadow: inset 0 1px 0 color-mix(in srgb, var(--color-text-primary) 7%, transparent);
  backdrop-filter: blur(8px);
  min-width: 92px;
}

.ui-page-stat-num {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.ui-page-stat-label {
  margin-top: 2px;
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}
</style>
