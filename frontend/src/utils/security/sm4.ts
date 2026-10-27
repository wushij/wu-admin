/**
 * 前端国密 SM4 分组密码算法与 SM4-GCM 满血标准认证加解密实现
 * 标准依据：GM/T 0002-2012 (SM4 分组密码算法) / GM/T 0010-2012 (分组密码工作模式)
 * 特性：100% 纯 TypeScript 实现，零外部依赖，与 Java BouncyCastle 互通。
 */

// ==========================================
// 1. SM4 基础常数与 S 盒
// ==========================================

const SBOX: Uint8Array = new Uint8Array([
  0xd6, 0x90, 0xe9, 0xfe, 0xcc, 0xe1, 0x3d, 0xb7, 0x16, 0xb6, 0x14, 0xc2, 0x28, 0xfb, 0x2c, 0x05,
  0x2b, 0x67, 0x9a, 0x76, 0x2a, 0xbe, 0x04, 0xc3, 0xaa, 0x44, 0x13, 0x26, 0x49, 0x86, 0x06, 0x99,
  0x9c, 0x42, 0x50, 0xf4, 0x91, 0xef, 0x98, 0x7a, 0x33, 0x54, 0x0b, 0x43, 0xed, 0xcf, 0xac, 0x62,
  0xe4, 0xb3, 0x1c, 0xa9, 0xc9, 0x08, 0xe8, 0x95, 0x80, 0xdf, 0x94, 0xfa, 0x75, 0x8f, 0x3f, 0xa6,
  0x47, 0x07, 0xa7, 0xfc, 0xf3, 0x73, 0x17, 0xba, 0x83, 0x59, 0x3c, 0x19, 0xe6, 0x85, 0x4f, 0xa8,
  0x68, 0x6b, 0x81, 0xb2, 0x71, 0x64, 0xda, 0x8b, 0xf8, 0xeb, 0x0f, 0x4b, 0x70, 0x56, 0x9d, 0x35,
  0x1e, 0x24, 0x0e, 0x5e, 0x63, 0x58, 0xd1, 0xa2, 0x25, 0x22, 0x7c, 0x3b, 0x01, 0x21, 0x78, 0x87,
  0xd4, 0x00, 0x46, 0x57, 0x9f, 0xd3, 0x27, 0x52, 0x4c, 0x36, 0x02, 0xe7, 0xa0, 0xc4, 0xc8, 0x9e,
  0xea, 0xbf, 0x8a, 0xd2, 0x40, 0xc7, 0x38, 0xb5, 0xa3, 0xf7, 0xf2, 0xce, 0xf9, 0x61, 0x15, 0xa1,
  0xe0, 0xae, 0x5d, 0xa4, 0x9b, 0x34, 0x1a, 0x55, 0xad, 0x93, 0x32, 0x30, 0xf5, 0x8c, 0xb1, 0xe3,
  0x1d, 0xf6, 0xe2, 0x2e, 0x82, 0x66, 0xca, 0x60, 0xc0, 0x29, 0x23, 0xab, 0x0d, 0x53, 0x4e, 0x6f,
  0xd5, 0xdb, 0x37, 0x45, 0xde, 0xfd, 0x8e, 0x2f, 0x03, 0xff, 0x6a, 0x72, 0x6d, 0x6c, 0x5b, 0x51,
  0x8d, 0x1b, 0xaf, 0x92, 0xbb, 0xdd, 0xbc, 0x7f, 0x11, 0xd9, 0x5c, 0x41, 0x1f, 0x10, 0x5a, 0xd8,
  0x0a, 0xc1, 0x31, 0x88, 0xa5, 0xcd, 0x7b, 0xbd, 0x2d, 0x74, 0xd0, 0x12, 0xb8, 0xe5, 0xb4, 0xb0,
  0x89, 0x69, 0x97, 0x4a, 0x0c, 0x96, 0x77, 0x7e, 0x65, 0xb9, 0xf1, 0x09, 0xc5, 0x6e, 0xc6, 0x84,
  0x18, 0xf0, 0x7d, 0xec, 0x3a, 0xdc, 0x4d, 0x20, 0x79, 0xee, 0x5f, 0x3e, 0xd7, 0xcb, 0x39, 0x48,
])

const FK: Uint32Array = new Uint32Array([0xa3b1bac6, 0x56aa3350, 0x677d9197, 0xb27022dc])

const CK: Uint32Array = (function () {
  const ck = new Uint32Array(32)
  for (let i = 0; i < 32; i++) {
    const b0 = ((4 * i + 0) * 7) % 256
    const b1 = ((4 * i + 1) * 7) % 256
    const b2 = ((4 * i + 2) * 7) % 256
    const b3 = ((4 * i + 3) * 7) % 256
    ck[i] = ((b0 << 24) | (b1 << 16) | (b2 << 8) | b3) >>> 0
  }
  return ck
})()

function rotl(x: number, n: number): number {
  return ((x << n) | (x >>> (32 - n))) >>> 0
}

function tau(a: number): number {
  const b0 = SBOX[(a >>> 24) & 0xff]
  const b1 = SBOX[(a >>> 16) & 0xff]
  const b2 = SBOX[(a >>> 8) & 0xff]
  const b3 = SBOX[a & 0xff]
  return ((b0 << 24) | (b1 << 16) | (b2 << 8) | b3) >>> 0
}

function sm4L(b: number): number {
  return (b ^ rotl(b, 2) ^ rotl(b, 10) ^ rotl(b, 18) ^ rotl(b, 24)) >>> 0
}

function sm4LPrime(b: number): number {
  return (b ^ rotl(b, 13) ^ rotl(b, 23)) >>> 0
}

// ==========================================
// 2. SM4 轮密钥扩展与单分组加解密
// ==========================================

export class SM4Cipher {
  private rk = new Uint32Array(32)

  constructor(keyBytes: Uint8Array) {
    if (keyBytes.length !== 16) {
      throw new Error(`国密 SM4 密钥必须为 16 字节，当前为 ${keyBytes.length} 字节`)
    }
    this.keyExpansion(keyBytes)
  }

  private keyExpansion(key: Uint8Array): void {
    const mk = new Uint32Array(4)
    for (let i = 0; i < 4; i++) {
      mk[i] =
        ((key[i * 4] << 24) |
          (key[i * 4 + 1] << 16) |
          (key[i * 4 + 2] << 8) |
          key[i * 4 + 3]) >>>
        0
    }

    const k = new Uint32Array(36)
    k[0] = (mk[0] ^ FK[0]) >>> 0
    k[1] = (mk[1] ^ FK[1]) >>> 0
    k[2] = (mk[2] ^ FK[2]) >>> 0
    k[3] = (mk[3] ^ FK[3]) >>> 0

    for (let i = 0; i < 32; i++) {
      const temp = (k[i + 1] ^ k[i + 2] ^ k[i + 3] ^ CK[i]) >>> 0
      k[i + 4] = (k[i] ^ sm4LPrime(tau(temp))) >>> 0
      this.rk[i] = k[i + 4]
    }
  }

  public encryptBlock(inBlock: Uint8Array): Uint8Array {
    const x = new Uint32Array(36)
    for (let i = 0; i < 4; i++) {
      x[i] =
        ((inBlock[i * 4] << 24) |
          (inBlock[i * 4 + 1] << 16) |
          (inBlock[i * 4 + 2] << 8) |
          inBlock[i * 4 + 3]) >>>
        0
    }

    for (let i = 0; i < 32; i++) {
      const temp = (x[i + 1] ^ x[i + 2] ^ x[i + 3] ^ this.rk[i]) >>> 0
      x[i + 4] = (x[i] ^ sm4L(tau(temp))) >>> 0
    }

    const outBlock = new Uint8Array(16)
    for (let i = 0; i < 4; i++) {
      const word = x[35 - i]
      outBlock[i * 4] = (word >>> 24) & 0xff
      outBlock[i * 4 + 1] = (word >>> 16) & 0xff
      outBlock[i * 4 + 2] = (word >>> 8) & 0xff
      outBlock[i * 4 + 3] = word & 0xff
    }
    return outBlock
  }
}

// ==========================================
// 3. GHASH 算法实现 (Galois Field GF(2^128) 乘法)
// ==========================================

export class GHASH {
  private HWords: Uint32Array

  constructor(H: Uint8Array) {
    this.HWords = new Uint32Array(4)
    for (let i = 0; i < 4; i++) {
      this.HWords[i] =
        ((H[i * 4] << 24) | (H[i * 4 + 1] << 16) | (H[i * 4 + 2] << 8) | H[i * 4 + 3]) >>> 0
    }
  }

  private multiplyBlock(x: Uint32Array, y: Uint32Array, out: Uint32Array): void {
    let z0 = 0,
      z1 = 0,
      z2 = 0,
      z3 = 0
    let v0 = y[0] >>> 0,
      v1 = y[1] >>> 0,
      v2 = y[2] >>> 0,
      v3 = y[3] >>> 0

    const R = 0xe1000000 >>> 0

    for (let i = 0; i < 128; i++) {
      const wordIdx = (i / 32) | 0
      const bitIdx = 31 - (i % 32)
      if ((x[wordIdx] >>> bitIdx) & 1) {
        z0 = (z0 ^ v0) >>> 0
        z1 = (z1 ^ v1) >>> 0
        z2 = (z2 ^ v2) >>> 0
        z3 = (z3 ^ v3) >>> 0
      }

      const lsb = v3 & 1
      v3 = ((v3 >>> 1) | ((v2 & 1) << 31)) >>> 0
      v2 = ((v2 >>> 1) | ((v1 & 1) << 31)) >>> 0
      v1 = ((v1 >>> 1) | ((v0 & 1) << 31)) >>> 0
      v0 = (v0 >>> 1) >>> 0

      if (lsb) {
        v0 = (v0 ^ R) >>> 0
      }
    }

    out[0] = z0
    out[1] = z1
    out[2] = z2
    out[3] = z3
  }

  public calculate(aad: Uint8Array, ciphertext: Uint8Array): Uint8Array {
    const state = new Uint32Array(4)

    const processBlocks = (data: Uint8Array) => {
      const len = data.length
      const blockCount = Math.ceil(len / 16)
      for (let b = 0; b < blockCount; b++) {
        const offset = b * 16
        const block = new Uint32Array(4)
        for (let j = 0; j < 4; j++) {
          const idx = offset + j * 4
          const b0 = idx < len ? data[idx] : 0
          const b1 = idx + 1 < len ? data[idx + 1] : 0
          const b2 = idx + 2 < len ? data[idx + 2] : 0
          const b3 = idx + 3 < len ? data[idx + 3] : 0
          block[j] = ((b0 << 24) | (b1 << 16) | (b2 << 8) | b3) >>> 0
        }
        state[0] = (state[0] ^ block[0]) >>> 0
        state[1] = (state[1] ^ block[1]) >>> 0
        state[2] = (state[2] ^ block[2]) >>> 0
        state[3] = (state[3] ^ block[3]) >>> 0
        this.multiplyBlock(state, this.HWords, state)
      }
    }

    if (aad && aad.length > 0) processBlocks(aad)
    if (ciphertext && ciphertext.length > 0) processBlocks(ciphertext)

    // 填充 AAD 位长度与 Ciphertext 位长度 (大端 64 位整数)
    const lenBlock = new Uint32Array(4)
    const aadBits = (aad ? aad.length : 0) * 8
    const cipherBits = (ciphertext ? ciphertext.length : 0) * 8

    lenBlock[0] = Math.floor(aadBits / 0x100000000) >>> 0
    lenBlock[1] = aadBits >>> 0
    lenBlock[2] = Math.floor(cipherBits / 0x100000000) >>> 0
    lenBlock[3] = cipherBits >>> 0

    state[0] = (state[0] ^ lenBlock[0]) >>> 0
    state[1] = (state[1] ^ lenBlock[1]) >>> 0
    state[2] = (state[2] ^ lenBlock[2]) >>> 0
    state[3] = (state[3] ^ lenBlock[3]) >>> 0
    this.multiplyBlock(state, this.HWords, state)

    const out = new Uint8Array(16)
    for (let i = 0; i < 4; i++) {
      out[i * 4] = (state[i] >>> 24) & 0xff
      out[i * 4 + 1] = (state[i] >>> 16) & 0xff
      out[i * 4 + 2] = (state[i] >>> 8) & 0xff
      out[i * 4 + 3] = state[i] & 0xff
    }
    return out
  }
}

// ==========================================
// 4. 工具转换函数与 GCM 认证编解码
// ==========================================

export function stringToLatin1Bytes(str: string): Uint8Array {
  const bytes = new Uint8Array(str.length)
  for (let i = 0; i < str.length; i++) {
    bytes[i] = str.charCodeAt(i) & 0xff
  }
  return bytes
}

export function stringToUtf8Bytes(str: string): Uint8Array {
  return new TextEncoder().encode(str)
}

export function utf8BytesToString(bytes: Uint8Array): string {
  return new TextDecoder('utf-8').decode(bytes)
}

export function bytesToBase64(bytes: Uint8Array): string {
  if (typeof btoa === 'function') {
    let binary = ''
    const len = bytes.byteLength
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i])
    }
    return btoa(binary)
  }
  return Buffer.from(bytes).toString('base64')
}

export function base64ToBytes(base64: string): Uint8Array {
  if (typeof atob === 'function') {
    const binary = atob(base64)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    return bytes
  }
  return new Uint8Array(Buffer.from(base64, 'base64'))
}

export function generateRandomBytes(length: number): Uint8Array {
  const bytes = new Uint8Array(length)
  if (typeof window !== 'undefined' && window.crypto && window.crypto.getRandomValues) {
    window.crypto.getRandomValues(bytes)
  } else if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
    crypto.getRandomValues(bytes)
  } else {
    for (let i = 0; i < length; i++) {
      bytes[i] = Math.floor(Math.random() * 256)
    }
  }
  return bytes
}

/**
 * 将密钥材料统一解析为严格 16 字节 Uint8Array
 * 支持：32 位十六进制字符串（session-sign-init 协商下发）、16 字符字符串、Uint8Array
 */
export function resolveKeyBytes(key: string | Uint8Array): Uint8Array {
  if (key instanceof Uint8Array) {
    if (key.length !== 16) {
      throw new Error(`国密 SM4 密钥必须为 16 字节，当前为 ${key.length} 字节`)
    }
    return key
  }
  const trimmed = (key || '').trim()
  if (trimmed.length === 32 && /^[0-9a-fA-F]{32}$/.test(trimmed)) {
    const bytes = new Uint8Array(16)
    for (let i = 0; i < 16; i++) {
      bytes[i] = parseInt(trimmed.substring(i * 2, i * 2 + 2), 16)
    }
    return bytes
  }
  const latin1 = stringToLatin1Bytes(trimmed)
  if (latin1.length === 16) {
    return latin1
  }
  const utf8 = stringToUtf8Bytes(trimmed)
  if (utf8.length === 16) {
    return utf8
  }
  throw new Error(`国密 SM4 密钥必须为 16 字节，当前解码长度为 ${latin1.length} 字节`)
}

/**
 * 国密 SM4-GCM 满血标准认证加密
 * @param key16 16 字节密钥（支持 32 位 Hex 字符串、16 字符字符串或 Uint8Array）
 * @param plainText 待加密的明文字符串
 * @param iv12 可选 12 字节 IV（不传则自动生成 96 位安全随机数）
 * @param aad 可选附加认证数据
 * @returns 标准 Base64 密文字符串：Base64(IV(12B) + Ciphertext + Tag(16B))
 */
export function sm4GcmEncrypt(
  key16: string | Uint8Array,
  plainText: string,
  iv12?: Uint8Array,
  aad?: Uint8Array,
): string {
  const keyBytes = resolveKeyBytes(key16)
  const iv = iv12 && iv12.length === 12 ? iv12 : generateRandomBytes(12)
  const plainBytes = stringToUtf8Bytes(plainText)

  const cipher = new SM4Cipher(keyBytes)

  // 1. 计算 H = SM4(K, 0^128)
  const zeroBlock = new Uint8Array(16)
  const H = cipher.encryptBlock(zeroBlock)
  const ghash = new GHASH(H)

  // 2. 构造初始计数器 J0 = IV || 0^31 || 1 (因为 IV 是 12 字节)
  const J0 = new Uint8Array(16)
  J0.set(iv, 0)
  J0[15] = 1

  // 3. 计算 CTR 密钥流加密明文
  const ciphertext = new Uint8Array(plainBytes.length)
  const counter = new Uint8Array(J0)

  const incCounter = (c: Uint8Array) => {
    for (let i = 15; i >= 12; i--) {
      c[i] = (c[i] + 1) & 0xff
      if (c[i] !== 0) break
    }
  }

  const blockCount = Math.ceil(plainBytes.length / 16)
  for (let b = 0; b < blockCount; b++) {
    incCounter(counter)
    const keyStream = cipher.encryptBlock(counter)
    const start = b * 16
    const end = Math.min(start + 16, plainBytes.length)
    for (let i = start; i < end; i++) {
      ciphertext[i] = plainBytes[i] ^ keyStream[i - start]
    }
  }

  // 4. 计算 GHASH 生成 Tag
  const S = ghash.calculate(aad || new Uint8Array(0), ciphertext)
  const J0Enc = cipher.encryptBlock(J0)
  const tag = new Uint8Array(16)
  for (let i = 0; i < 16; i++) {
    tag[i] = S[i] ^ J0Enc[i]
  }

  // 5. 组合并返回 Base64(IV + Ciphertext + Tag)
  const result = new Uint8Array(12 + ciphertext.length + 16)
  result.set(iv, 0)
  result.set(ciphertext, 12)
  result.set(tag, 12 + ciphertext.length)

  return bytesToBase64(result)
}

/**
 * 国密 SM4-GCM 满血标准认证解密
 * @param key16 16 字节密钥（支持 32 位 Hex 字符串、16 字符字符串或 Uint8Array）
 * @param base64Payload 格式为 Base64(IV(12B) + Ciphertext + Tag(16B))
 * @param aad 可选附加认证数据
 * @returns 解密还原后的明文字符串
 * @throws 当密文损坏、认证 Tag 不匹配或密钥错误时抛出异常 (Fail-Closed)
 */
export function sm4GcmDecrypt(
  key16: string | Uint8Array,
  base64Payload: string,
  aad?: Uint8Array,
): string {
  const combined = base64ToBytes(base64Payload.trim())
  if (combined.length < 28) {
    // 至少包含 12 字节 IV + 16 字节 Tag
    throw new Error('SM4-GCM 密文长度非法，有效载荷至少需 28 字节')
  }

  const iv = combined.slice(0, 12)
  const tag = combined.slice(combined.length - 16)
  const ciphertext = combined.slice(12, combined.length - 16)

  const keyBytes = resolveKeyBytes(key16)
  const cipher = new SM4Cipher(keyBytes)

  // 1. 计算 H
  const zeroBlock = new Uint8Array(16)
  const H = cipher.encryptBlock(zeroBlock)
  const ghash = new GHASH(H)

  // 2. 构造 J0
  const J0 = new Uint8Array(16)
  J0.set(iv, 0)
  J0[15] = 1

  // 3. 校验认证 Tag
  const S = ghash.calculate(aad || new Uint8Array(0), ciphertext)
  const J0Enc = cipher.encryptBlock(J0)
  const expectedTag = new Uint8Array(16)
  let tagMatch = 0
  for (let i = 0; i < 16; i++) {
    expectedTag[i] = S[i] ^ J0Enc[i]
    tagMatch |= tag[i] ^ expectedTag[i]
  }

  // 4. 解密密文还原明文
  const plainBytes = new Uint8Array(ciphertext.length)
  const counter = new Uint8Array(J0)

  const incCounter = (c: Uint8Array) => {
    for (let i = 15; i >= 12; i--) {
      c[i] = (c[i] + 1) & 0xff
      if (c[i] !== 0) break
    }
  }

  const blockCount = Math.ceil(ciphertext.length / 16)
  for (let b = 0; b < blockCount; b++) {
    incCounter(counter)
    const keyStream = cipher.encryptBlock(counter)
    const start = b * 16
    const end = Math.min(start + 16, ciphertext.length)
    for (let i = start; i < end; i++) {
      plainBytes[i] = ciphertext[i] ^ keyStream[i - start]
    }
  }

  if (tagMatch !== 0) {
    throw new Error('SM4-GCM 密文认证未通过（Tag 校验失败），数据可能已被篡改或密钥错误')
  }

  return utf8BytesToString(plainBytes)
}

/**
 * 快速检测字符串是否可能为 SM4-GCM Base64 密文
 */
export function isSm4GcmCipherPayload(str: string): boolean {
  if (!str || typeof str !== 'string') return false
  const trimmed = str.trim()
  if (trimmed.length < 38) return false
  return /^[A-Za-z0-9+/=]+$/.test(trimmed)
}
