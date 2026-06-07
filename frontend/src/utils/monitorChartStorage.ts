export function loadMonitorSession<T>(key: string, fallback: T): T {
  try {
    const raw = sessionStorage.getItem(key)
    if (!raw) return fallback
    return { ...fallback, ...JSON.parse(raw) as T }
  } catch {
    return fallback
  }
}

export function saveMonitorSession(key: string, value: unknown) {
  try {
    sessionStorage.setItem(key, JSON.stringify(value))
  } catch {
    /* quota / private mode */
  }
}

export function removeMonitorSession(key: string) {
  try {
    sessionStorage.removeItem(key)
  } catch {
    /* ignore */
  }
}

export function loadMonitorSessionFlag(key: string, fallback: boolean): boolean {
  try {
    const raw = sessionStorage.getItem(key)
    if (raw == null) return fallback
    return raw === '1' || raw === 'true'
  } catch {
    return fallback
  }
}

export function saveMonitorSessionFlag(key: string, value: boolean) {
  try {
    sessionStorage.setItem(key, value ? '1' : '0')
  } catch {
    /* ignore */
  }
}
