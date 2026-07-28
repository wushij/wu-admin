import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConfig } from '@/api/system/auth'
import { setSecurityConfig } from '@/utils/security-config'

let configPromise: Promise<void> | null = null

export const useSiteStore = defineStore('site', () => {
  const disableDevtool = ref(false)
  const configLoaded = ref(false)

  async function loadConfig() {
    if (configPromise) return configPromise
    configPromise = (async () => {
      try {
        const res = await getConfig()
        const data = res.data
        disableDevtool.value = data?.security?.disableDevtool === true

        // 从公开配置获取「签名/加密是否开启」标志，但未登录前暂不下发密钥
        const sm3SignEnabled = data?.security?.sm3SignEnabled === true
        const sm4EncryptEnabled = data?.security?.sm4EncryptEnabled === true

        // 先将开关状态写入内存（密钥字段暂空，待登录后自动下发）
        setSecurityConfig({
          sm4EncryptEnabled,
          sm3SignEnabled,
          sm2SignEnabled: sm3SignEnabled,
        })
      } catch {
        disableDevtool.value = false
      } finally {
        configLoaded.value = true
      }
    })()
    return configPromise
  }

  function ensureConfigLoaded() {
    if (configLoaded.value) return Promise.resolve()
    return loadConfig()
  }

  function setDisableDevtool(value: boolean) {
    disableDevtool.value = value
  }

  return { disableDevtool, configLoaded, loadConfig, ensureConfigLoaded, setDisableDevtool }
})
