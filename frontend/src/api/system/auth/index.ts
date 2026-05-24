import { get, post } from '@/utils/request'
import type { AuthInfo, LoginForm, LoginResult, RegisterForm } from '@/types/api'
import type { AuthPublicConfig } from '@/types/config'

export type AuthConfig = AuthPublicConfig

export interface CaptchaResult {
  uuid: string
  img?: string
  image?: string
}

export function login(data: LoginForm) {
  return post<LoginResult>('/auth/login', data)
}

export function getInfo() {
  return get<AuthInfo>('/auth/info')
}

export function getCaptcha(scene: string = 'login') {
  return get<CaptchaResult>('/auth/captcha', { scene })
}

export function register(data: RegisterForm) {
  return post<unknown>('/auth/register', data)
}

export function getConfig() {
  return get<AuthConfig>('/auth/config')
}

export function logout() {
  return post<unknown>('/auth/logout')
}
