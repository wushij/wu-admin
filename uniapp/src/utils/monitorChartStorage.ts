export function loadMonitorStorage<T>(key: string, fallback: T): T {
  try {
    const raw = uni.getStorageSync(key)
    if (!raw) return fallback
    if (typeof raw === 'string') {
      return { ...fallback, ...JSON.parse(raw) as T }
    }
    return { ...fallback, ...raw as T }
  } catch {
    return fallback
  }
}

export function saveMonitorStorage(key: string, value: unknown) {
  try {
    uni.setStorageSync(key, JSON.stringify(value))
  } catch {
    /* ignore */
  }
}

export function removeMonitorStorage(key: string) {
  try {
    uni.removeStorageSync(key)
  } catch {
    /* ignore */
  }
}

export function loadMonitorStorageFlag(key: string, fallback: boolean): boolean {
  try {
    const raw = uni.getStorageSync(key)
    if (raw === '' || raw == null) return fallback
    return raw === '1' || raw === 'true' || raw === true
  } catch {
    return fallback
  }
}

export function saveMonitorStorageFlag(key: string, value: boolean) {
  try {
    uni.setStorageSync(key, value ? '1' : '0')
  } catch {
    /* ignore */
  }
}
