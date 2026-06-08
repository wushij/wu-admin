import { ref } from 'vue'
import { getConfig } from '@/api/system/auth'

export function useLayoutSite() {
  const sitePlatformName = ref('Admin Platform')
  const sitePlatformSubtitle = ref('Management System')

  async function loadSiteConfig() {
    try {
      const res = await getConfig()
      if (res.data?.site) {
        if (res.data.site.platformName) sitePlatformName.value = res.data.site.platformName
        if (res.data.site.platformSubtitle) sitePlatformSubtitle.value = res.data.site.platformSubtitle
      }
    } catch {
      /* 使用默认值 */
    }
  }

  return { sitePlatformName, sitePlatformSubtitle, loadSiteConfig }
}
