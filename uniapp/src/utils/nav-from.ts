const PICKER_ROUTES = new Set([
  'pages-sub/system/tree-select',
  'pages-sub/system/user-select',
])

function normalizeNavUrl(url: string): string {
  const trimmed = url.trim()
  const qIndex = trimmed.indexOf('?')
  const pathPart = qIndex >= 0 ? trimmed.slice(0, qIndex) : trimmed
  const queryPart = qIndex >= 0 ? trimmed.slice(qIndex) : ''
  const path = pathPart.startsWith('/') ? pathPart : `/${pathPart}`
  return `${path}${queryPart}`
}

function pathOnly(url: string): string {
  const path = url.split('?')[0]
  return path.startsWith('/') ? path : `/${path}`
}

export function getCurrentPageUrl(): string {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1] as { route?: string; options?: Record<string, string> } | undefined
  if (!page?.route) return ''
  const path = pathOnly(page.route)
  const opts = page.options
  if (!opts) return path
  const qs = Object.entries(opts)
    .filter(([key, v]) => key !== 'from' && v != null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&')
  return qs ? `${path}?${qs}` : path
}

export function getCurrentRouteKey(): string {
  return getCurrentPages()[getCurrentPages().length - 1]?.route || ''
}

export function isPickerRoute(route: string): boolean {
  return PICKER_ROUTES.has(route.replace(/^\//, '').split('?')[0])
}

/** 打开选择页时附带来源，刷新后仍能正确返回 */
export function appendNavFromParam(url: string): string {
  const from = getCurrentPageUrl() || `/${getCurrentRouteKey()}`
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}from=${encodeURIComponent(from)}`
}

/** 从当前页 query.from 读取来源（F5 刷新后仍有效） */
export function getFromQueryParent(): string | null {
  const page = getCurrentPages().slice(-1)[0] as { options?: Record<string, string> } | undefined
  const raw = page?.options?.from
  if (!raw) return null
  try {
    return normalizeNavUrl(decodeURIComponent(String(raw)))
  } catch {
    return normalizeNavUrl(String(raw))
  }
}
