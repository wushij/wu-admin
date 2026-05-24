import request from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { AxiosRequestConfig } from 'axios'

export interface ConfigGroup {
  groupCode: string
  groupName?: string
  configValue?: string
  [key: string]: unknown
}

export function listConfigGroups() {
  return request.get('/system/config-group/list') as Promise<ApiResult<ConfigGroup[]>>
}

export function getConfigGroup(groupCode: string, config: AxiosRequestConfig = {}) {
  return request.get(`/system/config-group/${groupCode}`, config) as Promise<ApiResult<ConfigGroup>>
}

export function updateConfigGroup(groupCode: string, configValue: string) {
  return request.put(`/system/config-group/${groupCode}`, { configValue })
}
