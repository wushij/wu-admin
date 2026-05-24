import { get, del } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { LoginLogPageQuery, LoginLogVO } from '@/types/system'

export function getLoginLogList(params: LoginLogPageQuery) {
  return get<PageResult<LoginLogVO>>('/system/login-log/list', params)
}

export function deleteLoginLog(id: number) {
  return del<boolean>(`/system/login-log/${id}`)
}

export function clearLoginLog() {
  return del<boolean>('/system/login-log/clear')
}
