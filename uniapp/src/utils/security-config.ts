/**
 * 移动端接口安全配置（SM4 加密 / SM2 签名）
 *
 * 配置保存在模块闭包中，代码中不允许出现硬编码密钥。
 * 密钥仅接受运行时下发（如 /auth/config），不进行本地持久化，安全度高。
 */
export interface ClientSecurityConfig {
  /** 是否启用接口请求/响应 SM4 加密 */
  sm4EncryptEnabled?: boolean
  /** 是否启用接口 SM2 签名 */
  sm2SignEnabled?: boolean
  /** SM4 对称密钥（16 字节），未下发时不加密 */
  sm4Key?: string
  /** SM2 签名私钥 Hex，未下发时不签名 */
  sm2PrivateKey?: string
}

let securityConfig: ClientSecurityConfig = {}

/** 合并更新安全配置（仅覆盖传入字段） */
export function setSecurityConfig(next: ClientSecurityConfig): void {
  securityConfig = { ...securityConfig, ...next }
}

/** 读取当前安全配置快照 */
export function getSecurityConfig(): Readonly<ClientSecurityConfig> {
  return securityConfig
}

/** 重置安全配置（登出或测试场景使用） */
export function resetSecurityConfig(): void {
  securityConfig = {}
}
