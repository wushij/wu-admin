<template>
  <el-row :gutter="24" class="stats-row ops-stats-row">
    <el-col v-for="card in opsStatCards" :key="card.key" :xs="24" :sm="12" :lg="6">
      <div v-if="card.permission" v-permission="card.permission">
        <StatCard
          variant="ops"
          clickable
          :title="card.title"
          :value="statNumber(stats, card.valueKey)"
          :icon="card.icon"
          :icon-theme="card.iconTheme"
          :footer="card.footer(stats)"
          :to="card.to"
        />
      </div>
      <StatCard
        v-else
        variant="ops"
        clickable
        :title="card.title"
        :value="statNumber(stats, card.valueKey)"
        :icon="card.icon"
        :icon-theme="card.iconTheme"
        :footer="card.footer(stats)"
        :to="card.to"
      />
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import type { DashboardStats } from '@/api/dashboard'
import { opsStatCards, statNumber } from '../constants/statCards'
import StatCard from './StatCard.vue'

defineProps<{
  stats: DashboardStats
}>()
</script>

<style scoped>
.stats-row {
  margin-bottom: 32px;
}

.ops-stats-row {
  margin-top: -16px;
}
</style>
