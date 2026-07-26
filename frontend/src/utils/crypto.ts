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
 * sm-crypto 的 sm4 要求 key 为 32 字符 hex 串（16 字节），而后端 hutool
 * SmUtil.sm4(keyBytes) 直接对 key 字符串做 UTF-8 编码得到 16 字节。
 * 这里把原始 key 转为等价 hex，确保前后端使用相同的 16 字节密钥。
 */
function ensureSm4HexKey(rawKey: string): string {
  // 已经是标准 32 字符 hex 则直接使用
  if (rawKey.length === 32 && /^[0-9a-fA-F]{32}$/.test(rawKey)) return rawKey
  let hex = ''
  for (let i = 0; i < rawKey.length; i++) {
    hex += rawKey.charCodeAt(i).toString(16).padStart(2, '0')
  }
  return hex
}

/**
 * SM4 加密（ECB 模式 / PKCS7Padding）
 * 与后端 hutool SmUtil.sm4(keyBytes) 默认行为保持一致。
 * sm-crypto 的 sm4.encrypt 默认是 CBC 模式且需要 16 字节 IV，不显式指定
 * options 会报 `key is invalid`，所以必须固定 mode: 'ecb'。
 *
 * 注意：加密失败时**直接抛错**，不再静默回退到明文——避免「开关打开但实际裸数据」导致
 * 后端误以为密文而解密失败的 403 死循环。调用方（request.ts）需在已确认下发密钥的前提下调用。
 * @param plainText 明文字符串
 * @param secretKey 16 字节密钥字符串
 */
export function encryptSm4(plainText: string, secretKey: string): string {
  if (!plainText) return ''
  if (!secretKey) {
    throw new Error('SM4 加密失败：密钥未下发')
  }
  const hexKey = ensureSm4HexKey(secretKey)
  // 后端 ApiSecurityFilter 用 sm4.decryptStr(...) 解析请求体，期望 hex 字符串
  return sm4.encrypt(plainText, hexKey, { mode: 'ecb', padding: 'pkcs#7' } as any)
}

/**
 * SM4 解密（ECB 模式 / PKCS7Padding）
 * 与后端 hutool SmUtil.sm4(keyBytes).encryptBase64(plainResp) 输出对齐。
 * @param cipherText 密文字符串
 * @param secretKey 16 字节密钥字符串
 */
export function decryptSm4(cipherText: string, secretKey: string): string {
  if (!cipherText) return ''
  if (!secretKey) {
    console.warn('SM4 密钥缺失，跳过解密')
    return cipherText
  }
  try {
    const hexKey = ensureSm4HexKey(secretKey)
    const bytes = base64ToBytes(cipherText)
    return sm4.decrypt(bytes, hexKey, {
      mode: 'ecb',
      padding: 'pkcs#7',
    } as any)
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

/**
 * 将 Base64 字符串转换为字节数组，兼容浏览器与小程序环境
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
  // 备用纯 JS 实现
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
