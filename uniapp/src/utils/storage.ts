import { logger } from '@/utils/logger'

export function getStorage<T>(key: string, fallback?: T): T | undefined {
  try {
    const raw = uni.getStorageSync(key)
    if (raw === '' || raw == null) return fallback
    if (typeof raw === 'string') {
      try {
        return JSON.parse(raw) as T
      } catch {
        return raw as T
      }
    }
    return raw as T
  } catch {
    return fallback
  }
}

export function setStorage(key: string, value: unknown) {
  try {
    const data = typeof value === 'string' ? value : JSON.stringify(value)
    uni.setStorageSync(key, data)
  } catch (e) {
    logger.error('setStorage failed', key, e)
  }
}

export function removeStorage(key: string) {
  try {
    uni.removeStorageSync(key)
  } catch (e) {
    logger.error('removeStorage failed', key, e)
  }
}
