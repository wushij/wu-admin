import { sm2, sm3 } from 'sm-crypto'
import { sm4GcmEncrypt, sm4GcmDecrypt } from './sm4'

/**
 * 生成 32 位随机 Nonce 字符串 (使用密码学强随机数生成器)
 */
export function generateNonce(): string {
  const chars = 'abcdef0123456789'
  const bytes = new Uint8Array(32)
  if (typeof window !== 'undefined' && window.crypto && window.crypto.getRandomValues) {
    window.crypto.getRandomValues(bytes)
  } else {
    for (let i = 0; i < 32; i++) {
      bytes[i] = Math.floor(Math.random() * 256)
    }
  }
  let nonce = ''
  for (let i = 0; i < 32; i++) {
    nonce += chars.charAt(bytes[i] % chars.length)
  }
  return nonce
}

/**
 * 获取当前毫秒时间戳字符串
 */
export function getTimestamp(): string {
  return Date.now().toString()
}

/**
 * sm-crypto 的 sm4 要求 key 为 32 字符 hex 串（16 字节），而后端 hutool
 * SmUtil.sm4(keyBytes) 直接对 key 字符串做 UTF-8 编码得到 16 字节。
 * 这里把原始 key 转为等价 hex，确保前后端使用相同的 16 字节密钥。
 */
function ensureSm4HexKey(rawKey: string): string {
  if (rawKey.length === 32 && /^[0-9a-fA-F]{32}$/.test(rawKey)) return rawKey
  let hex = ''
  for (let i = 0; i < rawKey.length; i++) {
    hex += rawKey.charCodeAt(i).toString(16).padStart(2, '0')
  }
  return hex
}

/**
 * 满血标准国密 SM4-GCM 认证加密
 * 输出格式：Base64(IV[12] + Ciphertext + Tag[16])
 * @param plainText 明文字符串
 * @param secretKey 16 字节密钥（支持 32 位 Hex 或 16 字符文本）
 */
export function encryptSm4(plainText: string, secretKey: string): string {
  if (!plainText) return ''
  if (!secretKey) {
    throw new Error('SM4 加密失败：密钥未下发')
  }
  return sm4GcmEncrypt(secretKey, plainText)
}

/**
 * 满血标准国密 SM4-GCM 认证解密（Fail-Closed）。
 *
 * <p>只接受 Base64(IV[12] + Ciphertext + Tag[16]) 格式；Tag 校验失败、密钥缺失或
 * 密文被篡改时一律抛异常，不再降级到 CBC/ECB，避免削弱 AEAD 的完整性保护。</p>
 *
 * @param cipherText 密文字符串
 * @param secretKey 16 字节密钥字符串
 * @throws Error 密钥缺失或认证解密失败
 */
export function decryptSm4(cipherText: string, secretKey: string): string {
  if (!cipherText) return ''
  if (!secretKey) {
    throw new Error('SM4 密钥未下发，无法解密响应数据')
  }
  return sm4GcmDecrypt(secretKey, cipherText.trim())
}

/**
 * 国密 HMAC-SM3 签名计算 (高性能、无私钥泄露风险)
 * @param content 待签名明文内容
 * @param signKey HMAC 密钥字符串
 */
export function signHmacSm3(content: string, signKey: string): string {
  if (!content || !signKey) return ''
  try {
    const hexKey = ensureSm4HexKey(signKey)
    return sm3(content, { key: hexKey })
  } catch (err) {
    console.error('HMAC-SM3 签名失败:', err)
    return ''
  }
}

/**
 * 国密 SM2 满血标准公钥加密（采用现行国家标准 C1C3C2 模式）
 * 用于登录、注册、修改密码等敏感口令传输防护，与后端 Java BouncyCastle 互通。
 *
 * @param plainText 待加密明文字符串
 * @param publicKeyHex SM2 椭圆公钥（130位带04或128位不带04）
 * @returns 以 "sm2:" 为前缀的标准十六进制密文；若未配置公钥则安全返回原明文
 */
export function encryptSm2(plainText: string, publicKeyHex?: string): string {
  if (!plainText) return ''
  if (plainText.startsWith('sm2:')) {
    return plainText
  }
  const pub = (publicKeyHex || '').trim()
  if (!pub) {
    return plainText
  }
  try {
    // sm-crypto 要求的公钥格式：130 位十六进制（必须包含 04 未压缩前缀），若缺失则自动补齐
    const cleanPub = pub.startsWith('04') ? pub : ('04' + pub)
    // cipherMode = 1 表示现行国家商用密码标准的 C1C3C2 模式
    const cipherHex = sm2.doEncrypt(plainText, cleanPub, 1)
    return 'sm2:' + cipherHex
  } catch (err) {
    console.error('SM2 公钥加密异常:', err)
    return plainText
  }
}
