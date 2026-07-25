import { getFromQueryParent, isPickerRoute } from '@/utils/nav-from'
import { scheduleSyncH5BackButton } from '@/store/h5-back-button'
import { suppressPopstate } from '@/utils/nav-transition'

const PARENT_KEY = 'wu_nav_parent_map'
const PINNED_KEY = 'wu_nav_pinned_map'

const TAB_PAGES = new Set([
  '/pages/index/index',
  '/pages/work/index',
  '/pages/message/index',
  '/pages/mine/index',
])

const DEFAULT_FALLBACK = '/pages/work/index'

const EXPLICIT_PARENT: Record<string, string> = {
  'pages-sub/system/user/detail': '/pages-sub/system/user/index',
  'pages-sub/system/user/edit': '/pages-sub/system/user/index',
  'pages-sub/system/user/index': '/pages/work/index',
  'pages-sub/system/role/edit': '/pages-sub/system/role/index',
  'pages-sub/system/role/index': '/pages/work/index',
  'pages-sub/system/org/edit': '/pages-sub/system/org/index',
  'pages-sub/system/org/index': '/pages/work/index',
  'pages-sub/system/menu/edit': '/pages-sub/system/menu/index',
  'pages-sub/system/menu/index': '/pages/work/index',
  'pages-sub/system/dict/type-form': '/pages-sub/system/dict/index',
  'pages-sub/system/dict/data-form': '/pages-sub/system/dict/index',
  'pages-sub/system/dict/index': '/pages/work/index',
  'pages-sub/system/file/index': '/pages/work/index',
  'pages-sub/system/config/index': '/pages/work/index',
  'pages-sub/mine/mobile-bind': '/pages-sub/mine/profile',
  'pages-sub/mine/email-bind': '/pages-sub/mine/profile',
  'pages-sub/mine/profile': '/pages/mine/index',
  'pages-sub/mine/password': '/pages/mine/index',
  'pages-sub/mine/login-logs': '/pages/mine/index',
  'pages-sub/mine/account': '/pages/mine/index',
  'pages-sub/mine/about': '/pages/mine/index',
  'pages-sub/log/oper-log': '/pages/work/index',
  'pages-sub/log/login-log': '/pages/work/index',
  'pages-sub/monitor/online': '/pages/work/index',
  'pages-sub/monitor/server': '/pages/work/index',
  'pages-sub/monitor/job': '/pages/work/index',
  'pages-sub/monitor/cache': '/pages/work/index',
  'pages-sub/monitor/api-access': '/pages/work/index',
}

function normalizeRoute(route: string): string {
  return route.replace(/^\//, '').split('?')[0]
}

function resolveInferredParent(route: string): string {
  const normalized = normalizeRoute(route)

  if (isPickerRoute(normalized)) {
    const from = getFromQueryParent()
    if (from) return from
  }

  if (EXPLICIT_PARENT[normalized]) return EXPLICIT_PARENT[normalized]

  if (normalized.startsWith('pages-sub/msg/')) return '/pages/message/index'
  if (normalized.startsWith('pages-sub/mine/')) return '/pages/mine/index'

  const parts = normalized.split('/')
  const last = parts[parts.length - 1]
  if (['detail', 'edit', 'create', 'form', 'preview'].includes(last) && parts.length >= 2) {
    return `/${[...parts.slice(0, -1), 'index'].join('/')}`
  }

  if (normalized.startsWith('pages-sub/')) return DEFAULT_FALLBACK
  return DEFAULT_FALLBACK
}

function routeKey(url: string): string {
  return url.split('?')[0].replace(/^\//, '')
}

function pathOnly(url: string): string {
  const path = url.split('?')[0]
  return path.startsWith('/') ? path : `/${path}`
}

export function normalizeNavUrl(url: string): string {
  const trimmed = url.trim()
  const qIndex = trimmed.indexOf('?')
  const pathPart = qIndex >= 0 ? trimmed.slice(0, qIndex) : trimmed
  const queryPart = qIndex >= 0 ? trimmed.slice(qIndex) : ''
  const path = pathPart.startsWith('/') ? pathPart : `/${pathPart}`
  return `${path}${queryPart}`
}

function readPinnedMap(): Record<string, string> {
  // #ifdef H5
  if (typeof localStorage === 'undefined') return {}
  try {
    return JSON.parse(localStorage.getItem(PINNED_KEY) || '{}') as Record<string, string>
  } catch {
    return {}
  }
  // #endif
  // #ifndef H5
  return {}
  // #endif
}

function writePinnedMap(map: Record<string, string>) {
  // #ifdef H5
  if (typeof localStorage === 'undefined') return
  try {
    localStorage.setItem(PINNED_KEY, JSON.stringify(map))
  } catch {
    /* ignore */
  }
  // #endif
}

function readParentMap(): Record<string, string> {
  // #ifdef H5
  if (typeof localStorage === 'undefined') return {}
  try {
    return JSON.parse(localStorage.getItem(PARENT_KEY) || '{}') as Record<string, string>
  } catch {
    return {}
  }
  // #endif
  // #ifndef H5
  return {}
  // #endif
}

function writeParentMap(map: Record<string, string>) {
  // #ifdef H5
  if (typeof localStorage === 'undefined') return
  try {
    localStorage.setItem(PARENT_KEY, JSON.stringify(map))
  } catch {
    /* ignore */
  }
  // #endif
}

/** navigateTo 时记录：目标页 <- 来源页（刷新后仍有效） */
export function recordNavParent(targetUrl: string, fromUrl?: string) {
  // #ifdef H5
  if (!fromUrl || typeof localStorage === 'undefined') return
  const map = readParentMap()
  map[routeKey(targetUrl)] = normalizeNavUrl(fromUrl)
  writeParentMap(map)
  // #endif
}

/** 刷新后补全来源映射（不覆盖已有记录） */
export function ensureNavParent(route: string, currentUrl?: string) {
  // #ifdef H5
  const key = routeKey(currentUrl || route)
  if (readPinnedMap()[key]) return
  const map = readParentMap()
  if (map[key]) return

  const from = getFromQueryParent()
  if (from) {
    map[key] = from
    writeParentMap(map)
    return
  }

  if (isPickerRoute(normalizeRoute(route))) return

  map[key] = resolveInferredParent(route)
  writeParentMap(map)
  // #endif
}

/** 强制写入当前页的返回目标（刷新后仍有效，供 navigateToParent 使用） */
export function pinNavParent(parentUrl: string) {
  // #ifdef H5
  const route = getCurrentRoute()
  if (!route) return
  const parent = normalizeNavUrl(parentUrl)
  const keys = [routeKey(getCurrentPageUrl() || route), routeKey(route)]
  const parentMap = readParentMap()
  const pinnedMap = readPinnedMap()
  keys.forEach((key) => {
    if (!key) return
    parentMap[key] = parent
    pinnedMap[key] = parent
  })
  writeParentMap(parentMap)
  writePinnedMap(pinnedMap)
  // #endif
}

export function getNavParent(routeOrUrl?: string): string | null {
  // #ifdef H5
  const key = routeKey(routeOrUrl || '')
  if (!key) return null
  const pinned = readPinnedMap()[key]
  if (pinned) return pinned
  return readParentMap()[key] || null
  // #endif
  // #ifndef H5
  return null
  // #endif
}

function getCurrentPageUrl(): string {
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

function getCurrentRoute(): string {
  return getCurrentPages()[getCurrentPages().length - 1]?.route || ''
}

/** 统一解析 H5 刷新/浅栈时的返回目标 */
export function resolveBackTarget(route?: string, currentUrl?: string): string {
  const r = normalizeRoute(route || getCurrentRoute())
  const url = currentUrl || getCurrentPageUrl()

  return (
    getNavParent(url) ||
    getNavParent(r) ||
    getFromQueryParent() ||
    resolveInferredParent(r)
  )
}

function openTargetPage(targetUrl: string) {
  const target = normalizeNavUrl(targetUrl)
  const path = pathOnly(target)
  const route = normalizeRoute(path)

  const finish = () => scheduleSyncH5BackButton()

  const go = (fn: () => void) => {
    suppressPopstate()
    fn()
  }

  if (TAB_PAGES.has(path)) {
    go(() => uni.switchTab({ url: path, complete: finish }))
    return
  }

  const isModuleList = route.startsWith('pages-sub/') && route.endsWith('/index')
  const currentRoute = normalizeRoute(getCurrentRoute())
  const alreadyInSubPackage = currentRoute.startsWith('pages-sub/')

  if (isModuleList && alreadyInSubPackage) {
    go(() =>
      uni.redirectTo({
        url: target,
        complete: finish,
        fail: () => uni.reLaunch({ url: target, complete: finish }),
      }),
    )
    return
  }

  if (isModuleList) {
    const tab = pathOnly(resolveInferredParent(route))
    if (TAB_PAGES.has(tab)) {
      go(() =>
        uni.switchTab({
          url: tab,
          success: () => {
            suppressPopstate()
            uni.navigateTo({
              url: target,
              animationType: 'none',
              animationDuration: 0,
              complete: finish,
              fail: () =>
                uni.redirectTo({
                  url: target,
                  complete: finish,
                }),
            })
          },
          fail: () =>
            uni.redirectTo({
              url: target,
              complete: finish,
            }),
        }),
      )
      return
    }
  }

  go(() =>
    uni.redirectTo({
      url: target,
      complete: finish,
      fail: () => uni.reLaunch({ url: target, complete: finish }),
    }),
  )
}

/**
 * H5 返回上一页：
 * - 栈深 > 1：uni.navigateBack
 * - 刷新/浅栈：session 来源 / URL from / 路由推断
 */
let navigatingParent = false

export function navigateToParent() {
  if (navigatingParent) return

  const pages = getCurrentPages()
  const route = getCurrentRoute()
  const currentUrl = getCurrentPageUrl()

  if (pages.length > 1) {
    navigatingParent = true
    uni.navigateBack({
      complete: () => {
        navigatingParent = false
        scheduleSyncH5BackButton()
      },
      fail: () => {
        navigatingParent = false
        ensureNavParent(route, currentUrl)
        openTargetPage(resolveBackTarget(route, currentUrl))
      },
    })
    return
  }

  ensureNavParent(route, currentUrl)
  openTargetPage(resolveBackTarget(route, currentUrl))
}

export { resolveInferredParent, DEFAULT_FALLBACK }
