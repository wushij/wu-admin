import type { ExportFormat, ExportScope } from '@/utils/exportDownload'

export type ExportModule =
  | 'user'
  | 'login-log'
  | 'oper-log'
  | 'ticket'
  | 'approval'
  | 'api-access'
  | 'online-user'

const MODULE_PATH: Record<ExportModule, string> = {
  user: '/system/export/user',
  'login-log': '/system/export/login-log',
  'oper-log': '/system/export/oper-log',
  ticket: '/system/export/ticket',
  approval: '/system/export/approval',
  'api-access': '/monitor/api-access/export',
  'online-user': '/monitor/online/export',
}

const MODULE_LABEL: Record<ExportModule, string> = {
  user: '用户列表',
  'login-log': '登录日志',
  'oper-log': '操作日志',
  ticket: '工单列表',
  approval: '审批单列表',
  'api-access': 'API访问日志',
  'online-user': '在线用户',
}

export function getExportPath(module: ExportModule): string {
  return MODULE_PATH[module]
}

export function getExportLabel(module: ExportModule): string {
  return MODULE_LABEL[module]
}

export type ExportQueryParams = Record<string, string | number | boolean | null | undefined>

export function buildExportFilename(module: ExportModule, format: ExportFormat, scope: ExportScope): string {
  const label = MODULE_LABEL[module]
  const scopeLabel = scope === 'page' ? '当前页' : '筛选结果'
  const ext = format === 'csv' ? 'csv' : 'xlsx'
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const ts = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
  return `${label}_${scopeLabel}_${ts}.${ext}`
}
