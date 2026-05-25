import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConfig } from '@/api/system/auth'

export const useSiteStore = defineStore('site', () => {
  const disableDevtool = ref(false)
  const configLoaded = ref(false)

  async function loadConfig() {
    try {
      const res = await getConfig()
      const data = res.data
      disableDevtool.value = data?.security?.disableDevtool === true
    } catch {
      disableDevtool.value = false
    } finally {
      configLoaded.value = true
    }
  }

  function setDisableDevtool(value: boolean) {
    disableDevtool.value = value
  }

  return { disableDevtool, configLoaded, loadConfig, setDisableDevtool }
})
