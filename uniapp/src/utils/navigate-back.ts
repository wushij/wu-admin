import { isWhiteRoute } from '@/config/route'
import { hasToken } from '@/utils/auth'
import { hideH5RebuildMask, showH5RebuildMask } from '@/utils/h5-shallow-stack-mask'
import { getH5LaunchPath } from '@/utils/launch-route'
import {
  navigateToParent,
  resolveBackTarget,
  resolveInferredParent,
  pinNavParent,
  ensureNavParent,
} from '@/utils/nav-history'
import { getFromQueryParent, isPickerRoute } from '@/utils/nav-from'
import { scheduleSyncH5BackButton } from '@/store/h5-back-button'
import { dialogState, forceCollapseOverlayHistory } from '@/store/dialog'
import { shouldSuppressPopstate, suppressPopstate } from '@/utils/nav-transition'

const TAB_PAGES = new Set([
  '/pages/index/index',
  '/pages/work/index',
  '/pages/message/index',
  '/pages/mine/index',
])

const AUTH_ROUTES = new Set([
  'pages/login/index',
  'pages/login/forgot-password',
  'pages/register/index',
])

const SHALLOW_BACK_KEY = 'wu_shallow_back_target'

export const DEFAULT_SHALLOW_FALLBACK = '/pages/work/index'

function routeKey(url: string): string {
  return url.split('?')[0].replace(/^\//, '')
}

function pathOnly(url: string): string {
  const path = url.split('?')[0]
  return path.startsWith('/') ? path : `/${path}`
}

function normalizePageUrl(url: string): string {
  const trimmed = url.trim()
  const qIndex = trimmed.indexOf('?')
  const pathPart = qIndex >= 0 ? trimmed.slice(0, qIndex) : trimmed
  const queryPart = qIndex >= 0 ? trimmed.slice(qIndex) : ''
  const path = pathPart.startsWith('/') ? pathPart : `/${pathPart}`
  return `${path}${queryPart}`
}

export function getCurrentPageUrl(): string {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1] as { route?: string; options?: Record<string, string> } | undefined
  if (!page?.route) return ''
  const path = pathOnly(page.route)
  const opts = page.options
  if (!opts) return path
  const qs = Object.entries(opts)
    .filter(([, v]) => v != null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&')
  return qs ? `${path}?${qs}` : path
}

function needsStackRebuild(targetUrl: string): boolean {
  const targetPath = pathOnly(targetUrl)
  const last = targetPath.split('/').pop() || ''
  if (targetPath.includes('pages-sub/') && last === 'index') return true
  return (
    ['detail', 'edit', 'create', 'form', 'preview'].includes(last) ||
    last.endsWith('-detail') ||
    last.endsWith('-form')
  )
}

const fallbackByRoute = new Map<string, string>()

let trapRoute = ''
let handlingShallowBack = false
export function markNativeNavigateBack() {
  suppressPopstate()
}

export function isAuthRoute(route: string) {
  const path = route.replace(/^\//, '').split('?')[0]
  return AUTH_ROUTES.has(path)
}

export function getCurrentRoute(): string {
  const pages = getCurrentPages()
  return pages[pages.length - 1]?.route || ''
}

export function getActiveRoutePath(): string {
  return getH5LaunchPath() || getCurrentRoute()
}

export function registerPageShallowFallback(fallbackUrl: string) {
  const route = getCurrentRoute()
  if (route) fallbackByRoute.set(route, fallbackUrl)
  pinNavParent(fallbackUrl)
}

/** 根据 route 推断上一级（扩展规则，供非 H5 或兜底） */
export function resolveFallbackForRoute(route: string): string {
  const normalized = route.replace(/^\//, '').split('?')[0]
  if (fallbackByRoute.has(normalized)) return fallbackByRoute.get(normalized)!

  if (isPickerRoute(normalized)) {
    const from = getFromQueryParent()
    if (from) return from
  }

  const explicit: Record<string, string> = {
    'pages-sub/monitor/online-detail': '/pages-sub/monitor/online',
    'pages-sub/monitor/job-form': '/pages-sub/monitor/job',
    'pages-sub/monitor/online': '/pages/work/index',
    'pages-sub/monitor/server': '/pages/work/index',
    'pages-sub/monitor/job': '/pages/work/index',
    'pages-sub/monitor/cache': '/pages/work/index',
    'pages-sub/monitor/api-access': '/pages/work/index',
    'pages-sub/msg/chat/group-form': '/pages-sub/msg/chat/index',
    'pages-sub/msg/chat/group-detail': '/pages-sub/msg/chat/index',
    'pages-sub/msg/chat/detail': '/pages-sub/msg/chat/index',
    'pages-sub/system/role/menu-assign': '/pages-sub/system/role/index',
    'pages-sub/system/dict/type-form': '/pages-sub/system/dict/index',
    'pages-sub/system/dict/data-form': '/pages-sub/system/dict/index',
    'pages-sub/system/announce/detail': '/pages-sub/system/announce/index',
    'pages-sub/system/announce/form': '/pages-sub/system/announce/index',
    'pages-sub/system/file/preview': '/pages-sub/system/file/index',
    'pages-sub/system/user/detail': '/pages-sub/system/user/index',
    'pages-sub/system/user/index': '/pages/work/index',
    'pages-sub/mine/mobile-bind': '/pages-sub/mine/profile',
    'pages-sub/mine/profile': '/pages/mine/index',
    'pages-sub/log/oper-log': '/pages/work/index',
    'pages-sub/log/login-log': '/pages/work/index',
  }
  if (explicit[normalized]) return explicit[normalized]

  return resolveInferredParent(normalized)
}

export function resolveShallowBackTarget(route: string): string {
  const normalized = route.replace(/^\//, '').split('?')[0]
  if (fallbackByRoute.has(normalized)) return fallbackByRoute.get(normalized)!
  return resolveBackTarget(route, getCurrentPageUrl())
}

export function rememberShallowBackTarget(route: string) {
  const fallback = resolveShallowBackTarget(route)
  trapRoute = route
  // #ifdef H5
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.setItem(SHALLOW_BACK_KEY, fallback)
    sessionStorage.setItem('wu_shallow_back_from', route)
  }
  // #endif
  return fallback
}

function readShallowBackTarget(): string {
  // #ifdef H5
  if (typeof sessionStorage !== 'undefined') {
    const stored = sessionStorage.getItem(SHALLOW_BACK_KEY)
    if (stored) return stored
  }
  // #endif
  const route = trapRoute || getCurrentRoute()
  return route ? resolveShallowBackTarget(route) : DEFAULT_SHALLOW_FALLBACK
}

export function getShallowBackFallback(): string {
  return readShallowBackTarget()
}

export function shouldUseFallbackBack() {
  return getCurrentPages().length <= 1
}

export function redirectAuthedAwayFromAuthPage() {
  if (!hasToken()) return
  const path = getActiveRoutePath()
  if (!path || !isAuthRoute(path)) return
  navigateToFallback(readShallowBackTarget(), { rebuildStack: true })
}

export function installH5ShallowStackTrapIfNeeded() {
  if (typeof window === 'undefined' || !hasToken() || !shouldUseFallbackBack()) return

  const route = getCurrentRoute()
  if (!route || isAuthRoute(route) || isWhiteRoute(`/${route}`)) return
  if (isFormChildLeaveRoute(route)) return

  const fallback = rememberShallowBackTarget(route)
  if (window.history.state?.wuAdminShallowTrap && window.history.state?.route === route) return

  trapRoute = route
  window.history.pushState({ wuAdminShallowTrap: 1, route, fallback }, '', window.location.href)
}

function clearTrapRoute() {
  trapRoute = ''
  // #ifdef H5
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.removeItem(SHALLOW_BACK_KEY)
    sessionStorage.removeItem('wu_shallow_back_from')
  }
  // #endif
}

export function handleGlobalBackPress(): boolean {
  if (handlingShallowBack || formLeaveInProgress) return true
  if (!hasToken()) return false
  if (getCurrentPages().length > 1) return false

  const route = getCurrentRoute()
  if (!route || isAuthRoute(route)) return false

  setTimeout(() => {
    if (handlingShallowBack) return
    handlingShallowBack = true
    navigateToParent()
    setTimeout(() => {
      handlingShallowBack = false
    }, 400)
  }, 0)
  return true
}

function performShallowBack() {
  if (handlingShallowBack) return
  handlingShallowBack = true
  const fallback = readShallowBackTarget()
  clearTrapRoute()
  navigateToFallback(fallback, { rebuildStack: needsStackRebuild(fallback) })
  setTimeout(() => {
    handlingShallowBack = false
  }, 400)
}

export function handleShallowStackPopstate() {
  if (typeof window === 'undefined' || handlingShallowBack || formLeaveInProgress) return

  const path = getActiveRoutePath()
  if (hasToken() && path && isAuthRoute(path)) {
    performShallowBack()
    return
  }
  if (!hasToken()) {
    clearTrapRoute()
    return
  }
  if (getCurrentPages().length > 1) {
    clearTrapRoute()
    return
  }

  const route = getCurrentRoute()
  if (!route || isAuthRoute(route)) return
  if (shouldSuppressPopstate()) {
    clearTrapRoute()
    return
  }

  clearTrapRoute()
  handlingShallowBack = true
  setTimeout(() => {
    navigateToParent()
    setTimeout(() => {
      handlingShallowBack = false
    }, 400)
  }, 0)
}

function isFormChildRoute(route: string): boolean {
  const normalized = route.replace(/^\//, '').split('?')[0]
  const last = normalized.split('/').pop() || ''
  return (
    last === 'create' ||
    last === 'edit' ||
    last === 'form' ||
    last.endsWith('-form')
  )
}

/** 新建/编辑表单页：离开时应 redirect 到列表，避免 H5 历史栈残留 */
export function isFormChildLeaveRoute(route?: string): boolean {
  return isFormChildRoute(route || getCurrentRoute())
}

let formLeaveInProgress = false

export function isFormLeaveActive() {
  return formLeaveInProgress
}

function prepareFormPageLeave(collapseOverlay = false) {
  clearTrapRoute()
  suppressPopstate(1000)
  dialogState.historyLocked = false
  if (collapseOverlay) forceCollapseOverlayHistory()
  formLeaveInProgress = true
  setTimeout(() => {
    formLeaveInProgress = false
  }, 1000)
}

function formLeaveToParent(fallbackUrl: string, collapseOverlay = false) {
  prepareFormPageLeave(collapseOverlay)
  const target = normalizePageUrl(fallbackUrl)
  const pages = getCurrentPages()

  if (pages.length > 1) {
    markNativeNavigateBack()
    uni.navigateBack({
      delta: 1,
      complete: () => scheduleSyncH5BackButton(),
      fail: () =>
        uni.redirectTo({
          url: target,
          complete: () => scheduleSyncH5BackButton(),
          fail: () => uni.reLaunch({ url: target, complete: () => scheduleSyncH5BackButton() }),
        }),
    })
    return
  }

  uni.redirectTo({
    url: target,
    complete: () => scheduleSyncH5BackButton(),
    fail: () => uni.reLaunch({ url: target, complete: () => scheduleSyncH5BackButton() }),
  })
}

export function safeNavigateBack(fallbackUrl?: string, options?: { collapseOverlay?: boolean }) {
  const route = getCurrentRoute()

  if (fallbackUrl && isFormChildRoute(route)) {
    formLeaveToParent(fallbackUrl, options?.collapseOverlay ?? false)
    return
  }

  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack({
      fail: () => {
        if (fallbackUrl) {
          navigateToFallback(fallbackUrl)
        } else {
          navigateToParent()
        }
      },
    })
    return
  }
  if (fallbackUrl) {
    navigateToFallback(fallbackUrl, { rebuildStack: needsStackRebuild(fallbackUrl) })
    return
  }
  navigateToParent()
}

function rebuildStackAndOpen(parentUrl: string, targetUrl: string) {
  const parent = pathOnly(parentUrl)
  const target = normalizePageUrl(targetUrl)
  const finish = () => hideH5RebuildMask()

  if (parent === pathOnly(target) || TAB_PAGES.has(pathOnly(target))) {
    if (TAB_PAGES.has(pathOnly(target))) {
      uni.switchTab({ url: pathOnly(target), complete: finish })
    } else {
      uni.redirectTo({
        url: target,
        complete: finish,
        fail: () => uni.reLaunch({ url: target, complete: finish }),
      })
    }
    return
  }

  showH5RebuildMask()
  const openTarget = () => {
    uni.navigateTo({
      url: target,
      animationType: 'none',
      animationDuration: 0,
      complete: finish,
      fail: () =>
        uni.redirectTo({
          url: target,
          complete: finish,
          fail: () => uni.reLaunch({ url: target, complete: finish }),
        }),
    })
  }

  if (TAB_PAGES.has(parent)) {
    uni.switchTab({
      url: parent,
      success: () => openTarget(),
      fail: () =>
        uni.redirectTo({
          url: target,
          complete: finish,
          fail: () => uni.reLaunch({ url: target, complete: finish }),
        }),
    })
    return
  }

  uni.redirectTo({
    url: parent,
    success: () => openTarget(),
    fail: () =>
      uni.redirectTo({
        url: target,
        complete: finish,
        fail: () => uni.reLaunch({ url: target, complete: finish }),
      }),
  })
}

export function navigateToFallback(url: string, options?: { rebuildStack?: boolean }) {
  const target = normalizePageUrl(url)
  const path = pathOnly(url)
  const finish = () => scheduleSyncH5BackButton()
  if (TAB_PAGES.has(path)) {
    suppressPopstate()
    uni.switchTab({ url: path, complete: finish })
    return
  }
  if (options?.rebuildStack) {
    const route = path.replace(/^\//, '')
    const parentPath = resolveFallbackForRoute(route)
    suppressPopstate()
    rebuildStackAndOpen(parentPath, target)
    finish()
    return
  }
  suppressPopstate()
  uni.redirectTo({
    url: target,
    complete: finish,
    fail: () => uni.reLaunch({ url: target, complete: finish }),
  })
}

export function patchNavigateBackFail(args?: { fail?: (err: unknown) => void }) {
  const route = getCurrentRoute()
  if (!route || !args) return
  const origFail = args.fail
  args.fail = (err: unknown) => {
    if (getCurrentPages().length > 1) {
      const currentUrl = getCurrentPageUrl()
      ensureNavParent(route, currentUrl)
      navigateToFallback(resolveBackTarget(route, currentUrl))
    } else {
      navigateToParent()
    }
    origFail?.(err)
  }
}
