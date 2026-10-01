/** 从侧栏菜单树判断是否具有某 permission（与后端 @PreAuthorize 标识一致） */
export function hasMenuPerm(
  menus: { permission?: string; children?: unknown[] }[] | undefined,
  perm: string,
): boolean {
  if (!menus?.length) return false
  for (const menu of menus) {
    if (menu.permission === perm) return true
    if (menu.children?.length && hasMenuPerm(menu.children as typeof menus, perm)) return true
  }
  return false
}

export function hasMonitorPerm(
  permissions: string[] | undefined,
  menus: { permission?: string; children?: unknown[] }[] | undefined,
  perm: string,
): boolean {
  return (permissions || []).includes(perm) || hasMenuPerm(menus, perm)
}

/**
 * 是否可使用企业 IM。
 * 仅以侧栏菜单树为准（与路由鉴权一致）：系统菜单里停用企业 IM 后不会出现在 menus 中，
 * 此时不应请求群聊接口，避免无权限 403 弹窗。
 */
export function hasChatPerm(
  _permissions: string[] | undefined,
  menus: { permission?: string; children?: unknown[] }[] | undefined,
): boolean {
  return hasMenuPerm(menus, 'system:chat:list') || hasMenuPerm(menus, 'system:chat:query')
}
