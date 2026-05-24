import { get, del } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface LoginLogVO {
  id: number
  username?: string
  ipaddr?: string
  loginLocation?: string
  status?: number
  msg?: string
  loginTime?: string
  browser?: string
  os?: string
}

export interface LoginLogPageQuery extends PageQuery {
  username?: string
  ipaddr?: string
  status?: number | null
}

export function getLoginLogList(params: LoginLogPageQuery) {
  return get<PageResult<LoginLogVO>>('/system/login-log/list', params)
}

export function deleteLoginLog(id: number) {
  return del(`/system/login-log/${id}`)
}

export function clearLoginLog() {
  return del('/system/login-log/clear')
}
