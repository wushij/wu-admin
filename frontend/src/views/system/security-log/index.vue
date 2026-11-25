<template>
  <div class="app-container module-page security-log-page">
    <SecurityStatsHero :stats="stats" />

    <el-card class="content-card" shadow="never">
      <el-tabs v-model="activeTab" class="security-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="安全拦截事件" name="events">
          <SecurityEventsTab
            v-model:query-params="queryParams"
            :event-list="eventList"
            :loading="eventLoading"
            :total="eventTotal"
            @query="handleQuery"
            @reset="resetQuery"
            @reload="loadEventData"
            @clear-all="handleClearEvents"
            @filter-by-ip="filterByIp"
            @detail="showDetail"
            @block-ip="quickBlockIp"
            @delete-event="handleDeleteEvent"
          />
        </el-tab-pane>

        <el-tab-pane label="风险主体" name="risk">
          <SecurityRiskTab
            v-model:risk-query="riskQuery"
            :risk-list="riskList"
            :loading="riskLoading"
            :total="riskTotal"
            @query="handleRiskQuery"
            @reset="resetRiskQuery"
            @reload="loadRiskData"
            @manual-block="openManualBlockDialog"
            @show-events="showSubjectEvents"
            @unblock="handleUnblock"
            @block-ip="quickBlockIp"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <SecurityDetailDrawers
      v-model:detail-visible="detailVisible"
      v-model:subject-events-visible="subjectEventsVisible"
      v-model:block-dialog-visible="blockDialogVisible"
      v-model:block-form="blockForm"
      :active-event="activeEvent"
      :active-subject="activeSubject"
      :subject-events="subjectEvents"
      :subject-events-loading="subjectEventsLoading"
      @confirm-block="confirmManualBlock"
    />
  </div>
</template>

<script setup lang="ts">
import SecurityStatsHero from './components/SecurityStatsHero.vue'
import SecurityEventsTab from './components/SecurityEventsTab.vue'
import SecurityRiskTab from './components/SecurityRiskTab.vue'
import SecurityDetailDrawers from './components/SecurityDetailDrawers.vue'
import { useSecurityLogPage } from './composables/useSecurityLogPage'

const {
  activeTab,
  stats,
  queryParams,
  eventLoading,
  eventList,
  eventTotal,
  riskLoading,
  riskList,
  riskTotal,
  riskQuery,
  subjectEventsVisible,
  subjectEventsLoading,
  subjectEvents,
  activeSubject,
  detailVisible,
  activeEvent,
  blockDialogVisible,
  blockForm,
  loadEventData,
  loadRiskData,
  handleRiskQuery,
  resetRiskQuery,
  showSubjectEvents,
  handleTabChange,
  handleQuery,
  resetQuery,
  filterByIp,
  showDetail,
  handleUnblock,
  quickBlockIp,
  openManualBlockDialog,
  confirmManualBlock,
  handleDeleteEvent,
  handleClearEvents,
} = useSecurityLogPage()
</script>

<style scoped>
.security-log-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
</style>
