import { onBackPress, onShow } from '@dcloudio/uni-app'
import { toValue, type MaybeRefOrGetter } from 'vue'
import { showConfirm } from '@/utils/app-dialog'
import { closeTopAppDialog } from '@/utils/dialog-back-guard'
import {
  registerPageShallowFallback,
  safeNavigateBack,
  shouldUseFallbackBack,
} from '@/utils/navigate-back'

const LEAVE_CONFIRM = {
  title: '提示',
  content: '当前有未保存的修改，确定离开吗？',
  confirmText: '离开',
  cancelText: '继续编辑',
} as const

export function confirmUnsavedLeave() {
  return showConfirm(LEAVE_CONFIRM)
}

/** 拦截返回键：有未保存修改时弹窗确认（对齐 PC 系统配置） */
export function useUnsavedLeaveGuard(
  isDirty: MaybeRefOrGetter<boolean>,
  options?: { fallbackUrl?: string },
) {
  onShow(() => {
    if (options?.fallbackUrl) registerPageShallowFallback(options.fallbackUrl)
  })

  onBackPress(() => {
    if (closeTopAppDialog()) return true

    if (!toValue(isDirty)) {
      if (shouldUseFallbackBack() && options?.fallbackUrl) {
        safeNavigateBack(options.fallbackUrl)
        return true
      }
      return false
    }

    confirmUnsavedLeave().then((result) => {
      if (result.confirmed) safeNavigateBack(options?.fallbackUrl)
    })
    return true
  })
}
