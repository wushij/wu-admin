import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResult } from '@/types/api'
import { isApiSuccessCode } from '@/utils/api-response'

export type { ApiResult } from '@/types/api'

let lastForbiddenToastAt = 0
function showForbiddenOnce(message: string) {
  const now = Date.now()
  if (now - lastForbiddenToastAt < 2000) return
  lastForbiddenToastAt = now
  ElMessage.error(message)
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
    const { code, msg, message } = res
    
    // 根据实际返回结构调整
    if (isApiSuccessCode(code)) {
      return res  // 返回完整响应对象，前端用 res.data 访问数据
    } else if (code === 401) {
      const cfg = response.config as InternalAxiosRequestConfig
      if (isAuthPublicUrl(cfg.url)) {
        ElMessage.error(msg || message || '认证失败')
        return Promise.reject(new Error(msg || message || '认证失败'))
      }
      // 已登录态 token 失效
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
      return Promise.reject(new Error(msg || message || '未授权'))
    } else if (code === 403) {
      const cfg = response.config as InternalAxiosRequestConfig & { silent403?: boolean }
      if (!cfg?.silent403) {
        showForbiddenOnce(msg || message || '权限不足，无法操作')
      }
      return Promise.reject(new Error('权限不足'))
    } else {
      ElMessage.error(msg || message || '请求失败')
      return Promise.reject(new Error(msg || message || '请求失败'))
    }
  },
  (error) => {
    console.error('响应错误:', error)
    
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        const cfg = error.config as InternalAxiosRequestConfig
        if (isAuthPublicUrl(cfg?.url)) {
          ElMessage.error(data?.msg || data?.message || '认证失败')
        } else {
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          router.push('/login')
        }
      } else if (status === 403) {
        const cfg = error.config as InternalAxiosRequestConfig & { silent403?: boolean }
        if (!cfg?.silent403) {
          showForbiddenOnce(data?.msg || data?.message || '权限不足，无法访问')
        }
      } else {
        ElMessage.error(data?.msg || data?.message || error.message || '请求失败')
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
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
