<template>
  <div class="dashboard-container">
    <WelcomeBanner
      :nickname="userStore.userInfo.nickname || '管理员'"
      :platform-name="platformName"
      :platform-subtitle="platformSubtitle"
      :current-time="currentTime"
      :greeting-message="greetingMessage"
      :stats="stats"
    />

    <CoreStatsRow :stats="stats" :trends="trends" />
    <OpsStatsRow :stats="stats" />
    <BizAlertsRow v-if="showBizAlerts" :stats="stats" />

    <el-row :gutter="24" class="content-row">
      <el-col :xs="24" :lg="16">
        <QuickEntriesPanel />
      </el-col>
      <el-col :xs="24" :lg="8">
        <SystemInfoPanel :platform-subtitle="platformSubtitle" :meta-list="systemMetaList" />
      </el-col>
    </el-row>

    <el-row :gutter="24" class="content-row">
      <el-col :span="24">
        <RecentLoginsTable :rows="recentLogins" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import WelcomeBanner from './components/WelcomeBanner.vue'
import CoreStatsRow from './components/CoreStatsRow.vue'
import OpsStatsRow from './components/OpsStatsRow.vue'
import BizAlertsRow from './components/BizAlertsRow.vue'
import QuickEntriesPanel from './components/QuickEntriesPanel.vue'
import SystemInfoPanel from './components/SystemInfoPanel.vue'
import RecentLoginsTable from './components/RecentLoginsTable.vue'
import { useDashboardData } from './composables/useDashboardData'
import { useDashboardClock } from './composables/useDashboardClock'

const userStore = useUserStore()

const {
  platformName,
  platformSubtitle,
  stats,
  trends,
  recentLogins,
  systemMetaList,
  showBizAlerts,
  initDashboard,
} = useDashboardData()

const { currentTime, greetingMessage } = useDashboardClock()

onMounted(() => {
  initDashboard()
})
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}

.content-row {
  margin-top: 8px;
}
</style>
