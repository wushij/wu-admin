import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

export interface DashboardStats {
  userCount?: number
  roleCount?: number
  deptCount?: number
  menuCount?: number
  postCount?: number
  onlineCount?: number
  visitCount?: number
  todayVisits?: number
  yesterdayVisits?: number
  platformName?: string
  platformSubtitle?: string
  userTrend?: number
  roleTrend?: number
  deptTrend?: number
  menuTrend?: number
  [key: string]: unknown
}

export interface RecentLogin {
  username?: string
  nickname?: string
  ipaddr?: string
  loginTime?: string
  [key: string]: unknown
}

export function getDashboardStats() {
  return request.get('/dashboard/stats') as Promise<ApiResult<DashboardStats>>
}

export function recordVisit() {
  return request.get('/dashboard/visit') as Promise<ApiResult<unknown>>
}

export function getRecentLogins() {
  return request.get('/dashboard/recent-logins') as Promise<ApiResult<RecentLogin[]>>
}
