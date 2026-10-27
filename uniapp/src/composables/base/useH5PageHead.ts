import { watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { setH5DocumentTitle } from '@/utils/h5-document-head'

export function useH5PageHead(pageTitle: string) {
  const appStore = useAppStore()

  function syncHead() {
    setH5DocumentTitle(pageTitle, appStore.platformName)
  }

  onShow(syncHead)
  watch(() => appStore.platformName, syncHead)

  return { syncHead }
}
