import { computed } from 'vue'
import { usePermission } from '@/composables/usePermission'

export function useModulePermission(permission: string) {
  const { hasPerm } = usePermission()
  const allowed = computed(() => hasPerm(permission))
  return { allowed, hasPerm }
}
