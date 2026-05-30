import { get, put, post } from '@/utils/request'
import type { AxiosRequestConfig } from 'axios'
import type { PayOrderRecord } from '@/types/config'

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
