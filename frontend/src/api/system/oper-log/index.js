import { get, del } from '@/utils/request'

export function pageOperLog(params) {
  return get('/system/oper-log/page', params)
}

export function deleteOperLog(id) {
  return del(`/system/oper-log/${id}`)
}

export function cleanOperLog() {
  return del('/system/oper-log/clean')
}
