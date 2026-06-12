import Request from 'luch-request'
import type { ApiResult } from '@/types/api'
import { isApiSuccessCode } from '@/utils/api-response'
import { getToken, removeToken } from '@/utils/auth'
import { isWhiteRoute } from '@/config/route'
import { REQUEST_TIMEOUT } from '@/config/request'
import { extractApiErrorMessage, markErrorToastShown, showGlobalErrorToast } from '@/plugins/global-error-handler'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const AUTH_PUBLIC_SUFFIXES = [
  '/auth/login',
  '/auth/register',
  '/auth/captcha',
  '/auth/config',
  '/auth/sms-code',
  '/auth/forgot-password/check',
  '/auth/forgot-password/sms-code',
  '/auth/forgot-password/reset',
]

function isAuthPublicUrl(url?: string): boolean {
  if (!url) return false
  const path = url.split('?')[0]
  return AUTH_PUBLIC_SUFFIXES.some((suffix) => path === suffix || path.endsWith(suffix))
}

let lastForbiddenToastAt = 0

function showForbiddenOnce(message: string) {
  const now = Date.now()
  if (now - lastForbiddenToastAt < 2000) return
  lastForbiddenToastAt = now
  uni.showToast({ title: message, icon: 'none' })
}

function clearSessionAndRedirectLogin() {
  removeToken()
  uni.reLaunch({ url: '/pages/login/index' })
}

const http = new Request({
  baseURL: BASE_URL,
  timeout: REQUEST_TIMEOUT,
  header: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
  // 4xx/5xx 若仍返回 JSON 体，由拦截器统一解析 message
  validateStatus: (status) => status >= 200 && status < 600,
})

http.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.header = {
        ...config.header,
        Authorization: token,
      }
    }
    return config
  },
  (error) => Promise.reject(error),
)

// luch-request 响应拦截器返回 ApiResult 而非 HttpResponse
http.interceptors.response.use(
  ((response: { data: ApiResult; config?: { url?: string; silent403?: boolean } }) => {
    const res = response.data
    const code = res?.code
    const message = res.message || res.msg

    if (isApiSuccessCode(code)) {
      return res
    }

    if (code === 401) {
      const url = (response.config?.url || '') as string
      if (isAuthPublicUrl(url)) {
        uni.showToast({ title: message || '认证失败', icon: 'none' })
        return Promise.reject(new Error(message || '未授权'))
      }
      uni.showToast({ title: '登录已过期', icon: 'none' })
      clearSessionAndRedirectLogin()
      return Promise.reject(new Error(message || '未授权'))
    }

    if (code === 403) {
      const cfg = response.config
      if (!cfg?.silent403) {
        showForbiddenOnce(message || '权限不足')
      }
      return Promise.reject(new Error('权限不足'))
    }

    if (code === 429) {
      uni.showToast({ title: message || '操作过于频繁', icon: 'none' })
      return Promise.reject(new Error(message || '操作过于频繁'))
    }

    const msg = message || '请求失败'
    showGlobalErrorToast(msg)
    const err = new Error(msg)
    markErrorToastShown(err)
    return Promise.reject(err)
  }) as unknown as Parameters<typeof http.interceptors.response.use>[0],
  (error: { data?: ApiResult; statusCode?: number; errMsg?: string }) => {
    const msg = extractApiErrorMessage(error, '网络异常')
    showGlobalErrorToast(msg)
    const err = new Error(msg)
    markErrorToastShown(err)
    return Promise.reject(err)
  },
)

export default http

export const get = <T = unknown>(url: string, params?: object) =>
  http.get(url, { params }) as Promise<ApiResult<T>>

export const post = <T = unknown>(
  url: string,
  data?: unknown,
  config?: { params?: Record<string, unknown> },
) =>
  http.post(url, data as Record<string, unknown> | undefined, {
    params: config?.params,
  }) as Promise<ApiResult<T>>

export const put = <T = unknown>(
  url: string,
  data?: unknown,
  config?: { params?: Record<string, unknown> },
) =>
  http.put(url, data as Record<string, unknown> | undefined, {
    params: config?.params,
  }) as Promise<ApiResult<T>>

export const del = <T = unknown>(url: string, config?: { params?: Record<string, unknown> }) =>
  http.delete(url, undefined, { params: config?.params }) as Promise<ApiResult<T>>

export type { ApiResult } from '@/types/api'
