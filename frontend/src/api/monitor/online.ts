import { get, del } from '@/utils/request'

export interface OnlineUser {
  userId: number
  loginName?: string
  deptName?: string
  username?: string
  nickname?: string
  ipaddr?: string
  loginLocation?: string
  loginTime?: string
  lastAccessTime?: string
  browser?: string
  os?: string
  status?: number
}

export const getOnlineUserList = () => {
  return get<OnlineUser[]>('/monitor/online/list')
}

/** 强退：清除 Sa-Token 会话与在线记录 */
export const forceLogoutOnlineUser = (userId: number) => {
  return del(`/monitor/online/${userId}`)
}
