import { sm2, sm4 } from 'sm-crypto'

/**
 * 生成 32 位随机 Nonce 字符串
 */
export function generateNonce(): string {
  const chars = 'abcdef0123456789'
  let nonce = ''
  for (let i = 0; i < 32; i++) {
    nonce += chars.charAt(Math.floor(Math.random() * chars.length))
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
 * SM4 加密（CBC 模式 / PKCS7Padding）
 * @param plainText 明文字符串
 * @param secretKey 16 字节密钥字符串
 */
export function encryptSm4(plainText: string, secretKey = 'WuAdmin16BytesKey'): string {
  if (!plainText) return ''
  try {
    // sm4.encrypt 接收字符串或 byte 数组，返回 hex 或 base64
    return sm4.encrypt(plainText, secretKey)
  } catch (err) {
    console.error('SM4 加密失败:', err)
    return plainText
  }
}

/**
 * SM4 解密
 * @param cipherText 密文字符串
 * @param secretKey 16 字节密钥字符串
 */
export function decryptSm4(cipherText: string, secretKey = 'WuAdmin16BytesKey'): string {
  if (!cipherText) return ''
  try {
    return sm4.decrypt(cipherText, secretKey)
  } catch (err) {
    console.error('SM4 解密失败:', err)
    return cipherText
  }
}

/**
 * SM2 签名计算
 * @param content 待签名明文内容
 * @param privateKeyHex SM2 私钥 Hex 字符串
 */
export function signSm2(content: string, privateKeyHex: string): string {
  if (!content || !privateKeyHex) return ''
  try {
    return sm2.doSignature(content, privateKeyHex)
  } catch (err) {
    console.error('SM2 签名失败:', err)
    return ''
  }
}

/**
 * SM2 验签
 * @param content 签名明文内容
 * @param signature 签名 Hex 字符串
 * @param publicKeyHex SM2 公钥 Hex 字符串
 */
export function verifySm2(content: string, signature: string, publicKeyHex: string): boolean {
  if (!content || !signature || !publicKeyHex) return false
  try {
    return sm2.doVerifySignature(content, signature, publicKeyHex)
  } catch (err) {
    console.error('SM2 验签失败:', err)
    return false
  }
}
