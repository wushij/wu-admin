import { get, del } from '@/utils/request'

export const getOnlineUserList = () => {
  return get('/monitor/online/list')
}

/** 强退：清除 Sa-Token 会话与在线记录 */
export const forceLogoutOnlineUser = (userId) => {
  return del(`/monitor/online/${userId}`)
}
