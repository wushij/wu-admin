import { get, put, post } from '@/utils/request'
import type { ConfigGroup } from '@/types/system'
import type { PayOrderRecord, SmsLogRecord, EmailLogRecord } from '@/types/config-types'

export function listConfigGroups() {
  return get<ConfigGroup[]>('/system/config-group/list')
}

export function getConfigGroup(groupCode: string) {
  return get<ConfigGroup>(`/system/config-group/${groupCode}`)
}

export function updateConfigGroup(groupCode: string, configValue: string) {
  return put(`/system/config-group/${groupCode}`, { configValue })
}

export function testSms(phone: string, templateCode?: string) {
  return post<boolean>('/system/config-group/test-sms', {
    phone,
    ...(templateCode ? { templateCode } : {}),
  })
}

export function testEmail(toEmail: string) {
  return post<boolean>('/system/config-group/test-email', { toEmail })
}

export interface TestPaymentResult {
  orderNo: string
  qrcode?: string
  payUrl?: string
}

export function testPayment(type: 'wechat' | 'alipay') {
  return post<TestPaymentResult>('/system/config-group/test-payment', { type })
}

export function getPayOrderStatus(orderNo: string) {
  return get<PayOrderRecord>(`/pay/order/${orderNo}`)
}

export function getRecentSmsLogs(limit = 5) {
  return get<SmsLogRecord[]>('/system/config-group/sms-logs/recent', { limit })
}

export function getSmsLogs(params: {
  page: number
  size: number
  phone?: string
  status?: number | null
}) {
  const query: Record<string, unknown> = {
    page: params.page,
    size: params.size,
  }
  if (params.phone) query.phone = params.phone
  if (params.status != null) query.status = params.status
  return get<{ list: SmsLogRecord[]; total: number }>('/system/config-group/sms-logs', query)
}

export function getRecentEmailLogs(limit = 5) {
  return get<EmailLogRecord[]>('/system/config-group/email-logs/recent', { limit })
}

export function getEmailLogs(params: {
  page: number
  size: number
  email?: string
  status?: number | null
}) {
  const query: Record<string, unknown> = {
    page: params.page,
    size: params.size,
  }
  if (params.email) query.email = params.email
  if (params.status != null) query.status = params.status
  return get<{ list: EmailLogRecord[]; total: number }>('/system/config-group/email-logs', query)
}
