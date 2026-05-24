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
  rememberMe?: boolean
}

export interface RegisterConfig {
  enabled?: boolean
  captchaEnabled?: boolean
  captchaType?: string
  minPasswordLength?: number
}

export interface AuthPublicConfig {
  site?: SiteConfig
  login?: LoginConfig
  register?: RegisterConfig
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
}

export interface LoginAdminConfig {
  captchaEnabled: boolean
  captchaType: string
  rememberMe: boolean
  maxRetryCount: number
  lockTime: number
}

export interface RegisterAdminConfig {
  enabled: boolean
  captchaEnabled: boolean
  captchaType: string
  defaultRoleCode: string
  needAudit: boolean
  minPasswordLength: number
}

export type ConfigGroupCode = 'site' | 'session' | 'file' | 'rateLimit' | 'login' | 'register'

export interface ConfigGroupMap {
  site: AdminSiteConfig
  session: SessionConfig
  file: FileStorageConfig
  rateLimit: RateLimitConfig
  login: LoginAdminConfig
  register: RegisterAdminConfig
}
