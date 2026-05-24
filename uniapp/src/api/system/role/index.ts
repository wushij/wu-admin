import { get, post, put, del } from '@/utils/request'

import type { PageQuery, PageResult } from '@/types/api'

import type { RoleSaveDTO, RoleVO } from '@/types/system'



export function getRolePage(params: PageQuery & { name?: string; status?: number | null }) {

  return get<PageResult<RoleVO>>('/system/role/page', params)

}



export function getRoleList(params?: { name?: string; status?: number | null }) {

  return get<RoleVO[]>('/system/role/list', params)

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



export function getRecycleRolePage(params: PageQuery & { name?: string; status?: number | null }) {

  return get<PageResult<RoleVO>>('/system/role/recycle/page', params)

}



export function restoreRole(id: number) {

  return put('/system/role/restore', null, { params: { id } })

}



export function deleteRolePermanent(id: number) {

  return del('/system/role/delete-permanent', { params: { id } })

}


