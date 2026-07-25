import { useUserStore } from '@/store/user'

export function usePermission() {
  const userStore = useUserStore()

  function hasPerm(permission: string) {
    return userStore.hasPermission(permission)
  }

  /** 与 PC 侧栏一致：仅匹配启用菜单树，避免 query 别名导致停用菜单仍出现在工作台 */
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
