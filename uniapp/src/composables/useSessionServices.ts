import { useMessageStore } from '@/store/message'
import { preloadDicts } from '@/composables/useDict'
import { COMMON_DICT_TYPES } from '@/constants/dict'
import { startMonitorBackground, stopMonitorBackground } from '@/composables/useMonitorBackground'

/** 登录 / 冷启动后初始化消息服务 */
export function startSessionServices() {
  const messageStore = useMessageStore()
  messageStore.initWebSocket()
  preloadDicts([...COMMON_DICT_TYPES]).catch(() => {})
  startMonitorBackground()
}

export function stopSessionServices() {
  useMessageStore().destroyWebSocket()
  stopMonitorBackground()
}
