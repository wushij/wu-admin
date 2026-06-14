import type { Component } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import type { DashboardStats } from '@/api/dashboard'
import {
  User,
  UserFilled,
  OfficeBuilding,
  FolderOpened,
  Checked,
  Tickets,
  Briefcase,
  Menu,
  Timer,
} from '@element-plus/icons-vue'

export interface DashboardTrends {
  user: number
  role: number
  menu: number
  dept: number
}

/** 工作台顶部 10 项核心指标（两行 × 五列） */
export interface TopStatCardConfig {
  key: string
  title: string
  valueKey: keyof DashboardStats
  icon: Component
  iconTheme: string
  trendKey?: keyof DashboardTrends
  flatFooterWhenZero?: boolean
  permission?: string
  to?: RouteLocationRaw
  footer?: (stats: DashboardStats, trends: DashboardTrends) => string
}

export const topStatCards: TopStatCardConfig[] = [
  {
    key: 'user',
    title: '用户总数',
    valueKey: 'userCount',
    trendKey: 'user',
    icon: User,
    iconTheme: 'user',
    to: '/system/user',
    footer: (_s, t) => formatTrendFooter(t.user),
  },
  {
    key: 'online',
    title: '在线用户',
    valueKey: 'onlineCount',
    icon: UserFilled,
    iconTheme: 'online',
    to: '/monitor/online',
    permission: 'monitor:online:list',
    footer: () => '实时监控',
  },
  {
    key: 'menu',
    title: '菜单数量',
    valueKey: 'menuCount',
    icon: Menu,
    iconTheme: 'menu',
    permission: 'system:menu:list',
    to: '/system/menu',
    footer: () => '目录 / 菜单 / 按钮',
  },
  {
    key: 'job',
    title: '定时任务',
    valueKey: 'jobTotalCount',
    icon: Timer,
    iconTheme: 'job',
    permission: 'monitor:job:list',
    to: '/monitor/job',
    footer: (s) => {
      const running = s.jobRunningCount ?? 0
      const paused = s.jobPausedCount ?? 0
      return `运行中 ${running} · 暂停 ${paused}`
    },
  },
  {
    key: 'file-store',
    title: '文件存储',
    valueKey: 'fileCount',
    icon: FolderOpened,
    iconTheme: 'file-store',
    to: '/system/file',
    permission: 'sys:file:list',
    footer: (s) => `上限 ${s.fileMaxSizeMb ?? 50}MB`,
  },
  {
    key: 'approval-pending',
    title: '待我审批',
    valueKey: 'approvalPendingCount',
    icon: Checked,
    iconTheme: 'approval',
    permission: 'system:approval:list',
    to: '/system/approval',
    footer: () => '审批单中心',
  },
  {
    key: 'ticket-open',
    title: '待处理工单',
    valueKey: 'ticketOpenCount',
    icon: Tickets,
    iconTheme: 'ticket',
    permission: 'system:ticket:list',
    to: '/system/ticket',
    footer: (s) => {
      const overdue = s.ticketOverdueCount ?? 0
      return overdue > 0 ? `超时 ${overdue} 个` : '流程中心'
    },
  },
  {
    key: 'role',
    title: '角色总数',
    valueKey: 'roleCount',
    trendKey: 'role',
    icon: UserFilled,
    iconTheme: 'role',
    to: '/system/role',
    footer: (_s, t) => formatTrendFooter(t.role),
  },
  {
    key: 'dept',
    title: '部门总数',
    valueKey: 'deptCount',
    trendKey: 'dept',
    icon: OfficeBuilding,
    iconTheme: 'dept',
    to: '/system/org',
    footer: (_s, t) => formatTrendFooter(t.dept),
  },
  {
    key: 'post',
    title: '岗位总数',
    valueKey: 'postCount',
    icon: Briefcase,
    iconTheme: 'post',
    to: '/system/org',
    footer: (s) => `部门 ${s.deptCount ?? 0} 个`,
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
