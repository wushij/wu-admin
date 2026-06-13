import { computed, ref } from 'vue'
import { getCacheInfo, getCacheStats } from '@/api/monitor/cache'
import type { CacheInfo, CacheStats } from '@/types/system'

export const CACHE_MAX_CHART_POINTS = 20

export const CACHE_PATTERN_PRESETS = [
  { label: '全部', pattern: '*' },
  { label: '系统缓存', pattern: 'cache:sys:*' },
  { label: '仪表盘', pattern: 'dashboard:*' },
  { label: 'API', pattern: 'api:*' },
] as const

export function calcChartYMax(values: number[], floor = 10) {
  if (!values.length) return floor
  const max = Math.max(...values)
  return Math.max(floor, Math.ceil(max * 1.15))
}

export function formatCacheTtl(ttl?: number) {
  if (ttl === -1) return '永久有效'
  if (ttl == null || ttl === -2) return '已过期或不存在'
  if (ttl >= 86400) return `${Math.floor(ttl / 86400)} 天`
  if (ttl >= 3600) return `${Math.floor(ttl / 3600)} 小时`
  if (ttl >= 60) return `${Math.floor(ttl / 60)} 分钟`
  if (ttl === 0) return '即将过期'
  return `${ttl} 秒`
}

export function formatCacheValue(value: unknown) {
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

export function useCacheMonitor() {
  const info = ref<CacheInfo | null>(null)
  const stats = ref<CacheStats | null>(null)
  const timeLabels = ref<string[]>([])
  const qpsHistory = ref<number[]>([])
  const hitRateHistory = ref<(number | null)[]>([])
  const clientsHistory = ref<number[]>([])

  const liveQps = computed(() => stats.value?.ops ?? info.value?.instantaneousOpsPerSec ?? null)

  const liveHitRateText = computed(() => {
    const rate = stats.value?.cumulativeHitRate
    return rate != null ? `${Math.round(rate * 100)}%` : '—'
  })

  /** 仅 Redis 配置了 maxmemory 时才有意义的比例，与 PC 端饼图逻辑一致 */
  const memoryPercent = computed(() => {
    const s = stats.value
    if (!s?.maxMemoryConfigured || !s.maxMemory || s.maxMemory <= 0) return null
    const used = s.usedMemory ?? 0
    return Math.min(100, (used / s.maxMemory) * 100)
  })

  /** 未配置 maxmemory 时展示绝对占用量，避免相对整机内存算出 0.0% */
  const memoryDisplay = computed(() => {
    if (memoryPercent.value != null) return undefined
    return stats.value?.usedMemoryHuman || '—'
  })

  const memoryHint = computed(() => {
    const s = stats.value
    if (!s) return '—'
    if (s.maxMemoryConfigured && s.maxMemory) {
      return `${s.usedMemoryHuman || '—'} / ${formatBytes(s.maxMemory)}`
    }
    return '未配置 maxmemory，仅展示当前占用'
  })

  function pushStatsPoint(data: CacheStats) {
    const label = new Date().toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
    timeLabels.value = [...timeLabels.value, label].slice(-CACHE_MAX_CHART_POINTS)
    qpsHistory.value = [...qpsHistory.value, data.ops ?? 0].slice(-CACHE_MAX_CHART_POINTS)
    hitRateHistory.value = [
      ...hitRateHistory.value,
      data.hitRate != null ? Math.round(data.hitRate * 100) : null,
    ].slice(-CACHE_MAX_CHART_POINTS)
    clientsHistory.value = [...clientsHistory.value, data.connectedClients ?? 0].slice(
      -CACHE_MAX_CHART_POINTS,
    )
  }

  async function fetchCacheData() {
    const [infoRes, statsRes] = await Promise.all([getCacheInfo(), getCacheStats()])
    info.value = infoRes.data || null
    stats.value = statsRes.data || null
    if (stats.value) pushStatsPoint(stats.value)
  }

  return {
    info,
    stats,
    timeLabels,
    qpsHistory,
    hitRateHistory,
    clientsHistory,
    liveQps,
    liveHitRateText,
    memoryPercent,
    memoryDisplay,
    memoryHint,
    fetchCacheData,
  }
}

function formatBytes(bytes: number) {
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
