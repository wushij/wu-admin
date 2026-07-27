/**
 * 客户端接口安全配置（SM4 加密 / SM2 签名）
 *
 * 配置保存在模块闭包中并借助 sessionStorage 进行会话级缓存，
 * 解决页面刷（F5）时内存配置丢失导致请求缺失 X-Signature 签名头的问题。
 */
export interface ClientSecurityConfig {
  /** 是否启用接口请求/响应 SM4 加密 */
  sm4EncryptEnabled?: boolean
  /** 是否启用接口 SM2 / HMAC-SM3 签名 */
  sm2SignEnabled?: boolean
  sm3SignEnabled?: boolean
  /** SM4 对称密钥（16 字节），未下发时不加密 */
  sm4Key?: string
  /** SM2 签名私钥或 HMAC-SM3 签名 Key */
  sm2PrivateKey?: string
  sm3SignKey?: string
}

const SEC_STORAGE_KEY = 'wu_client_sec_config'

function loadFromStorage(): ClientSecurityConfig {
  try {
    const raw = sessionStorage.getItem(SEC_STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

let securityConfig: ClientSecurityConfig = loadFromStorage()

/** 合并更新安全配置（仅覆盖传入字段并缓存至 sessionStorage） */
export function setSecurityConfig(next: ClientSecurityConfig): void {
  securityConfig = { ...securityConfig, ...next }
  try {
    sessionStorage.setItem(SEC_STORAGE_KEY, JSON.stringify(securityConfig))
  } catch {
    /* 忽略 sessionStorage 写入异常 */
  }
}

/** 读取当前安全配置快照 */
export function getSecurityConfig(): Readonly<ClientSecurityConfig> {
  if (!securityConfig.sm4Key && !securityConfig.sm3SignKey && !securityConfig.sm2PrivateKey) {
    securityConfig = loadFromStorage()
  }
  return securityConfig
}

/** 重置安全配置（登出或测试场景使用） */
export function resetSecurityConfig(): void {
  securityConfig = {}
  try {
    sessionStorage.removeItem(SEC_STORAGE_KEY)
  } catch {
    /* 忽略 sessionStorage 移除异常 */
  }
}
