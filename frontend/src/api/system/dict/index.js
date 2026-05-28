import { get, post, put, del } from '@/utils/request'

export function pageDictType(params) {
  return get('/system/dict-type/page', params)
}

export function listDictType() {
  return get('/system/dict-type/list')
}

export function getDictType(id) {
  return get(`/system/dict-type/${id}`)
}

export function exportDictType(id) {
  return get(`/system/dict-type/${id}/export`)
}

export function createDictType(data) {
  return post('/system/dict-type', data)
}

export function updateDictType(data) {
  return put('/system/dict-type', data)
}

export function deleteDictType(id) {
  return del(`/system/dict-type/${id}`)
}

export function copyDictType(id) {
  return post(`/system/dict-type/${id}/copy`)
}

export function listDictDataByType(dictType) {
  return get(`/system/dict-data/type/${dictType}`)
}

export function listDictDataForManage(dictType) {
  return get(`/system/dict-data/manage/${dictType}`)
}

export function batchDictData(types) {
  return get('/system/dict-data/batch', { types: types.join(',') })
}

/** 刷新服务端 Redis 字典全量缓存 */
export function refreshDictCache() {
  return post('/system/dict-data/refresh-cache')
}

export function createDictData(data) {
  return post('/system/dict-data', data)
}

export function updateDictData(data) {
  return put('/system/dict-data', data)
}

export function deleteDictData(id) {
  return del(`/system/dict-data/${id}`)
}
