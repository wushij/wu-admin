<template>
  <SkeletonBlock v-if="loading" mode="cards" :count="5" class="stats-skeleton" />
  <div v-else class="stats-grid">
    <div
      v-for="(card, idx) in visibleCards"
      :key="card.key"
      class="stats-grid-cell motion-stagger-item"
      :style="{ '--stagger-index': idx }"
    >
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
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import {
  topStatCards,
  statNumber,
  type DashboardTrends,
  type TopStatCardConfig,
} from '../constants/statCards'
import StatCard from './StatCard.vue'

const props = defineProps<{
  loading?: boolean
  stats: DashboardStats
  trends: DashboardTrends
}>()

const userStore = useUserStore()

const visibleCards = computed(() =>
  topStatCards.filter((card) => !card.permission || hasMenuPerm(userStore.menus, card.permission)),
)

function resolveFooter(card: TopStatCardConfig) {
  return card.footer?.(props.stats, props.trends) ?? ''
}
</script>

<style scoped>
.stats-skeleton {
  margin-bottom: var(--space-5);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-3);
  margin-bottom: var(--space-5);
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
