import { computed, nextTick, onMounted, onUnmounted, ref, type Ref } from 'vue'
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
  stopServerMonitorBackground,
} from '../serverMonitorChart'
import { isAdmin } from '@/composables/useMonitorBackground'
import {
  buildCategoryXAxis,
  buildChartTooltip,
  buildValueYAxis,
  createGradientAreaLineSeries,
  readChartTheme,
  useChartThemeObserver,
} from '@/utils/chartTheme'

const LINE_CHART_GRID = {
  left: 8,
  right: 20,
  top: 44,
  bottom: 36,
  containLabel: true,
}

export interface ServerMonitorChartRefs {
  cpuChartRef: Ref<HTMLElement | null>
  memoryChartRef: Ref<HTMLElement | null>
}

export function useServerMonitorPage(chartRefs: ServerMonitorChartRefs) {
  const { cpuChartRef, memoryChartRef } = chartRefs
  const userStore = useUserStore()
  const canQuery = computed(() =>
    hasMonitorPerm(userStore.userInfo.permissions, userStore.menus, 'monitor:server:query'),
  )

  const info = ref<ServerInfo>({})
  /** 仅手动点「刷新」时用于按钮转圈 */
  const refreshing = ref(false)
  /** 仅手动点「刷新」时用于磁盘表格 loading */
  const tableLoading = ref(false)

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
    const theme = readChartTheme()
    const { palette } = theme
    const { timeLabels, cpuData, heapData } = serverChartHistory

    if (cpuChartRef.value) {
      if (!cpuChart) cpuChart = echarts.init(cpuChartRef.value)
      cpuChart.setOption(
        {
          tooltip: {
            ...buildChartTooltip(theme, 'axis'),
            formatter: (params: unknown) => {
              const list = Array.isArray(params) ? params : [params]
              const p = list[0] as { axisValue?: string; data?: number | null }
              return p.data == null
                ? `${p.axisValue}<br/>暂无 CPU 采样`
                : `${p.axisValue}<br/>CPU: ${p.data}%`
            },
          },
          grid: { ...LINE_CHART_GRID },
          xAxis: buildCategoryXAxis(timeLabels, theme),
          yAxis: buildValueYAxis(theme, { name: 'CPU%', min: 0, max: 100 }),
          series: [
            createGradientAreaLineSeries({
              echarts,
              name: 'CPU',
              data: cpuData,
              color: palette.sky,
              connectNulls: false,
            }),
          ],
        },
        true,
      )
    }

    if (memoryChartRef.value) {
      if (!memoryChart) memoryChart = echarts.init(memoryChartRef.value)
      memoryChart.setOption(
        {
          tooltip: { ...buildChartTooltip(theme, 'axis'), formatter: '{b}<br/>JVM 堆: {c}%' },
          grid: { ...LINE_CHART_GRID },
          xAxis: buildCategoryXAxis(timeLabels, theme),
          yAxis: buildValueYAxis(theme, { name: '堆内存%', min: 0, max: 100 }),
          series: [
            createGradientAreaLineSeries({
              echarts,
              name: 'JVM 堆',
              data: heapData,
              color: palette.indigo,
            }),
          ],
        },
        true,
      )
    }

    cpuChart?.resize()
    memoryChart?.resize()
  }

  useChartThemeObserver(() => updateCharts())

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
    if (!canQuery.value) return
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
    if (!isAdmin(userStore)) {
      stopServerMonitorBackground()
    }
  })

  return {
    canQuery,
    info,
    refreshing,
    tableLoading,
    autoRefresh: serverAutoRefresh,
    cpuDisplay,
    heapDisplay,
    physicalDisplay,
    maxDiskPercent,
    refreshByUser,
    toggleAutoRefresh,
    diskProgressStatus,
  }
}
