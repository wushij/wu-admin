import { get, put } from '@/utils/request'
import type { AxiosRequestConfig } from 'axios'

export interface ConfigGroup {
  groupCode: string
  groupName?: string
  configValue?: string
  remark?: string
  createTime?: string
  updateTime?: string
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
