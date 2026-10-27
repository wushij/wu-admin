import { withTokenQuery } from '@/api/system/file/index'

/** 聊天头像：优先用实时用户资料，其次消息/联系人里存的地址 */
export function avatarFallback(name?: string | null) {
  return (name || 'U').charAt(0).toUpperCase()
}

function toAvatarSrc(url?: string | null): string | undefined {
  if (!url?.trim()) return undefined
  const u = url.trim()
  if (u.startsWith('http://') || u.startsWith('https://')) return u
  const path = u.startsWith('/api') ? u : u.startsWith('/') ? `/api${u}` : `/api/${u}`
  return withTokenQuery(path)
}

export function buildUserAvatarMap(
  entries: Array<{ userId: number; avatar?: string | null }>
) {
  const map = new Map<number, string>()
  entries.forEach(({ userId, avatar }) => {
    if (avatar) map.set(userId, avatar)
  })
  return map
}

export function resolveChatAvatar(
  userId: number | undefined,
  avatarMap: Map<number, string>,
  storedAvatar?: string | null
) {
  let raw: string | undefined
  if (userId != null) {
    raw = avatarMap.get(userId)
  }
  if (!raw) raw = storedAvatar || undefined
  return toAvatarSrc(raw)
}
