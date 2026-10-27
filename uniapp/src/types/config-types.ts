/** 公开认证配置 /auth/config */
export interface SiteConfig {
  platformName?: string
  platformSubtitle?: string
  loginWelcome?: string
  registerTitle?: string
  copyright?: string
  icpEnabled?: boolean
  icpNumber?: string
  icpUrl?: string
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
  icpEnabled: boolean
  icpNumber: string
  icpUrl: string
}

export interface SessionAdminConfig {
  tokenExpireHours: number
  sessionSignExpireHours: number
}

export interface SecurityAdminConfig {
  disableDevtool: boolean
  isConcurrent: boolean
  sm4EncryptEnabled?: boolean
  /** 是否启用登录/注册/重置密码等国密 SM2 传输加密（默认开启） */
  sm2EncryptEnabled?: boolean
  sm2SignEnabled?: boolean
  /** 是否启用接口 HMAC-SM3 签名（与 sm2SignEnabled 同义，后端配置中仍保留该字段） */
  sm3SignEnabled?: boolean
  timestampEnabled?: boolean
  nonceEnabled?: boolean
  /** 风控总开关 */
  riskEnabled?: boolean
  /** 风险分滑动窗口（分钟），窗口外分值自动失效 */
  riskWindowMinutes?: number
  /** 关注阈值 */
  riskNoticeScore?: number
  /** 警告阈值 */
  riskWarnScore?: number
  /** 挑战阈值 */
  riskChallengeScore?: number
  /** 风险分触发临时封禁的阈值 */
  riskBlockScore?: number
  /** 风险分触发最高档封禁的阈值 */
  riskBanScore?: number
  /** 临时封禁时长（秒） */
  riskBlockSeconds?: number
  /** 最高档封禁时长（秒） */
  riskBanSeconds?: number
  /** 是否启用风控挑战（高风险主体写操作需先完成安全验证） */
  riskChallengeEnabled?: boolean
  /** 风险告警 webhook 地址，为空表示不推送 */
  riskAlertWebhook?: string
  /** 是否启用行为识别（翻页遍历、批量导出、越权探测） */
  riskBehaviorEnabled?: boolean
  /** 翻页扫描阈值：窗口内不同页码数达到该值判定为遍历 */
  riskPageScanThreshold?: number
  /** 批量导出阈值：窗口内导出/下载次数达到该值判定为批量拉取 */
  riskExportThreshold?: number
  /** 是否启用蜜罐诱捕（默认关闭，开启前需确认蜜罐前缀下没有真实接口） */
  honeypotEnabled?: boolean
  /** 蜜罐命中时追加的风险分 */
  honeypotRiskDelta?: number
  /** 蜜罐命中后的封禁时长（秒） */
  honeypotBlockSeconds?: number
  /** 蜜罐响应随机延迟上限（毫秒） */
  honeypotTarpitMs?: number
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

export type StorageEngineType = 'local' | 'minio' | 'cos' | 'oss'

export interface FileStorageConfig {
  type?: StorageEngineType
  localPath?: string
  localDomain?: string
  minioDomain?: string
  minioEndpoint?: string
  minioAccessKey?: string
  minioSecretKey?: string
  minioBucket?: string
  cosRegion?: string
  cosBucket?: string
  cosSecretId?: string
  cosSecretKey?: string
  cosDomain?: string
  ossEndpoint?: string
  ossBucket?: string
  ossAccessKeyId?: string
  ossAccessKeySecret?: string
  ossDomain?: string
  maxSizeMb: number
  allowedExtensions: string
}

export interface StorageConfigVO {
  type: StorageEngineType
  localPath: string
  localDomain: string
  minioDomain: string
  minioEndpoint: string
  minioAccessKey: string
  minioBucket: string
  minioSecretKeyConfigured: boolean
  cosRegion: string
  cosBucket: string
  cosSecretId: string
  cosDomain: string
  cosSecretKeyConfigured: boolean
  ossEndpoint: string
  ossBucket: string
  ossAccessKeyId: string
  ossDomain: string
  ossAccessKeySecretConfigured: boolean
  maxSizeMb: number
  allowedExtensions: string
  activeType: StorageEngineType
  activeBucket: string
  healthy: boolean
}

export interface StorageConfigDTO {
  type: StorageEngineType
  localPath?: string
  localDomain?: string
  minioDomain?: string
  minioEndpoint?: string
  minioAccessKey?: string
  minioSecretKey?: string
  minioBucket?: string
  cosRegion?: string
  cosBucket?: string
  cosSecretId?: string
  cosSecretKey?: string
  cosDomain?: string
  ossEndpoint?: string
  ossBucket?: string
  ossAccessKeyId?: string
  ossAccessKeySecret?: string
  ossDomain?: string
  maxSizeMb?: number
  allowedExtensions?: string
}

export interface RateLimitConfig {
  captchaPerIpMinute: number
  loginPerIpMinute: number
  registerPerIpMinute: number
  smsPerIpMinute: number
  smsSendIntervalSeconds: number
  smsPerPhoneDaily: number
  smsPerIpDaily: number
  /** AI 对话：单用户每分钟请求次数上限（0 表示不限制） */
  aiChatPerUserMinute: number
  /** 通用业务 API：单 IP 每分钟请求上限（应用层滑动窗口兜底，0 表示不限制） */
  apiPerIpMinute?: number
  /** 认证接口：单 IP 每分钟请求上限（应用层滑动窗口兜底，0 表示不限制） */
  authPerIpMinute?: number
  /** 文件访问：单 IP 每分钟请求上限（应用层滑动窗口兜底，0 表示不限制） */
  filesPerIpMinute?: number
}

/** AI 对话角色级每日 token 配额项 */
export interface RoleTokenQuota {
  roleId: number
  tokensDaily: number
}

export interface AiAdminConfig {
  assistantEnabled?: boolean
  globalKnowledge: string
  answerScope: 'focus' | 'open'
  tokensPerUserDaily: number
  roleTokenQuotas: RoleTokenQuota[]
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
  /** 是否启用接口 HMAC-SM3 签名（sm2SignEnabled 为历史开关名） */
  sm2SignEnabled?: boolean
  sm3SignEnabled?: boolean
  /** 是否启用高敏感口令 SM2 加密 */
  sm2EncryptEnabled?: boolean
  /** 后端下发的 SM2 公钥 */
  sm2PublicKey?: string
}

export interface AiPublicConfig {
  assistantEnabled?: boolean
}

export interface AuthPublicConfig {
  site?: SiteConfig
  login?: LoginConfig
  register?: RegisterConfig
  security?: SecurityPublicConfig
  ai?: AiPublicConfig
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
  | 'ai'
