import { get, put, post } from '@/utils/request'
import type {
  UserProfile,
  ProfileUpdateDTO,
  ChangePasswordDTO,
  ProfilePasswordSmsResetDTO,
  ProfileMobileBindSmsCodeDTO,
  ProfileMobileBindDTO,
  ProfileLoginLogQuery,
  ProfileLoginLogPage,
} from '@/types/profile'

export function getProfile() {
  return get<UserProfile>('/auth/profile')
}

export function updateProfile(data: ProfileUpdateDTO) {
  return put<unknown>('/auth/profile', data)
}

export function changePassword(data: ChangePasswordDTO) {
  return put<unknown>('/auth/profile/password', data)
}

/** 发码前须先完成滑块，传 uuid（token）与 code（offsetX） */
export function sendProfilePasswordSmsCode(slider: { uuid: string; code: string }) {
  return post<boolean>('/auth/profile/password/sms-code', slider)
}

export function resetPasswordBySms(data: ProfilePasswordSmsResetDTO) {
  return put<unknown>('/auth/profile/password/sms-reset', data)
}

/** 绑定手机号发码（须先滑块，传 uuid + code） */
export function sendProfileMobileBindSmsCode(data: ProfileMobileBindSmsCodeDTO) {
  return post<boolean>('/auth/profile/mobile/sms-code', data)
}

export function bindProfileMobile(data: ProfileMobileBindDTO) {
  return put<unknown>('/auth/profile/mobile', data)
}

export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<{ url: string }>('/auth/profile/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getMyLoginLogs(params: ProfileLoginLogQuery) {
  return get<ProfileLoginLogPage>('/auth/profile/login-logs', params)
}
