/** 深色 Hero 右侧光晕，与 ModuleIcon 主题色呼应 */
export const MODULE_HERO_GLOW: Record<string, string> = {
  user: 'rgba(102, 126, 234, 0.32)',
  role: 'rgba(245, 87, 108, 0.32)',
  menu: 'rgba(79, 172, 254, 0.32)',
  dept: 'rgba(67, 233, 123, 0.28)',
  post: 'rgba(14, 165, 233, 0.28)',
  dict: 'rgba(99, 102, 241, 0.32)',
  config: 'rgba(100, 116, 139, 0.28)',
  log: 'rgba(250, 112, 154, 0.28)',
  'file-store': 'rgba(99, 102, 241, 0.28)',
  approval: 'rgba(161, 140, 209, 0.28)',
  ticket: 'rgba(247, 151, 30, 0.28)',
  notice: 'rgba(236, 72, 153, 0.28)',
  chat: 'rgba(249, 115, 22, 0.28)',
  online: 'rgba(102, 126, 234, 0.28)',
  monitor: 'rgba(20, 184, 166, 0.28)',
  server: 'rgba(139, 92, 246, 0.28)',
  cache: 'rgba(245, 158, 11, 0.28)',
  job: 'rgba(20, 184, 166, 0.28)',
  api: 'rgba(102, 126, 234, 0.28)',
  default: 'rgba(99, 102, 241, 0.28)',
}

export function moduleHeroGlow(theme: string) {
  return MODULE_HERO_GLOW[theme] ?? MODULE_HERO_GLOW.default
}
