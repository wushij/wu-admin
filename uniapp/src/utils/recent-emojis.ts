const STORAGE_KEY = 'chat_recent_emojis'
const MAX_RECENT = 32

export function getRecentEmojis(): string[] {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    if (!raw) return []
    const list = JSON.parse(String(raw)) as unknown
    if (!Array.isArray(list)) return []
    return list.filter((e): e is string => typeof e === 'string' && e.length > 0)
  } catch {
    return []
  }
}

export function recordRecentEmoji(emoji: string) {
  const trimmed = emoji.trim()
  if (!trimmed) return
  const prev = getRecentEmojis().filter((e) => e !== trimmed)
  const next = [trimmed, ...prev].slice(0, MAX_RECENT)
  uni.setStorageSync(STORAGE_KEY, JSON.stringify(next))
  return next
}
