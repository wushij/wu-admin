const GROUP_AVATAR_COLORS = ['#576b95', '#10aeff', '#07c160', '#fa9d3b', '#6467f0', '#354b70']

export function groupAvatarColor(name?: string) {
  const s = name || 'G'
  let hash = 0
  for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash)
  return GROUP_AVATAR_COLORS[Math.abs(hash) % GROUP_AVATAR_COLORS.length]
}
