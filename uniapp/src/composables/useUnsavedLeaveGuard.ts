import { onBackPress, onShow } from '@dcloudio/uni-app'
import { closeTopAppDialog, isAppDialogOpen } from '@/utils/dialog-back-guard'
import {
  registerPageShallowFallback,
  safeNavigateBack,
  shouldUseFallbackBack,
  isFormLeaveActive,
} from '@/utils/navigate-back'

let suppressLeaveUntil = 0

function markLeaveDialogDismissed() {
  suppressLeaveUntil = Date.now() + 500
}

function scheduleNavigateBack(fallbackUrl?: string) {
  setTimeout(() => safeNavigateBack(fallbackUrl), 0)
}

/** 弹窗/表单页返回处理进行中（供全局 onBackPress 避免冲突） */
export function isLeaveConfirmActive() {
  return isAppDialogOpen() || isFormLeaveActive()
}

/** 表单页返回：关闭弹层；浅栈时回列表。栈深>1 时不拦截，一次返回即可退出 */
export function useUnsavedLeaveGuard(options?: { fallbackUrl?: string }) {
  onShow(() => {
    if (options?.fallbackUrl) registerPageShallowFallback(options.fallbackUrl)
  })

  onBackPress(() => {
    if (closeTopAppDialog()) {
      markLeaveDialogDismissed()
      return true
    }
    if (isAppDialogOpen()) return true
    if (Date.now() < suppressLeaveUntil) return true

    if (options?.fallbackUrl && shouldUseFallbackBack()) {
      scheduleNavigateBack(options.fallbackUrl)
      return true
    }
    return false
  })
}
