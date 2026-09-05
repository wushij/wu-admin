import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConfig, sessionSignInit } from '@/api/system/auth'
import { setSecurityConfig, getClientId } from '@/utils/security-config'

export const useAppStore = defineStore('app', () => {
  const platformName = ref('Wu-Admin')
  const platformSubtitle = ref('')
  const loginWelcome = ref('欢迎登录')
  const registerTitle = ref('注册账号')
  const copyright = ref('')
  const icpEnabled = ref(true)
  const icpNumber = ref('粤ICP备2026045343号-1')
  const icpUrl = ref('https://beian.miit.gov.cn')
  const aiAssistantEnabled = ref(true)
  const configLoaded = ref(false)

  async function loadPublicConfig() {
    const res = await getConfig()
    const site = res.data?.site
    if (site?.platformName) platformName.value = site.platformName
    if (site?.platformSubtitle) platformSubtitle.value = site.platformSubtitle
    if (site?.loginWelcome) loginWelcome.value = site.loginWelcome
    if (site?.registerTitle) registerTitle.value = site.registerTitle
    if (site?.copyright !== undefined) copyright.value = site.copyright
    if (site?.icpEnabled !== undefined) icpEnabled.value = site.icpEnabled
    if (site?.icpNumber !== undefined) icpNumber.value = site.icpNumber
    if (site?.icpUrl !== undefined) icpUrl.value = site.icpUrl
    aiAssistantEnabled.value = res.data?.ai?.assistantEnabled !== false

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
    copyright,
    icpEnabled,
    icpNumber,
    icpUrl,
    aiAssistantEnabled,
    configLoaded,
    loadPublicConfig,
  }
})

