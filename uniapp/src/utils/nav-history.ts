import { getFromQueryParent, isPickerRoute } from '@/utils/nav-from'

const PARENT_KEY = 'wu_nav_parent_map'

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
  'pages-sub/system/org/edit': '/pages-sub/system/org/index',
  'pages-sub/system/org/index': '/pages/work/index',
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

function readParentMap(): Record<string, string> {
  // #ifdef H5
  if (typeof sessionStorage === 'undefined') return {}
  try {
    return JSON.parse(sessionStorage.getItem(PARENT_KEY) || '{}') as Record<string, string>
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
  if (typeof sessionStorage === 'undefined') return
  try {
    sessionStorage.setItem(PARENT_KEY, JSON.stringify(map))
  } catch {
    /* ignore */
  }
  // #endif
}

/** navigateTo 时记录：目标页 <- 来源页（刷新后仍有效） */
export function recordNavParent(targetUrl: string, fromUrl?: string) {
  // #ifdef H5
  if (!fromUrl || typeof sessionStorage === 'undefined') return
  const map = readParentMap()
  map[routeKey(targetUrl)] = normalizeNavUrl(fromUrl)
  writeParentMap(map)
  // #endif
}

/** 刷新后补全来源映射（不覆盖已有记录） */
export function ensureNavParent(route: string, currentUrl?: string) {
  // #ifdef H5
  const key = routeKey(currentUrl || route)
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

export function getNavParent(routeOrUrl?: string): string | null {
  // #ifdef H5
  const key = routeKey(routeOrUrl || '')
  if (!key) return null
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

  if (TAB_PAGES.has(path)) {
    uni.switchTab({ url: path })
    return
  }

  const route = normalizeRoute(path)
  const isModuleList = route.startsWith('pages-sub/') && route.endsWith('/index')
  if (isModuleList) {
    const tab = pathOnly(resolveInferredParent(route))
    if (TAB_PAGES.has(tab)) {
      uni.switchTab({
        url: tab,
        success: () => {
          uni.navigateTo({
            url: target,
            animationType: 'none',
            animationDuration: 0,
            fail: () => uni.redirectTo({ url: target }),
          })
        },
        fail: () => uni.redirectTo({ url: target }),
      })
      return
    }
  }

  uni.redirectTo({
    url: target,
    fail: () => uni.reLaunch({ url: target }),
  })
}

/**
 * H5 返回上一页：
 * - 栈深 > 1：uni.navigateBack
 * - 刷新/浅栈：session 来源 / URL from / 路由推断
 */
export function navigateToParent() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }

  const route = getCurrentRoute()
  const currentUrl = getCurrentPageUrl()
  ensureNavParent(route, currentUrl)
  openTargetPage(resolveBackTarget(route, currentUrl))
}

export { resolveInferredParent, DEFAULT_FALLBACK }
