/**
 * 移动端接口安全配置（SM4 加密 / SM2/HMAC-SM3 签名）
 *
 * 【安全加固 P0 - 移动端】密钥材料仅保存在模块闭包内存中。
 * 原方案使用 uni.setStorageSync 持久化密钥，等价于写入 H5 LocalStorage，
 * 跨会话永久存在，是比 PC 端 SessionStorage 更严重的漏洞，本次已全部移除。
 *
 * 应用冷启动时由 app store 的 loadPublicConfig() 重新调用
 * POST /auth/session-sign-init 获取新的临时会话密钥。
 */
export interface ClientSecurityConfig {
  /** 是否启用接口请求/响应 SM4 加密 */
  sm4EncryptEnabled?: boolean
  /** 是否启用接口 SM2 / HMAC-SM3 签名 */
  sm2SignEnabled?: boolean
  sm3SignEnabled?: boolean
  /** SM4 对称密钥（16 字节），由 session-sign-init 下发，仅存内存 */
  sm4Key?: string
  /** HMAC-SM3 签名 Key，由 session-sign-init 下发，仅存内存 */
  sm2PrivateKey?: string
  sm3SignKey?: string
}

/**
 * 移动端会话级随机 ID（纯内存，不持久化）。
 * 每次 App 冷启动重新生成，用于 X-Client-Id 请求头和 /auth/session-sign-init 接口。
 */
const _clientId: string = (() => {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789'
  const bytes = new Uint8Array(32)
  if (typeof window !== 'undefined' && window.crypto?.getRandomValues) {
    window.crypto.getRandomValues(bytes)
  } else {
    for (let i = 0; i < 32; i++) bytes[i] = Math.floor(Math.random() * 256)
  }
  let id = ''
  for (let i = 0; i < 32; i++) id += chars[bytes[i] % chars.length]
  return id
})()

/** 内存闭包安全配置（不持久化到任何存储介质，包括 uni.setStorageSync） */
let securityConfig: ClientSecurityConfig = {}

/** 获取当前移动端会话 ID（用于 X-Client-Id 请求头与 session-sign-init 接口） */
export function getClientId(): string {
  return _clientId
}

/** 合并更新安全配置（仅更新内存闭包，严禁写入 uni.setStorageSync/LocalStorage） */
export function setSecurityConfig(next: ClientSecurityConfig): void {
  securityConfig = { ...securityConfig, ...next }
}

/** 读取当前安全配置快照 */
export function getSecurityConfig(): Readonly<ClientSecurityConfig> {
  return securityConfig
}

/** 重置安全配置（登出时调用） */
export function resetSecurityConfig(): void {
  securityConfig = {}
}
