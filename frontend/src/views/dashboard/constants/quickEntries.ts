import type { Component } from 'vue'
import {
  User,
  Menu,
  Collection,
  Tools,
  Checked,
  ChatDotRound,
  Notification,
  Timer,
  EditPen,
  Cpu,
  Coin,
} from '@element-plus/icons-vue'

export interface QuickEntry {
  key: string
  name: string
  desc: string
  path: string
  permission: string
  icon: Component
  theme: string
  query?: Record<string, string>
}

/** 工作台固定 12 个核心快捷入口（3×4）；服务/缓存监控同排相邻 */
export const quickEntries: QuickEntry[] = [
  { key: 'user', name: '用户管理', desc: '账号与状态维护', path: '/system/user', permission: 'system:user:list', icon: User, theme: 'user' },
  { key: 'menu', name: '菜单管理', desc: '目录菜单按钮', path: '/system/menu', permission: 'system:menu:list', icon: Menu, theme: 'menu' },
  { key: 'dict', name: '字典管理', desc: '业务枚举维护', path: '/system/dict', permission: 'system:dict:list', icon: Collection, theme: 'dict' },
  { key: 'config', name: '系统配置', desc: '登录注册与会话', path: '/system/config', permission: 'system:config:list', icon: Tools, theme: 'config' },
  { key: 'approval', name: '审批单中心', desc: '流程单审批归档', path: '/system/approval', permission: 'system:approval:list', icon: Checked, theme: 'approval' },
  { key: 'chat', name: '企业IM', desc: '私聊与群聊消息', path: '/message/chat', permission: 'system:chat:list', icon: ChatDotRound, theme: 'chat' },
  { key: 'notice', name: '系统通知', desc: '公告与消息推送', path: '/message/notice', permission: 'system:announce:list', icon: Notification, theme: 'notice' },
  { key: 'job', name: '定时任务', desc: '调度与日志清理', path: '/monitor/job', permission: 'monitor:job:list', icon: Timer, theme: 'job' },
  { key: 'online', name: '在线用户', desc: '会话与强退', path: '/monitor/online', permission: 'monitor:online:list', icon: User, theme: 'monitor' },
  { key: 'server', name: '服务器监控', desc: 'CPU / 内存 / 磁盘', path: '/monitor/server', permission: 'monitor:server:list', icon: Cpu, theme: 'server' },
  { key: 'cache', name: '缓存监控', desc: 'Redis 状态与键值', path: '/monitor/cache', permission: 'monitor:cache:list', icon: Coin, theme: 'cache' },
  { key: 'oper-log', name: '操作日志', desc: '行为审计追溯', path: '/system/oper-log', permission: 'system:operLog:list', icon: EditPen, theme: 'log' },
]
