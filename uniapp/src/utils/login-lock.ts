/** 登录失败锁定展示文案 */
export function formatLoginLockRemain(remainSeconds?: number | null): string {
  if (!remainSeconds || remainSeconds <= 0) return '即将自动解除'
  const min = Math.max(1, Math.ceil(remainSeconds / 60))
  return `约 ${min} 分钟后自动解除`
}

export interface LoginLockUserLike {
  username?: string
  loginLocked?: boolean
  loginFailCount?: number
  loginRecentIp?: string | null
  loginIpLocked?: boolean
  loginIpFailCount?: number
}

export function hasAccountLoginLock(user?: LoginLockUserLike | null): boolean {
  return Boolean(user?.loginLocked)
}

export function hasIpLoginLock(user?: LoginLockUserLike | null): boolean {
  return Boolean(user?.loginIpLocked)
}

export function canUnlockLoginLock(user?: LoginLockUserLike | null): boolean {
  return hasAccountLoginLock(user) || hasIpLoginLock(user)
}

export function buildUnlockLoginConfirm(user: LoginLockUserLike & { nickname?: string }): { title: string; content: string } {
  const name = user.nickname || user.username || '该用户'
  const ip = user.loginRecentIp || '—'
  const accountLocked = hasAccountLoginLock(user)
  const ipLocked = hasIpLoginLock(user)

  if (accountLocked && ipLocked) {
    return {
      title: '解除登录锁定',
      content:
        `确认解除用户「${name}」的登录失败锁定，并解除最近登录 IP「${ip}」的临时锁定？\n解锁后可立即尝试登录。`,
    }
  }
  if (accountLocked) {
    return {
      title: '解除登录锁定',
      content: `确认解除用户「${name}」的登录失败锁定？\n解锁后该账号可立即尝试登录。`,
    }
  }
  if (ipLocked) {
    return {
      title: '解除 IP 锁定',
      content: `确认解除用户「${name}」最近登录 IP「${ip}」的临时锁定？\n解锁后该 IP 可立即尝试登录。`,
    }
  }
  return {
    title: '解除登录锁定',
    content: `确认解除用户「${name}」的登录限制？`,
  }
}
