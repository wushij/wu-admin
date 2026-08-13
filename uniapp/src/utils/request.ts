import Request from 'luch-request'
import type { ApiResult } from '@/types/api'
import { isApiSuccessCode } from '@/utils/api-response'
import { getToken, removeToken } from '@/utils/auth'
import { isWhiteRoute } from '@/config/route'
import { REQUEST_TIMEOUT } from '@/config/request'
import {
  extractApiErrorMessage,
  isBenignRequestError,
  markErrorToastShown,
  showGlobalErrorToast,
} from '@/plugins/global-error-handler'
import { resolveApiBaseUrl } from '@/utils/api-base'
import { generateNonce, getTimestamp, encryptSm4, decryptSm4, signHmacSm3 } from '@/utils/crypto'
import { getSecurityConfig, getClientId, requestSessionSignKey, clearSignKeys } from '@/utils/security-config'

function isSignKeyExpiredMessage(text?: string): boolean {
  if (!text) return false
  return text.includes('签名密钥') || text.includes('签名验证失败') || text.includes('X-Signature')
}

async function retryUniappRequestWithNewSignKey(config: any): Promise<any> {
  config._isRetrySign = true
  clearSignKeys()
  try {
    await requestSessionSignKey(http)
    if (config._rawBody !== undefined) {
      config.data = config._rawBody
    }
    return http.request(config)
  } catch (err) {
    return Promise.reject(err)
  }
}

const BASE_URL = resolveApiBaseUrl()

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
  async (config) => {
    if (config.data === null) {
      config.data = undefined
    }
    const customCfg = config as any
    if (customCfg._rawBody === undefined) {
      customCfg._rawBody = config.data
    } else {
      config.data = customCfg._rawBody
    }
    const token = getToken()
    const timestamp = getTimestamp()
    const nonce = generateNonce()

    // 竞态保护：当有 Token 且为一般业务请求时，若会话签名密钥尚未初始化完成，主动等待其完毕
    if (
      token &&
      !isAuthPublicUrl(config.url) &&
      !config.url?.includes('/auth/session-sign-init') &&
      !config.url?.includes('/auth/logout')
    ) {
      const currentSec = getSecurityConfig()
      if (!currentSec.sm3SignKey && !currentSec.sm4Key) {
        try {
          await requestSessionSignKey(http)
        } catch {
          /* 忽略异常，继续向下尝试发送 */
        }
      }
    }

    config.header = {
      ...config.header,
      'X-Timestamp': timestamp,
      'X-Nonce': nonce,
      // 【安全加固 P0】携带会话 clientId，服务端据此从 Redis 查找临时签名密钥
      'X-Client-Id': getClientId(),
    }

    if (token) {
      config.header.Authorization = token
    }

    // 自动植入接口请求 SM4 加密 / SM2 数字签名
    const secConfig = getSecurityConfig()
    const isFormData = typeof FormData !== 'undefined' && config.data instanceof FormData

    if (secConfig.sm4EncryptEnabled && secConfig.sm4Key) {
      config.header['X-Accept-Encrypted'] = '1'
      if (config.data && !isFormData) {
        const plainStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
        config.data = encryptSm4(plainStr, secConfig.sm4Key) as any
        config.header['X-Encrypted'] = '1'
      }
    }

    const isSignEnabled = (secConfig.sm3SignEnabled || secConfig.sm2SignEnabled) && secConfig.sm3SignKey
    if (isSignEnabled) {
      let bodyStr = ''
      if (config.data && !isFormData) {
        bodyStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
        bodyStr = bodyStr.trim()
        if (bodyStr.startsWith('"') && bodyStr.endsWith('"') && bodyStr.length > 2) {
          bodyStr = bodyStr.substring(1, bodyStr.length - 1)
        }
      }
      let fullPath = config.url || ''
      if (fullPath.startsWith('/api/')) {
        fullPath = fullPath.substring(4)
      } else if (!fullPath.startsWith('/')) {
        fullPath = '/' + fullPath
      }
      if (config.params && typeof config.params === 'object') {
        const queryParts: string[] = []
        Object.entries(config.params).forEach(([key, val]) => {
          if (val !== undefined && val !== null) {
            queryParts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(val))}`)
          }
        })
        const qs = queryParts.join('&')
        if (qs) {
          fullPath += (fullPath.includes('?') ? '&' : '?') + qs
        }
      }
      try {
        fullPath = decodeURIComponent(fullPath)
      } catch {}

      const signContent = `${(config.method || 'GET').toUpperCase()}\n${fullPath}\n${timestamp}\n${nonce}\n${bodyStr}`
      const signKey = secConfig.sm3SignKey || ''
      const signature = signHmacSm3(signContent, signKey)
      if (signature) {
        config.header['X-Signature'] = signature
      }
    }

    return config
  },
  (error) => Promise.reject(error),
)

// luch-request 响应拦截器返回 ApiResult 而非 HttpResponse
http.interceptors.response.use(
  ((response: { data: any; config?: { url?: string; silent403?: boolean }; header?: any }) => {
    const secConfig = getSecurityConfig()
    // 若响应标明 SM4 加密，且密钥已下发，自动解密 (兼容 CORS 限制：如果启用加密且返回的是非 JSON 字符串，也尝试解密)
    const isEncrypted = response?.header?.['x-encrypted'] === '1' || response?.header?.['X-Encrypted'] === '1' || 
      (secConfig.sm4EncryptEnabled && typeof response.data === 'string' && !response.data.trim().startsWith('{') && !response.data.trim().startsWith('['));
    if (isEncrypted && typeof response.data === 'string') {
      const { sm4Key } = secConfig
      if (sm4Key) {
        const plainJson = decryptSm4(response.data, sm4Key)
        try {
          response.data = JSON.parse(plainJson)
        } catch {
          response.data = plainJson
        }
      }
    }

    const res = response.data as ApiResult
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
      const cfg = response.config as any
      if (isSignKeyExpiredMessage(message) && !cfg?._isRetrySign && !cfg?.url?.includes('/auth/session-sign-init')) {
        return retryUniappRequestWithNewSignKey(cfg)
      }
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
  (error: { data?: any; statusCode?: number; errMsg?: string; header?: any; config?: any }) => {
    const secConfig = getSecurityConfig()
    // 若错误响应标明 SM4 加密，且密钥已下发，自动解密 (CORS 容错)
    const isEncrypted = error && (error.header?.['x-encrypted'] === '1' || error.header?.['X-Encrypted'] === '1' || 
      (secConfig.sm4EncryptEnabled && typeof error.data === 'string' && !error.data.trim().startsWith('{') && !error.data.trim().startsWith('[')));
    if (isEncrypted && typeof error.data === 'string') {
      const { sm4Key } = secConfig
      if (sm4Key) {
        const plainJson = decryptSm4(error.data, sm4Key)
        try {
          error.data = JSON.parse(plainJson)
        } catch {
          error.data = plainJson
        }
      }
    }

    const url = (error as any)?.config?.url || ''
    if (url.includes('/auth/session-sign-init')) {
      return Promise.reject(error)
    }

    const msg = extractApiErrorMessage(error, '网络异常')
    const cfg = (error as any)?.config

    if ((error.statusCode === 403 || isSignKeyExpiredMessage(msg)) && !cfg?._isRetrySign && !url.includes('/auth/session-sign-init')) {
      return retryUniappRequestWithNewSignKey(cfg)
    }
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
  http.post(url, (data === null ? undefined : data) as Record<string, unknown> | undefined, {
    params: config?.params,
  }) as Promise<ApiResult<T>>

export const put = <T = unknown>(
  url: string,
  data?: unknown,
  config?: { params?: Record<string, unknown> },
) =>
  http.put(url, (data === null ? undefined : data) as Record<string, unknown> | undefined, {
    params: config?.params,
  }) as Promise<ApiResult<T>>

export const del = <T = unknown>(url: string, config?: { params?: Record<string, unknown> }) =>
  http.delete(url, undefined, { params: config?.params }) as Promise<ApiResult<T>>

export type { ApiResult } from '@/types/api'
