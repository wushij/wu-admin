import request from '@/utils/request'
import type { ApiResult, PageQuery, PageResult } from '@/types/api'

export interface UserVO {
  id: number
  username: string
  nickname?: string
  mobile?: string
  email?: string
  status?: number
  deptId?: number
  postIds?: number[]
  createTime?: string
  [key: string]: unknown
}

export interface UserPageQuery extends PageQuery {
  username?: string
  nickname?: string
  mobile?: string
  status?: number | null
  deptId?: number | null
}

export function getUserList() {
  return request.get('/system/user/list') as Promise<ApiResult<UserVO[]>>
}

export function getUserPage(params: UserPageQuery) {
  return request.get('/system/user/page', { params }) as Promise<ApiResult<PageResult<UserVO>>>
}

export function getUser(id: number) {
  return request.get('/system/user/get', { params: { id } }) as Promise<ApiResult<UserVO>>
}

export function createUser(data: Record<string, unknown>) {
  return request.post('/system/user/create', data)
}

export function updateUser(data: Record<string, unknown>) {
  return request.put('/system/user/update', data)
}

export function deleteUser(id: number) {
  return request.delete('/system/user/delete', { params: { id } })
}

export function getUserRoleIds(userId: number) {
  return request.get('/system/user/get-role-ids', { params: { userId } }) as Promise<ApiResult<number[]>>
}

export function assignUserRole(data: { userId: number; roleIds: number[] }) {
  return request.post('/system/user/assign-role', data)
}

export function updateUserStatus(id: number, status: number) {
  return request.put('/system/user/update-status', null, { params: { id, status } })
}

export function resetUserPassword(id: number, password: string) {
  return request.put('/system/user/reset-password', null, { params: { id, password } })
}

export function getRecycleUserPage(params: PageQuery) {
  return request.get('/system/user/recycle/page', { params }) as Promise<ApiResult<PageResult<UserVO>>>
}

export function restoreUser(id: number) {
  return request.put('/system/user/restore', null, { params: { id } })
}

export function deleteUserPermanent(id: number) {
  return request.delete('/system/user/delete-permanent', { params: { id } })
}
