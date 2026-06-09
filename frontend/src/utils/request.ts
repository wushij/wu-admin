import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResult } from '@/types/api'
import { isApiSuccessCode } from '@/utils/api-response'
import { getErrorMessage, markErrorToastShown } from '@/utils/axiosError'

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

/** 公开认证接口不携带管理员 token，避免干扰注册/登录 */
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

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: '/api', // 统一通过 /api 访问后端
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token && !isAuthPublicUrl(config.url)) {
      config.headers['Authorization'] = token
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    const { code, message } = res

    // 根据实际返回结构调整
    if (isApiSuccessCode(code)) {
      return res  // 返回完整响应对象，前端用 res.data 访问数据
    }

    const cfg = response.config as InternalAxiosRequestConfig & { silent403?: boolean }
    const text = resolveBodyMessage(message)

    if (code === 401) {
      if (isAuthPublicUrl(cfg.url)) {
        ElMessage.error(text || '认证失败')
        return rejectWithToast(text || '认证失败')
      }
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
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
          localStorage.removeItem('token')
          router.push('/login')
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
  }
)

// 导出请求方法
export default service

// 便捷方法
/** GET 查询参数（axios 会序列化为 query string） */
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
