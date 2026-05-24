import request, { get, post, put, del } from '@/utils/request'
import type { PageQuery } from '@/types/api'

export interface DeptVO {
  id: number
  name: string
  parentId?: number
  sort?: number
  status?: number
  leader?: string
  phone?: string
  email?: string
  children?: DeptVO[]
  [key: string]: unknown
}

export function getDeptTree(params?: Record<string, unknown>) {
  return get<DeptVO[]>('/system/dept/tree', params)
}

export function getDeptList() {
  return get<DeptVO[]>('/system/dept/list')
}

export function getDept(id: number) {
  return get<DeptVO>('/system/dept/get', { id })
}

export function createDept(data: Record<string, unknown>) {
  return post('/system/dept/create', data)
}

export function updateDept(data: Record<string, unknown>) {
  return put('/system/dept/update', data)
}

export function deleteDept(id: number) {
  return request.delete('/system/dept/delete', { params: { id } })
}

export function moveDept(id: number, parentId: number, sort: number) {
  return request.put('/system/dept/move', null, { params: { id, parentId, sort } })
}

export function updateDeptStatus(id: number, status: number) {
  return put('/system/dept/update-status', { id, status })
}

export function getRecycleDeptPage(params: PageQuery) {
  return get<import('@/types/api').PageResult<DeptVO>>('/system/dept/recycle/page', params)
}

export function restoreDept(id: number) {
  return put('/system/dept/restore', { id })
}

export function deleteDeptPermanent(id: number) {
  return del('/system/dept/delete-permanent', { id })
}

/** @deprecated 使用 getDeptTree */
export function getDeptListAsTree(params?: Record<string, unknown>) {
  return getDeptTree(params)
}
