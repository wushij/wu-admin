const ACTIVE_TOKEN_KEY = 'uniapp_leader_pick_token'

export interface LeaderPickResult {
  id: number | null
  label: string
}

function resultKey(token: string) {
  return `uniapp_leader_pick_result_${token}`
}

export function beginLeaderPick(): string {
  const token = String(Date.now())
  try {
    uni.setStorageSync(ACTIVE_TOKEN_KEY, token)
  } catch {
    /* ignore */
  }
  return token
}

export function commitLeaderPick(token: string, result: LeaderPickResult) {
  if (!token) return
  try {
    uni.setStorageSync(resultKey(token), JSON.stringify(result))
    uni.setStorageSync(ACTIVE_TOKEN_KEY, token)
  } catch {
    /* ignore */
  }
}

export function consumeLeaderPick(token?: string): LeaderPickResult | null {
  const active = token || String(uni.getStorageSync(ACTIVE_TOKEN_KEY) || '')
  if (!active) return null
  try {
    const raw = uni.getStorageSync(resultKey(active))
    uni.removeStorageSync(resultKey(active))
    uni.removeStorageSync(ACTIVE_TOKEN_KEY)
    if (!raw) return null
    const data = JSON.parse(String(raw)) as LeaderPickResult
    if (!data || typeof data.label !== 'string') return null
    return {
      id: data.id == null || data.id <= 0 ? null : Number(data.id),
      label: data.label,
    }
  } catch {
    try {
      uni.removeStorageSync(resultKey(active))
      uni.removeStorageSync(ACTIVE_TOKEN_KEY)
    } catch {
      /* ignore */
    }
    return null
  }
}

export function clearLeaderPick() {
  try {
    const active = String(uni.getStorageSync(ACTIVE_TOKEN_KEY) || '')
    if (active) uni.removeStorageSync(resultKey(active))
    uni.removeStorageSync(ACTIVE_TOKEN_KEY)
  } catch {
    /* ignore */
  }
}
