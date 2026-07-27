import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConfig, sessionSignInit } from '@/api/system/auth'
import { setSecurityConfig, getClientId } from '@/utils/security-config'

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

        // 从公开配置获取「签名/加密是否开启」标志，但不再从此接口取密钥
        const sm3SignEnabled = data?.security?.sm3SignEnabled === true
        const sm4EncryptEnabled = data?.security?.sm4EncryptEnabled === true

        // 先将开关状态写入内存（密钥字段暂空）
        setSecurityConfig({
          sm4EncryptEnabled,
          sm3SignEnabled,
          sm2SignEnabled: sm3SignEnabled,
        })

        // 若签名功能已开启，调用 session-sign-init 获取本会话专属临时密钥（30 分钟有效）
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
