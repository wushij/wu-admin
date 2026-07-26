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
 * SM4 加密（国密对称加密）
 */
export function encryptSm4(plainText: string, secretKey: string): string {
  if (!plainText) return ''
  if (!secretKey) {
    console.warn('移动端 SM4 密钥缺失，跳过加密')
    return plainText
  }
  try {
    return sm4.encrypt(plainText, secretKey)
  } catch (err) {
    console.error('移动端 SM4 加密失败:', err)
    return plainText
  }
}

/**
 * SM4 解密
 */
export function decryptSm4(cipherText: string, secretKey: string): string {
  if (!cipherText) return ''
  if (!secretKey) {
    console.warn('移动端 SM4 密钥缺失，跳过解密')
    return cipherText
  }
  try {
    return sm4.decrypt(cipherText, secretKey)
  } catch (err) {
    console.error('移动端 SM4 解密失败:', err)
    return cipherText
  }
}

/**
 * SM2 数字签名
 */
export function signSm2(content: string, privateKeyHex: string): string {
  if (!content || !privateKeyHex) return ''
  try {
    return sm2.doSignature(content, privateKeyHex)
  } catch (err) {
    console.error('移动端 SM2 签名失败:', err)
    return ''
  }
}
