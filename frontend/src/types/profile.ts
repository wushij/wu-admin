import type { PageQuery, PageResult } from '@/types/api'
import type { LoginLogVO } from '@/api/system/login-log'

/** 个人中心资料（/auth/profile） */
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
  mobile?: string
  email?: string
  avatar?: string
}

export interface ChangePasswordDTO {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface ProfileLoginLogQuery extends PageQuery {}

export type ProfileLoginLogPage = PageResult<LoginLogVO>
