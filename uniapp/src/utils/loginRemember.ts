/** 登录页「记住我」本地凭据（uni 存储） */
export const LOGIN_REMEMBER_STORAGE_KEY = 'login-remember'

export type LoginRememberMode = 'account' | 'sms'

export interface LoginRememberPayload {
  mode: LoginRememberMode
  rememberMe: true
  username?: string
  password?: string
  phone?: string
}

function utf8ToBytes(str: string): Uint8Array {
  const encoded = encodeURIComponent(str)
  const bytes: number[] = []
  for (let i = 0; i < encoded.length; i++) {
    if (encoded[i] === '%') {
      bytes.push(parseInt(encoded.slice(i + 1, i + 3), 16))
      i += 2
    } else {
      bytes.push(encoded.charCodeAt(i))
    }
  }
  return new Uint8Array(bytes)
}

function bytesToUtf8(bytes: Uint8Array): string {
  let encoded = ''
  for (let i = 0; i < bytes.length; i++) {
    encoded += `%${bytes[i].toString(16).padStart(2, '0')}`
  }
  return decodeURIComponent(encoded)
}

function encodeText(value: string): string {
  try {
    const buffer = utf8ToBytes(value).buffer
    return uni.arrayBufferToBase64(buffer as ArrayBuffer)
  } catch {
    return value
  }
}

function decodeText(value: string): string {
  if (!value) return ''
  try {
    const buffer = uni.base64ToArrayBuffer(value)
    return bytesToUtf8(new Uint8Array(buffer))
  } catch {
    /* 兼容旧版 btoa 存的数据 */
    try {
      if (typeof atob === 'function') {
        return decodeURIComponent(atob(value))
      }
    } catch {
      /* ignore */
    }
    return ''
  }
}

export function loadLoginRemember(): LoginRememberPayload | null {
  try {
    const raw = uni.getStorageSync(LOGIN_REMEMBER_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw as string) as {
      mode?: LoginRememberMode
      rememberMe?: boolean
      username?: string
      password?: string
      phone?: string
      passwordEnc?: string
    }
    if (parsed.rememberMe !== true || !parsed.mode) return null
    const payload: LoginRememberPayload = {
      mode: parsed.mode,
      rememberMe: true,
    }
    if (parsed.username) payload.username = parsed.username
    if (parsed.phone) payload.phone = parsed.phone
    if (parsed.passwordEnc) {
      payload.password = decodeText(parsed.passwordEnc)
    } else if (parsed.password) {
      payload.password = parsed.password
    }
    return payload
  } catch {
    return null
  }
}

export function saveLoginRemember(payload: LoginRememberPayload): void {
  const stored: Record<string, unknown> = {
    mode: payload.mode,
    rememberMe: true,
  }
  if (payload.username) stored.username = payload.username
  if (payload.phone) stored.phone = payload.phone
  if (payload.password) stored.passwordEnc = encodeText(payload.password)
  uni.setStorageSync(LOGIN_REMEMBER_STORAGE_KEY, JSON.stringify(stored))
}

export function clearLoginRemember(): void {
  uni.removeStorageSync(LOGIN_REMEMBER_STORAGE_KEY)
}
