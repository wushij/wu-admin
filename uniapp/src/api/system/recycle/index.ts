import { get } from '@/utils/request'

export interface RecycleSummary {
  user: number
  role: number
  menu: number
  dept: number
  post: number
  ticket: number
  approval: number
  dict: number
  dictData: number
  announce: number
  job: number
  file: number
  gen: number
  total: number
}

export function getRecycleSummary() {
  return get<RecycleSummary>('/system/recycle/summary')
}

