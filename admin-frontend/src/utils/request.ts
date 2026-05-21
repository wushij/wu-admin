import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: '/api', // 统一通过 /api 访问网关
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
    if (token) {
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
    if (code === 200 || code === 0) {
      return res  // 返回完整响应对象，前端用 res.data 访问数据
    } else if (code === 401) {
      // 未授权，跳转登录
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
      return Promise.reject(new Error(msg || message || '未授权'))
    } else if (code === 403) {
      // 权限不足
      ElMessage.error('权限不足，无法操作')
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
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('权限不足，无法访问')
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
export const get = (url: string, params?: any) => {
  return service.get(url, { params })
}

export const post = (url: string, data?: any) => {
  return service.post(url, data)
}

export const put = (url: string, data?: any) => {
  return service.put(url, data)
}

export const del = (url: string) => {
  return service.delete(url)
}
