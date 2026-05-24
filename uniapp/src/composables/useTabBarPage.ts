import { onShow } from '@dcloudio/uni-app'
import { useTabBarStore } from '@/store/tabBar'

/** Tab 页 onShow 时同步自定义 TabBar 选中态 */
export function useTabBarPage(index: number) {
  const tabBarStore = useTabBarStore()
  onShow(() => {
    tabBarStore.setSelected(index)
  })
}
