import { get } from '@/utils/request'

/** 工作台统计（与后端 DashboardStatsVO 对齐） */
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
  userPendingCount?: number
  userDisabledCount?: number
  todayLoginSuccess?: number
  todayLoginFail?: number
  fileCount?: number
  fileMaxSizeMb?: number
  fileAllowedExtensions?: string
  tokenExpireHours?: number
  loginCaptchaEnabled?: boolean
  loginCaptchaType?: string
  loginRememberMe?: boolean
  loginMaxRetryCount?: number
  loginLockTimeMinutes?: number
  registerEnabled?: boolean
  registerNeedAudit?: boolean
  ticketOpenCount?: number
  ticketOverdueCount?: number
  approvalPendingCount?: number
  jobTotalCount?: number
  jobRunningCount?: number
  jobPausedCount?: number
}

export interface RecentLogin {
  userId?: number
  username?: string
  nickname?: string
  avatar?: string
  ipaddr?: string
  loginLocation?: string
  loginTime?: string
  browser?: string
  os?: string
  status?: number
}

export function getDashboardStats() {
  return get<DashboardStats>('/dashboard/stats')
}

export function recordVisit() {
  return get<unknown>('/dashboard/visit')
}

export function getRecentLogins() {
  return get<RecentLogin[]>('/dashboard/recent-logins')
}
