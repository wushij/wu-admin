import { useUserStore } from '@/store/user'

/**
 * 权限控制指令
 * 使用方式: v-permission="'system:user:create'" 或 v-permission="['system:user:create', 'system:user:update']"
 */
export const permission = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const permissions = userStore.userInfo.permissions || []
    const requiredPermissions = binding.value
    
    if (!requiredPermissions) return
    
    // 如果是字符串，转为数组
    const permissionList = Array.isArray(requiredPermissions) 
      ? requiredPermissions 
      : [requiredPermissions]
    
    // 检查是否有权限（只要有一个权限就通过）
    const codes = userStore.userInfo.permissions || []
    const hasPermission = permissionList.some(perm =>
      codes.includes(perm) || checkPermissionFromMenus(userStore.menus, perm)
    )
    
    if (!hasPermission) {
      // 没有权限，移除元素
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * 从菜单树中判断是否拥有某权限标识（与角色勾选的菜单/按钮一致）
 */
export function hasMenuPermission(menus, permission) {
  return checkPermissionFromMenus(menus, permission)
}

/**
 * 从菜单列表中检查权限
 */
function checkPermissionFromMenus(menus, permission) {
  if (!menus || !Array.isArray(menus)) return false
  
  for (const menu of menus) {
    // 检查当前菜单的权限标识
    if (menu.permission === permission) {
      return true
    }
    // 递归检查子菜单
    if (menu.children && menu.children.length > 0) {
      if (checkPermissionFromMenus(menu.children, permission)) {
        return true
      }
    }
  }
  return false
}

/**
 * 角色控制指令
 * 使用方式: v-role="'admin'" 或 v-role="['admin', 'editor']"
 */
export const role = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const userRoles = userStore.userInfo.roles || []
    const requiredRoles = binding.value
    
    if (!requiredRoles) return
    
    // 如果是字符串，转为数组
    const roleList = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles]
    
    // 检查是否有角色
    const hasRole = roleList.some(role => userRoles.includes(role))
    
    if (!hasRole) {
      el.parentNode?.removeChild(el)
    }
  }
}

// 注册全局指令
export function setupPermissionDirectives(app) {
  app.directive('permission', permission)
  app.directive('role', role)
}
