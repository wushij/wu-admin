/** 登录页「记住我」本地凭据（仅前端，勿在公共设备使用） */
export const LOGIN_REMEMBER_STORAGE_KEY = 'login-remember'

export type LoginRememberMode = 'account' | 'sms'

export interface LoginRememberPayload {
  mode: LoginRememberMode
  rememberMe: true
  username?: string
  password?: string
  phone?: string
}

function encodeText(value: string): string {
  try {
    return btoa(encodeURIComponent(value))
  } catch {
    return value
  }
}

function decodeText(value: string): string {
  try {
    return decodeURIComponent(atob(value))
  } catch {
    return value
  }
}

export function loadLoginRemember(): LoginRememberPayload | null {
  try {
    const raw = localStorage.getItem(LOGIN_REMEMBER_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as {
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
  localStorage.setItem(LOGIN_REMEMBER_STORAGE_KEY, JSON.stringify(stored))
}

export function clearLoginRemember(): void {
  localStorage.removeItem(LOGIN_REMEMBER_STORAGE_KEY)
}
