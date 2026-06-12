import { get } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { OperLogPageQuery, OperLogVO } from '@/types/system'

export function pageOperLog(params: OperLogPageQuery) {
  return get<PageResult<OperLogVO>>('/system/oper-log/page', params)
}
