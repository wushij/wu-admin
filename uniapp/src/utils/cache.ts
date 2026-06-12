import { STORAGE_KEYS } from '@/constants/storage-keys'
import { getStorage, removeStorage, setStorage } from '@/utils/storage'

const DEFAULT_TTL_MS = 30 * 60 * 1000

interface CacheEntry<T> {
  value: T
  expireAt: number
}

export function getCache<T>(key: string): T | undefined {
  const entry = getStorage<CacheEntry<T>>(key)
  if (!entry) return undefined
  if (Date.now() > entry.expireAt) {
    removeStorage(key)
    return undefined
  }
  return entry.value
}

export function setCache<T>(key: string, value: T, ttlMs = DEFAULT_TTL_MS) {
  setStorage(key, { value, expireAt: Date.now() + ttlMs } satisfies CacheEntry<T>)
}

export function clearDictCache() {
  removeStorage(STORAGE_KEYS.DICT_CACHE)
}
