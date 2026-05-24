<template>
  <div class="stats-grid">
    <div v-for="card in visibleCards" :key="card.key" class="stats-grid-cell">
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
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardStats } from '@/api/dashboard'
import { useUserStore } from '@/store/user'
import { hasMenuPerm } from '@/utils/hasMenuPerm'
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

const userStore = useUserStore()

/** 仅以启用菜单树为准，停用菜单对应指标卡不展示 */
const visibleCards = computed(() =>
  topStatCards.filter((card) => !card.permission || hasMenuPerm(userStore.menus, card.permission)),
)

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
