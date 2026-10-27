import { useUserStore } from '@/store/user'

export function usePermission() {
  const userStore = useUserStore()

  function hasPerm(permission: string) {
    return userStore.hasPermission(permission)
  }

  /**
   * 工作台入口：优先匹配启用菜单树，停用菜单不会出现在侧栏/工作台。
   * 后端下发的权限码同样只包含启用菜单，菜单树未命中时回退权限码，
   * 避免按钮级权限被误判为无权限（与 PC 端保持一致）。
   */
  function hasMenuPerm(permission: string) {
    return userStore.hasMenuPermission(permission)
  }

  function hasAnyPerm(...permissions: string[]) {
    return permissions.some((p) => hasPerm(p))
  }

  function filterByPerm<T extends { permission?: string }>(items: T[]) {
    return items.filter((item) => !item.permission || hasPerm(item.permission))
  }

  function filterByEnabledMenu<T extends { permission?: string }>(items: T[]) {
    return items.filter((item) => !item.permission || hasMenuPerm(item.permission))
  }

  return { hasPerm, hasMenuPerm, hasAnyPerm, filterByPerm, filterByEnabledMenu }
}
