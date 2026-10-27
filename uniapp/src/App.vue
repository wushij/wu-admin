<template>
  <MessageNotification />
  <H5BackButton />
  <AiWuAssistant />
</template>

<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import MessageNotification from '@/components/business/message/MessageNotification/index.vue'
import H5BackButton from '@/components/base/H5BackButton/index.vue'
import AiWuAssistant from '@/components/business/ai/AiWuAssistant/index.vue'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { setupRouteGuard, bootstrapSession } from '@/utils/nav/route-guard'
import { ensureH5Favicon } from '@/utils/h5-document-head'
import { hasToken } from '@/core/auth'
import { shouldRedirectAuthedUserToHome } from '@/utils/nav/launch-route'
import { startSessionServices } from '@/composables/base/useSessionServices'
import { onMonitorAppHide, onMonitorAppShow } from '@/composables/monitor/useMonitorBackground'
import { useMessageStore } from '@/store/message'

onLaunch(async () => {
  setupRouteGuard()
  ensureH5Favicon()

  const appStore = useAppStore()
  try {
    await appStore.loadPublicConfig()
  } catch {
    /* 离线或后端未启动时使用默认文案 */
  }

  const userStore = useUserStore()
  if (hasToken()) {
    await bootstrapSession(() => userStore.ensureUserLoaded() as Promise<any>)
    if (hasToken()) {
      startSessionServices()
      if (shouldRedirectAuthedUserToHome()) {
        uni.switchTab({ url: '/pages/index/index' })
      }
    }
  }

  uni.onNetworkStatusChange((res) => {
    if (res.isConnected && hasToken()) {
      useMessageStore().reconnectWebSocket()
    }
  })
})

onShow(() => {
  if (hasToken()) {
    onMonitorAppShow()
    useMessageStore().reconnectWebSocket()
  }
})

onHide(() => {
  onMonitorAppHide()
})
</script>

<style lang="scss">
@use '@/styles/reset.scss';
@use '@/styles/iconfont.scss';
@use '@/styles/module-themes.scss';
@use '@/styles/common.scss';
@use '@/styles/toast-h5.scss';
@use '@/styles/chat-markdown-global.scss';
</style>
