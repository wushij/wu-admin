import { useUserStore } from '@/store/user'
import type { MenuTreeNode } from '@/types/api'

export type PermissionValue = string | string[]

/**
 * 递归检查菜单树中是否存在目标权限标识
 */
export function checkPermissionFromMenus(menus: MenuTreeNode[] | undefined, permission: string): boolean {
  if (!menus?.length) return false

  for (const menu of menus) {
    if (menu.permission === permission) return true
    if (menu.children?.length && checkPermissionFromMenus(menu.children as MenuTreeNode[], permission)) {
      return true
    }
  }
  return false
}

/**
 * 权限判断核心方法
 * 1. 管理员（admin / super_admin 角色）拥有所有权限
 * 2. 具备通配符 '*:*:*' 权限拥有所有权限
 * 3. 匹配 userStore.userInfo.permissions 权限码集合（支持按钮与接口级权限）
 * 4. 匹配 userStore.menus 菜单树（支持页面级权限）
 */
export function hasPermission(permission: PermissionValue): boolean {
  const userStore = useUserStore()
  const roles = userStore.userInfo?.roles || []
  if (roles.includes('admin') || roles.includes('super_admin')) {
    return true
  }

  const codes = userStore.userInfo?.permissions || []
  if (codes.includes('*:*:*')) {
    return true
  }

  const permissionList = Array.isArray(permission) ? permission : [permission]
  return permissionList.some(
    perm => codes.includes(perm) || checkPermissionFromMenus(userStore.menus, perm)
  )
}

/**
 * 从菜单树中判断是否拥有某权限标识
 * 说明：后端下发的 userStore.menus 仅包含目录与页面（已过滤 type=3 按钮），
 * 若菜单树未命中，会自动回退检查 userStore.userInfo.permissions 或超级管理员角色，避免按钮权限被误判为无权限。
 */
export function hasMenuPermission(menus: MenuTreeNode[] | undefined, permission: string): boolean {
  if (checkPermissionFromMenus(menus, permission)) {
    return true
  }
  const userStore = useUserStore()
  const roles = userStore.userInfo?.roles || []
  if (roles.includes('admin') || roles.includes('super_admin')) {
    return true
  }
  const codes = userStore.userInfo?.permissions || []
  return codes.includes('*:*:*') || codes.includes(permission)
}

/** 兼容简写：从侧栏菜单树判断是否具有某 permission（与后端 @PreAuthorize 标识一致） */
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
