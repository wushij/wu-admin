import { ref } from 'vue'
import { getConfig } from '@/api/system/auth'

export function useLayoutSite() {
  const sitePlatformName = ref('Admin Platform')
  const sitePlatformSubtitle = ref('统一运维 · 高效管控')
  const siteCopyright = ref('')
  const siteIcpEnabled = ref(true)
  const siteIcpNumber = ref('粤ICP备2026045343号-1')
  const siteIcpUrl = ref('https://beian.miit.gov.cn')

  async function loadSiteConfig() {
    try {
      const res = await getConfig()
      if (res.data?.site) {
        if (res.data.site.platformName) sitePlatformName.value = res.data.site.platformName
        if (res.data.site.platformSubtitle) sitePlatformSubtitle.value = res.data.site.platformSubtitle
        if (res.data.site.copyright !== undefined) siteCopyright.value = res.data.site.copyright
        if (res.data.site.icpEnabled !== undefined) siteIcpEnabled.value = res.data.site.icpEnabled
        if (res.data.site.icpNumber !== undefined) siteIcpNumber.value = res.data.site.icpNumber
        if (res.data.site.icpUrl !== undefined) siteIcpUrl.value = res.data.site.icpUrl
      }
    } catch {
      /* 使用默认值 */
    }
  }

  return {
    sitePlatformName,
    sitePlatformSubtitle,
    siteCopyright,
    siteIcpEnabled,
    siteIcpNumber,
    siteIcpUrl,
    loadSiteConfig,
  }
}
