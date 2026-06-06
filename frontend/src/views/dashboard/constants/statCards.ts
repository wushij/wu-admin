import type { Component } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import type { DashboardStats } from '@/api/dashboard'
import {
  User,
  UserFilled,
  Menu,
  OfficeBuilding,
  CircleCheck,
  FolderOpened,
  Briefcase,
} from '@element-plus/icons-vue'

export interface DashboardTrends {
  user: number
  role: number
  menu: number
  dept: number
}

export interface CoreStatCardConfig {
  key: string
  title: string
  valueKey: keyof DashboardStats
  trendKey: keyof DashboardTrends
  icon: Component
  iconTheme: string
  flatFooterWhenZero?: boolean
}

export interface OpsStatCardConfig {
  key: string
  title: string
  valueKey: keyof DashboardStats
  icon: Component
  iconTheme: string
  permission?: string
  to: RouteLocationRaw
  footer: (stats: DashboardStats) => string
}

export interface BizAlertConfig {
  key: string
  label: string
  valueKey: keyof DashboardStats
  permission: string
  to: RouteLocationRaw
  valueClass?: 'warn' | 'danger'
  visible: (stats: DashboardStats) => boolean
}

export const coreStatCards: CoreStatCardConfig[] = [
  { key: 'user', title: '用户总数', valueKey: 'userCount', trendKey: 'user', icon: User, iconTheme: 'user' },
  { key: 'role', title: '角色总数', valueKey: 'roleCount', trendKey: 'role', icon: UserFilled, iconTheme: 'role' },
  { key: 'menu', title: '菜单总数', valueKey: 'menuCount', trendKey: 'menu', icon: Menu, iconTheme: 'menu', flatFooterWhenZero: true },
  { key: 'dept', title: '部门总数', valueKey: 'deptCount', trendKey: 'dept', icon: OfficeBuilding, iconTheme: 'dept' },
]

export const opsStatCards: OpsStatCardConfig[] = [
  {
    key: 'pending-user',
    title: '待审核用户',
    valueKey: 'userPendingCount',
    icon: User,
    iconTheme: 'pending-user',
    permission: 'system:approval:list',
    to: { path: '/system/approval', query: { formType: 'REGISTER', status: 'SUBMITTED' } },
    footer: () => '注册审核',
  },
  {
    key: 'login-success',
    title: '今日登录成功',
    valueKey: 'todayLoginSuccess',
    icon: CircleCheck,
    iconTheme: 'login-success',
    to: '/system/login-log',
    footer: (stats) => `失败 ${stats.todayLoginFail ?? 0} 次`,
  },
  {
    key: 'file-store',
    title: '文件存储',
    valueKey: 'fileCount',
    icon: FolderOpened,
    iconTheme: 'file-store',
    to: '/system/file',
    footer: (stats) => `单文件上限 ${stats.fileMaxSizeMb ?? 50}MB`,
  },
  {
    key: 'post',
    title: '岗位数',
    valueKey: 'postCount',
    icon: Briefcase,
    iconTheme: 'post',
    to: '/system/org',
    footer: (stats) => `组织管理 · 部门 ${stats.deptCount ?? 0}`,
  },
]

export const bizAlertCards: BizAlertConfig[] = [
  {
    key: 'ticket-open',
    label: '待处理工单',
    valueKey: 'ticketOpenCount',
    permission: 'system:ticket:list',
    to: '/system/ticket',
    valueClass: 'warn',
    visible: (stats) => (stats.ticketOpenCount ?? 0) > 0 || (stats.ticketOverdueCount ?? 0) > 0,
  },
  {
    key: 'ticket-overdue',
    label: '超时工单',
    valueKey: 'ticketOverdueCount',
    permission: 'system:ticket:list',
    to: '/system/ticket',
    valueClass: 'danger',
    visible: (stats) => (stats.ticketOverdueCount ?? 0) > 0,
  },
  {
    key: 'approval-pending',
    label: '待审批',
    valueKey: 'approvalPendingCount',
    permission: 'system:approval:list',
    to: '/system/approval',
    visible: (stats) => (stats.approvalPendingCount ?? 0) > 0,
  },
]

export function formatTrendFooter(trend: number, flatWhenZero = false): string {
  if (flatWhenZero && trend === 0) return '与昨日持平'
  return `较昨日 ${trend >= 0 ? '+' : ''}${trend}%`
}

export function statNumber(stats: DashboardStats, key: keyof DashboardStats): number {
  const value = stats[key]
  return typeof value === 'number' ? value : 0
}
