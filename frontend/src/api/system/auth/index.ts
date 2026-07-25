import { get, post } from '@/utils/request'
import type { AuthInfo, LoginForm, LoginResult, RegisterForm } from '@/types/api'
import type { AuthPublicConfig } from '@/types/config'

export type AuthConfig = AuthPublicConfig

export interface CaptchaResult {
  uuid: string
  img?: string
  image?: string
}

export interface SliderChallengeResult {
  token: string
  bgIndex: number
  pieceTop: number
  targetX: number
}

export function getSliderChallenge(scene: string = 'login') {
  return get<SliderChallengeResult>('/auth/slider-challenge', { scene })
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

export function sendSmsCode(phone: string, slider?: { uuid: string; code: string }) {
  const payload: { phone: string; uuid?: string; code?: string } = { phone }
  if (slider) {
    payload.uuid = slider.uuid
    payload.code = slider.code
  }
  return post<boolean>('/auth/sms-code', payload)
}

export function sendEmailCode(email: string, slider?: { uuid: string; code: string }) {
  const payload: { email: string; uuid?: string; code?: string } = { email }
  if (slider) {
    payload.uuid = slider.uuid
    payload.code = slider.code
  }
  return post<boolean>('/auth/email-code', payload)
}

export function logout() {
  return post<unknown>('/auth/logout')
}
