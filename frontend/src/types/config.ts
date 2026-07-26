/** 公开认证配置 /auth/config（site、login、register 等分组） */
export interface SiteConfig {
  platformName?: string
  platformSubtitle?: string
  loginWelcome?: string
  registerTitle?: string
  copyright?: string
}

export interface LoginConfig {
  captchaEnabled?: boolean
  captchaType?: string
  smsLoginEnabled?: boolean
  smsLoginSliderCaptchaEnabled?: boolean
  emailLoginEnabled?: boolean
  emailLoginSliderCaptchaEnabled?: boolean
  rememberMe?: boolean
  smsEnabled?: boolean
  emailEnabled?: boolean
}

export interface RegisterConfig {
  enabled?: boolean
  captchaEnabled?: boolean
  captchaType?: string
  minPasswordLength?: number
}

export interface SecurityPublicConfig {
  disableDevtool?: boolean
  /** 是否启用接口 SM4 加密（密钥由后端运行时下发，前端不硬编码） */
  sm4EncryptEnabled?: boolean
  /** SM4 对称密钥（16 字节），仅在启用加密时下发 */
  sm4Key?: string
}

export interface AuthPublicConfig {
  site?: SiteConfig
  login?: LoginConfig
  register?: RegisterConfig
  security?: SecurityPublicConfig
}

/** 系统配置页各分组（与后端 configValue JSON 结构一致） */
export interface AdminSiteConfig {
  platformName: string
  platformSubtitle: string
  loginWelcome: string
  registerTitle: string
  copyright: string
}

export interface SessionConfig {
  tokenExpireHours: number
}

export interface FileStorageConfig {
  maxSizeMb: number
  allowedExtensions: string
}

export interface RateLimitConfig {
  captchaPerIpMinute: number
  loginPerIpMinute: number
  registerPerIpMinute: number
  smsPerIpMinute: number
  smsSendIntervalSeconds: number
  smsPerPhoneDaily: number
  smsPerIpDaily: number
}

export interface LoginAdminConfig {
  captchaEnabled: boolean
  captchaType: string
  smsLoginEnabled: boolean
  smsLoginSliderCaptchaEnabled: boolean
  emailLoginEnabled?: boolean
  emailLoginSliderCaptchaEnabled?: boolean
  rememberMe: boolean
  maxRetryCount: number
  maxRetryCountIp: number
  lockTime: number
}

export interface RegisterAdminConfig {
  enabled: boolean
  captchaEnabled: boolean
  captchaType: string
  defaultRoleCode: string
  needAudit: boolean
  minPasswordLength: number
  auditorUserIds: number[]
}

export interface SecurityConfig {
  disableDevtool: boolean
  /** Sa-Token is-concurrent，false 表示禁止多端同时在线 */
  isConcurrent: boolean
  /** 是否启用接口请求/响应数据加密 (国密 SM4) */
  sm4EncryptEnabled?: boolean
  /** 是否启用接口数字签名验签 (国密 SM2) */
  sm2SignEnabled?: boolean
  /** 校验请求时间，防止过期请求 (时间戳) */
  timestampEnabled?: boolean
  /** 校验随机数，防止重放攻击 (Nonce) */
  nonceEnabled?: boolean
  /** SM4 秘钥 */
  sm4SecretKey?: string
}

export interface ThirdPartyOAuthConfig {
  enabled: boolean
  appId?: string
  appSecret?: string
  privateKey?: string
  publicKey?: string
  clientId?: string
  clientSecret?: string
}

export interface ThirdPartyConfig {
  wechat: Pick<ThirdPartyOAuthConfig, 'enabled' | 'appId' | 'appSecret'>
  alipay: Pick<ThirdPartyOAuthConfig, 'enabled' | 'appId' | 'privateKey' | 'publicKey'>
  github: Pick<ThirdPartyOAuthConfig, 'enabled' | 'clientId' | 'clientSecret'>
  google: Pick<ThirdPartyOAuthConfig, 'enabled' | 'clientId' | 'clientSecret'> & {
    redirectUri?: string
  }
}

export interface WechatPayConfig {
  enabled: boolean
  mchId: string
  appId: string
  apiV3Key: string
  privateKey: string
  certSerialNo: string
  notifyUrl: string
}

export interface AlipayPayConfig {
  enabled: boolean
  appId: string
  privateKey: string
  publicKey: string
  signType: string
  gatewayUrl: string
  notifyUrl: string
  returnUrl: string
}

export interface PaymentConfig {
  wechatPay: WechatPayConfig
  alipay: AlipayPayConfig
}

export interface SmsConfig {
  enabled: boolean
  provider: 'aliyunAuth' | 'tencent'
  accessKeyId: string
  accessKeySecret: string
  signName: string
  tencentAppId: string
  templateVerifyCode: string
  templateModifyPhone: string
  templateResetPassword: string
  templateBindPhone: string
  templateVerifyBindPhone: string
  /** 阿里云短信认证 CheckSmsVerifyCode 方案名，可留空 */
  schemeName: string
  /** 验证码有效期（分钟），用于短信认证模板 min 参数 */
  codeExpireMinutes: number
}

export interface SmsLogRecord {
  id: number
  phone: string
  content: string
  smsType: string
  templateId: string
  provider: string
  status: number
  resultMsg: string
  bizId: string
  sendTime: string
  createTime: string
}

export interface EmailLogRecord {
  id?: number
  email: string
  subject?: string
  content?: string
  scene?: string
  provider?: string
  status: number
  resultMsg?: string
  ip?: string
  createTime?: string
}

export interface EmailConfig {
  enabled: boolean
  provider: 'qq' | '163' | 'gmail' | 'custom'
  host: string
  port: number
  username: string
  password: string
  fromName: string
  authEnabled: boolean
  securityType: 'SSL' | 'TLS' | 'STARTTLS' | 'NONE'
  connectionTimeoutMs: number
  timeoutMs: number
  writeTimeoutMs: number
  encoding: string
  debug: boolean
  codeExpireMinutes: number
  codeLength: number
  dailyLimitPerEmail: number
  sendIntervalSeconds: number
}

export type ConfigGroupCode =
  | 'site'
  | 'session'
  | 'file'
  | 'rateLimit'
  | 'login'
  | 'register'
  | 'thirdParty'
  | 'payment'
  | 'sms'
  | 'email'
  | 'security'

export interface ConfigGroupMap {
  site: AdminSiteConfig
  session: SessionConfig
  file: FileStorageConfig
  rateLimit: RateLimitConfig
  login: LoginAdminConfig
  register: RegisterAdminConfig
  thirdParty: ThirdPartyConfig
  payment: PaymentConfig
  sms: SmsConfig
  email: EmailConfig
  security: SecurityConfig
}

export interface PayOrderRecord {
  orderNo: string
  payType: string
  status: string
  transactionId?: string
  amount?: string
  createTime?: string
  paidTime?: string
}

