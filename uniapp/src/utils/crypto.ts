import { sm2, sm3, sm4 } from 'sm-crypto'

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
 * 生成 16 字节 (32 位 Hex) 随机 IV 向量
 */
function generateRandomIvHex(): string {
  const bytes = new Uint8Array(16)
  if (typeof window !== 'undefined' && window.crypto && window.crypto.getRandomValues) {
    window.crypto.getRandomValues(bytes)
  } else {
    for (let i = 0; i < 16; i++) {
      bytes[i] = Math.floor(Math.random() * 256)
    }
  }
  let hex = ''
  for (let i = 0; i < 16; i++) {
    hex += bytes[i].toString(16).padStart(2, '0')
  }
  return hex
}

/**
 * 获取当前毫秒时间戳字符串
 */
export function getTimestamp(): string {
  return Date.now().toString()
}

/**
 * 格式化密钥Hex
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
 * SM4 加密（CBC 模式 + 16 字节随机 IV + PKCS7Padding）
 * 输出格式：IV (32位Hex) + SM4_CBC_Cipher (Hex)
 */
export function encryptSm4(plainText: string, secretKey: string): string {
  if (!plainText) return ''
  if (!secretKey) {
    throw new Error('SM4 加密失败：密钥未下发')
  }
  const hexKey = ensureSm4HexKey(secretKey)
  const hexIv = generateRandomIvHex()
  const cipherHex = sm4.encrypt(plainText, hexKey, {
    mode: 'cbc',
    iv: hexIv,
    padding: 'pkcs#7',
  } as any)
  return hexIv + cipherHex
}

/**
 * SM4 解密（支持 CBC 模式提取 16 字节 IV 向量及旧版 ECB 降级）
 */
export function decryptSm4(cipherText: string, secretKey: string): string {
  if (!cipherText) return ''
  if (!secretKey) {
    console.warn('移动端 SM4 密钥缺失，跳过解密')
    return cipherText
  }
  try {
    const hexKey = ensureSm4HexKey(secretKey)
    const text = cipherText.trim()
    if (text.length > 32 && /^[0-9a-fA-F]+$/.test(text)) {
      const hexIv = text.substring(0, 32)
      const rawCipher = text.substring(32)
      try {
        const decrypted = sm4.decrypt(rawCipher, hexKey, {
          mode: 'cbc',
          iv: hexIv,
          padding: 'pkcs#7',
        } as any)
        if (decrypted) return decrypted
      } catch {
        /* CBC 降级 */
      }
    }
    const bytes = base64ToBytes(text)
    return sm4.decrypt(bytes, hexKey, {
      mode: 'ecb',
      padding: 'pkcs#7',
    } as any)
  } catch (err) {
    console.error('移动端 SM4 解密失败:', err)
    return cipherText
  }
}

/**
 * 国密 HMAC-SM3 签名计算
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
 * SM2 签名计算
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

/**
 * 将 Base64 字符串转换为字节数组
 */
function base64ToBytes(base64: string): number[] {
  if (typeof atob === 'function') {
    const binary = atob(base64)
    const bytes = new Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    return bytes
  }
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
  const lookup = new Uint8Array(256)
  for (let i = 0; i < chars.length; i++) {
    lookup[chars.charCodeAt(i)] = i
  }
  let bufferLength = base64.length * 0.75
  if (base64[base64.length - 1] === '=') {
    bufferLength--
    if (base64[base64.length - 2] === '=') {
      bufferLength--
    }
  }
  const bytes = new Array(bufferLength)
  let p = 0
  for (let i = 0; i < base64.length; i += 4) {
    const b1 = lookup[base64.charCodeAt(i)]
    const b2 = lookup[base64.charCodeAt(i + 1)]
    const b3 = lookup[base64.charCodeAt(i + 2)]
    const b4 = lookup[base64.charCodeAt(i + 3)]
    bytes[p++] = (b1 << 2) | (b2 >> 4)
    if (p < bufferLength) bytes[p++] = ((b2 & 15) << 4) | (b3 >> 2)
    if (p < bufferLength) bytes[p++] = ((b3 & 3) << 6) | b4
  }
  return bytes
}
