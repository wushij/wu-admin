const STORAGE_KEY = 'chat_recent_emojis'
const MAX_RECENT = 45

/** 默认常用表情 45 个（始终展示，最近用过的排在前面；总数不超过 MAX_RECENT） */
export const DEFAULT_RECENT_EMOJIS = [
  '😀', '😂', '🤣', '😊', '🥲', '😭', '😅', '😁', '😉', '🥰',
  '😘', '🙄', '😴', '😡', '🤔', '😮', '😎', '🤗', '😤', '🙈',
  '👍', '👏', '🙏', '👌', '✌️', '🤝', '💪', '🫡', '👀', '🙋',
  '❤️', '💖', '💯', '🔥', '✨', '⭐', '🌹', '💐', '🎉', '🎁',
  '✅', '☕', '🍺', '🐶', '🐱',
] as const
function loadSavedRecent(): string[] {
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

/** 最近使用过的表情 + 默认常用表情（去重，用过的在前） */
export function getRecentEmojis(): string[] {
  const saved = loadSavedRecent()
  const merged = [...saved]
  for (const emoji of DEFAULT_RECENT_EMOJIS) {
    if (!merged.includes(emoji)) merged.push(emoji)
  }
  return merged.slice(0, MAX_RECENT)
}

export function recordRecentEmoji(emoji: string) {
  const trimmed = emoji.trim()
  if (!trimmed) return getRecentEmojis()
  const saved = loadSavedRecent().filter((e) => e !== trimmed)
  const nextSaved = [trimmed, ...saved].slice(0, MAX_RECENT)
  uni.setStorageSync(STORAGE_KEY, JSON.stringify(nextSaved))
  return getRecentEmojis()
}
