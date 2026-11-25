<template>
  <div ref="rootRef" class="dashboard-container">
    <WelcomeBanner
      :nickname="userStore.userInfo.nickname || '管理员'"
      :avatar-src="avatarSrc"
      :avatar-fallback="avatarFallback"
      :platform-name="platformName"
      :platform-subtitle="platformSubtitle"
      :current-time="currentTime"
      :greeting-message="greetingMessage"
      :stats="stats"
    />

    <DashboardStatsGrid :loading="loading" :stats="stats" :trends="trends" />

    <el-row :gutter="20" class="content-row motion-reveal">
      <el-col :xs="24" :lg="16">
        <QuickEntriesPanel />
      </el-col>
      <el-col :xs="24" :lg="8">
        <SystemInfoPanel :platform-subtitle="platformSubtitle" :meta-list="systemMetaList" />
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row motion-reveal">
      <el-col :span="24">
        <RecentLoginsTable :rows="recentLogins" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/store/user'
import { resolveChatAvatar, avatarFallback as getAvatarFallback } from '@/utils/chat-avatar'
import { useScrollReveal } from '@/composables/useMotion'
import WelcomeBanner from './components/WelcomeBanner.vue'
import DashboardStatsGrid from './components/DashboardStatsGrid.vue'
import QuickEntriesPanel from './components/QuickEntriesPanel.vue'
import SystemInfoPanel from './components/SystemInfoPanel.vue'
import RecentLoginsTable from './components/RecentLoginsTable.vue'
import { useDashboardData } from './composables/useDashboardData'
import { useDashboardClock } from './composables/useDashboardClock'

const userStore = useUserStore()
const rootRef = ref<HTMLElement | null>(null)
useScrollReveal(rootRef)

const avatarSrc = computed(() =>
  resolveChatAvatar(undefined, new Map(), userStore.userInfo.avatar),
)

const avatarFallback = computed(() =>
  getAvatarFallback(userStore.userInfo.nickname || userStore.userInfo.username),
)

const {
  loading,
  platformName,
  platformSubtitle,
  stats,
  trends,
  recentLogins,
  systemMetaList,
  initDashboard,
} = useDashboardData()

const { currentTime, greetingMessage } = useDashboardClock()

onMounted(async () => {
  if (userStore.isLoggedIn) {
    await userStore.refreshUserStore().catch(() => {})
  }
  initDashboard()
})
</script>

<style scoped>
.dashboard-container {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.content-row {
  margin-bottom: var(--space-5);
}

.content-row:last-child {
  margin-bottom: 0;
}
</style>
