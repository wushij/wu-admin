import { get, post, del } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface SecurityEventVO {
  id: number
  eventType: string
  principal: string
  ipaddr: string
  requestUri: string
  requestMethod: string
  userAgent: string
  riskDelta: number
  totalRiskScore: number
  action: string
  reason: string
  createTime: string
}

export interface SecurityEventPageQuery extends PageQuery {
  ipaddr?: string
  eventType?: string
  action?: string
}

export interface RiskSubjectVO {
  /** 主体标识，形如 user:12 / ip:1.2.3.4 */
  principal: string
  /** 主体类型：user / ip */
  principalType: 'user' | 'ip'
  /** 主体自身标识：用户 ID 或 IP */
  principalId: string
  /** 关联的真实 IP */
  ipaddr?: string | null
  /** 用户账号/用户名（主体类型为 user 时存在） */
  username?: string | null
  /** 用户昵称（主体类型为 user 时存在） */
  nickname?: string | null
  /** 滑动窗口内累计风险分 */
  riskScore: number
  /** 风险等级 0～4 */
  riskLevel: number
  /** 风险等级文案 */
  riskLevelText: string
  blocked: boolean
  remainingSeconds: number
  reason: string
  /** 最近一次触发风险的时间 */
  lastEventTime?: string | null
  /** 窗口内事件次数 */
  eventCount: number
}

export interface RiskSubjectPageQuery extends PageQuery {
  keyword?: string
  level?: number
  blocked?: boolean
}

export interface SecurityStatsVO {
  todayTotal: number
  todayBlocked: number
  /** 今日自动封禁次数（action=FORBIDDEN） */
  todayForbidden: number
  /** 今日警告次数（action=WARNING） */
  todayWarning: number
  /** 今日挑战次数（action=CHALLENGE） */
  todayChallenge: number
  todayUaBlock: number
  todayReplayBlock: number
  /** 今日蜜罐诱捕命中次数 */
  todayHoneypot: number
  /** 今日越权探测次数 */
  todayPermissionDenied: number
  activeBlockedCount: number
  /** 当前存在风险分的主体总数 */
  activeRiskCount: number
  /** 风险等级分布：等级文案 → 数量 */
  riskLevelDistribution: Record<string, number>
  /** 风险分最高的主体 */
  topRiskSubjects: RiskSubjectVO[]
}

/**
 * 分页获取安全拦截事件
 */
export function getSecurityEvents(params: SecurityEventPageQuery) {
  return get<PageResult<SecurityEventVO>>('/system/security/events', params)
}

/**
 * 获取安全拦截大盘统计数据
 */
export function getSecurityStats() {
  return get<SecurityStatsVO>('/system/security/stats')
}

/**
 * 获取当前处于封禁中的风险名单（不分页，供大盘使用）
 */
export function getRiskList() {
  return get<RiskSubjectVO[]>('/system/security/risk-list')
}

/**
 * 分页查询风险主体，包含未封禁但存在风险分的主体
 */
export function getRiskSubjects(params: RiskSubjectPageQuery) {
  return get<PageResult<RiskSubjectVO>>('/system/security/risk-subjects', params)
}

/**
 * 查询风险主体最近的安全事件
 */
export function getRiskSubjectEvents(principal: string, limit = 20) {
  return get<SecurityEventVO[]>('/system/security/risk-subject/events', { principal, limit })
}

/**
 * 查询风险主体当前分数与等级
 */
export function getRiskSubjectScore(principal: string) {
  return get<{ principal: string; score: number; level: number; levelText: string }>(
    '/system/security/risk-subject/score',
    { principal }
  )
}

/**
 * 解除风险封禁
 */
export function unblockRiskSubject(principal: string, resetRisk = true) {
  return post<boolean>('/system/security/unblock', null, {
    params: { principal, resetRisk }
  })
}

/**
 * 手动封禁主体
 */
export function blockRiskSubject(principal: string, durationSeconds = 3600, reason = '管理员手动封禁') {
  return post<boolean>('/system/security/block', null, {
    params: { principal, durationSeconds, reason }
  })
}

/**
 * 删除单条安全事件
 */
export function deleteSecurityEvent(id: number) {
  return del<boolean>(`/system/security/events/${id}`)
}

/**
 * 清空安全事件日志
 */
export function clearSecurityEvents() {
  return del<boolean>('/system/security/events/clear')
}
