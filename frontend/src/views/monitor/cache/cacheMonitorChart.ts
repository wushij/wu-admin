import { reactive, ref, toRaw } from 'vue'
import { getCacheStats, type CacheStats } from '@/api/monitor/cache'
import {
  loadMonitorSession,
  loadMonitorSessionFlag,
  removeMonitorSession,
  saveMonitorSession,
  saveMonitorSessionFlag,
} from '@/utils/monitorChartStorage'

export const CACHE_CHART_STORAGE_KEY = 'wu-admin:monitor:cache:chart'
export const CACHE_AUTO_REFRESH_KEY = 'wu-admin:monitor:cache:autoRefresh'

export const MAX_CACHE_CHART_POINTS = 20
export const CACHE_STATS_INTERVAL_MS = 3000

export interface CacheChartHistory {
  timeLabels: string[]
  qpsData: number[]
  hitRateData: (number | null)[]
  clientsData: number[]
  lastMemoryStats: CacheStats | null
  lastLiveQps: number | null
  lastHitRateText: string
}

const defaultChartHistory = (): CacheChartHistory => ({
  timeLabels: [],
  qpsData: [],
  hitRateData: [],
  clientsData: [],
  lastMemoryStats: null,
  lastLiveQps: null,
  lastHitRateText: '-',
})

/** 须 reactive，否则概览「当前 QPS」等 computed 无法随采样更新 */
export const cacheChartHistory = reactive(
  loadMonitorSession(CACHE_CHART_STORAGE_KEY, defaultChartHistory()),
)
export const cacheAutoRefresh = ref(loadMonitorSessionFlag(CACHE_AUTO_REFRESH_KEY, true))

let statsTimer: ReturnType<typeof setInterval> | null = null
let chartRenderHandler: (() => Promise<void>) | null = null
let backgroundActive = false
let statsFetchInFlight = false

function persistChartHistory() {
  saveMonitorSession(CACHE_CHART_STORAGE_KEY, toRaw(cacheChartHistory))
}

function persistAutoRefresh() {
  saveMonitorSessionFlag(CACHE_AUTO_REFRESH_KEY, cacheAutoRefresh.value)
}

export function recordCacheStatsPoint(stats: CacheStats) {
  cacheChartHistory.lastLiveQps = stats.ops ?? null
  if (stats.cumulativeHitRate != null) {
    cacheChartHistory.lastHitRateText = `${Math.round(stats.cumulativeHitRate * 100)}%`
  } else {
    cacheChartHistory.lastHitRateText = '-'
  }
  cacheChartHistory.lastMemoryStats = stats

  const now = new Date().toLocaleTimeString()
  cacheChartHistory.timeLabels.push(now)
  if (cacheChartHistory.timeLabels.length > MAX_CACHE_CHART_POINTS) cacheChartHistory.timeLabels.shift()

  cacheChartHistory.qpsData.push(stats.ops ?? 0)
  if (cacheChartHistory.qpsData.length > MAX_CACHE_CHART_POINTS) cacheChartHistory.qpsData.shift()

  cacheChartHistory.hitRateData.push(stats.hitRate != null ? Math.round(stats.hitRate * 100) : null)
  if (cacheChartHistory.hitRateData.length > MAX_CACHE_CHART_POINTS) cacheChartHistory.hitRateData.shift()

  const clients = stats.connectedClients ?? 0
  cacheChartHistory.clientsData.push(clients)
  if (cacheChartHistory.clientsData.length > MAX_CACHE_CHART_POINTS) cacheChartHistory.clientsData.shift()

  persistChartHistory()
}

export async function fetchCacheStatsPoint() {
  if (statsFetchInFlight) return
  statsFetchInFlight = true
  try {
    const res = await getCacheStats()
    if (res.data) {
      recordCacheStatsPoint(res.data)
      await chartRenderHandler?.()
    }
  } catch {
    /* ignore */
  } finally {
    statsFetchInFlight = false
  }
}

export function startCacheStatsPolling() {
  if (statsTimer || !cacheAutoRefresh.value || !backgroundActive) return
  statsTimer = setInterval(fetchCacheStatsPoint, CACHE_STATS_INTERVAL_MS)
}

export function stopCacheStatsPolling() {
  if (statsTimer) {
    clearInterval(statsTimer)
    statsTimer = null
  }
}

export function syncCacheStatsPolling() {
  if (cacheAutoRefresh.value && backgroundActive) {
    startCacheStatsPolling()
  } else {
    stopCacheStatsPolling()
  }
}

export function setCacheChartRenderHandler(handler: (() => Promise<void>) | null) {
  chartRenderHandler = handler
}

export function setCacheAutoRefresh(val: string | number | boolean) {
  cacheAutoRefresh.value = val === true || val === 'true' || val === 1
  persistAutoRefresh()
  if (cacheAutoRefresh.value) {
    fetchCacheStatsPoint()
  }
  syncCacheStatsPolling()
}

/** 登录后全局后台采样（无需进入缓存监控页） */
export function startCacheMonitorBackground() {
  if (backgroundActive) {
    syncCacheStatsPolling()
    return
  }
  backgroundActive = true
  fetchCacheStatsPoint()
  syncCacheStatsPolling()
}

export function stopCacheMonitorBackground() {
  backgroundActive = false
  stopCacheStatsPolling()
}

export function resetCacheMonitorBackground() {
  stopCacheMonitorBackground()
  Object.assign(cacheChartHistory, defaultChartHistory())
  cacheAutoRefresh.value = true
  removeMonitorSession(CACHE_CHART_STORAGE_KEY)
  removeMonitorSession(CACHE_AUTO_REFRESH_KEY)
}

export function onMonitorVisibilityChange(hidden: boolean) {
  if (hidden) {
    stopCacheStatsPolling()
    return
  }
  if (backgroundActive && cacheAutoRefresh.value) {
    fetchCacheStatsPoint()
    startCacheStatsPolling()
  }
}
