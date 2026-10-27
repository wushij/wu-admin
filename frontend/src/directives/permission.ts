import type { App, Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/store/user'
import {
  hasPermission,
  hasMenuPermission,
  type PermissionValue
} from '@/utils/permission'

// 向后兼容导出
export { hasPermission, hasMenuPermission }
export type { PermissionValue }

/**
 * 权限控制指令
 * 使用方式: v-permission="'system:user:create'" 或 v-permission="['system:user:create', 'system:user:update']"
 */
export const permission: Directive<HTMLElement, PermissionValue> = {
  mounted(el, binding) {
    const requiredPermissions = binding.value
    if (!requiredPermissions) return

    if (!hasPermission(requiredPermissions)) {
      el.parentNode?.removeChild(el)
    }
  },
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
