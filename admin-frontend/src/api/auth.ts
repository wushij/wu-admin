import { get, post } from '@/utils/request'

/**
 * 认证相关API
 */

// 获取验证码
export const getCaptcha = () => {
  return get('/auth/captcha')
}

// 登录
export const login = (data: { username: string; password: string; uuid?: string; code?: string }) => {
  return post('/auth/login', data)
}

// 注册
export const register = (data: { username: string; password: string; nickname?: string }) => {
  return post('/auth/register', data)
}

// 退出登录
export const logout = () => {
  return post('/auth/logout')
}

// 获取用户信息
export const getUserInfo = () => {
  return get('/auth/info')
}
