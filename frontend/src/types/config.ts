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

export interface SecurityPublicConfig {
  disableDevtool?: boolean
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

export interface SecurityConfig {
  disableDevtool: boolean
  /** Sa-Token is-concurrent，false 表示禁止多端同时在线 */
  isConcurrent: boolean
}

export type ConfigGroupCode = 'site' | 'session' | 'file' | 'rateLimit' | 'login' | 'register' | 'security'

export interface ConfigGroupMap {
  site: AdminSiteConfig
  session: SessionConfig
  file: FileStorageConfig
  rateLimit: RateLimitConfig
  login: LoginAdminConfig
  register: RegisterAdminConfig
  security: SecurityConfig
}
