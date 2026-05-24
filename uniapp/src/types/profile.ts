import type { PageQuery, PageResult } from '@/types/api'

export interface UserProfile {
  userId: number
  username: string
  nickname?: string
  mobile?: string
  email?: string
  avatar?: string
  status?: number
  deptId?: number
  deptName?: string
  roleIds?: number[]
  roleNames?: string[]
  roleCodes?: string[]
  postIds?: number[]
  postNames?: string[]
  createTime?: string
  updateTime?: string
  lastLoginTime?: string
  lastLoginIp?: string
  lastLoginLocation?: string
  minPasswordLength?: number
}

export interface ProfileUpdateDTO {
  nickname?: string
  email?: string
  avatar?: string
}

export interface ChangePasswordDTO {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface ProfilePasswordSmsResetDTO {
  smsCode: string
  newPassword: string
  confirmPassword: string
}

export interface ProfileMobileBindSmsCodeDTO {
  mobile: string
  uuid: string
  code: string
}

export interface ProfileMobileBindDTO {
  mobile: string
  smsCode: string
}

export interface ProfileLoginLogQuery extends PageQuery {}

export interface LoginLogVO {
  username?: string
  ipaddr?: string
  loginLocation?: string
  browser?: string
  os?: string
  loginTime?: string
  status?: number
}

export type ProfileLoginLogPage = PageResult<LoginLogVO>
