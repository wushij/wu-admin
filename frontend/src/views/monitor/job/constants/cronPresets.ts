export const cronPresets = [
  { label: '每 30 秒', value: '0/30 * * * * ?' },
  { label: '每分钟', value: '0 * * * * ?' },
  { label: '每 5 分钟', value: '0 0/5 * * * ?' },
  { label: '每天 0 点', value: '0 0 0 * * ?' },
  { label: '每天 2:30', value: '0 30 2 * * ?' },
  { label: '每周一 9 点', value: '0 0 9 ? * MON' },
]

export function tplIcon(icon?: string) {
  const map: Record<string, string> = {
    Document: '📄',
    ChatDotRound: '💬',
    ChatLineRound: '👥',
    Timer: '⏱️',
    Bell: '🔔',
    Tickets: '🎫',
  }
  return map[icon || ''] || '📌'
}

export function groupTagType(group?: string) {
  return group === 'SYSTEM' ? 'warning' : 'info'
}
