import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResult } from '@/types/api'
import { isApiSuccessCode } from '@/utils/api-response'
import { getErrorMessage, markErrorToastShown } from '@/utils/axiosError'
import { useUserStore } from '@/store/user'
import { generateNonce, getTimestamp, encryptSm4, decryptSm4, signSm2 } from '@/utils/crypto'

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

    // 读取客户端配置或 SessionStorage 中的安全策略（如从安全配置中读出的配置）
    const secConfig = (window as unknown as { __WU_ADMIN_SECURITY__?: {
      sm4EncryptEnabled?: boolean
      sm2SignEnabled?: boolean
      sm4Key?: string
      sm2PrivateKey?: string
    } }).__WU_ADMIN_SECURITY__ || {}

    if (secConfig.sm4EncryptEnabled && config.data) {
      const plainStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
      config.data = encryptSm4(plainStr, secConfig.sm4Key || 'WuAdmin16BytesKey')
      config.headers['X-Encrypted'] = '1'
    }

    if (secConfig.sm2SignEnabled && secConfig.sm2PrivateKey) {
      const bodyStr = typeof config.data === 'string' ? config.data : (config.data ? JSON.stringify(config.data) : '')
      const signContent = `${config.method?.toUpperCase()}\n${config.url || ''}\n${timestamp}\n${nonce}\n${bodyStr}`
      const signature = signSm2(signContent, secConfig.sm2PrivateKey)
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
    // 若响应标明 SM4 加密，自动解密
    if (response?.headers?.['x-encrypted'] === '1' && typeof response.data === 'string') {
      const secConfig = (window as unknown as { __WU_ADMIN_SECURITY__?: { sm4Key?: string } }).__WU_ADMIN_SECURITY__ || {}
      const plainJson = decryptSm4(response.data, secConfig.sm4Key || 'WuAdmin16BytesKey')
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
