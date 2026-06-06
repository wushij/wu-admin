import { onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { preloadDicts } from '@/composables/useDict'
import { COMMON_DICT_TYPES } from '@/constants/dict'

export function useLayoutBootstrap(options: {
  initTheme: () => void
  loadSiteConfig: () => Promise<void>
  loadMessages: () => Promise<void>
}) {
  const userStore = useUserStore()
  const messageStore = useMessageStore()

  onMounted(async () => {
    options.initTheme()

    if (!userStore.menus || userStore.menus.length === 0) {
      try {
        await userStore.getUserInfo()
      } catch (error) {
        console.error('获取用户信息失败', error)
      }
    }
    options.loadMessages()
    messageStore.initWebSocket()
    options.loadSiteConfig()
    preloadDicts(COMMON_DICT_TYPES).catch(() => {})
  })

  onUnmounted(() => {
    messageStore.destroyWebSocket()
  })
}
