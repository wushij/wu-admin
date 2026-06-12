/** 无需登录即可访问的页面路径（与 pages.json 同步） */
export const ROUTE_WHITE_LIST = [
  '/pages/login/index',
  '/pages/login/forgot-password',
  '/pages/register/index',
] as const

export function isWhiteRoute(url: string): boolean {
  const path = url.split('?')[0]
  return ROUTE_WHITE_LIST.some((item) => path === item || path.endsWith(item))
}
