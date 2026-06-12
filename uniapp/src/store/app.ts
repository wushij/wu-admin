import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConfig } from '@/api/system/auth'

export const useAppStore = defineStore('app', () => {
  const platformName = ref('Wu-Admin')
  const platformSubtitle = ref('')
  const loginWelcome = ref('欢迎登录')
  const registerTitle = ref('注册账号')
  const configLoaded = ref(false)

  async function loadPublicConfig() {
    const res = await getConfig()
    const site = res.data?.site
    if (site?.platformName) platformName.value = site.platformName
    if (site?.platformSubtitle) platformSubtitle.value = site.platformSubtitle
    if (site?.loginWelcome) loginWelcome.value = site.loginWelcome
    if (site?.registerTitle) registerTitle.value = site.registerTitle
    configLoaded.value = true
    return res
  }

  return {
    platformName,
    platformSubtitle,
    loginWelcome,
    registerTitle,
    configLoaded,
    loadPublicConfig,
  }
})
