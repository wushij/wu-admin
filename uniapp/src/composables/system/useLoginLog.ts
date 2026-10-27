import type { LoginLogVO } from '@/types/system'

export const LOGIN_LOG_DETAIL_STORAGE_KEY = 'uniapp_login_log_detail'

export function storeLoginLogDetail(row: LoginLogVO) {
  try {
    uni.setStorageSync(LOGIN_LOG_DETAIL_STORAGE_KEY, row)
  } catch {
    /* ignore */
  }
}

export function readLoginLogDetail(expectedId?: number): LoginLogVO | null {
  try {
    const row = uni.getStorageSync(LOGIN_LOG_DETAIL_STORAGE_KEY) as LoginLogVO
    if (!row?.id) return null
    if (expectedId != null && row.id !== expectedId) return null
    return row
  } catch {
    return null
  }
}

export function loginStatusLabel(status?: number | null) {
  return status === 0 ? '成功' : '失败'
}

export function loginStatusEffect(status?: number | null): 'success' | 'danger' {
  return status === 0 ? 'success' : 'danger'
}
