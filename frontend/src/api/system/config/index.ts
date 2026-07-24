import { get, put, post } from '@/utils/request'
import type { AxiosRequestConfig } from 'axios'
import type { PayOrderRecord, SmsLogRecord } from '@/types/config'

export interface ConfigGroup {
  groupCode: string
  groupName?: string
  configValue?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface TestPaymentResult {
  orderNo: string
  qrcode?: string
  payUrl?: string
}

export function listConfigGroups() {
  return get<ConfigGroup[]>('/system/config-group/list')
}

export function getConfigGroup(groupCode: string, config: AxiosRequestConfig = {}) {
  return get<ConfigGroup>(`/system/config-group/${groupCode}`, undefined, config)
}

export function updateConfigGroup(groupCode: string, configValue: string) {
  return put(`/system/config-group/${groupCode}`, { configValue })
}

export function testPayment(type: 'wechat' | 'alipay') {
  return post<TestPaymentResult>('/system/config-group/test-payment', { type })
}

export function getPayOrderStatus(orderNo: string) {
  return get<PayOrderRecord>(`/pay/order/${orderNo}`)
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
