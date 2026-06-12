import { onShow, onBackPress } from '@dcloudio/uni-app'
import { syncH5BackButtonForRoute } from '@/store/h5-back-button'
import { navigateToParent } from '@/utils/nav-history'

/** 子包列表页：H5 返回按钮 + 物理返回键 */
export function useH5ListPageNav() {
  onShow(() => {
    const route = getCurrentPages().slice(-1)[0]?.route as string | undefined
    if (route) syncH5BackButtonForRoute(route)
  })

  onBackPress(() => {
    if (getCurrentPages().length > 1) return false
    navigateToParent()
    return true
  })
}
