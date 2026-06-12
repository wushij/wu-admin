import { useUserStore } from '@/store/user'

export function usePermission() {
  const userStore = useUserStore()

  function hasPerm(permission: string) {
    return userStore.hasPermission(permission)
  }

  function hasAnyPerm(...permissions: string[]) {
    return permissions.some((p) => hasPerm(p))
  }

  function filterByPerm<T extends { permission?: string }>(items: T[]) {
    return items.filter((item) => !item.permission || hasPerm(item.permission))
  }

  return { hasPerm, hasAnyPerm, filterByPerm }
}
