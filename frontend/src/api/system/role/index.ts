import request from '@/utils/request'
import type { ApiResult, PageQuery, PageResult } from '@/types/api'

export interface RoleVO {
  id: number
  name: string
  code: string
  status?: number
  sort?: number
  remark?: string
  [key: string]: unknown
}

export function getRoleList(params?: Record<string, unknown>) {
  return request.get('/system/role/list', { params }) as Promise<ApiResult<RoleVO[]>>
}

export function getRolePage(params: PageQuery) {
  return request.get('/system/role/page', { params }) as Promise<ApiResult<PageResult<RoleVO>>>
}

export function getRole(id: number) {
  return request.get('/system/role/get', { params: { id } }) as Promise<ApiResult<RoleVO>>
}

export function createRole(data: Record<string, unknown>) {
  return request.post('/system/role/create', data)
}

export function updateRole(data: Record<string, unknown>) {
  return request.put('/system/role/update', data)
}

export function deleteRole(id: number) {
  return request.delete('/system/role/delete', { params: { id } })
}

export function getRoleMenuIds(roleId: number) {
  return request.get('/system/role/get-menu-ids', { params: { roleId } }) as Promise<ApiResult<number[]>>
}

export function assignRoleMenu(data: { roleId: number; menuIds: number[] }) {
  return request.post('/system/role/assign-menu', data)
}

export function updateRoleStatus(id: number, status: number) {
  return request.put('/system/role/update-status', null, { params: { id, status } })
}

export function getRecycleRolePage(params: PageQuery) {
  return request.get('/system/role/recycle/page', { params }) as Promise<ApiResult<PageResult<RoleVO>>>
}

export function restoreRole(id: number) {
  return request.put('/system/role/restore', null, { params: { id } })
}

export function deleteRolePermanent(id: number) {
  return request.delete('/system/role/delete-permanent', { params: { id } })
}
