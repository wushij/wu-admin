import { del, get } from '@/utils/request'

export interface CacheStats {
  usedMemory?: number
  maxMemory?: number
  maxMemoryConfigured?: boolean
  systemMemory?: number
  usedMemoryHuman?: string
  ops?: number
  /** 本采样周期命中率 0~1，无读写为 null */
  hitRate?: number | null
  /** 累计命中率 0~1 */
  cumulativeHitRate?: number | null
  keyspaceHits?: number
  keyspaceMisses?: number
  connectedClients?: number
}

export interface CacheInfo {
  redisVersion?: string
  redisMode?: string
  os?: string
  tcpPort?: number
  uptimeInDays?: number
  connectedClients?: number
  dbSize?: number
  usedMemoryHuman?: string
  usedMemoryPeakHuman?: string
  totalCommandsProcessed?: number
  instantaneousOpsPerSec?: number
}

export interface CacheKeyItem {
  key: string
  type?: string
  ttl?: number
}

export interface CacheKeysResult {
  items: CacheKeyItem[]
  count: number
  limit: number
  truncated: boolean
}

export interface CacheValueDetail {
  key: string
  type: string
  ttl: number
  value: unknown
}

export function getCacheStats() {
  return get<CacheStats>('/monitor/cache/stats')
}

export function getCacheInfo() {
  return get<CacheInfo>('/monitor/cache/info')
}

export function scanCacheKeys(pattern = '*', limit = 200) {
  return get<CacheKeysResult>('/monitor/cache/keys', { pattern, limit })
}

export function getCacheValue(key: string) {
  return get<CacheValueDetail>('/monitor/cache/value', { key })
}

export function deleteCacheKey(key: string) {
  return del<boolean>('/monitor/cache', { params: { key } })
}
