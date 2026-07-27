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

    // 从公开配置获取「签名/加密是否开启」标志，但不再从此接口取密钥
    const sm3SignEnabled = res.data?.security?.sm3SignEnabled === true
    const sm4EncryptEnabled = res.data?.security?.sm4EncryptEnabled === true

    // 先将开关状态写入内存闭包（密钥字段暂空）
    setSecurityConfig({
      sm4EncryptEnabled,
      sm3SignEnabled,
      sm2SignEnabled: sm3SignEnabled,
    })

    // 若签名功能已开启，调用 session-sign-init 获取本次启动专属临时密钥（30 分钟有效）
    // 密钥仅存于内存，不写入 uni.setStorageSync
    if (sm3SignEnabled) {
      try {
        const signRes = await sessionSignInit(getClientId())
        if (signRes.data?.enabled) {
          setSecurityConfig({
            sm3SignKey: signRes.data.sm3SignKey,
            sm2PrivateKey: signRes.data.sm2PrivateKey,
          })
        }
      } catch {
        // 密钥获取失败不阻断应用启动，签名功能将被跳过
      }
    }

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

