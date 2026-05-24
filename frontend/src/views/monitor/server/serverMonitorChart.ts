import { ref } from 'vue'
import { getServerInfo, type ServerInfo } from '@/api/monitor/server'
import {
  loadMonitorSession,
  loadMonitorSessionFlag,
  removeMonitorSession,
  saveMonitorSession,
  saveMonitorSessionFlag,
} from '@/utils/monitorChartStorage'

export const SERVER_CHART_STORAGE_KEY = 'wu-admin:monitor:server:chart'
export const SERVER_AUTO_REFRESH_KEY = 'wu-admin:monitor:server:autoRefresh'

export const MAX_SERVER_CHART_POINTS = 20
export const SERVER_POLL_INTERVAL_MS = 5000

export interface ServerChartHistory {
  timeLabels: string[]
  cpuData: (number | null)[]
  heapData: number[]
  lastServerInfo: ServerInfo | null
}

const defaultChartHistory = (): ServerChartHistory => ({
  timeLabels: [],
  cpuData: [],
  heapData: [],
  lastServerInfo: null,
})

function resolveCpuChartValue(data: ServerInfo): number | null {
  const sys = data.cpu?.systemCpuPercent
  if (sys != null) return Math.round(sys)
  const proc = data.cpu?.processCpuPercent
  if (proc != null) return Math.round(proc)
  const load = data.cpu?.systemLoadAverage
  if (load != null && load >= 0) {
    return Math.min(Math.round(load * 100 / (data.cpu?.availableProcessors || 1)), 100)
  }
  return null
}

export const serverChartHistory = loadMonitorSession(SERVER_CHART_STORAGE_KEY, defaultChartHistory())
export const serverAutoRefresh = ref(loadMonitorSessionFlag(SERVER_AUTO_REFRESH_KEY, true))

let pollTimer: ReturnType<typeof setInterval> | null = null
let chartRenderHandler: (() => Promise<void>) | null = null
let infoUpdateHandler: ((data: ServerInfo) => void) | null = null
let backgroundActive = false
let serverFetchInFlight = false

function persistChartHistory() {
  saveMonitorSession(SERVER_CHART_STORAGE_KEY, serverChartHistory)
}

function persistAutoRefresh() {
  saveMonitorSessionFlag(SERVER_AUTO_REFRESH_KEY, serverAutoRefresh.value)
}

export function recordServerStatsPoint(data: ServerInfo) {
  const now = new Date().toLocaleTimeString()
  serverChartHistory.timeLabels.push(now)
  if (serverChartHistory.timeLabels.length > MAX_SERVER_CHART_POINTS) serverChartHistory.timeLabels.shift()

  serverChartHistory.cpuData.push(resolveCpuChartValue(data))
  if (serverChartHistory.cpuData.length > MAX_SERVER_CHART_POINTS) serverChartHistory.cpuData.shift()

  const heapPercent = data.memory?.heapUsedPercent ?? 0
  serverChartHistory.heapData.push(Math.round(heapPercent))
  if (serverChartHistory.heapData.length > MAX_SERVER_CHART_POINTS) serverChartHistory.heapData.shift()

  persistChartHistory()
}

export async function fetchServerPoint() {
  if (serverFetchInFlight) return
  serverFetchInFlight = true
  try {
    const res = await getServerInfo()
    if (res.data) {
      serverChartHistory.lastServerInfo = res.data
      recordServerStatsPoint(res.data)
      infoUpdateHandler?.(res.data)
      await chartRenderHandler?.()
    }
  } catch {
    /* ignore */
  } finally {
    serverFetchInFlight = false
  }
}

export function startServerPolling() {
  if (pollTimer || !serverAutoRefresh.value || !backgroundActive) return
  pollTimer = setInterval(fetchServerPoint, SERVER_POLL_INTERVAL_MS)
}

export function stopServerPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

export function syncServerPolling() {
  if (serverAutoRefresh.value && backgroundActive) {
    startServerPolling()
  } else {
    stopServerPolling()
  }
}

export function setServerChartRenderHandler(handler: (() => Promise<void>) | null) {
  chartRenderHandler = handler
}

export function setServerInfoUpdateHandler(handler: ((data: ServerInfo) => void) | null) {
  infoUpdateHandler = handler
}

export function setServerAutoRefresh(val: string | number | boolean) {
  serverAutoRefresh.value = val === true || val === 'true' || val === 1
  persistAutoRefresh()
  if (serverAutoRefresh.value) {
    fetchServerPoint()
  }
  syncServerPolling()
}

export function startServerMonitorBackground() {
  if (backgroundActive) {
    syncServerPolling()
    return
  }
  backgroundActive = true
  fetchServerPoint()
  syncServerPolling()
}

export function stopServerMonitorBackground() {
  backgroundActive = false
  stopServerPolling()
}

export function resetServerMonitorBackground() {
  stopServerMonitorBackground()
  Object.assign(serverChartHistory, defaultChartHistory())
  serverAutoRefresh.value = true
  removeMonitorSession(SERVER_CHART_STORAGE_KEY)
  removeMonitorSession(SERVER_AUTO_REFRESH_KEY)
}

export function onServerMonitorVisibilityChange(hidden: boolean) {
  if (hidden) {
    stopServerPolling()
    return
  }
  if (backgroundActive && serverAutoRefresh.value) {
    fetchServerPoint()
    startServerPolling()
  }
}
