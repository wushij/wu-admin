export type MenuTypeTag = 'success' | 'primary' | 'warning' | 'info' | 'danger'

export const typeTagMap: Record<number, { label: string; tag: MenuTypeTag }> = {
  1: { label: '目录', tag: 'info' },
  2: { label: '菜单', tag: 'success' },
  3: { label: '按钮', tag: 'warning' },
}

export function menuTypeMeta(type: number | undefined) {
  return type != null ? typeTagMap[type] : undefined
}

export const componentPresets = [
  'system/user/index',
  'system/role/index',
  'system/menu/index',
  'system/org/index',
  'system/dict/index',
  'system/oper-log/index',
  'system/login-log/index',
  'system/file/index',
]
