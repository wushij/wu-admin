import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult, RecyclePageQuery } from '@/types/api'

export interface UserVO {
  id: number
  username: string
  nickname?: string
  mobile?: string
  email?: string
  avatar?: string
  status?: number
  deptId?: number
  deptName?: string
  postIds?: number[]
  postNames?: string
  roleIds?: number[]
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface UserPageQuery extends PageQuery {
  username?: string
  nickname?: string
  mobile?: string
  status?: number | null
  deptId?: number | null
  postId?: number | null
}

export type UserRecycleQuery = RecyclePageQuery &
  Pick<UserPageQuery, 'username' | 'mobile' | 'status' | 'deptId'>

/** 创建/更新用户请求体 */
export interface UserSaveDTO {
  id?: number | null
  username: string
  nickname?: string
  password?: string
  mobile?: string
  email?: string
  deptId?: number | null
  status?: number
  roleId?: number | undefined
  postIds?: number[]
  remark?: string
}

export function getUserList() {
  return get<UserVO[]>('/system/user/list')
}

export function getUserPage(params: UserPageQuery) {
  return get<PageResult<UserVO>>('/system/user/page', params)
}

export function getUser(id: number) {
  return get<UserVO>('/system/user/get', { id })
}

export function createUser(data: UserSaveDTO) {
  return post('/system/user/create', data)
}

export function updateUser(data: UserSaveDTO) {
  return put('/system/user/update', data)
}

export function deleteUser(id: number) {
  return del('/system/user/delete', { params: { id } })
}

export function getUserRoleIds(userId: number) {
  return get<number[]>('/system/user/get-role-ids', { userId })
}

export function assignUserRole(data: { userId: number; roleIds: number[] }) {
  return post('/system/user/assign-role', data)
}

export function updateUserStatus(id: number, status: number) {
  return put('/system/user/update-status', null, { params: { id, status } })
}

export function resetUserPassword(id: number, password: string) {
  return put('/system/user/reset-password', null, { params: { id, password } })
}

export function getRecycleUserPage(params: UserRecycleQuery) {
  return get<PageResult<UserVO>>('/system/user/recycle/page', params)
}

export function restoreUser(id: number) {
  return put('/system/user/restore', null, { params: { id } })
}

export function deleteUserPermanent(id: number) {
  return del('/system/user/delete-permanent', { params: { id } })
}
