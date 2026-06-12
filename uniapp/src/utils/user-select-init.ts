const SESSION_KEY = 'uniapp_user_select_session'

export interface UserSelectSession {
  token: number
  initialId: number | null
  currentId?: number | null
}

export function openUserSelectSession(initialId: number | null | undefined) {
  const session: UserSelectSession = {
    token: Date.now(),
    initialId: initialId != null && initialId > 0 ? initialId : null,
    currentId: undefined,
  }
  uni.setStorageSync(SESSION_KEY, JSON.stringify(session))
  return session
}

export function readUserSelectSession(): UserSelectSession | null {
  try {
    const raw = uni.getStorageSync(SESSION_KEY)
    if (!raw) return null
    const data = JSON.parse(String(raw)) as UserSelectSession
    if (!data || typeof data.token !== 'number') return null
    return {
      token: data.token,
      initialId: data.initialId != null && data.initialId > 0 ? data.initialId : null,
      currentId:
        data.currentId === null
          ? null
          : data.currentId != null && data.currentId > 0
            ? data.currentId
            : undefined,
    }
  } catch {
    return null
  }
}

export function writeUserSelectSessionSelection(id: number | null) {
  const session = readUserSelectSession()
  if (!session) return
  session.currentId = id
  uni.setStorageSync(SESSION_KEY, JSON.stringify(session))
}

export function clearUserSelectSession() {
  try {
    uni.removeStorageSync(SESSION_KEY)
  } catch {
    /* ignore */
  }
}
