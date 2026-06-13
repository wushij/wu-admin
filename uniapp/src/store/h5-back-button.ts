import { reactive } from 'vue'
import { getH5LaunchPath } from '@/utils/launch-route'

export const h5BackButtonState = reactive({
  visible: false,
})

function normalizeRouteKey(route?: string): string {
  return (route || '').replace(/^\//, '').split('?')[0]
}

/** 读取当前应展示返回按钮的路由（优先地址栏，避免 redirect 后页面栈滞后） */
function readActiveRoute(): string {
  const launch = normalizeRouteKey(getH5LaunchPath())
  if (launch.startsWith('pages-sub/')) return launch

  const pages = getCurrentPages()
  const top = normalizeRouteKey((pages[pages.length - 1] as { route?: string } | undefined)?.route)
  return top
}

function isListIndexRoute(route: string): boolean {
  return route.startsWith('pages-sub/') && route.endsWith('/index')
}

/** 根据当前路由同步 H5 自定义返回按钮显隐 */
export function syncH5BackButtonForRoute() {
  // #ifdef H5
  const route = readActiveRoute()
  if (!route.startsWith('pages-sub/')) {
    h5BackButtonState.visible = false
    return
  }

  const shallow = getCurrentPages().length <= 1
  // 栈深 > 1 时系统导航栏自带返回；列表页刷新后用页面内蓝色返回
  if (!shallow || isListIndexRoute(route)) {
    h5BackButtonState.visible = false
    return
  }

  h5BackButtonState.visible = true
  // #endif
}

let syncSeq = 0

/** 导航/刷新后延迟多次同步，避免 Tab 页 onShow 抢先关掉返回按钮 */
export function scheduleSyncH5BackButton() {
  // #ifdef H5
  const seq = ++syncSeq
  const run = () => {
    if (seq !== syncSeq) return
    syncH5BackButtonForRoute()
  }
  run()
  setTimeout(run, 0)
  setTimeout(run, 80)
  setTimeout(run, 200)
  setTimeout(run, 450)
  // #endif
}
