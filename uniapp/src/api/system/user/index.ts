import { get, post, put, del } from '@/utils/request'

import type { PageResult } from '@/types/api'

import type { UserPageQuery, UserSaveDTO, UserVO } from '@/types/user'



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



export function deleteUserPermanent(id: number) {

  return del('/system/user/delete-permanent', { params: { id } })

}



export function getRecycleUserPage(params: { pageNo?: number; pageSize?: number; username?: string }) {

  return get<PageResult<UserVO>>('/system/user/recycle/page', params)

}



export function restoreUser(id: number) {

  return put('/system/user/restore', null, { params: { id } })

}



export function resetUserPassword(id: number, password: string) {

  return put('/system/user/reset-password', null, { params: { id, password } })

}



export function updateUserStatus(id: number, status: number) {

  return put('/system/user/update-status', null, { params: { id, status } })

}



export function getUserRoleIds(userId: number) {

  return get<number[]>('/system/user/get-role-ids', { userId })

}


