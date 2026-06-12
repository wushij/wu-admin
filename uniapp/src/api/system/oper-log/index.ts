import { get, del } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { OperLogPageQuery, OperLogVO } from '@/types/system'

export function pageOperLog(params: OperLogPageQuery) {
  return get<PageResult<OperLogVO>>('/system/oper-log/page', params)
}

export function deleteOperLog(id: number) {
  return del<boolean>(`/system/oper-log/${id}`)
}

export function cleanOperLog() {
  return del<boolean>('/system/oper-log/clean')
}
