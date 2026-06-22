import { isWhiteRoute } from '@/config/route'
import { hasToken } from '@/utils/auth'
import { getCurrentPageUrl, getCurrentRoute } from '@/utils/navigate-back'
import { recordNavParent } from '@/utils/nav-history'
import { useUserStore } from '@/store/user'
import { mobileQuickEntries } from '@/constants/quickEntries'

const TAB_ROUTES = new Set([
  'pages/index/index',
  'pages/work/index',
  'pages/message/index',
  'pages/mine/index',
])

let deferFromTabOnce = false

export function setupRouteGuard() {
  const pathPermMap = new Map<string, string>()
  for (const e of mobileQuickEntries) {
    if (e.path && e.permission) pathPermMap.set(e.path, e.permission)
  }

  uni.addInterceptor('navigateTo', {
    invoke(args: { url: string; events?: Record<string, (...args: unknown[]) => void> }) {
      if (deferFromTabOnce) {
        deferFromTabOnce = false
        return true
      }

      const path = args.url.split('?')[0]
      if (isWhiteRoute(path)) return true
      if (!hasToken()) {
        uni.reLaunch({ url: '/pages/login/index' })
        return false
      }

      const requiredPerm = pathPermMap.get(path)
      if (requiredPerm) {
        const userStore = useUserStore()
        if (!userStore.hasMenuPermission(requiredPerm)) {
          uni.showToast({ title: '暂无权限访问', icon: 'none' })
          // 优先回到工作台功能入口页
          uni.switchTab({ url: '/pages/work/index' })
          return false
        }
      }

      const fromRoute = getCurrentRoute()
      const fromUrl = getCurrentPageUrl() || (fromRoute ? `/${fromRoute}` : '')
      recordNavParent(args.url, fromUrl)

      // #ifdef H5
      if (fromRoute && TAB_ROUTES.has(fromRoute)) {
        deferFromTabOnce = true
        setTimeout(() => uni.navigateTo(args), 0)
        return false
      }
      // #endif

      return true
    },
  })

  const redirectMethods = ['redirectTo', 'reLaunch', 'switchTab'] as const
  redirectMethods.forEach((method) => {
    uni.addInterceptor(method, {
      invoke(args: { url: string }) {
        const path = args.url.split('?')[0]
        if (isWhiteRoute(path)) return true
        if (!hasToken()) {
          uni.reLaunch({ url: '/pages/login/index' })
          return false
        }
        const requiredPerm = pathPermMap.get(path)
        if (requiredPerm) {
          const userStore = useUserStore()
          if (!userStore.hasMenuPermission(requiredPerm)) {
            uni.showToast({ title: '暂无权限访问', icon: 'none' })
            uni.switchTab({ url: '/pages/work/index' })
            return false
          }
        }
        return true
      },
    })
  })
}

export async function bootstrapSession(refreshUser: () => Promise<void>) {
  if (!hasToken()) return
  try {
    await refreshUser()
  } catch {
    /* refreshUser 内部会 logout */
  }
}
