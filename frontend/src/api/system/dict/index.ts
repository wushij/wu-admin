import { get, post, put, del } from '@/utils/request'
import type { DictDataItem, PageQuery, PageResult } from '@/types/api'

export interface DictTypeVO {
  id: number
  dictName: string
  dictType: string
  status?: number
  remark?: string
  dataCount?: number
  [key: string]: unknown
}

export function pageDictType(params: PageQuery) {
  return get<PageResult<DictTypeVO>>('/system/dict-type/page', params)
}

export function listDictType() {
  return get<DictTypeVO[]>('/system/dict-type/list')
}

export function getDictType(id: number) {
  return get<DictTypeVO>(`/system/dict-type/${id}`)
}

export function exportDictType(id: number) {
  return get<Record<string, unknown>>(`/system/dict-type/${id}/export`)
}

export function createDictType(data: Record<string, unknown>) {
  return post('/system/dict-type', data)
}

export function updateDictType(data: Record<string, unknown>) {
  return put('/system/dict-type', data)
}

export function deleteDictType(id: number) {
  return del(`/system/dict-type/${id}`)
}

export function copyDictType(id: number) {
  return post(`/system/dict-type/${id}/copy`)
}

export function listDictDataByType(dictType: string) {
  return get<DictDataItem[]>(`/system/dict-data/type/${dictType}`)
}

export function listDictDataForManage(dictType: string) {
  return get<DictDataItem[]>(`/system/dict-data/manage/${dictType}`)
}

export function batchDictData(types: string[]) {
  return get<Record<string, DictDataItem[]>>('/system/dict-data/batch', { types: types.join(',') })
}

/** 刷新服务端 Redis 字典全量缓存 */
export function refreshDictCache() {
  return post('/system/dict-data/refresh-cache')
}

export function createDictData(data: Record<string, unknown>) {
  return post('/system/dict-data', data)
}

export function updateDictData(data: Record<string, unknown>) {
  return put('/system/dict-data', data)
}

export function deleteDictData(id: number) {
  return del(`/system/dict-data/${id}`)
}
