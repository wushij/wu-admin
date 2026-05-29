import { get } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface ApiAccessLogRow {
  id?: number
  userId?: number
  username?: string
  apiPath?: string
  method?: string
  success?: number
  createTime?: string
  [key: string]: unknown
}

export interface ApiAccessStatistics {
  totalCount?: number
  successCount?: number
  failCount?: number
  dailyStats?: Record<string, { total?: number; success?: number; fail?: number }>
  topPaths?: { apiPath?: string; count?: number }[]
  topUsers?: { userId: number; username?: string; count?: number }[]
  methodCount?: Record<string, number>
}

/** API 访问统计 */
export const getApiAccessPage = (params: PageQuery) => {
  return get<PageResult<ApiAccessLogRow>>('/monitor/api-access/page', params)
}

export const getApiAccessStatistics = (params: Record<string, unknown>) => {
  return get<ApiAccessStatistics>('/monitor/api-access/statistics', params)
}
