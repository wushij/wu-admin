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
