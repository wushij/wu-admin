import { get, post, put, del } from '@/utils/request'

import type { DictDataItem, PageResult } from '@/types/api'

import type { DictTypeSaveDTO, DictTypeVO } from '@/types/system'



export function pageDictType(params: {

  pageNo?: number

  pageSize?: number

  dictName?: string

  dictType?: string

  status?: number | null

}) {

  return get<PageResult<DictTypeVO>>('/system/dict-type/page', params)

}



export function getDictType(id: number) {

  return get<DictTypeVO>(`/system/dict-type/${id}`)

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



export function listDictDataByType(dictType: string) {

  return get<DictDataItem[]>(`/system/dict-data/type/${dictType}`)

}



export function listDictDataForManage(dictType: string) {

  return get<DictDataItem[]>(`/system/dict-data/manage/${dictType}`)

}



export function batchDictData(types: string[]) {

  return get<Record<string, DictDataItem[]>>('/system/dict-data/batch', {

    types: types.join(','),

  })

}



export function refreshDictCache() {

  return post('/system/dict-data/refresh-cache')

}



export function createDictData(data: DictDataItem) {

  return post('/system/dict-data', data)

}



export function updateDictData(data: DictDataItem) {

  return put('/system/dict-data', data)

}



export function deleteDictData(id: number) {

  return del(`/system/dict-data/${id}`)

}



export function getRecycleDictTypePage(params: {

  pageNo: number

  pageSize: number

  dictName?: string

  dictType?: string

}) {

  return get<PageResult<DictTypeVO>>('/system/dict-type/recycle/page', params)

}



export function restoreDictType(id: number) {

  return put('/system/dict-type/restore', null, { params: { id } })

}



export function deleteDictTypePermanent(id: number) {

  return del('/system/dict-type/delete-permanent', { params: { id } })

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


