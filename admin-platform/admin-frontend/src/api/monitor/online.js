import { get, del, post } from '@/utils/request'

export const getOnlineUserList = () => {
  return get('/monitor/online/list')
}

/** 强退：清 RBAC Token，并尝试清网关 Sa-Token（无会话时不报错） */
export const forceLogoutOnlineUser = async (userId) => {
  await del(`/monitor/online/${userId}`)
  const kickRes = await post(`/auth/force-kick/${userId}`)
  return kickRes
}
