import { withTokenQuery } from '@/api/system/file/index'

export function avatarFallback(name?: string | null): string {
  const text = (name || 'U').trim()
  return text ? text.charAt(0).toUpperCase() : 'U'
}

export function resolveAvatarUrl(url?: string | null): string | undefined {
  if (!url?.trim()) return undefined
  const u = url.trim()
  if (u.startsWith('http://') || u.startsWith('https://')) return withTokenQuery(u)
  const path = u.startsWith('/api') ? u : u.startsWith('/') ? `/api${u}` : `/api/${u}`
  return withTokenQuery(path)
}

export function buildUserAvatarMap(entries: Array<{ userId: number; avatar?: string | null }>) {
  const map = new Map<number, string>()
  entries.forEach(({ userId, avatar }) => {
    if (userId && avatar) map.set(userId, avatar)
  })
  return map
}

export function resolveChatAvatar(
  userId: number | undefined,
  avatarMap: Map<number, string>,
  storedAvatar?: string | null,
) {
  let raw: string | undefined
  if (userId != null) raw = avatarMap.get(userId)
  if (!raw) raw = storedAvatar || undefined
  return resolveAvatarUrl(raw)
}
