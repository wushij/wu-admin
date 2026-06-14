import { computed, onMounted, onUnmounted, ref, type Ref } from 'vue'
import type { ServerInfo } from '@/types/system'
import {
  fetchServerPoint,
  serverAutoRefresh,
  serverChartHistory,
  setServerAutoRefresh,
  setServerInfoUpdateHandler,
  startServerMonitorBackground,
} from '@/composables/monitor/serverMonitorChart'

export { resolveCpuChartValue } from '@/composables/monitor/serverMonitorChart'

export const SERVER_MAX_CHART_POINTS = 20

export function diskProgressTone(percent?: number | null): 'success' | 'warning' | 'danger' {
  const p = percent ?? 0
  if (p >= 90) return 'danger'
  if (p >= 75) return 'warning'
  return 'success'
}

export function useServerMonitor() {
  const info = ref<ServerInfo | null>(serverChartHistory.lastServerInfo)

  const timeLabels = computed(() => serverChartHistory.timeLabels)
  const cpuHistory = computed(() => serverChartHistory.cpuData)
  const heapHistory = computed(() => serverChartHistory.heapData)

  const cpuDisplay = computed(() => {
    const p = info.value?.cpu?.systemCpuPercent ?? info.value?.cpu?.processCpuPercent
    if (p != null) return `${p.toFixed(1)}%`
    const load = info.value?.cpu?.systemLoadAverage
    if (load != null && load >= 0) return load.toFixed(2)
    return '—'
  })

  const heapDisplay = computed(() => {
    const p = info.value?.memory?.heapUsedPercent
    return p != null ? `${p.toFixed(1)}%` : '—'
  })

  const physicalDisplay = computed(() => {
    const p = info.value?.memory?.physicalUsedPercent
    return p != null ? `${p.toFixed(1)}%` : '—'
  })

  const maxDiskPercent = computed(() => {
    const disks = info.value?.disks || []
    if (!disks.length) return 0
    return Math.max(...disks.map((d) => d.usedPercent ?? 0))
  })

  async function fetchServerInfo() {
    await fetchServerPoint()
  }

  onMounted(() => {
    startServerMonitorBackground()
    setServerInfoUpdateHandler((data) => {
      info.value = data
    })
    if (serverChartHistory.lastServerInfo) {
      info.value = serverChartHistory.lastServerInfo
    }
  })

  onUnmounted(() => {
    setServerInfoUpdateHandler(null)
  })

  return {
    info,
    timeLabels,
    cpuHistory,
    heapHistory,
    cpuDisplay,
    heapDisplay,
    physicalDisplay,
    maxDiskPercent,
    fetchServerInfo,
    autoRefresh: serverAutoRefresh,
    setAutoRefresh: setServerAutoRefresh,
  }
}
