import type { App, Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/store/user'
import type { MenuTreeNode } from '@/types/api'

type PermissionValue = string | string[]

/**
 * 权限控制指令
 * 使用方式: v-permission="'system:user:create'" 或 v-permission="['system:user:create', 'system:user:update']"
 */
export const permission: Directive<HTMLElement, PermissionValue> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const requiredPermissions = binding.value
    if (!requiredPermissions) return

    const permissionList = Array.isArray(requiredPermissions)
      ? requiredPermissions
      : [requiredPermissions]

    const codes = userStore.userInfo.permissions || []
    const hasPermission = permissionList.some(perm =>
      codes.includes(perm) || checkPermissionFromMenus(userStore.menus, perm)
    )

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  },
}

/** 从菜单树中判断是否拥有某权限标识（与角色勾选的菜单/按钮一致） */
export function hasMenuPermission(menus: MenuTreeNode[], permission: string): boolean {
  return checkPermissionFromMenus(menus, permission)
}

function checkPermissionFromMenus(menus: MenuTreeNode[] | undefined, permission: string): boolean {
  if (!menus?.length) return false

  for (const menu of menus) {
    if (menu.permission === permission) return true
    if (menu.children?.length && checkPermissionFromMenus(menu.children, permission)) {
      return true
    }
  }
  return false
}

/**
 * 角色控制指令
 * 使用方式: v-role="'admin'" 或 v-role="['admin', 'editor']"
 */
export const role: Directive<HTMLElement, PermissionValue> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const userRoles = userStore.userInfo.roles || []
    const requiredRoles = binding.value
    if (!requiredRoles) return

    const roleList = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles]
    const hasRole = roleList.some(r => userRoles.includes(r))

    if (!hasRole) {
      el.parentNode?.removeChild(el)
    }
  },
}

/** 注册全局指令 */
export function setupPermissionDirectives(app: App): void {
  app.directive('permission', permission as Directive)
  app.directive('role', role as Directive)
}

export type PermissionBinding = DirectiveBinding<PermissionValue>
