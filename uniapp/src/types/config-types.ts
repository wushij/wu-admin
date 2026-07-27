/** 公开认证配置 /auth/config */
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

export interface SiteAdminConfig {
  platformName: string
  platformSubtitle: string
  loginWelcome: string
  registerTitle: string
  copyright: string
}

export interface SessionAdminConfig {
  tokenExpireHours: number
}

export interface SecurityAdminConfig {
  disableDevtool: boolean
  isConcurrent: boolean
  sm4EncryptEnabled?: boolean
  sm2SignEnabled?: boolean
  timestampEnabled?: boolean
  nonceEnabled?: boolean
  sm4SecretKey?: string
}

export interface SmsAdminConfig {
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
  schemeName: string
  codeExpireMinutes: number
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

export interface ThirdPartyOAuthConfig {
  enabled: boolean
  appId?: string
  appSecret?: string
  privateKey?: string
  publicKey?: string
  clientId?: string
  clientSecret?: string
  redirectUri?: string
}

export interface ThirdPartyConfig {
  wechat: Pick<ThirdPartyOAuthConfig, 'enabled' | 'appId' | 'appSecret'>
  alipay: Pick<ThirdPartyOAuthConfig, 'enabled' | 'appId' | 'privateKey' | 'publicKey'>
  github: Pick<ThirdPartyOAuthConfig, 'enabled' | 'clientId' | 'clientSecret'>
  google: Pick<ThirdPartyOAuthConfig, 'enabled' | 'clientId' | 'clientSecret' | 'redirectUri'>
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

export interface SecurityPublicConfig {
  disableDevtool?: boolean
  /** 是否启用接口 SM4 加密 */
  sm4EncryptEnabled?: boolean
  /** SM4 对称密钥（16 字节） */
  sm4Key?: string
  /** 是否启用接口 SM2 / HMAC-SM3 签名 */
  sm2SignEnabled?: boolean
  sm3SignEnabled?: boolean
  /** SM2 签名私钥或 HMAC-SM3 签名 Key */
  sm2PrivateKey?: string
  sm3SignKey?: string
}

export interface AuthPublicConfig {
  site?: SiteConfig
  login?: LoginConfig
  register?: RegisterConfig
  security?: SecurityPublicConfig
}

export interface SmsLogRecord {
  id?: number
  phone?: string
  content?: string
  provider?: string
  status?: number
  resultMsg?: string
  createTime?: string
}

export interface EmailLogRecord {
  id?: number
  email?: string
  subject?: string
  content?: string
  scene?: string
  provider?: string
  status?: number
  resultMsg?: string
  ip?: string
  createTime?: string
}

export interface PayOrderRecord {
  orderNo?: string
  status?: string
  amount?: number
}

export interface EmailAdminConfig {
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
