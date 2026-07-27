import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResult } from '@/types/api'
import { isApiSuccessCode } from '@/utils/api-response'
import { getErrorMessage, markErrorToastShown } from '@/utils/axiosError'
import { useUserStore } from '@/store/user'
import { generateNonce, getTimestamp, encryptSm4, decryptSm4, signSm2, signHmacSm3 } from '@/utils/crypto'
import { getSecurityConfig } from '@/utils/security-config'

export type { ApiResult } from '@/types/api'
export { getErrorMessage, isErrorToastShown } from '@/utils/axiosError'

let lastForbiddenToastAt = 0
function showForbiddenOnce(message: string) {
  const now = Date.now()
  if (now - lastForbiddenToastAt < 2000) return
  lastForbiddenToastAt = now
  ElMessage.error(message)
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

function clearSessionAndRedirectLogin() {
  try {
    useUserStore().logout()
  } catch {
    /* store 可能尚未初始化 */
  }
  router.push('/login')
}

/** 公开认证接口（登录 Cookie 写入前不依赖会话） */
const AUTH_PUBLIC_SUFFIXES = [
  '/auth/login',
  '/auth/register',
  '/auth/captcha',
  '/auth/config',
  '/auth/sms-code',
]

function isAuthPublicUrl(url?: string): boolean {
  if (!url) return false
  const path = url.split('?')[0]
  return AUTH_PUBLIC_SUFFIXES.some((suffix) => path === suffix || path.endsWith(suffix))
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

service.interceptors.request.use(
  (config) => {
    // 自动植入时间戳与 Nonce 防重放 Request Headers
    const timestamp = getTimestamp()
    const nonce = generateNonce()

    config.headers['X-Timestamp'] = timestamp
    config.headers['X-Nonce'] = nonce

    // 读取运行时下发的安全策略（闭包保存，不挂 window，密钥未下发时不启用加密/签名）
    const secConfig = getSecurityConfig()
    const isFormData = config.data instanceof FormData

    if (secConfig.sm4EncryptEnabled && secConfig.sm4Key) {
      config.headers['X-Accept-Encrypted'] = '1'
      if (config.data && !isFormData) {
        const plainStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
        config.data = encryptSm4(plainStr, secConfig.sm4Key)
        config.headers['X-Encrypted'] = '1'
      }
    }

    const isSignEnabled = (secConfig.sm3SignEnabled || secConfig.sm2SignEnabled) && (secConfig.sm3SignKey || secConfig.sm2PrivateKey)
    if (isSignEnabled) {
      const timestamp = getTimestamp()
      const nonce = generateNonce()
      config.headers['X-Timestamp'] = timestamp
      config.headers['X-Nonce'] = nonce

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
        const queryParams = new URLSearchParams()
        Object.entries(config.params).forEach(([key, val]) => {
          if (val !== undefined && val !== null) {
            queryParams.append(key, String(val))
          }
        })
        const qs = queryParams.toString()
        if (qs) {
          fullPath += (fullPath.includes('?') ? '&' : '?') + qs
        }
      }
      try {
        fullPath = decodeURIComponent(fullPath)
      } catch {}

      const signContent = `${config.method?.toUpperCase()}\n${fullPath}\n${timestamp}\n${nonce}\n${bodyStr}`
      const signKey = secConfig.sm3SignKey || secConfig.sm2PrivateKey || ''
      const signature = signHmacSm3(signContent, signKey) || signSm2(signContent, signKey)
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
    // 若响应标明 SM4 加密，且密钥已下发，自动解密 (兼容 CORS 限制：如果启用加密且返回的是非 JSON 字符串，也尝试解密)
    const isEncrypted = response?.headers?.['x-encrypted'] === '1' || 
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

    const res = response.data
    const { code, message } = res

    if (isApiSuccessCode(code)) {
      return res
    }

    const cfg = response.config as InternalAxiosRequestConfig & { silent403?: boolean }
    const text = resolveBodyMessage(message)

    if (code === 401) {
      if (isAuthPublicUrl(cfg.url)) {
        ElMessage.error(text || '认证失败')
        return rejectWithToast(text || '认证失败')
      }
      ElMessage.error('登录已过期，请重新登录')
      clearSessionAndRedirectLogin()
      return rejectWithToast(text || '未授权')
    }

    if (code === 403) {
      if (!cfg?.silent403) {
        showForbiddenOnce(text || '权限不足，无法操作')
      }
      return rejectWithToast('权限不足')
    }

    if (code === 429) {
      ElMessage.warning(text || '操作过于频繁，请稍后再试')
      return rejectWithToast(text || '操作过于频繁，请稍后再试')
    }

    ElMessage.error(text)
    return rejectWithToast(text)
  },
  (error) => {
    console.error('响应错误:', error)

    if (error.response) {
      const secConfig = getSecurityConfig()
      // 若错误响应标明 SM4 加密，且密钥已下发，自动解密 (CORS 容错)
      const isEncrypted = error.response.headers?.['x-encrypted'] === '1' || 
        (secConfig.sm4EncryptEnabled && typeof error.response.data === 'string' && !error.response.data.trim().startsWith('{') && !error.response.data.trim().startsWith('['));
      if (isEncrypted && typeof error.response.data === 'string') {
        const { sm4Key } = secConfig
        if (sm4Key) {
          const plainJson = decryptSm4(error.response.data, sm4Key)
          try {
            error.response.data = JSON.parse(plainJson)
          } catch {
            error.response.data = plainJson
          }
        }
      }

      const { status } = error.response
      const cfg = error.config as InternalAxiosRequestConfig & { silent403?: boolean }
      const text = getErrorMessage(error) || error.message || '请求失败'

      if (status === 401) {
        if (isAuthPublicUrl(cfg?.url)) {
          ElMessage.error(text || '认证失败')
        } else {
          ElMessage.error('登录已过期，请重新登录')
          clearSessionAndRedirectLogin()
        }
      } else if (status === 403) {
        if (!cfg?.silent403) {
          showForbiddenOnce(text || '权限不足，无法访问')
        }
      } else if (status === 429) {
        ElMessage.warning(text || '操作过于频繁，请稍后再试')
      } else {
        ElMessage.error(text)
      }
      markErrorToastShown(error)
    } else {
      ElMessage.error('网络异常，请检查网络连接')
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
  return service.post(url, data, config) as Promise<ApiResult<T>>
}

export const put = <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) => {
  return service.put(url, data, config) as Promise<ApiResult<T>>
}

export const del = <T = unknown>(url: string, config?: AxiosRequestConfig) => {
  return service.delete(url, config) as Promise<ApiResult<T>>
}
