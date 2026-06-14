import type { IconName } from '@/constants/iconfont'

export interface QuickEntry {
  key: string
  name: string
  desc: string
  permission: string
  icon: IconName
  theme: string
  path?: string
}

export interface QuickEntryGroup {
  title: string
  keys: string[]
}

export const mobileQuickEntries: QuickEntry[] = [
  {
    key: 'user',
    name: '用户管理',
    desc: '账号与状态',
    permission: 'system:user:list',
    icon: 'friends-o',
    theme: 'user',
    path: '/pages-sub/system/user/index',
  },
  {
    key: 'org',
    name: '组织管理',
    desc: '部门与岗位',
    permission: 'system:dept:list',
    icon: 'cluster-o',
    theme: 'dept',
    path: '/pages-sub/system/org/index',
  },
  {
    key: 'role',
    name: '角色管理',
    desc: '权限角色',
    permission: 'system:role:list',
    icon: 'shield-o',
    theme: 'role',
    path: '/pages-sub/system/role/index',
  },
  {
    key: 'menu',
    name: '菜单管理',
    desc: '目录与权限',
    permission: 'system:menu:list',
    icon: 'apps-o',
    theme: 'menu',
    path: '/pages-sub/system/menu/index',
  },
  {
    key: 'dict',
    name: '字典管理',
    desc: '业务枚举',
    permission: 'system:dict:list',
    icon: 'notes-o',
    theme: 'dict',
    path: '/pages-sub/system/dict/index',
  },
  {
    key: 'config',
    name: '系统配置',
    desc: '登录与会话',
    permission: 'system:config:list',
    icon: 'setting-o',
    theme: 'config',
    path: '/pages-sub/system/config/index',
  },
  {
    key: 'file',
    name: '文件列表',
    desc: '上传与预览',
    permission: 'sys:file:list',
    icon: 'coupon-o',
    theme: 'file-store',
    path: '/pages-sub/system/file/index',
  },
  {
    key: 'approval',
    name: '审批中心',
    desc: '流程审批',
    permission: 'system:approval:list',
    icon: 'completed',
    theme: 'approval',
    path: '/pages-sub/system/approval/index',
  },
  {
    key: 'ticket',
    name: '工单管理',
    desc: '处理与流转',
    permission: 'system:ticket:list',
    icon: 'records-o',
    theme: 'ticket',
    path: '/pages-sub/system/ticket/index',
  },
  {
    key: 'chat',
    name: '企业 IM',
    desc: '私聊群聊',
    permission: 'system:chat:list',
    icon: 'chat-o',
    theme: 'chat',
    path: '/pages-sub/msg/chat/index',
  },
  {
    key: 'notice',
    name: '通知管理',
    desc: '发布与推送',
    permission: 'system:announce:list',
    icon: 'bell',
    theme: 'notice',
    path: '/pages-sub/system/announce/index',
  },
  {
    key: 'recycle',
    name: '回收中心',
    desc: '恢复与彻底清除',
    permission: 'system:recycle:list',
    icon: 'notes-o',
    theme: 'log',
    path: '/pages-sub/system/recycle/index',
  },
  {
    key: 'online',
    name: '在线用户',
    desc: '会话强退',
    permission: 'monitor:online:list',
    icon: 'manager-o',
    theme: 'monitor',
    path: '/pages-sub/monitor/online',
  },
  {
    key: 'server',
    name: '服务器监控',
    desc: 'CPU / 内存 / JVM / 磁盘',
    permission: 'monitor:server:list',
    icon: 'desktop-o',
    theme: 'server',
    path: '/pages-sub/monitor/server',
  },
  {
    key: 'cache',
    name: '缓存监控',
    desc: 'Redis 状态',
    permission: 'monitor:cache:list',
    icon: 'balance-list-o',
    theme: 'cache',
    path: '/pages-sub/monitor/cache',
  },
  {
    key: 'job',
    name: '定时任务',
    desc: '调度日志',
    permission: 'monitor:job:list',
    icon: 'clock-o',
    theme: 'job',
    path: '/pages-sub/monitor/job',
  },
  {
    key: 'api-access',
    name: 'API 统计',
    desc: '访问分析',
    permission: 'monitor:apiAccess:list',
    icon: 'chart-trending-o',
    theme: 'api',
    path: '/pages-sub/monitor/api-access',
  },
  {
    key: 'oper-log',
    name: '操作日志',
    desc: '行为审计',
    permission: 'system:operLog:list',
    icon: 'edit',
    theme: 'log',
    path: '/pages-sub/log/oper-log',
  },
  {
    key: 'login-log',
    name: '登录日志',
    desc: '登录审计',
    permission: 'system:loginLog:list',
    icon: 'contact-o',
    theme: 'log',
    path: '/pages-sub/log/login-log',
  },
]

export const quickEntryGroups: QuickEntryGroup[] = [
  { title: '系统管理', keys: ['user', 'role', 'menu', 'org', 'dict', 'config', 'recycle'] },
  { title: '文件管理', keys: ['file'] },
  { title: '流程中心', keys: ['approval', 'ticket'] },
  { title: '消息协作', keys: ['chat', 'notice'] },
  { title: '监控运维', keys: ['online', 'server', 'cache', 'job', 'api-access'] },
  { title: '日志审计', keys: ['oper-log', 'login-log'] },
]

export const entryMap = Object.fromEntries(mobileQuickEntries.map((e) => [e.key, e])) as Record<
  string,
  QuickEntry
>
