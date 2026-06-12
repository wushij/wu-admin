import { computed, ref } from 'vue'
import { getApiAccessStatistics } from '@/api/monitor/api-access'
import type { ApiAccessStatistics } from '@/types/system'

export const API_ACCESS_STAT_DAYS = 7

export const METHOD_COLORS: Record<string, string> = {
  GET: '#67c23a',
  POST: '#409eff',
  PUT: '#e6a23c',
  DELETE: '#f56c6c',
}

export interface ApiAccessStatsView {
  totalCount: number
  successCount: number
  failCount: number
  dailyStats: Record<string, { total?: number; success?: number; fail?: number }>
  topPaths: Array<{ apiPath?: string; count?: number }>
  topUsers: Array<{ userId: number; username?: string; count?: number }>
  methodCount: Record<string, number>
}

export function formatLocalDate(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function parseLocalDate(str: string) {
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function buildStatDateRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - (API_ACCESS_STAT_DAYS - 1))
  return {
    startDate: formatLocalDate(start),
    endDate: formatLocalDate(end),
  }
}

export function buildDailyDateKeys(startDate: string, endDate: string) {
  const keys: string[] = []
  let cur = parseLocalDate(startDate)
  const end = parseLocalDate(endDate)
  while (cur <= end) {
    keys.push(formatLocalDate(cur))
    cur = new Date(cur.getFullYear(), cur.getMonth(), cur.getDate() + 1)
  }
  return keys
}

export function formatChartDayLabel(dateKey: string) {
  return dateKey.length >= 10 ? dateKey.slice(5) : dateKey
}

export function normalizeApiAccessStats(data?: ApiAccessStatistics | null): ApiAccessStatsView {
  return {
    totalCount: data?.totalCount ?? 0,
    successCount: data?.successCount ?? 0,
    failCount: data?.failCount ?? 0,
    dailyStats: data?.dailyStats ?? {},
    topPaths: data?.topPaths ?? [],
    topUsers: data?.topUsers ?? [],
    methodCount: data?.methodCount ?? {},
  }
}

export function calcChartYMax(values: number[], floor = 10) {
  if (!values.length) return floor
  const max = Math.max(...values)
  return Math.max(floor, Math.ceil(max * 1.15))
}

export function useApiAccessMonitor() {
  const stats = ref<ApiAccessStatsView>(normalizeApiAccessStats(null))
  const statRange = ref(buildStatDateRange())

  const dailyKeys = computed(() =>
    buildDailyDateKeys(statRange.value.startDate, statRange.value.endDate),
  )

  const dailyLabels = computed(() => dailyKeys.value.map(formatChartDayLabel))

  const dailyTotalSeries = computed(() =>
    dailyKeys.value.map((key) => stats.value.dailyStats[key]?.total ?? 0),
  )

  const dailySuccessSeries = computed(() =>
    dailyKeys.value.map((key) => stats.value.dailyStats[key]?.success ?? 0),
  )

  const dailyFailSeries = computed(() =>
    dailyKeys.value.map((key) => stats.value.dailyStats[key]?.fail ?? 0),
  )

  const trendYMax = computed(() => calcChartYMax(dailyTotalSeries.value, 5))

  const successRate = computed(() => {
    const total = stats.value.totalCount
    if (!total) return null
    return (stats.value.successCount / total) * 100
  })

  const methodItems = computed(() => {
    const entries = Object.entries(stats.value.methodCount)
    if (!entries.length) return []
    const max = Math.max(...entries.map(([, count]) => count), 1)
    return entries
      .sort((a, b) => b[1] - a[1])
      .map(([method, count]) => ({
        method,
        count,
        percent: Math.min(100, (count / max) * 100),
        color: METHOD_COLORS[method] || '#909399',
      }))
  })

  const topPaths = computed(() => stats.value.topPaths.slice(0, 10))

  const topUsers = computed(() => stats.value.topUsers.slice(0, 10))

  const maxPathCount = computed(() => {
    const counts = topPaths.value.map((item) => item.count ?? 0)
    return Math.max(...counts, 1)
  })

  const maxUserCount = computed(() => {
    const counts = topUsers.value.map((item) => item.count ?? 0)
    return Math.max(...counts, 1)
  })

  async function fetchStats() {
    const res = await getApiAccessStatistics(statRange.value)
    stats.value = normalizeApiAccessStats(res.data)
  }

  return {
    stats,
    statRange,
    dailyLabels,
    dailyTotalSeries,
    dailySuccessSeries,
    dailyFailSeries,
    trendYMax,
    successRate,
    methodItems,
    topPaths,
    topUsers,
    maxPathCount,
    maxUserCount,
    fetchStats,
  }
}
