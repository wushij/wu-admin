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
  /** 是否启用接口 HMAC-SM3 签名（sm2SignEnabled 为历史开关名，实际控制 HMAC-SM3） */
  sm2SignEnabled?: boolean
  sm3SignEnabled?: boolean
  /** SM4 对称密钥（16 字节），由 session-sign-init 下发，仅存内存 */
  sm4Key?: string
  /** HMAC-SM3 签名 Key，由 session-sign-init 下发，仅存内存 */
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
  // 只清除会话密钥，保留签名/加密开关：
  // 开关来自 /auth/config 且只加载一次，若一并清空，
  // 退出后重新登录会因开关丢失而不再附加 X-Signature，导致后端 403
  clearSignKeys()
}

/**
 * 仅清除会话密钥材料（签名密钥失效自愈时调用）。
 * 保留签名/加密开关，并清空 Promise 缓存，允许 requestSessionSignKey 重新发起协商。
 */
export function clearSignKeys(): void {
  securityConfig = {
    sm4EncryptEnabled: securityConfig.sm4EncryptEnabled,
    sm3SignEnabled: securityConfig.sm3SignEnabled,
    sm2SignEnabled: securityConfig.sm2SignEnabled,
  }
  // 同时清空 Promise 缓存，确保下次调用可重新获取新密钥
  sessionSignPromise = null
}

let sessionSignPromise: Promise<any> | null = null

/**
 * 请求初始化会话签名密钥。
 *
 * 修复竞态 bug：
 * - 成功后不清空 sessionSignPromise，保持为已 resolved 的 Promise 缓存。
 * - 后续调用直接返回缓存，不会重复发 session-sign-init，不会覆盖 Redis 里的密钥。
 * - 失败时清空 Promise，允许下次重试。
 * - resetSecurityConfig（登出）时一并清空，保证下次登录重新初始化。
 */
export function requestSessionSignKey(httpInstance: any): Promise<any> {
  // 快速返回：密钥已在内存，无需再请求
  const existing = securityConfig
  if (existing.sm3SignKey || existing.sm4Key) {
    return sessionSignPromise ?? Promise.resolve(null)
  }

  // Promise 锁：正在请求中，复用同一个 Promise，防止并发多次
  if (sessionSignPromise) {
    return sessionSignPromise
  }

  sessionSignPromise = (async () => {
    try {
      const res = (await httpInstance.post('/auth/session-sign-init', undefined, {
        params: { clientId: getClientId() }
      })) as any
      if (res.data?.enabled) {
        setSecurityConfig({
          sm3SignKey: res.data.sm3SignKey,
          sm4Key: res.data.sm4Key,
        })
      }
      // 成功后不清空 Promise，保持为已 resolved 的缓存，防止后续调用重复发请求
      return res
    } catch (err) {
      // 失败时清空 Promise，允许下次重试
      sessionSignPromise = null
      throw err
    }
  })()

  return sessionSignPromise
}

/** 供拦截器或其它地方判断当前是否正在加载密钥 */
export function isSessionInitializing(): boolean {
  return sessionSignPromise !== null
}

/** 供拦截器或其它地方等待当前正在进行的密钥初始化 */
export async function waitSessionInitialized(): Promise<void> {
  if (sessionSignPromise) {
    try {
      await sessionSignPromise
    } catch {
      /* 忽略 */
    }
  }
}

