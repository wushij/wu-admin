import { get, del } from '@/core/http'
import type { OnlineUser } from '@/types/system'

export function getOnlineUserList() {
  return get<OnlineUser[]>('/monitor/online/list')
}

export function forceLogoutOnlineUser(userId: number) {
  return del(`/monitor/online/${userId}`)
}
