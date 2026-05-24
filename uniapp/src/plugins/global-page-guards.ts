import type { App } from 'vue'
import { isWhiteRoute } from '@/config/route'
import { hasToken } from '@/utils/auth'
import { closeTopAppDialog } from '@/utils/dialog-back-guard'
import { isLeaveConfirmActive } from '@/composables/useUnsavedLeaveGuard'
import {
  installH5ShallowStackTrapIfNeeded,
  redirectAuthedAwayFromAuthPage,
  rememberShallowBackTarget,
  handleGlobalBackPress,
  getCurrentPageUrl,
} from '@/utils/navigate-back'
import { scheduleSyncH5BackButton } from '@/store/h5-back-button'
import { ensureNavParent } from '@/utils/nav-history'

const TAB_PAGE_ROUTES = new Set([
  'pages/index/index',
  'pages/work/index',
  'pages/message/index',
  'pages/mine/index',
])

/** 所有页面统一：H5 浅栈返回（先补全来源映射，再计算返回目标） */
export function installGlobalPageGuards(app: App) {
  app.mixin({
    onShow() {
      // #ifdef H5
      redirectAuthedAwayFromAuthPage()
      if (!hasToken()) return

      const pages = getCurrentPages()
      const route = pages[pages.length - 1]?.route as string | undefined
      if (!route || isWhiteRoute(`/${route}`)) return

      // Tab 页 onShow 不处理浅栈/返回按钮，避免盖住子包页的返回按钮状态
      if (TAB_PAGE_ROUTES.has(route)) return

      ensureNavParent(route, getCurrentPageUrl())
      rememberShallowBackTarget(route)
      installH5ShallowStackTrapIfNeeded()
      scheduleSyncH5BackButton()
      // #endif
    },
    onBackPress() {
      if (closeTopAppDialog()) return true
      if (isLeaveConfirmActive()) return true
      return handleGlobalBackPress()
    },
  })
}
