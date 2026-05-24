import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult, RecyclePageQuery } from '@/types/api'

export interface RoleVO {
  id: number
  name: string
  code: string
  status?: number
  sort?: number
  remark?: string
  dataScope?: number
  createTime?: string
  updateTime?: string
}

export interface RoleListQuery {
  name?: string
  status?: number | null
}

export type RoleRecycleQuery = RecyclePageQuery & Pick<RoleListQuery, 'name' | 'status'>

export interface RoleSaveDTO {
  id?: number | null
  name: string
  code: string
  sort?: number
  status?: number
  remark?: string
}

export function getRoleList(params?: RoleListQuery) {
  return get<RoleVO[]>('/system/role/list', params)
}

export function getRolePage(params: PageQuery) {
  return get<PageResult<RoleVO>>('/system/role/page', params)
}

export function getRole(id: number) {
  return get<RoleVO>('/system/role/get', { id })
}

export function createRole(data: RoleSaveDTO) {
  return post('/system/role/create', data)
}

export function updateRole(data: RoleSaveDTO) {
  return put('/system/role/update', data)
}

export function deleteRole(id: number) {
  return del('/system/role/delete', { params: { id } })
}

export function getRoleMenuIds(roleId: number) {
  return get<number[]>('/system/role/get-menu-ids', { roleId })
}

export function assignRoleMenu(data: { roleId: number; menuIds: number[] }) {
  return post('/system/role/assign-menu', data)
}

export function updateRoleStatus(id: number, status: number) {
  return put('/system/role/update-status', null, { params: { id, status } })
}

export function getRecycleRolePage(params: RoleRecycleQuery) {
  return get<PageResult<RoleVO>>('/system/role/recycle/page', params)
}

export function restoreRole(id: number) {
  return put('/system/role/restore', null, { params: { id } })
}

export function deleteRolePermanent(id: number) {
  return del('/system/role/delete-permanent', { params: { id } })
}
