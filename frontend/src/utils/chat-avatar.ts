/** 聊天头像：优先用实时用户资料，其次消息/联系人里存的地址 */
export function avatarFallback(name?: string | null) {
  return (name || 'U').charAt(0).toUpperCase()
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
  if (userId != null) {
    const live = avatarMap.get(userId)
    if (live) return live
  }
  return storedAvatar || undefined
}
