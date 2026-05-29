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
  [key: string]: unknown
}
