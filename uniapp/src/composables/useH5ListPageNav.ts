import { onLoad, onShow, onBackPress } from '@dcloudio/uni-app'
import { scheduleSyncH5BackButton } from '@/store/h5-back-button'
import { navigateToParent, pinNavParent, resolveInferredParent } from '@/utils/nav-history'

function pinListPageParent() {
  const route = getCurrentPages().slice(-1)[0]?.route as string | undefined
  if (!route) return
  pinNavParent(resolveInferredParent(route))
  scheduleSyncH5BackButton()
}

/** 子包列表页：H5 返回按钮 + 物理返回键 */
export function useH5ListPageNav() {
  onLoad(() => {
    pinListPageParent()
  })

  onShow(() => {
    pinListPageParent()
  })

  onBackPress(() => {
    if (getCurrentPages().length > 1) return false
    navigateToParent()
    return true
  })
}
