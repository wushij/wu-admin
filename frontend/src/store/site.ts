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
        // 将后端下发的接口加密/签名策略注入闭包与 sessionStorage 缓存
        setSecurityConfig({
          sm4EncryptEnabled: data?.security?.sm4EncryptEnabled === true,
          sm4Key: data?.security?.sm4Key,
          sm2SignEnabled: data?.security?.sm2SignEnabled === true,
          sm2PrivateKey: data?.security?.sm2PrivateKey,
          sm3SignEnabled: data?.security?.sm3SignEnabled === true,
          sm3SignKey: data?.security?.sm3SignKey,
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
