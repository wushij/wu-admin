import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { EChartsType } from 'echarts'
import {
  deleteCacheKey,
  getCacheInfo,
  getCacheValue,
  scanCacheKeys,
  type CacheInfo,
  type CacheKeyItem,
  type CacheStats,
  type CacheValueDetail,
} from '@/api/monitor/cache'
import { useUserStore } from '@/store/user'
import { hasMonitorPerm } from '@/utils/hasMenuPerm'
import {
  cacheAutoRefresh,
  cacheChartHistory,
  fetchCacheStatsPoint,
  setCacheAutoRefresh,
  setCacheChartRenderHandler,
  startCacheMonitorBackground,
} from '../cacheMonitorChart'

/** 表格固定高度，避免切换筛选时行数变化导致页面跳动 */
const KEYS_TABLE_HEIGHT = 480

const PATTERN_PRESETS = [
  { label: '全部', pattern: '*' },
  { label: '系统缓存', pattern: 'cache:sys:*' },
  { label: '仪表盘', pattern: 'dashboard:*' },
  { label: 'API', pattern: 'api:*' },
] as const

const LINE_CHART_GRID = {
  left: 8,
  right: 20,
  top: 44,
  bottom: 36,
  containLabel: true,
}

export function useCacheMonitorPage() {
  const userStore = useUserStore()
  const canList = computed(() =>
    hasMonitorPerm(userStore.userInfo.permissions, userStore.menus, 'monitor:cache:list'),
  )
  const canDelete = computed(() =>
    hasMonitorPerm(userStore.userInfo.permissions, userStore.menus, 'monitor:cache:delete'),
  )

  const info = ref<CacheInfo>({})
  const keys = ref<CacheKeyItem[]>([])
  const keysLoading = ref(false)
  const keysTruncated = ref(false)
  const searchPattern = ref('*')
  const scanLimit = ref(200)
  const keyFilter = ref('')
  const refreshing = ref(false)

  const liveQps = computed(() => cacheChartHistory.lastLiveQps)
  const liveHitRateText = computed(() => cacheChartHistory.lastHitRateText)

  const detailVisible = ref(false)
  const cacheDetail = ref<CacheValueDetail>({
    key: '',
    type: '',
    ttl: -1,
    value: null,
  })

  const memoryChartRef = ref<HTMLElement | null>(null)
  const qpsChartRef = ref<HTMLElement | null>(null)
  const hitRateChartRef = ref<HTMLElement | null>(null)
  const clientsChartRef = ref<HTMLElement | null>(null)

  let memoryChart: EChartsType | null = null
  let qpsChart: EChartsType | null = null
  let hitRateChart: EChartsType | null = null
  let clientsChart: EChartsType | null = null
  let echartsModule: typeof import('echarts') | null = null

  const filteredKeys = computed(() => {
    const keyword = keyFilter.value.trim().toLowerCase()
    if (!keyword) return keys.value
    return keys.value.filter((item) => item.key.toLowerCase().includes(keyword))
  })

  const pagination = ref({ page: 1, pageSize: 10 })

  const pagedKeys = computed(() => {
    const start = (pagination.value.page - 1) * pagination.value.pageSize
    return filteredKeys.value.slice(start, start + pagination.value.pageSize)
  })

  watch(keyFilter, () => {
    pagination.value.page = 1
  })

  async function ensureEcharts() {
    if (!echartsModule) {
      echartsModule = await import('echarts')
    }
    return echartsModule
  }

  async function loadInfo() {
    try {
      const res = await getCacheInfo()
      info.value = res.data || {}
    } catch {
      /* ignore */
    }
  }

  async function updateCharts(stats: CacheStats = cacheChartHistory.lastMemoryStats ?? {}) {
    const echarts = await ensureEcharts()
    const { timeLabels, qpsData, hitRateData, clientsData } = cacheChartHistory

    const used = stats.usedMemory ?? 0
    const max = stats.maxMemory ?? 0
    const showQuotaPie = stats.maxMemoryConfigured === true && max > 0 && max >= used
    const remainValue = showQuotaPie ? Math.max(max - used, 0) : 0

    if (memoryChartRef.value) {
      if (!memoryChart) memoryChart = echarts.init(memoryChartRef.value)
      if (showQuotaPie) {
        memoryChart.setOption({
          tooltip: {
            trigger: 'item',
            formatter: (p: { name?: string; value?: number; percent?: number }) => {
              const pct = p.percent != null ? p.percent.toFixed(1) : '0'
              return `${p.name}<br/>${formatBytes(Number(p.value))}（${pct}%）`
            },
          },
          series: [{
            type: 'pie',
            radius: ['48%', '68%'],
            center: ['50%', '54%'],
            label: {
              show: true,
              formatter: (p: { name?: string; percent?: number }) =>
                `${p.name}\n${p.percent != null ? p.percent.toFixed(1) : '0'}%`,
            },
            data: [
              { value: used, name: '已用', itemStyle: { color: '#67c23a' } },
              { value: remainValue, name: '剩余配额', itemStyle: { color: '#e5e7eb' } },
            ],
          }],
        })
      } else {
        memoryChart.setOption({
          tooltip: { show: false },
          title: {
            text: stats.usedMemoryHuman || formatBytes(used),
            subtext: 'Redis 当前占用',
            left: 'center',
            top: '42%',
            textStyle: { fontSize: 18, fontWeight: 600 },
            subtextStyle: { fontSize: 12, color: '#909399' },
          },
          series: [{
            type: 'pie',
            radius: ['48%', '68%'],
            center: ['50%', '54%'],
            label: { show: false },
            data: [{ value: 1, name: '已用', itemStyle: { color: '#67c23a' } }],
          }],
        })
      }
    }

    if (qpsChartRef.value) {
      if (!qpsChart) qpsChart = echarts.init(qpsChartRef.value)
      qpsChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { ...LINE_CHART_GRID },
        xAxis: { type: 'category', data: [...timeLabels], boundaryGap: false },
        yAxis: { type: 'value', name: 'QPS', nameGap: 12, minInterval: 1 },
        series: [{
          data: [...qpsData],
          type: 'line',
          smooth: true,
          areaStyle: { opacity: 0.15 },
          itemStyle: { color: '#67c23a' },
        }],
      })
    }

    if (hitRateChartRef.value) {
      if (!hitRateChart) hitRateChart = echarts.init(hitRateChartRef.value)
      hitRateChart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: (params: unknown) => {
            const list = Array.isArray(params) ? params : [params]
            const p = list[0] as { axisValue?: string; data?: number | null }
            const val = p.data
            return val == null ? `${p.axisValue}<br/>本周期无读写` : `${p.axisValue}<br/>命中率: ${val}%`
          },
        },
        grid: { ...LINE_CHART_GRID },
        xAxis: { type: 'category', data: [...timeLabels], boundaryGap: false },
        yAxis: { type: 'value', name: '命中率%', nameGap: 12, min: 0, max: 100 },
        series: [{
          data: [...hitRateData],
          type: 'line',
          smooth: true,
          connectNulls: false,
          areaStyle: { opacity: 0.15 },
          itemStyle: { color: '#409eff' },
        }],
      })
    }

    if (clientsChartRef.value) {
      if (!clientsChart) clientsChart = echarts.init(clientsChartRef.value)
      const clientRange = calcYAxisRange(clientsData)
      clientsChart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: (params: unknown) => {
            const list = Array.isArray(params) ? params : [params]
            const p = list[0] as { axisValue?: string; data?: number }
            return `${p.axisValue}<br/>连接数: ${p.data ?? 0}`
          },
        },
        grid: { ...LINE_CHART_GRID },
        xAxis: { type: 'category', data: [...timeLabels], boundaryGap: false },
        yAxis: {
          type: 'value',
          name: '连接数',
          nameGap: 12,
          minInterval: 1,
          min: clientRange.min,
          max: clientRange.max,
        },
        series: [{
          data: [...clientsData],
          type: 'line',
          smooth: true,
          areaStyle: { opacity: 0.15 },
          itemStyle: { color: '#e6a23c' },
        }],
      })
    }

    memoryChart?.resize()
    qpsChart?.resize()
    hitRateChart?.resize()
    clientsChart?.resize()
  }

  async function loadKeys() {
    keysLoading.value = true
    try {
      const res = await scanCacheKeys(searchPattern.value, scanLimit.value)
      keys.value = res.data?.items || []
      keysTruncated.value = !!res.data?.truncated
      pagination.value.page = 1
    } finally {
      keysLoading.value = false
    }
  }

  async function refreshAll() {
    refreshing.value = true
    try {
      await Promise.all([loadInfo(), fetchCacheStatsPoint(), loadKeys()])
    } finally {
      refreshing.value = false
    }
  }

  function applyPreset(pattern: string) {
    if (searchPattern.value === pattern) {
      return
    }
    searchPattern.value = pattern
    keyFilter.value = ''
    loadKeys()
  }

  async function copyKey(key: string) {
    try {
      await navigator.clipboard.writeText(key)
      ElMessage.success('已复制键名')
    } catch {
      ElMessage.error('复制失败')
    }
  }

  async function handleView(key: string) {
    detailVisible.value = true
    try {
      const res = await getCacheValue(key)
      cacheDetail.value = res.data || { key, type: 'unknown', ttl: -1, value: null }
    } catch {
      ElMessage.error('获取缓存详情失败')
      detailVisible.value = false
    }
  }

  async function handleDelete(key: string) {
    try {
      await ElMessageBox.confirm(`确定删除缓存键「${key}」吗？此操作不可恢复。`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
      await deleteCacheKey(key)
      ElMessage.success('删除成功')
      keys.value = keys.value.filter((item) => item.key !== key)
      loadInfo()
    } catch {
      /* cancel or error */
    }
  }

  function formatTTL(ttl?: number) {
    if (ttl === -1) return '永久有效'
    if (ttl == null || ttl === -2) return '已过期或不存在'
    if (ttl >= 86400) return `${Math.floor(ttl / 86400)} 天`
    if (ttl >= 3600) return `${Math.floor(ttl / 3600)} 小时`
    if (ttl >= 60) return `${Math.floor(ttl / 60)} 分钟`
    return `${ttl} 秒`
  }

  function formatValue(value: unknown) {
    if (value == null) return ''
    if (typeof value === 'string') {
      try {
        return JSON.stringify(JSON.parse(value), null, 2)
      } catch {
        return value
      }
    }
    return JSON.stringify(value, null, 2)
  }

  function handleResize() {
    memoryChart?.resize()
    qpsChart?.resize()
    hitRateChart?.resize()
    clientsChart?.resize()
  }

  function disposeCharts() {
    memoryChart?.dispose()
    qpsChart?.dispose()
    hitRateChart?.dispose()
    clientsChart?.dispose()
    memoryChart = null
    qpsChart = null
    hitRateChart = null
    clientsChart = null
  }

  function toggleAutoRefresh(val: string | number | boolean) {
    setCacheAutoRefresh(val)
  }

  onMounted(async () => {
    if (!canList.value) return
    startCacheMonitorBackground()
    setCacheChartRenderHandler(() => updateCharts(cacheChartHistory.lastMemoryStats ?? {}))
    await nextTick()
    if (cacheChartHistory.timeLabels.length) {
      await updateCharts(cacheChartHistory.lastMemoryStats ?? {})
    }
    await refreshAll()
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    setCacheChartRenderHandler(null)
    window.removeEventListener('resize', handleResize)
    disposeCharts()
  })

  return {
    canList,
    canDelete,
    info,
    keysLoading,
    keysTruncated,
    searchPattern,
    scanLimit,
    keyFilter,
    refreshing,
    autoRefresh: cacheAutoRefresh,
    liveQps,
    liveHitRateText,
    patternPresets: PATTERN_PRESETS,
    detailVisible,
    cacheDetail,
    memoryChartRef,
    qpsChartRef,
    hitRateChartRef,
    clientsChartRef,
    pagedKeys,
    pagination,
    filteredKeys,
    keysTableHeight: KEYS_TABLE_HEIGHT,
    refreshAll,
    applyPreset,
    copyKey,
    loadKeys,
    handleView,
    handleDelete,
    formatTTL,
    formatValue,
    toggleAutoRefresh,
  }
}

function calcYAxisRange(values: number[], padding = 3) {
  if (!values.length) return { min: 0, max: 10 }
  const min = Math.min(...values)
  const max = Math.max(...values)
  if (min === max) {
    return { min: Math.max(0, min - padding), max: max + padding }
  }
  return { min: Math.max(0, min - padding), max: max + padding }
}

function formatBytes(bytes: number): string {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let n = bytes
  let i = 0
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024
    i += 1
  }
  return `${n.toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}
