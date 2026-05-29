import { get, del } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface OperLogVO {
  id: number
  title?: string
  businessType?: number
  method?: string
  requestMethod?: string
  operName?: string
  operUrl?: string
  operIp?: string
  operParam?: string
  jsonResult?: string
  status?: number
  errorMsg?: string
  operTime?: string
  costTime?: number
  [key: string]: unknown
}

export function pageOperLog(params: PageQuery) {
  return get<PageResult<OperLogVO>>('/system/oper-log/page', params)
}

export function deleteOperLog(id: number) {
  return del(`/system/oper-log/${id}`)
}

export function cleanOperLog() {
  return del('/system/oper-log/clean')
}
