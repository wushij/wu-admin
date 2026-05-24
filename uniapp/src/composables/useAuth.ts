import { computed } from 'vue'
import { useUserStore } from '@/store/user'
import { getToken, hasToken, removeToken, setToken } from '@/utils/auth'

export function useAuth() {
  const userStore = useUserStore()
  const isLoggedIn = computed(() => hasToken() && !!userStore.userInfo)

  return {
    isLoggedIn,
    getToken,
    hasToken,
    setToken,
    removeToken,
    logout: () => userStore.logout(),
  }
}
