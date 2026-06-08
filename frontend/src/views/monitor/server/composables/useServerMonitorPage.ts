import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import type { EChartsType } from 'echarts'
import { type ServerInfo } from '@/api/monitor/server'
import { useUserStore } from '@/store/user'
import { hasMonitorPerm } from '@/utils/hasMenuPerm'
import {
  fetchServerPoint,
  serverAutoRefresh,
  serverChartHistory,
  setServerAutoRefresh,
  setServerChartRenderHandler,
  setServerInfoUpdateHandler,
  startServerMonitorBackground,
} from '../serverMonitorChart'

const LINE_CHART_GRID = {
  left: 8,
  right: 20,
  top: 44,
  bottom: 36,
  containLabel: true,
}

export function useServerMonitorPage() {
  const userStore = useUserStore()
  const canList = computed(() =>
    hasMonitorPerm(userStore.userInfo.permissions, userStore.menus, 'monitor:server:list'),
  )

  const info = ref<ServerInfo>({})
  /** 仅手动点「刷新」时用于按钮转圈 */
  const refreshing = ref(false)
  /** 仅手动点「刷新」时用于磁盘表格 loading */
  const tableLoading = ref(false)

  const cpuChartRef = ref<HTMLElement | null>(null)
  const memoryChartRef = ref<HTMLElement | null>(null)

  let cpuChart: EChartsType | null = null
  let memoryChart: EChartsType | null = null
  let echartsModule: typeof import('echarts') | null = null

  const cpuDisplay = computed(() => {
    const p = info.value.cpu?.systemCpuPercent ?? info.value.cpu?.processCpuPercent
    if (p != null) return `${p.toFixed(1)}%`
    const load = info.value.cpu?.systemLoadAverage
    if (load != null && load >= 0) return load.toFixed(2)
    return '-'
  })

  const heapDisplay = computed(() => {
    const p = info.value.memory?.heapUsedPercent
    return p != null ? `${p.toFixed(1)}%` : '-'
  })

  const physicalDisplay = computed(() => {
    const p = info.value.memory?.physicalUsedPercent
    return p != null ? `${p.toFixed(1)}%` : '-'
  })

  const maxDiskPercent = computed(() => {
    const disks = info.value.disks || []
    if (!disks.length) return 0
    return Math.max(...disks.map((d) => d.usedPercent ?? 0))
  })

  async function ensureEcharts() {
    if (!echartsModule) {
      echartsModule = await import('echarts')
    }
    return echartsModule
  }

  async function refreshByUser() {
    if (refreshing.value) return
    refreshing.value = true
    tableLoading.value = true
    try {
      await fetchServerPoint()
    } finally {
      refreshing.value = false
      tableLoading.value = false
    }
  }

  async function updateCharts() {
    const echarts = await ensureEcharts()
    const { timeLabels, cpuData, heapData } = serverChartHistory

    if (cpuChartRef.value) {
      if (!cpuChart) cpuChart = echarts.init(cpuChartRef.value)
      cpuChart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: (params: unknown) => {
            const list = Array.isArray(params) ? params : [params]
            const p = list[0] as { axisValue?: string; data?: number | null }
            return p.data == null
              ? `${p.axisValue}<br/>暂无 CPU 采样`
              : `${p.axisValue}<br/>CPU: ${p.data}%`
          },
        },
        grid: { ...LINE_CHART_GRID },
        xAxis: { type: 'category', data: [...timeLabels], boundaryGap: false },
        yAxis: { type: 'value', name: 'CPU%', nameGap: 12, min: 0, max: 100 },
        series: [{
          data: [...cpuData],
          type: 'line',
          smooth: true,
          connectNulls: false,
          areaStyle: { opacity: 0.15 },
          itemStyle: { color: '#409eff' },
        }],
      })
    }

    if (memoryChartRef.value) {
      if (!memoryChart) memoryChart = echarts.init(memoryChartRef.value)
      memoryChart.setOption({
        tooltip: { trigger: 'axis', formatter: '{b}<br/>JVM 堆: {c}%' },
        grid: { ...LINE_CHART_GRID },
        xAxis: { type: 'category', data: [...timeLabels], boundaryGap: false },
        yAxis: { type: 'value', name: '堆内存%', nameGap: 12, min: 0, max: 100 },
        series: [{
          data: [...heapData],
          type: 'line',
          smooth: true,
          areaStyle: { opacity: 0.15 },
          itemStyle: { color: '#67c23a' },
        }],
      })
    }

    cpuChart?.resize()
    memoryChart?.resize()
  }

  function diskProgressStatus(percent: number) {
    if (percent >= 90) return 'exception'
    if (percent >= 75) return 'warning'
    return undefined
  }

  function toggleAutoRefresh(val: string | number | boolean) {
    setServerAutoRefresh(val)
  }

  function handleResize() {
    cpuChart?.resize()
    memoryChart?.resize()
  }

  function disposeCharts() {
    cpuChart?.dispose()
    memoryChart?.dispose()
    cpuChart = null
    memoryChart = null
  }

  onMounted(async () => {
    if (!canList.value) return
    startServerMonitorBackground()
    setServerInfoUpdateHandler((data) => {
      info.value = data
    })
    setServerChartRenderHandler(() => updateCharts())
    if (serverChartHistory.lastServerInfo) {
      info.value = serverChartHistory.lastServerInfo
    }
    await nextTick()
    if (serverChartHistory.timeLabels.length) {
      await updateCharts()
    }
    await fetchServerPoint()
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    setServerChartRenderHandler(null)
    setServerInfoUpdateHandler(null)
    window.removeEventListener('resize', handleResize)
    disposeCharts()
  })

  return {
    canList,
    info,
    refreshing,
    tableLoading,
    autoRefresh: serverAutoRefresh,
    cpuDisplay,
    heapDisplay,
    physicalDisplay,
    maxDiskPercent,
    cpuChartRef,
    memoryChartRef,
    refreshByUser,
    toggleAutoRefresh,
    diskProgressStatus,
  }
}
