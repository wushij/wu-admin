import { getOnlineUserList } from '@/api/monitor/online'
import type { OnlineUser } from '@/types/system'

const STORAGE_KEY = 'uniapp_online_user_detail'

export function setOnlineUserDetail(user: Record<string, unknown>) {
  uni.setStorageSync(STORAGE_KEY, JSON.stringify(user))
}

export function getOnlineUserDetail(userId: number): Record<string, unknown> | null {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    if (!raw) return null
    const user = JSON.parse(String(raw)) as Record<string, unknown>
    if (Number(user.userId) !== userId) return null
    return user
  } catch {
    return null
  }
}

export function clearOnlineUserDetail() {
  uni.removeStorageSync(STORAGE_KEY)
}

export async function resolveOnlineUserDetail(userId: number): Promise<OnlineUser | null> {
  const cached = getOnlineUserDetail(userId)
  if (cached) return cached as unknown as OnlineUser

  const res = await getOnlineUserList()
  const hit = (res.data || []).find((u) => u.userId === userId)
  if (hit) {
    setOnlineUserDetail(hit as unknown as Record<string, unknown>)
    return hit
  }
  return null
}
