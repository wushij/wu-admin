import type { IconName } from '@/constants/iconfont'
import type { DashboardStats } from '@/api/dashboard'

export interface StatCardConfig {
  key: string
  title: string
  valueKey: keyof DashboardStats
  icon: IconName
  theme: string
  permission?: string
  path?: string
  footer?: (stats: DashboardStats) => string
}

export const mobileStatCards: StatCardConfig[] = [
  {
    key: 'user',
    title: '用户总数',
    valueKey: 'userCount',
    icon: 'friends-o',
    theme: 'user',
    path: '/pages-sub/system/user/index',
    footer: (s) => {
      const trend = s.userTrend ?? 0
      return trend === 0 ? '与昨日持平' : `较昨日 ${trend >= 0 ? '+' : ''}${trend}%`
    },
  },
  {
    key: 'online',
    title: '在线用户',
    valueKey: 'onlineCount',
    icon: 'manager-o',
    theme: 'online',
    permission: 'monitor:online:list',
    path: '/pages-sub/monitor/online',
    footer: () => '实时监控',
  },
  {
    key: 'approval',
    title: '待我审批',
    valueKey: 'approvalPendingCount',
    icon: 'completed',
    theme: 'approval',
    permission: 'system:approval:list',
    path: '/pages-sub/system/approval/index',
    footer: () => '待我处理的审批',
  },
  {
    key: 'ticket',
    title: '待我处理',
    valueKey: 'ticketOpenCount',
    icon: 'records-o',
    theme: 'ticket',
    permission: 'system:ticket:list',
    path: '/pages-sub/system/ticket/index',
    footer: (s) => {
      const overdue = s.ticketOverdueCount ?? 0
      return overdue > 0 ? `超时 ${overdue} 个` : '待我处理的工单'
    },
  },
  {
    key: 'file',
    title: '文件数量',
    valueKey: 'fileCount',
    icon: 'coupon-o',
    theme: 'file-store',
    permission: 'sys:file:list',
    path: '/pages-sub/system/file/index',
    footer: () => '文件管理',
  },
  {
    key: 'job',
    title: '定时任务',
    valueKey: 'jobTotalCount',
    icon: 'clock-o',
    theme: 'job',
    permission: 'monitor:job:list',
    path: '/pages-sub/monitor/job',
    footer: (s) => {
      const running = s.jobRunningCount ?? 0
      const paused = s.jobPausedCount ?? 0
      return `运行 ${running} · 暂停 ${paused}`
    },
  },
  {
    key: 'chat',
    title: '企业 IM',
    valueKey: 'chatUnreadCount',
    icon: 'chat-o',
    theme: 'chat',
    permission: 'system:chat:list',
    path: '/pages-sub/msg/chat/index',
    footer: (s) => {
      const unread = s.chatUnreadCount ?? 0
      return unread > 0 ? `${unread} 条未读` : '私聊与群聊'
    },
  },
  {
    key: 'config',
    title: '系统配置',
    valueKey: 'configGroupCount',
    icon: 'setting-o',
    theme: 'config',
    permission: 'system:config:list',
    path: '/pages-sub/system/config/index',
    footer: (s) => {
      const hours = s.tokenExpireHours ?? 0
      return hours > 0 ? `会话 ${hours}h` : '登录注册与会话'
    },
  },
]

export function statValue(stats: DashboardStats, key: keyof DashboardStats): number {
  const value = stats[key]
  return typeof value === 'number' ? value : 0
}
