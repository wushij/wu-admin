import { ref } from 'vue'
import {
  getDashboardStats,
  getRecentLogins,
  recordVisit,
  type DashboardStats,
  type RecentLogin,
} from '@/api/dashboard'
import { logger } from '@/utils/logger'

const defaultStats = (): DashboardStats => ({
  userCount: 0,
  onlineCount: 0,
  approvalPendingCount: 0,
  jobTotalCount: 0,
  jobRunningCount: 0,
  jobPausedCount: 0,
  todayVisits: 0,
  ticketOpenCount: 0,
  ticketOverdueCount: 0,
  userTrend: 0,
})

export function useDashboard(options?: { recentLogins?: boolean }) {
  const loading = ref(false)
  const stats = ref<DashboardStats>(defaultStats())
  const recentLogins = ref<RecentLogin[]>([])
  const loadRecentLoginsEnabled = options?.recentLogins !== false

  async function loadStats() {
    try {
      const res = await getDashboardStats()
      stats.value = { ...stats.value, ...(res.data || {}) }
    } catch (error) {
      logger.error('获取统计数据失败', error)
    }
  }

  async function loadRecentLogins() {
    try {
      const res = await getRecentLogins()
      recentLogins.value = (res.data || []).slice(0, 5)
    } catch (error) {
      logger.error('获取最近登录失败', error)
    }
  }

  async function trackVisit() {
    try {
      await recordVisit()
    } catch {
      /* 非关键路径 */
    }
  }

  async function refresh() {
    loading.value = true
    try {
      const tasks: Promise<void>[] = [loadStats(), trackVisit()]
      if (loadRecentLoginsEnabled) tasks.push(loadRecentLogins())
      await Promise.all(tasks)
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    stats,
    recentLogins,
    refresh,
  }
}
