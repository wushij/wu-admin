import { get, post, put, del } from '@/utils/request'
import type { DictDataItem, PageQuery, PageResult } from '@/types/api'

export interface DictTypeVO {
  id: number
  dictName: string
  dictType: string
  status?: number
  remark?: string
  dataCount?: number
  createTime?: string
  updateTime?: string
}

export interface DictTypeSaveDTO {
  id?: number
  dictName: string
  dictType: string
  status?: number
  remark?: string
}

export type DictDataSaveDTO = DictDataItem

export interface DictTypeExportData {
  dictType?: DictTypeVO
  dictDataList?: DictDataItem[]
}

export interface DictTypePageQuery extends PageQuery {
  dictName?: string
  dictType?: string
  status?: number | null
}

export function pageDictType(params: DictTypePageQuery) {
  return get<PageResult<DictTypeVO>>('/system/dict-type/page', params)
}

export function listDictType() {
  return get<DictTypeVO[]>('/system/dict-type/list')
}

export function getDictType(id: number) {
  return get<DictTypeVO>(`/system/dict-type/${id}`)
}

export function exportDictType(id: number) {
  return get<DictTypeExportData>(`/system/dict-type/${id}/export`)
}

export function createDictType(data: DictTypeSaveDTO) {
  return post('/system/dict-type', data)
}

export function updateDictType(data: DictTypeSaveDTO) {
  return put('/system/dict-type', data)
}

export function deleteDictType(id: number) {
  return del(`/system/dict-type/${id}`)
}

export function copyDictType(id: number) {
  return post(`/system/dict-type/${id}/copy`)
}

export function getRecycleDictTypePage(params: { pageNo: number; pageSize: number; dictName?: string; dictType?: string }) {
  return get<PageResult<DictTypeVO>>('/system/dict-type/recycle/page', params)
}

export function restoreDictType(id: number) {
  return put('/system/dict-type/restore', null, { params: { id } })
}

export function deleteDictTypePermanent(id: number) {
  return del('/system/dict-type/delete-permanent', { params: { id } })
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

export function createDictData(data: DictDataSaveDTO) {
  return post('/system/dict-data', data)
}

export function updateDictData(data: DictDataSaveDTO) {
  return put('/system/dict-data', data)
}

export function deleteDictData(id: number) {
  return del(`/system/dict-data/${id}`)
}

export function getRecycleDictDataPage(params: {
  pageNo: number
  pageSize: number
  dictType?: string
  dictLabel?: string
}) {
  return get<PageResult<DictDataItem>>('/system/dict-data/recycle/page', params)
}

export function restoreDictData(id: number) {
  return put('/system/dict-data/restore', null, { params: { id } })
}

export function deleteDictDataPermanent(id: number) {
  return del('/system/dict-data/delete-permanent', { params: { id } })
}
