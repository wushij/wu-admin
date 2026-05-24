/** H5 刷新后从地址栏解析当前页面路径（不含 query） */
export function getH5LaunchPath(): string {
  // #ifdef H5
  const hash = window.location.hash || ''
  if (hash.startsWith('#/')) {
    return hash.slice(2).split('?')[0]
  }
  const pathname = window.location.pathname.replace(/^\//, '')
  if (pathname && pathname !== 'index.html') {
    return pathname.split('?')[0]
  }
  // #endif
  return ''
}

const AUTH_PAGES = new Set(['pages/login/index', 'pages/register/index'])

/** 已登录用户仅当落在登录/注册页时才应跳转到首页 */
export function shouldRedirectAuthedUserToHome(): boolean {
  const path = getH5LaunchPath()
  if (path) return AUTH_PAGES.has(path)

  const pages = getCurrentPages()
  const route = pages[pages.length - 1]?.route || ''
  return AUTH_PAGES.has(route)
}
