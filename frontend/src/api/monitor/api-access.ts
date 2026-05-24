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
  costTime?: number
  userAgent?: string
}

export interface ApiAccessPageQuery extends PageQuery {
  userId?: number | null
  apiPath?: string
  method?: string | null
  success?: number | null
  startTime?: string
  endTime?: string
}

export interface ApiAccessStatisticsQuery {
  startDate?: string
  endDate?: string
  startTime?: string
  endTime?: string
}

export interface ApiAccessStatistics {
  totalCount?: number
  successCount?: number
  failCount?: number
  dailyStats?: Record<string, ApiAccessDailyStat>
  topPaths?: ApiAccessTopPath[]
  topUsers?: ApiAccessTopUser[]
  methodCount?: Record<string, number>
}

export interface ApiAccessDailyStat {
  total?: number
  success?: number
  fail?: number
}

export interface ApiAccessTopPath {
  apiPath?: string
  count?: number
}

export interface ApiAccessTopUser {
  userId: number
  username?: string
  count?: number
}

/** 统计面板展示用（字段均有默认值） */
export interface ApiAccessStatsView {
  totalCount: number
  successCount: number
  failCount: number
  dailyStats: Record<string, ApiAccessDailyStat>
  topPaths: ApiAccessTopPath[]
  topUsers: ApiAccessTopUser[]
  methodCount: Record<string, number>
}

export function createEmptyApiAccessStats(): ApiAccessStatsView {
  return {
    totalCount: 0,
    successCount: 0,
    failCount: 0,
    dailyStats: {},
    topPaths: [],
    topUsers: [],
    methodCount: {},
  }
}

export function toApiAccessStatsView(data?: ApiAccessStatistics | null): ApiAccessStatsView {
  return {
    totalCount: data?.totalCount ?? 0,
    successCount: data?.successCount ?? 0,
    failCount: data?.failCount ?? 0,
    dailyStats: data?.dailyStats ?? {},
    topPaths: data?.topPaths ?? [],
    topUsers: data?.topUsers ?? [],
    methodCount: data?.methodCount ?? {},
  }
}

/** API 访问统计 */
export const getApiAccessPage = (params: ApiAccessPageQuery) => {
  return get<PageResult<ApiAccessLogRow>>('/monitor/api-access/page', params)
}

export const getApiAccessStatistics = (params: ApiAccessStatisticsQuery) => {
  return get<ApiAccessStatistics>('/monitor/api-access/statistics', params)
}
