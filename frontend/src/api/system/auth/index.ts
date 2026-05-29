import request from '@/utils/request'
import type { ApiResult, AuthInfo, LoginForm, LoginResult, RegisterForm } from '@/types/api'
import type { AuthPublicConfig } from '@/types/config'

export type AuthConfig = AuthPublicConfig

export interface CaptchaResult {
  uuid: string
  img?: string
  image?: string
  [key: string]: unknown
}

export function login(data: LoginForm) {
  return request.post('/auth/login', data) as Promise<ApiResult<LoginResult>>
}

export function getInfo() {
  return request.get('/auth/info') as Promise<ApiResult<AuthInfo>>
}

export function getCaptcha(scene: string = 'login') {
  return request.get('/auth/captcha', { params: { scene } }) as Promise<ApiResult<CaptchaResult>>
}

export function register(data: RegisterForm) {
  return request.post('/auth/register', data) as Promise<ApiResult<unknown>>
}

export function getConfig() {
  return request.get('/auth/config') as Promise<ApiResult<AuthConfig>>
}

export function logout() {
  return request.post('/auth/logout') as Promise<ApiResult<unknown>>
}
