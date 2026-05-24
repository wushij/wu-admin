import { get, put, post } from '@/utils/request'
import { uploadFile } from '@/utils/upload'
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

export function sendProfilePasswordSmsCode(slider: { uuid: string; code: string }) {
  return post<boolean>('/auth/profile/password/sms-code', slider)
}

export function resetPasswordBySms(data: ProfilePasswordSmsResetDTO) {
  return put<unknown>('/auth/profile/password/sms-reset', data)
}

export function sendProfilePasswordEmailCode() {
  return post<boolean>('/auth/profile/password/email-code')
}

export function resetPasswordByEmail(data: { emailCode: string; newPassword: string; confirmPassword?: string }) {
  return put<unknown>('/auth/profile/password/email-reset', data)
}

export function sendProfileMobileBindSmsCode(data: ProfileMobileBindSmsCodeDTO) {
  return post<boolean>('/auth/profile/mobile/sms-code', data)
}

export function bindProfileMobile(data: ProfileMobileBindDTO) {
  return put<unknown>('/auth/profile/mobile', data)
}

export function sendProfileEmailBindCode(data: { email: string }) {
  return post<boolean>('/auth/profile/email/code', data)
}

export function bindProfileEmail(data: { email: string; code: string }) {
  return put<unknown>('/auth/profile/email', data)
}

export function uploadAvatar(filePath: string) {
  return uploadFile<{ url: string }>({ url: '/auth/profile/avatar', filePath })
}

export function getMyLoginLogs(params: ProfileLoginLogQuery) {
  return get<ProfileLoginLogPage>('/auth/profile/login-logs', params)
}
