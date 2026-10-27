const KEY = 'uniapp_user_select_multi_ids'

export function setUserSelectMultiIds(ids: number[]) {
  try {
    uni.setStorageSync(KEY, JSON.stringify((ids || []).filter((id) => Number(id) > 0)))
  } catch {
    /* ignore */
  }
}

export function getUserSelectMultiIds(): number[] {
  try {
    const raw = uni.getStorageSync(KEY)
    if (!raw) return []
    const arr = JSON.parse(String(raw))
    if (!Array.isArray(arr)) return []
    return arr.map(Number).filter((id) => Number.isFinite(id) && id > 0)
  } catch {
    return []
  }
}

export function clearUserSelectMultiIds() {
  try {
    uni.removeStorageSync(KEY)
  } catch {
    /* ignore */
  }
}
