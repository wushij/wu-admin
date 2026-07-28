import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConfig, sessionSignInit } from '@/api/system/auth'
import { setSecurityConfig, getClientId } from '@/utils/security-config'

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

    // 从公开配置获取「签名/加密是否开启」标志，但未登录前暂不下发密钥
    const sm3SignEnabled = res.data?.security?.sm3SignEnabled === true
    const sm4EncryptEnabled = res.data?.security?.sm4EncryptEnabled === true

    // 先将开关状态写入内存闭包（密钥字段暂空，待登录后由 userStore 自动下发）
    setSecurityConfig({
      sm4EncryptEnabled,
      sm3SignEnabled,
      sm2SignEnabled: sm3SignEnabled,
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

