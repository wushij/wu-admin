<template>
  <div class="stats-grid">
    <template v-for="card in topStatCards" :key="card.key">
      <div v-if="card.permission" v-permission="card.permission" class="stats-grid-cell">
        <StatCard
          compact
          variant="core"
          clickable
          :title="card.title"
          :value="statNumber(stats, card.valueKey)"
          :trend="card.trendKey !== undefined ? trends[card.trendKey] : undefined"
          :icon="card.icon"
          :icon-theme="card.iconTheme"
          :footer="resolveFooter(card)"
          :to="card.to"
        />
      </div>
      <div v-else class="stats-grid-cell">
        <StatCard
          compact
          variant="core"
          :clickable="!!card.to"
          :title="card.title"
          :value="statNumber(stats, card.valueKey)"
          :trend="card.trendKey !== undefined ? trends[card.trendKey] : undefined"
          :icon="card.icon"
          :icon-theme="card.iconTheme"
          :footer="resolveFooter(card)"
          :to="card.to"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { DashboardStats } from '@/api/dashboard'
import {
  topStatCards,
  statNumber,
  type DashboardTrends,
  type TopStatCardConfig,
} from '../constants/statCards'
import StatCard from './StatCard.vue'

const props = defineProps<{
  stats: DashboardStats
  trends: DashboardTrends
}>()

function resolveFooter(card: TopStatCardConfig) {
  return card.footer?.(props.stats, props.trends) ?? ''
}
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 24px;
}

@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.stats-grid-cell {
  min-width: 0;
}
</style>
