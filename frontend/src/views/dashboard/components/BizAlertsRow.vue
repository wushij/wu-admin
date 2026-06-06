<template>
  <el-row :gutter="24" class="stats-row biz-stats-row">
    <el-col
      v-for="card in visibleCards"
      :key="card.key"
      :xs="24"
      :sm="12"
      :lg="6"
    >
      <div v-permission="card.permission">
        <StatCard
          variant="biz"
          clickable
          :title="card.label"
          :value="statNumber(stats, card.valueKey)"
          :biz-value-class="card.valueClass ?? ''"
          :to="card.to"
        />
      </div>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardStats } from '@/api/dashboard'
import { bizAlertCards, statNumber } from '../constants/statCards'
import StatCard from './StatCard.vue'

const props = defineProps<{
  stats: DashboardStats
}>()

const visibleCards = computed(() => bizAlertCards.filter((card) => card.visible(props.stats)))
</script>

<style scoped>
.stats-row {
  margin-bottom: 32px;
}

.biz-stats-row {
  margin-top: -16px;
}
</style>
