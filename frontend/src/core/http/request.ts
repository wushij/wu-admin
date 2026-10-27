import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResult } from '@/types/api'
import { isApiSuccessCode } from './api-response'
import { getErrorMessage, markErrorToastShown, isErrorToastShown } from './axiosError'
import { useUserStore } from '@/store/user'
import { generateNonce, getTimestamp, encryptSm4, decryptSm4, signHmacSm3 } from '@/utils/crypto'
import { getSecurityConfig, getClientId, requestSessionSignKey, clearSignKeys } from '@/utils/security-config'

export type { ApiResult } from '@/types/api'
export { getErrorMessage, isErrorToastShown }

let lastForbiddenToastAt = 0
function showForbiddenOnce(message: string) {
  const now = Date.now()
  if (now - lastForbiddenToastAt < 2000) return
  lastForbiddenToastAt = now
  ElMessage.error(message)
}

/** 同一类提示在短时间窗口内只展示一次，避免并发请求把页面刷出多条重复 Toast */
const TOAST_DEDUPE_WINDOW_MS = 3000
const recentToastAt = new Map<string, number>()

function showMessageOnce(message: string, type: 'error' | 'warning' = 'error') {
  const key = `${type}:${message}`
  const now = Date.now()
  const last = recentToastAt.get(key)
  if (last != null && now - last < TOAST_DEDUPE_WINDOW_MS) return
  if (recentToastAt.size > 50) {
    for (const [item, at] of recentToastAt) {
      if (now - at >= TOAST_DEDUPE_WINDOW_MS) recentToastAt.delete(item)
    }
  }
  recentToastAt.set(key, now)
  if (type === 'warning') {
    ElMessage.warning(message)
  } else {
    ElMessage.error(message)
  }
}

function rejectWithToast(message: string, source?: unknown): Promise<never> {
  const err = source instanceof Error ? source : new Error(message)
  if (!err.message) {
    err.message = message
  }
  markErrorToastShown(err)
  return Promise.reject(err)
}

function resolveBodyMessage(message?: string, fallback = '请求失败') {
  return message || fallback
}

/** 并发请求同时收到 401 时，只清理一次会话、只跳转一次登录页 */
let lastSessionClearAt = 0
function clearSessionAndRedirectLogin() {
  const now = Date.now()
  if (now - lastSessionClearAt < 2000) return
  lastSessionClearAt = now
  try {
    useUserStore().logout()
  } catch {
    /* store 可能尚未初始化 */
  }
  if (router.currentRoute?.value?.path !== '/login') {
    void router.push('/login')
  }
}

/** 公开认证接口（登录 Cookie 写入前不依赖会话，也无需会话签名密钥） */
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

/** 是否已有登录态（用于避免未登录时反复请求会话密钥） */
function isLoggedIn(): boolean {
  try {
    return !!useUserStore().isLoggedIn
  } catch {
    return false
  }
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

function isSignKeyExpiredMessage(text?: string): boolean {
  if (!text) return false
  return text.includes('签名密钥') || text.includes('签名验证失败') || text.includes('X-Signature')
}

async function retryRequestWithNewSignKey(cfg: any): Promise<any> {
  cfg._isRetrySign = true
  clearSignKeys()
  try {
    await requestSessionSignKey(service)
    if (cfg._rawBody !== undefined) {
      cfg.data = cfg._rawBody
    }
    return service(cfg)
  } catch (err) {
    return Promise.reject(err)
  }
}

service.interceptors.request.use(
  async (config) => {
    // Axios 会把 null body 序列化为字面量字符串 "null"（typeof null === 'object'），
    // 而前端签名逻辑对 null 判定为空串 "" -> 导致网关验签失败 403。
    if (config.data === null) {
      config.data = undefined
    }
    const customCfg = config as InternalAxiosRequestConfig & { _rawBody?: any; _isRetrySign?: boolean }
    if (customCfg._rawBody === undefined) {
      customCfg._rawBody = config.data
    } else {
      config.data = customCfg._rawBody
    }

    // 自动植入时间戳、Nonce、ClientId 防重放 Request Headers
    const timestamp = getTimestamp()
    const nonce = generateNonce()

    // 竞态保护：针对一般业务请求，若密钥还未获取，主动触发并等待单例密钥获取
    if (
      !isAuthPublicUrl(config.url) &&
      !config.url?.includes('/auth/session-sign-init') &&
      !config.url?.includes('/auth/logout')
    ) {
      const currentSec = getSecurityConfig()
      if (!currentSec.sm3SignKey && !currentSec.sm4Key && isLoggedIn()) {
        try {
          await requestSessionSignKey(service)
        } catch {
          /* 忽略异常 */
        }
      }
    }

    config.headers['X-Timestamp'] = timestamp
    config.headers['X-Nonce'] = nonce
    // 【安全加固 P0】携带会话 clientId，服务端据此从 Redis 查找临时签名密钥
    config.headers['X-Client-Id'] = getClientId()

    // 读取运行时下发的安全策略（闭包保存，不挂 window，密钥未下发时不启用加密/签名）
    const secConfig = getSecurityConfig()
    const isFormData = config.data instanceof FormData

    if (secConfig.sm4EncryptEnabled && secConfig.sm4Key) {
      config.headers['X-Accept-Encrypted'] = '1'
      config.headers['X-Encrypted-Payload'] = 'SM4-GCM'
      if (config.data && !isFormData) {
        const plainStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
        config.data = encryptSm4(plainStr, secConfig.sm4Key)
        config.headers['X-Encrypted'] = '1'
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
      // 提取完整的 URI (包含经过 Axios 序列化后的 params QueryString)
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

      const signContent = `${config.method?.toUpperCase()}\n${fullPath}\n${timestamp}\n${nonce}\n${bodyStr}`
      const signKey = secConfig.sm3SignKey || ''
      const signature = signHmacSm3(signContent, signKey)
      if (signature) {
        config.headers['X-Signature'] = signature
      }
    }

    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  },
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const secConfig = getSecurityConfig()
    // 仅信任后端明确声明的加密响应头，不再根据响应体形态猜测，避免把明文误判为密文
    const payloadType = response?.headers?.['x-encrypted-payload']
    const isEncrypted = response?.headers?.['x-encrypted'] === '1'
      || (typeof payloadType === 'string' && payloadType.toUpperCase() === 'SM4-GCM')
    if (isEncrypted && typeof response.data === 'string') {
      const { sm4Key } = secConfig
      if (!sm4Key) {
        // 不在这里逐条弹窗：并发请求会同时命中该分支，统一去重并只处理一次会话
        showMessageOnce('会话安全凭据缺失，无法解密响应数据，请重新登录')
        clearSessionAndRedirectLogin()
        return rejectWithToast('响应数据解密失败：密钥未下发')
      }
      let plainJson: string
      try {
        plainJson = decryptSm4(response.data, sm4Key)
      } catch (err) {
        console.error('响应数据国密 SM4-GCM 认证解密失败:', err)
        showMessageOnce('响应数据认证解密失败，请刷新页面后重试')
        return rejectWithToast('响应数据认证解密失败')
      }
      try {
        response.data = JSON.parse(plainJson)
      } catch {
        response.data = plainJson
      }
    }

    const res = response.data
    const { code, message } = res

    if (isApiSuccessCode(code)) {
      return res
    }

    const cfg = response.config as InternalAxiosRequestConfig & { silent403?: boolean; _isRetrySign?: boolean }
    const text = resolveBodyMessage(message)

    if (code === 401) {
      if (isAuthPublicUrl(cfg.url)) {
        showMessageOnce(text || '认证失败')
        return rejectWithToast(text || '认证失败')
      }
      // 并发请求只提示一次，避免整屏都是“登录已过期”
      showMessageOnce('登录已过期，请重新登录')
      clearSessionAndRedirectLogin()
      return rejectWithToast(text || '未授权')
    }

    if (code === 403) {
      // 🔐 自动静默恢复：签名密钥过期时自动重新协商并发起一次重试
      if (isSignKeyExpiredMessage(text) && !cfg._isRetrySign && !cfg.url?.includes('/auth/session-sign-init')) {
        return retryRequestWithNewSignKey(cfg)
      }
      if (!cfg?.silent403) {
        showForbiddenOnce(text || '权限不足，无法操作')
      }
      return rejectWithToast('权限不足')
    }

    if (code === 429) {
      showMessageOnce(text || '操作过于频繁，请稍后再试', 'warning')
      return rejectWithToast(text || '操作过于频繁，请稍后再试')
    }

    if (cfg?.url?.includes('/auth/session-sign-init')) {
      return Promise.reject(new Error(text || '获取签名密钥失败'))
    }

    showMessageOnce(text)
    return rejectWithToast(text)
  },
  (error) => {
    console.error('响应错误:', error)

    const cfg = error.config as InternalAxiosRequestConfig & { silent403?: boolean; _isRetrySign?: boolean }

    // 401 必须优先于解密处理：会话失效时响应通常仍带加密头，
    // 若先走解密分支，并发请求会同时弹“凭据缺失”，把页面刷满重复提示。
    if (error.response?.status === 401 && !isAuthPublicUrl(cfg?.url)) {
      showMessageOnce('登录已过期，请重新登录')
      clearSessionAndRedirectLogin()
      markErrorToastShown(error)
      return Promise.reject(error)
    }

    if (error.response) {
      const secConfig = getSecurityConfig()
      // 仅信任后端明确声明的加密响应头，解密失败必须显式报错（Fail-Closed）
      const payloadType = error.response.headers?.['x-encrypted-payload']
      const isEncrypted = error.response.headers?.['x-encrypted'] === '1'
        || (typeof payloadType === 'string' && payloadType.toUpperCase() === 'SM4-GCM')
      if (isEncrypted && typeof error.response.data === 'string') {
        const { sm4Key } = secConfig
        if (!sm4Key) {
          showMessageOnce('会话安全凭据缺失，无法解密响应数据，请重新登录')
          clearSessionAndRedirectLogin()
          return rejectWithToast('响应数据解密失败：密钥未下发')
        }
        let plainJson: string
        try {
          plainJson = decryptSm4(error.response.data, sm4Key)
        } catch (err) {
          console.error('错误响应国密 SM4-GCM 认证解密失败:', err)
          showMessageOnce('响应数据认证解密失败，请刷新页面后重试')
          return rejectWithToast('响应数据认证解密失败')
        }
        try {
          error.response.data = JSON.parse(plainJson)
        } catch {
          error.response.data = plainJson
        }
      }

      const { status } = error.response
      const text = getErrorMessage(error) || error.message || '请求失败'

      // 🔐 静默排除：如果是初始化会话签名密钥失败，一律静默 Reject 不弹窗
      if (cfg?.url?.includes('/auth/session-sign-init')) {
        return Promise.reject(error)
      }

      if (status === 401) {
        if (isAuthPublicUrl(cfg?.url)) {
          showMessageOnce(text || '认证失败')
        } else {
          // 未认证或 Token 过期时，静默清理 Token 并平滑跳转至登录页，不在页面上弹出多余的红色吐司
          clearSessionAndRedirectLogin()
        }
      } else if (status === 403) {
        // 🔐 自动静默恢复：签名密钥过期时自动重新协商并发起一次重试
        if (isSignKeyExpiredMessage(text) && !cfg?._isRetrySign && !cfg?.url?.includes('/auth/session-sign-init')) {
          return retryRequestWithNewSignKey(cfg)
        }
        if (!cfg?.silent403) {
          showForbiddenOnce(text || '权限不足，无法访问')
        }
      } else if (status === 429) {
        showMessageOnce(text || '操作过于频繁，请稍后再试', 'warning')
      } else {
        showMessageOnce(text)
      }
      markErrorToastShown(error)
    } else {
      showMessageOnce('网络异常，请检查网络连接')
      markErrorToastShown(error)
    }

    return Promise.reject(error)
  },
)

export default service

export type HttpQueryParamValue = string | number | boolean | null | undefined
export type HttpQueryParams = Record<string, HttpQueryParamValue>

export const get = <T = unknown>(url: string, params?: object, config?: AxiosRequestConfig) => {
  return service.get(url, { ...config, params }) as Promise<ApiResult<T>>
}

export const post = <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) => {
  return service.post(url, data === null ? undefined : data, config) as Promise<ApiResult<T>>
}

export const put = <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) => {
  return service.put(url, data === null ? undefined : data, config) as Promise<ApiResult<T>>
}

export const del = <T = unknown>(url: string, config?: AxiosRequestConfig) => {
  return service.delete(url, config) as Promise<ApiResult<T>>
}
