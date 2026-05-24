import type { OperLogVO } from '@/types/system'

export const OPER_LOG_DETAIL_STORAGE_KEY = 'uniapp_oper_log_detail'

export const OPER_LOG_BUSINESS_TYPE_MAP: Record<number, string> = {
  0: '其他',
  1: '新增',
  2: '修改',
  3: '删除',
  4: '查询',
  5: '导出',
  6: '导入',
}

export function businessTypeLabel(type?: number | null) {
  if (type == null) return '其他'
  return OPER_LOG_BUSINESS_TYPE_MAP[type] ?? '其他'
}

export function businessTypeTone(type?: number | null): 'primary' | 'success' | 'warning' | 'danger' | 'default' {
  switch (type) {
    case 1:
      return 'success'
    case 2:
      return 'warning'
    case 3:
      return 'danger'
    case 4:
      return 'default'
    case 5:
    case 6:
      return 'primary'
    default:
      return 'default'
  }
}

export function formatOperLogJson(raw?: string | null) {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

export function storeOperLogDetail(row: OperLogVO) {
  try {
    uni.setStorageSync(OPER_LOG_DETAIL_STORAGE_KEY, row)
  } catch {
    /* ignore */
  }
}

export function readOperLogDetail(expectedId?: number): OperLogVO | null {
  try {
    const row = uni.getStorageSync(OPER_LOG_DETAIL_STORAGE_KEY) as OperLogVO
    if (!row?.id) return null
    if (expectedId != null && row.id !== expectedId) return null
    return row
  } catch {
    return null
  }
}

export function requestMethodClass(method?: string) {
  if (!method) return ''
  return `oper-log__method--${method.toLowerCase()}`
}
