import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getProfile } from '@/api/system/profile'
import { useUserStore } from '@/store/user'
import type { UserProfile } from '@/types/profile'
import { logger } from '@/utils/logger'

export function useMinePage() {
  const userStore = useUserStore()
  const profile = ref<UserProfile | null>(null)
  const loading = ref(false)

  const nickname = computed(
    () =>
      userStore.userInfo.nickname ||
      profile.value?.nickname ||
      userStore.userInfo.username ||
      '-',
  )
  const username = computed(() => userStore.userInfo.username || profile.value?.username || '-')
  const avatar = computed(() => userStore.userInfo.avatar || profile.value?.avatar || '')
  const roles = computed(() => profile.value?.roleNames || userStore.userInfo.roles || [])

  async function loadProfile() {
    loading.value = true
    try {
      const res = await getProfile()
      profile.value = res.data
      userStore.patchUserInfo({
        nickname: res.data.nickname,
        avatar: res.data.avatar,
      })
    } catch (e) {
      logger.error(e)
    } finally {
      loading.value = false
    }
  }

  onMounted(loadProfile)
  onShow(loadProfile)

  return {
    profile,
    loading,
    nickname,
    username,
    avatar,
    roles,
    loadProfile,
  }
}
