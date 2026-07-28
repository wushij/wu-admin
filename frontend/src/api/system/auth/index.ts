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

/** 会话签名密钥初始化返回结果 */
export interface SessionSignResult {
  /** 服务端签名功能是否已开启 */
  enabled: boolean
  /** HMAC-SM3 签名密钥（enabled=true 且签名开启时有值，仅存内存） */
  sm3SignKey?: string
  /** 会话 SM4 对称密钥（32 位 Hex，enabled=true 且加密开启时有值） */
  sm4Key?: string
  /** 临时密钥有效期（分钟） */
  ttlMinutes?: number
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

/**
 * 初始化会话签名密钥（安全加固 P0）。
 * 携带内存随机 clientId 向服务端申请一个 30 分钟有效期的临时 HMAC-SM3 签名密钥，
 * 该密钥仅存于内存闭包，不写入任何持久化存储。
 */
export function sessionSignInit(clientId: string) {
  return post<SessionSignResult>(`/auth/session-sign-init?clientId=${encodeURIComponent(clientId)}`)
}
