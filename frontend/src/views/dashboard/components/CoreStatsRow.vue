<template>
  <el-row :gutter="24" class="stats-row">
    <el-col v-for="card in coreStatCards" :key="card.key" :xs="24" :sm="12" :lg="6">
      <StatCard
        variant="core"
        :title="card.title"
        :value="statNumber(stats, card.valueKey)"
        :trend="trends[card.trendKey]"
        :icon="card.icon"
        :icon-theme="card.iconTheme"
        :footer="formatTrendFooter(trends[card.trendKey], card.flatFooterWhenZero)"
      />
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import type { DashboardStats } from '@/api/dashboard'
import { coreStatCards, formatTrendFooter, statNumber, type DashboardTrends } from '../constants/statCards'
import StatCard from './StatCard.vue'

defineProps<{
  stats: DashboardStats
  trends: DashboardTrends
}>()
</script>

<style scoped>
.stats-row {
  margin-bottom: 32px;
}
</style>
