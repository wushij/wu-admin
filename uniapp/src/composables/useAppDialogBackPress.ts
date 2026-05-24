import { onBackPress } from '@dcloudio/uni-app'
import { closeTopAppDialog } from '@/utils/dialog-back-guard'

/** 页面内拦截返回键：优先关闭自定义弹窗，避免误触退出或跳页 */
export function useAppDialogBackPress() {
  onBackPress(() => (closeTopAppDialog() ? true : false))
}
