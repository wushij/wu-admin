import { get, post } from '@/utils/request'
import type { AuthInfo, LoginForm, LoginResult, RegisterForm } from '@/types/api'
import type { AuthPublicConfig } from '@/types/config'

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
  return get<AuthPublicConfig>('/auth/config')
}

export function sendSmsCode(phone: string, sliderCode?: string) {
  const payload: { phone: string; code?: string } = { phone }
  if (sliderCode) payload.code = sliderCode
  return post<boolean>('/auth/sms-code', payload)
}

export function logout() {
  return post<unknown>('/auth/logout')
}

export interface ForgotPasswordCheckResult {
  maskedMobile: string
  minPasswordLength: number
}

export function checkForgotPassword(username: string) {
  return post<ForgotPasswordCheckResult>('/auth/forgot-password/check', { username })
}

export function sendForgotPasswordSmsCode(username: string, sliderCode?: string) {
  const payload: { username: string; code?: string } = { username }
  if (sliderCode) payload.code = sliderCode
  return post<boolean>('/auth/forgot-password/sms-code', payload)
}

export function resetForgotPassword(data: {
  username: string
  smsCode: string
  newPassword: string
  confirmPassword: string
}) {
  return post<boolean>('/auth/forgot-password/reset', data)
}
