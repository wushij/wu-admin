import Request from 'luch-request'
import type { ApiResult } from '@/types/api'
import { isApiSuccessCode } from '@/core/api-response'
import { getToken, removeToken } from '@/core/auth'
import { isWhiteRoute } from '@/config/route'
import { REQUEST_TIMEOUT } from '@/config/request'
import {
  extractApiErrorMessage,
  isBenignRequestError,
  markErrorToastShown,
  showGlobalErrorToast,
} from '@/plugins/global-error-handler'
import { resolveApiBaseUrl } from '@/core/api-base'
import { generateNonce, getTimestamp, encryptSm4, decryptSm4, signHmacSm3 } from '@/utils/security/crypto'
import { getSecurityConfig, getClientId, requestSessionSignKey, clearSignKeys } from '@/utils/security/security-config'

function isSignKeyExpiredMessage(text?: string): boolean {
  if (!text) return false
  return text.includes('签名密钥') || text.includes('签名验证失败') || text.includes('X-Signature')
}

/** 并发 403 只协商一次新密钥，避免互相 clearSignKeys 把 Redis 密钥冲掉 */
let signKeyRetryLock: Promise<void> | null = null

async function retryUniappRequestWithNewSignKey(config: any): Promise<any> {
  config._isRetrySign = true
  if (!signKeyRetryLock) {
    signKeyRetryLock = (async () => {
      clearSignKeys()
      await requestSessionSignKey(http)
    })().finally(() => {
      signKeyRetryLock = null
    })
  }
  try {
    await signKeyRetryLock
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
  '/auth/sms-code/verify',
  '/auth/email-code',
  '/auth/forgot-password/check',
  '/auth/forgot-password/sms-code',
  '/auth/forgot-password/reset',
  '/auth/slider-challenge',
  '/auth/slider-verify',
  '/auth/session-sign-init',
  '/auth/info',
  '/auth/logout',
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

/** 同一类提示在短时间窗口内只展示一次，避免并发请求把页面刷出多条重复 Toast */
const TOAST_DEDUPE_WINDOW_MS = 3000
const recentToastAt = new Map<string, number>()

function showToastOnce(message: string) {
  const now = Date.now()
  const last = recentToastAt.get(message)
  if (last != null && now - last < TOAST_DEDUPE_WINDOW_MS) return
  if (recentToastAt.size > 50) {
    for (const [item, at] of recentToastAt) {
      if (now - at >= TOAST_DEDUPE_WINDOW_MS) recentToastAt.delete(item)
    }
  }
  recentToastAt.set(message, now)
  uni.showToast({ title: message, icon: 'none' })
}

/** 并发请求同时收到 401 时，只清理一次会话、只跳转一次登录页 */
let lastSessionClearAt = 0
function clearSessionAndRedirectLogin() {
  const now = Date.now()
  if (now - lastSessionClearAt < 2000) return
  lastSessionClearAt = now
  // 会话失效时必须清空内存中的 SM4/SM3 会话密钥：
  // 否则残留的旧密钥会继续用于加密登录请求，而登录接口会在服务端轮换密钥，
  // 导致登录响应无法用旧密钥解密（GCM Tag 校验失败）。
  clearSignKeys()
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

    // 以「手里是否已有密钥」为准，不要等 /auth/config 开关。
    // 生产环境整页刷新后，页面请求常早于 config 返回；若此时不签名，网关会 403，下拉刷新显示「加载失败」。
    const secConfig = getSecurityConfig()
    const isFormData = typeof FormData !== 'undefined' && config.data instanceof FormData

    if (secConfig.sm4Key && !customCfg.skipEncrypt) {
      config.header['X-Accept-Encrypted'] = '1'
      if (config.data && !isFormData) {
        const plainStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
        config.data = encryptSm4(plainStr, secConfig.sm4Key) as any
        config.header['X-Encrypted'] = '1'
        config.header['X-Encrypted-Payload'] = 'SM4-GCM'
      }
    }

    if (secConfig.sm3SignKey) {
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
    // 仅信任后端明确声明的加密响应头，不再根据响应体形态猜测
    const payloadType = response?.header?.['x-encrypted-payload'] ?? response?.header?.['X-Encrypted-Payload']
    const isEncrypted = response?.header?.['x-encrypted'] === '1'
      || response?.header?.['X-Encrypted'] === '1'
      || (typeof payloadType === 'string' && payloadType.toUpperCase() === 'SM4-GCM')
    if (isEncrypted && typeof response.data === 'string') {
      const { sm4Key } = secConfig
      if (!sm4Key) {
        showToastOnce('会话安全凭据缺失，请重新登录')
        return Promise.reject(new Error('响应数据解密失败：密钥未下发'))
      }
      let plainJson: string
      try {
        plainJson = decryptSm4(response.data, sm4Key)
      } catch (err) {
        console.error('响应数据国密 SM4-GCM 认证解密失败:', err)
        showToastOnce('响应数据认证解密失败')
        return Promise.reject(new Error('响应数据认证解密失败'))
      }
      try {
        response.data = JSON.parse(plainJson)
      } catch {
        response.data = plainJson
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
        showToastOnce(message || '认证失败')
        return Promise.reject(new Error(message || '未授权'))
      }
      // 并发请求只提示一次，避免整屏都是“登录已过期”
      showToastOnce('登录已过期')
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
      showToastOnce(message || '操作过于频繁')
      return Promise.reject(new Error(message || '操作过于频繁'))
    }

    // 🔐 对会话签名初始化接口做静默过滤与登录态过期自愈：
    // 若客户端本地有 Token 却收到具备登录态要求或 401，说明服务端 Token 已失效，触发清理与重定向
    const reqUrl = (response.config?.url || '') as string
    if (reqUrl.includes('/auth/session-sign-init')) {
      if (getToken() && (message?.includes('具备登录态') || message?.includes('登录已过期') || code === 401)) {
        showToastOnce('登录已过期')
        clearSessionAndRedirectLogin()
      }
      return Promise.reject(new Error(message || '获取签名密钥失败'))
    }

    const msg = message || '请求失败'
    showGlobalErrorToast(msg)
    const err = new Error(msg)
    markErrorToastShown(err)
    return Promise.reject(err)
  }) as unknown as Parameters<typeof http.interceptors.response.use>[0],
  (error: { data?: any; statusCode?: number; errMsg?: string; header?: any; config?: any }) => {
    const secConfig = getSecurityConfig()

    // 401 必须优先于解密处理：会话失效时响应通常仍带加密头，
    // 若先走解密分支，并发请求会同时弹“凭据缺失”，把页面刷满重复提示。
    const earlyUrl = (error as any)?.config?.url || ''
    if (error?.statusCode === 401 && !isAuthPublicUrl(earlyUrl)) {
      showToastOnce('登录已过期')
      clearSessionAndRedirectLogin()
      const sessionErr = new Error('登录已过期')
      markErrorToastShown(sessionErr)
      return Promise.reject(sessionErr)
    }

    // 仅信任后端明确声明的加密响应头，解密失败必须显式报错（Fail-Closed）
    const payloadType = error?.header?.['x-encrypted-payload'] ?? error?.header?.['X-Encrypted-Payload']
    const isEncrypted = error && (error.header?.['x-encrypted'] === '1'
      || error.header?.['X-Encrypted'] === '1'
      || (typeof payloadType === 'string' && payloadType.toUpperCase() === 'SM4-GCM'))
    if (isEncrypted && typeof error.data === 'string') {
      const { sm4Key } = secConfig
      if (!sm4Key) {
        showToastOnce('会话安全凭据缺失，请重新登录')
        return Promise.reject(new Error('响应数据解密失败：密钥未下发'))
      }
      let plainJson: string
      try {
        plainJson = decryptSm4(error.data, sm4Key)
      } catch (err) {
        console.error('错误响应国密 SM4-GCM 认证解密失败:', err)
        showToastOnce('响应数据认证解密失败')
        return Promise.reject(new Error('响应数据认证解密失败'))
      }
      try {
        error.data = JSON.parse(plainJson)
      } catch {
        error.data = plainJson
      }
    }

    const url = (error as any)?.config?.url || ''
    if (url.includes('/auth/session-sign-init')) {
      if (error?.statusCode === 401 && getToken()) {
        uni.showToast({ title: '登录已过期', icon: 'none' })
        clearSessionAndRedirectLogin()
      }
      return Promise.reject(error)
    }

    // H5 下拉刷新/切后台会 abort 进行中的请求，不能当失败处理，更不能触发清密钥重试
    if (isBenignRequestError(error)) {
      return Promise.reject(error)
    }

    const msg = extractApiErrorMessage(error, '网络异常')
    const cfg = (error as any)?.config

    // 只有签名密钥失效才重新协商；普通权限不足(403)不能重置会话密钥
    if (isSignKeyExpiredMessage(msg) && !cfg?._isRetrySign && !url.includes('/auth/session-sign-init')) {
      return retryUniappRequestWithNewSignKey(cfg)
    }
    showGlobalErrorToast(msg)
    const err = new Error(msg)
    markErrorToastShown(err)
    return Promise.reject(err)
  },
)

export default http

export const get = <T = unknown>(
  url: string,
  params?: object,
  config?: { silent403?: boolean },
) =>
  http.get(url, { params, ...config } as any) as Promise<ApiResult<T>>

export const post = <T = unknown>(
  url: string,
  data?: unknown,
  config?: { params?: Record<string, unknown>; skipEncrypt?: boolean },
) =>
  http.post(url, (data === null ? undefined : data) as Record<string, unknown> | undefined, {
    params: config?.params,
    skipEncrypt: config?.skipEncrypt,
  } as any) as Promise<ApiResult<T>>

export const put = <T = unknown>(
  url: string,
  data?: unknown,
  config?: { params?: Record<string, unknown> },
) =>
  http.put(url, (data === null ? undefined : data) as Record<string, unknown> | undefined, {
    params: config?.params,
  }) as Promise<ApiResult<T>>

export const del = <T = unknown>(
  url: string,
  config?: { params?: Record<string, unknown>; data?: unknown },
) =>
  http.delete(url, (config?.data ?? undefined) as Record<string, unknown> | undefined, {
    params: config?.params,
  }) as Promise<ApiResult<T>>

export type { ApiResult } from '@/types/api'
