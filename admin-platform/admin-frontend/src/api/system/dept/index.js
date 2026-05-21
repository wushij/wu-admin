import request, { get, post, put, del } from '@/utils/request'

export function getDeptTree(params) {
  return get('/system/dept/tree', params)
}

export function getDeptList() {
  return get('/system/dept/list')
}

export function getDept(id) {
  return get('/system/dept/get', { id })
}

export function createDept(data) {
  return post('/system/dept/create', data)
}

export function updateDept(data) {
  return put('/system/dept/update', data)
}

export function deleteDept(id) {
  return request.delete('/system/dept/delete', { params: { id } })
}

export function moveDept(id, parentId, sort) {
  return request.put('/system/dept/move', null, { params: { id, parentId, sort } })
}

export function updateDeptStatus(id, status) {
  return put('/system/dept/update-status', { id, status })
}

export function getRecycleDeptPage(params) {
  return get('/system/dept/recycle/page', params)
}

export function restoreDept(id) {
  return put('/system/dept/restore', { id })
}

export function deleteDeptPermanent(id) {
  return del('/system/dept/delete-permanent', { id })
}

/** @deprecated 使用 getDeptTree */
export function getDeptListAsTree(params) {
  return getDeptTree(params)
}
