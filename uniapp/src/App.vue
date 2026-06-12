<template>
  <MessageNotification />
  <H5BackButton />
</template>

<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import MessageNotification from '@/components/business/MessageNotification/index.vue'
import H5BackButton from '@/components/common/H5BackButton/index.vue'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { setupRouteGuard, bootstrapSession } from '@/utils/route-guard'
import { setupDialogBackGuard } from '@/utils/dialog-back-guard'
import { ensureH5Favicon } from '@/utils/h5-document-head'
import { hasToken } from '@/utils/auth'
import { shouldRedirectAuthedUserToHome } from '@/utils/launch-route'
import { startSessionServices } from '@/composables/useSessionServices'
import { useMessageStore } from '@/store/message'

onLaunch(async () => {
  setupRouteGuard()
  setupDialogBackGuard()
  ensureH5Favicon()

  const appStore = useAppStore()
  try {
    await appStore.loadPublicConfig()
  } catch {
    /* 离线或后端未启动时使用默认文案 */
  }

  const userStore = useUserStore()
  if (hasToken()) {
    await bootstrapSession(() => userStore.refreshUserStore())
    startSessionServices()
    if (shouldRedirectAuthedUserToHome()) {
      uni.switchTab({ url: '/pages/index/index' })
    }
  }

  uni.onNetworkStatusChange((res) => {
    if (res.isConnected && hasToken()) {
      useMessageStore().initWebSocket()
    }
  })
})
</script>

<style lang="scss">
@import '@/styles/reset.scss';
@import '@/styles/iconfont.scss';
@import '@/styles/module-themes.scss';
@import '@/styles/common.scss';
@import '@/styles/toast-h5.scss';
</style>
