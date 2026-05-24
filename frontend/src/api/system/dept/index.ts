import { get, post, put, del } from '@/utils/request'
import type { PageResult, RecyclePageQuery } from '@/types/api'

export interface DeptVO {
  id: number
  name: string
  parentId?: number
  sort?: number
  status?: number
  leaderName?: string
  leaderUserId?: number | null
  phone?: string
  email?: string
  userCount?: number
  children?: DeptVO[]
  createTime?: string
  updateTime?: string
}

export interface DeptTreeQuery {
  name?: string
  status?: number | null
}

export type DeptRecycleQuery = RecyclePageQuery & Pick<DeptTreeQuery, 'name' | 'status'>

export interface DeptSaveDTO {
  id?: number | null
  parentId?: number | null
  name: string
  leaderName?: string
  leaderUserId?: number | null
  phone?: string
  email?: string
  sort?: number
  status?: number
}

export function getDeptTree(params?: DeptTreeQuery) {
  return get<DeptVO[]>('/system/dept/tree', params)
}

export function getDeptList() {
  return get<DeptVO[]>('/system/dept/list')
}

export function getDept(id: number) {
  return get<DeptVO>('/system/dept/get', { id })
}

export function createDept(data: DeptSaveDTO) {
  return post('/system/dept/create', data)
}

export function updateDept(data: DeptSaveDTO) {
  return put('/system/dept/update', data)
}

export function deleteDept(id: number) {
  return del('/system/dept/delete', { params: { id } })
}

export function moveDept(id: number, parentId: number, sort: number) {
  return put('/system/dept/move', null, { params: { id, parentId, sort } })
}

export function updateDeptStatus(id: number, status: number) {
  return put('/system/dept/update-status', null, { params: { id, status } })
}

export function getRecycleDeptPage(params: DeptRecycleQuery) {
  return get<PageResult<DeptVO>>('/system/dept/recycle/page', params)
}

export function restoreDept(id: number) {
  return put('/system/dept/restore', null, { params: { id } })
}

export function deleteDeptPermanent(id: number) {
  return del('/system/dept/delete-permanent', { params: { id } })
}
