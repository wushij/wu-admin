import { STORAGE_KEYS } from '@/constants/storage-keys'

export function getToken(): string {
  return uni.getStorageSync(STORAGE_KEYS.TOKEN) || ''
}

export function setToken(token: string): void {
  uni.setStorageSync(STORAGE_KEYS.TOKEN, token)
}

export function removeToken(): void {
  uni.removeStorageSync(STORAGE_KEYS.TOKEN)
}

export function hasToken(): boolean {
  return !!getToken()
}

