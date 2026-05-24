import { get, del } from '@/utils/request'

export interface OnlineUser {
  userId: number
  username?: string
  nickname?: string
  ipaddr?: string
  loginTime?: string
  browser?: string
  os?: string
}

export const getOnlineUserList = () => {
  return get<OnlineUser[]>('/monitor/online/list')
}

/** 强退：清除 Sa-Token 会话与在线记录 */
export const forceLogoutOnlineUser = (userId: number) => {
  return del(`/monitor/online/${userId}`)
}
