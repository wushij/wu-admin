/**
 * 移动端国密 SM4 分组密码算法与 SM4-GCM 满血标准认证加解密实现
 * 标准依据：GM/T 0002-2012 (SM4 分组密码算法) / GM/T 0010-2012 (分组密码工作模式)
 * 特性：100% 纯 TypeScript 实现，零外部依赖，跨平台（H5 / 微信小程序 / App）安全运行，与 Java BouncyCastle 逐字节互通。
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
    const v = new Uint32Array(4)
    v[0] = y[0]
    v[1] = y[1]
    v[2] = y[2]
    v[3] = y[3]

    const z = new Uint32Array(4)
    const R = 0xe1000000 >>> 0

    for (let i = 0; i < 128; i++) {
      const wordIdx = Math.floor(i / 32)
      const bitIdx = 31 - (i % 32)
      if (((x[wordIdx] >>> bitIdx) & 1) !== 0) {
        z[0] = (z[0] ^ v[0]) >>> 0
        z[1] = (z[1] ^ v[1]) >>> 0
        z[2] = (z[2] ^ v[2]) >>> 0
        z[3] = (z[3] ^ v[3]) >>> 0
      }

      const lsb = (v[3] & 1) !== 0
      v[3] = ((v[3] >>> 1) | ((v[2] & 1) << 31)) >>> 0
      v[2] = ((v[2] >>> 1) | ((v[1] & 1) << 31)) >>> 0
      v[1] = ((v[1] >>> 1) | ((v[0] & 1) << 31)) >>> 0
      v[0] = (v[0] >>> 1) >>> 0

      if (lsb) {
        v[0] = (v[0] ^ R) >>> 0
      }
    }

    out[0] = z[0]
    out[1] = z[1]
    out[2] = z[2]
    out[3] = z[3]
  }

  public calculate(aad: Uint8Array, ciphertext: Uint8Array): Uint8Array {
    const y = new Uint32Array(4)
    const block = new Uint32Array(4)
    const temp = new Uint32Array(4)

    const processData = (data: Uint8Array) => {
      const len = data.length
      const fullBlocks = Math.floor(len / 16)
      for (let b = 0; b < fullBlocks; b++) {
        const offset = b * 16
        for (let i = 0; i < 4; i++) {
          block[i] =
            ((data[offset + i * 4] << 24) |
              (data[offset + i * 4 + 1] << 16) |
              (data[offset + i * 4 + 2] << 8) |
              data[offset + i * 4 + 3]) >>>
            0
        }
        y[0] = (y[0] ^ block[0]) >>> 0
        y[1] = (y[1] ^ block[1]) >>> 0
        y[2] = (y[2] ^ block[2]) >>> 0
        y[3] = (y[3] ^ block[3]) >>> 0
        this.multiplyBlock(y, this.HWords, temp)
        y[0] = temp[0]
        y[1] = temp[1]
        y[2] = temp[2]
        y[3] = temp[3]
      }

      const remainder = len % 16
      if (remainder > 0) {
        const padded = new Uint8Array(16)
        padded.set(data.subarray(fullBlocks * 16), 0)
        for (let i = 0; i < 4; i++) {
          block[i] =
            ((padded[i * 4] << 24) |
              (padded[i * 4 + 1] << 16) |
              (padded[i * 4 + 2] << 8) |
              padded[i * 4 + 3]) >>>
            0
        }
        y[0] = (y[0] ^ block[0]) >>> 0
        y[1] = (y[1] ^ block[1]) >>> 0
        y[2] = (y[2] ^ block[2]) >>> 0
        y[3] = (y[3] ^ block[3]) >>> 0
        this.multiplyBlock(y, this.HWords, temp)
        y[0] = temp[0]
        y[1] = temp[1]
        y[2] = temp[2]
        y[3] = temp[3]
      }
    }

    if (aad && aad.length > 0) {
      processData(aad)
    }

    if (ciphertext && ciphertext.length > 0) {
      processData(ciphertext)
    }

    // 长度块：[ len(A)*8 (64位大端) || len(C)*8 (64位大端) ]
    // 用 Number 拆分高低 32 位，避免 BigInt 在低版本小程序/旧 WebView 解析阶段直接失败
    const aadBits = (aad ? aad.length : 0) * 8
    const cipherBits = (ciphertext ? ciphertext.length : 0) * 8

    block[0] = Math.floor(aadBits / 0x100000000) >>> 0
    block[1] = aadBits >>> 0
    block[2] = Math.floor(cipherBits / 0x100000000) >>> 0
    block[3] = cipherBits >>> 0

    y[0] = (y[0] ^ block[0]) >>> 0
    y[1] = (y[1] ^ block[1]) >>> 0
    y[2] = (y[2] ^ block[2]) >>> 0
    y[3] = (y[3] ^ block[3]) >>> 0
    this.multiplyBlock(y, this.HWords, temp)

    const out = new Uint8Array(16)
    for (let i = 0; i < 4; i++) {
      out[i * 4] = (temp[i] >>> 24) & 0xff
      out[i * 4 + 1] = (temp[i] >>> 16) & 0xff
      out[i * 4 + 2] = (temp[i] >>> 8) & 0xff
      out[i * 4 + 3] = temp[i] & 0xff
    }
    return out
  }
}

// ==========================================
// 4. 辅助函数（跨端字符编解码、安全随机数）
// ==========================================

export function stringToLatin1Bytes(str: string): Uint8Array {
  const bytes = new Uint8Array(str.length)
  for (let i = 0; i < str.length; i++) {
    bytes[i] = str.charCodeAt(i) & 0xff
  }
  return bytes
}

export function stringToUtf8Bytes(str: string): Uint8Array {
  if (typeof TextEncoder !== 'undefined') {
    return new TextEncoder().encode(str)
  }
  // 兼容小程序无 TextEncoder 环境
  const utf8: number[] = []
  for (let i = 0; i < str.length; i++) {
    let charcode = str.charCodeAt(i)
    if (charcode < 0x80) utf8.push(charcode)
    else if (charcode < 0x800) {
      utf8.push(0xc0 | (charcode >> 6), 0x80 | (charcode & 0x3f))
    } else if (charcode < 0xd800 || charcode >= 0xe000) {
      utf8.push(0xe0 | (charcode >> 12), 0x80 | ((charcode >> 6) & 0x3f), 0x80 | (charcode & 0x3f))
    } else {
      i++
      charcode = 0x10000 + (((charcode & 0x3ff) << 10) | (str.charCodeAt(i) & 0x3ff))
      utf8.push(
        0xf0 | (charcode >> 18),
        0x80 | ((charcode >> 12) & 0x3f),
        0x80 | ((charcode >> 6) & 0x3f),
        0x80 | (charcode & 0x3f),
      )
    }
  }
  return new Uint8Array(utf8)
}

export function utf8BytesToString(bytes: Uint8Array): string {
  if (typeof TextDecoder !== 'undefined') {
    return new TextDecoder('utf-8').decode(bytes)
  }
  let out = ''
  let i = 0
  const len = bytes.length
  while (i < len) {
    const c = bytes[i++]
    if (c >> 4 <= 7) {
      out += String.fromCharCode(c)
    } else if (c >> 4 === 12 || c >> 4 === 13) {
      const char2 = bytes[i++]
      out += String.fromCharCode(((c & 0x1f) << 6) | (char2 & 0x3f))
    } else if (c >> 4 === 14) {
      const char2 = bytes[i++]
      const char3 = bytes[i++]
      out += String.fromCharCode(((c & 0x0f) << 12) | ((char2 & 0x3f) << 6) | ((char3 & 0x3f) << 0))
    } else if (c >> 4 === 15) {
      const char2 = bytes[i++]
      const char3 = bytes[i++]
      const char4 = bytes[i++]
      const code =
        (((c & 0x07) << 18) | ((char2 & 0x3f) << 12) | ((char3 & 0x3f) << 6) | (char4 & 0x3f)) -
        0x10000
      out += String.fromCharCode((code >> 10) + 0xd800, (code & 0x3ff) + 0xdc00)
    }
  }
  return out
}

const B64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
const B64_LOOKUP = new Uint8Array(256)
for (let i = 0; i < B64_CHARS.length; i++) {
  B64_LOOKUP[B64_CHARS.charCodeAt(i)] = i
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
  let result = ''
  const len = bytes.length
  for (let i = 0; i < len; i += 3) {
    const b0 = bytes[i]
    const b1 = i + 1 < len ? bytes[i + 1] : 0
    const b2 = i + 2 < len ? bytes[i + 2] : 0
    result += B64_CHARS[(b0 >> 2) & 0x3f]
    result += B64_CHARS[((b0 & 3) << 4) | ((b1 >> 4) & 0x0f)]
    result += i + 1 < len ? B64_CHARS[((b1 & 0x0f) << 2) | ((b2 >> 6) & 0x03)] : '='
    result += i + 2 < len ? B64_CHARS[b2 & 0x3f] : '='
  }
  return result
}

export function base64ToBytes(base64: string): Uint8Array {
  const clean = base64.trim()
  if (typeof atob === 'function') {
    const binary = atob(clean)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    return bytes
  }
  let bufferLength = clean.length * 0.75
  if (clean.endsWith('==')) bufferLength -= 2
  else if (clean.endsWith('=')) bufferLength -= 1
  const bytes = new Uint8Array(bufferLength)
  let p = 0
  for (let i = 0; i < clean.length; i += 4) {
    const b1 = B64_LOOKUP[clean.charCodeAt(i)]
    const b2 = B64_LOOKUP[clean.charCodeAt(i + 1)]
    const b3 = B64_LOOKUP[clean.charCodeAt(i + 2)]
    const b4 = B64_LOOKUP[clean.charCodeAt(i + 3)]
    bytes[p++] = (b1 << 2) | (b2 >> 4)
    if (p < bufferLength) bytes[p++] = ((b2 & 15) << 4) | (b3 >> 2)
    if (p < bufferLength) bytes[p++] = ((b3 & 3) << 6) | b4
  }
  return bytes
}

let randomFallbackCounter = 0

export function generateRandomBytes(length: number): Uint8Array {
  const bytes = new Uint8Array(length)

  // H5 / 新版本小程序 / App WebView：优先使用平台密码学安全随机源
  const webCrypto = typeof globalThis !== 'undefined'
    ? (globalThis as { crypto?: { getRandomValues?: (array: Uint8Array) => void } }).crypto
    : undefined
  if (webCrypto && typeof webCrypto.getRandomValues === 'function') {
    webCrypto.getRandomValues(bytes)
    return bytes
  }

  // 微信小程序部分基础库提供 wx.getRandomValues；不存在时再走兜底
  const wxApi = typeof globalThis !== 'undefined'
    ? (globalThis as {
        wx?: { getRandomValues?: (array: Uint8Array) => void }
      }).wx
    : undefined
  if (wxApi && typeof wxApi.getRandomValues === 'function') {
    wxApi.getRandomValues(bytes)
    return bytes
  }

  // 最后兜底：混合时间、单调计数器与 Math.random。
  // 这里明确不具备密码学强度，仅用于没有安全随机源的老旧运行环境，避免模块直接不可用。
  let seed = (Date.now() ^ (randomFallbackCounter++)) >>> 0
  for (let i = 0; i < length; i++) {
    seed = (seed * 1664525 + 1013904223) >>> 0
    bytes[i] = (seed ^ Math.floor(Math.random() * 256)) & 0xff
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
  throw new Error(`无法将提供的密钥转换为有效的 16 字节 SM4 密钥 (Hex 长度: ${trimmed.length})`)
}

// ==========================================
// 5. SM4-GCM 满血加解密对外 API
// ==========================================

/**
 * 国密 SM4-GCM 满血标准认证加密
 * @param key16 16 字节密钥（支持 32 位 Hex 字符串、16 字符字符串或 Uint8Array）
 * @param plainText 待加密明文字符串
 * @param explicitIv 可选自定义 12 字节 IV（缺省时自动生成密码学强随机 12 字节 IV）
 * @param aad 可选附加认证数据 (Additional Authenticated Data)
 * @returns 规范认证密文 Base64(IV(12B) + Ciphertext + Tag(16B))
 */
export function sm4GcmEncrypt(
  key16: string | Uint8Array,
  plainText: string,
  explicitIv?: Uint8Array,
  aad?: Uint8Array,
): string {
  if (plainText === undefined || plainText === null) {
    plainText = ''
  }
  const plainBytes = stringToUtf8Bytes(plainText)
  const keyBytes = resolveKeyBytes(key16)
  const cipher = new SM4Cipher(keyBytes)

  // 1. 初始化 12 字节安全随机 IV
  let iv: Uint8Array
  if (explicitIv) {
    if (explicitIv.length !== 12) {
      throw new Error(`SM4-GCM 标准 IV 长度必须为 12 字节，当前为 ${explicitIv.length} 字节`)
    }
    iv = explicitIv
  } else {
    iv = generateRandomBytes(12)
  }

  // 2. 计算 H = E_K(0^128)
  const zeroBlock = new Uint8Array(16)
  const H = cipher.encryptBlock(zeroBlock)
  const ghash = new GHASH(H)

  // 3. 构造计数器初始值 J0 = IV || 0^31 || 1
  const J0 = new Uint8Array(16)
  J0.set(iv, 0)
  J0[15] = 1

  // 4. CTR 模式加密明文
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

  // 5. 计算 GHASH 生成 Tag
  const S = ghash.calculate(aad || new Uint8Array(0), ciphertext)
  const J0Enc = cipher.encryptBlock(J0)
  const tag = new Uint8Array(16)
  for (let i = 0; i < 16; i++) {
    tag[i] = S[i] ^ J0Enc[i]
  }

  // 6. 组合并返回 Base64(IV + Ciphertext + Tag)
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
