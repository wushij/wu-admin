import { encryptSm2 } from './crypto'
import { getSecurityConfig } from './security-config'

/**
 * 口令传输加密统一入口。
 *
 * <p>前端所有「提交密码」的入口（登录、注册、修改密码、短信/邮箱重置、后台重置）
 * 都应经过这里，避免出现「只有登录加密、其它入口明文」的不一致。</p>
 *
 * <p>当服务端未启用 SM2 或尚未下发公钥时原样返回，保证旧后端与灰度环境可用；
 * 后端 {@code SystemConfigHelper#decryptPasswordIfEncrypted} 对明文同样兼容。</p>
 */
export function encryptPasswordForTransport(plain: string): string {
  if (!plain) return plain
  const secConfig = getSecurityConfig()
  if (!secConfig.sm2EncryptEnabled || !secConfig.sm2PublicKey) {
    return plain
  }
  return encryptSm2(plain, secConfig.sm2PublicKey)
}
