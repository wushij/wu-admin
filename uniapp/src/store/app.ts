import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConfig } from '@/api/system/auth'

import { setSecurityConfig } from '@/utils/security-config'

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
    
    // 注入运行时加密/签名安全配置
    setSecurityConfig({
      sm4EncryptEnabled: res.data?.security?.sm4EncryptEnabled === true,
      sm4Key: res.data?.security?.sm4Key,
      sm2SignEnabled: res.data?.security?.sm2SignEnabled === true,
      sm2PrivateKey: res.data?.security?.sm2PrivateKey,
      sm3SignEnabled: res.data?.security?.sm3SignEnabled === true,
      sm3SignKey: res.data?.security?.sm3SignKey,
    })

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
