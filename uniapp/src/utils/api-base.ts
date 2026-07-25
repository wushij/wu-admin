/** 是否为仅开发环境可用的 API 地址（本机 / 局域网 IP） */
function isDevOnlyApiUrl(url: string): boolean {
  if (!url.startsWith('http://') && !url.startsWith('https://')) return false
  try {
    const { hostname } = new URL(url)
    if (hostname === 'localhost' || hostname === '127.0.0.1') return true
    if (hostname.startsWith('10.')) return true
    if (hostname.startsWith('192.168.')) return true
    const m = /^172\.(\d+)\./.exec(hostname)
    if (m) {
      const second = Number(m[1])
      if (second >= 16 && second <= 31) return true
    }
    return false
  } catch {
    return false
  }
}

/** 解析 API 根路径：H5 线上访问时忽略误打包的局域网地址，改走同域 /api */
export function resolveApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL || '/api'

  // #ifdef MP-WEIXIN
  if (import.meta.env.PROD && configured.startsWith('/')) {
    const origin = (import.meta.env.VITE_API_PRODUCTION_ORIGIN || '').replace(/\/$/, '')
    if (origin) return `${origin}${configured}`
  }
  // #endif

  // #ifdef H5
  if (typeof window !== 'undefined') {
    const { hostname } = window.location
    const isLocalHost = hostname === 'localhost' || hostname === '127.0.0.1'
    if (!isLocalHost && isDevOnlyApiUrl(configured)) {
      return '/api'
    }
  }
  // #endif

  return configured
}
