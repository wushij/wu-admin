import { computed, ref } from 'vue'
import { getServerInfo } from '@/api/monitor/server'
import type { ServerInfo } from '@/types/system'

export const SERVER_MAX_CHART_POINTS = 20

export function resolveCpuChartValue(data: ServerInfo): number | null {
  const sys = data.cpu?.systemCpuPercent
  if (sys != null) return Math.round(sys * 10) / 10
  const proc = data.cpu?.processCpuPercent
  if (proc != null) return Math.round(proc * 10) / 10
  const load = data.cpu?.systemLoadAverage
  if (load != null && load >= 0) {
    return Math.min(Math.round((load * 100) / (data.cpu?.availableProcessors || 1)), 100)
  }
  return null
}

export function diskProgressTone(percent?: number | null): 'success' | 'warning' | 'danger' {
  const p = percent ?? 0
  if (p >= 90) return 'danger'
  if (p >= 75) return 'warning'
  return 'success'
}

export function useServerMonitor() {
  const info = ref<ServerInfo | null>(null)
  const timeLabels = ref<string[]>([])
  const cpuHistory = ref<(number | null)[]>([])
  const heapHistory = ref<number[]>([])

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

  function pushPoint(data: ServerInfo) {
    const label = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    timeLabels.value = [...timeLabels.value, label].slice(-SERVER_MAX_CHART_POINTS)
    cpuHistory.value = [...cpuHistory.value, resolveCpuChartValue(data)].slice(-SERVER_MAX_CHART_POINTS)
    const heap = Math.round(data.memory?.heapUsedPercent ?? 0)
    heapHistory.value = [...heapHistory.value, heap].slice(-SERVER_MAX_CHART_POINTS)
  }

  async function fetchServerInfo() {
    const res = await getServerInfo()
    info.value = res.data || null
    if (info.value) pushPoint(info.value)
  }

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
  }
}
