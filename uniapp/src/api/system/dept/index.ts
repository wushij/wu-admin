import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { DeptVO, DeptSaveDTO, PostVO, PostSaveDTO } from '@/types/system'

export function getDeptTree(params?: { name?: string; status?: number | null }) {
  return get<DeptVO[]>('/system/dept/tree', params)
}

export function getDeptList() {
  return get<DeptVO[]>('/system/dept/list')
}

export function getDept(id: number) {
  return get<DeptVO>('/system/dept/get', { id })
}

export function createDept(data: DeptSaveDTO) {
  return post<number>('/system/dept/create', data)
}

export function updateDept(data: DeptSaveDTO) {
  return put('/system/dept/update', data)
}

export function deleteDept(id: number) {
  return del('/system/dept/delete', { params: { id } })
}

export function getPostTree() {
  return get<PostVO[]>('/system/post/tree')
}

export function getPost(id: number) {
  return get<PostVO>(`/system/post/${id}`)
}

export function createPost(data: PostSaveDTO) {
  return post('/system/post', data)
}

export function updatePost(data: PostSaveDTO) {
  return put('/system/post', data)
}

export function deletePost(id: number) {
  return del(`/system/post/${id}`)
}

export function getRecycleDeptPage(params: { pageNo?: number; pageSize?: number; name?: string }) {
  return get<PageResult<DeptVO>>('/system/dept/recycle/page', params)
}

export function restoreDept(id: number) {
  return put('/system/dept/restore', null, { params: { id } })
}

export function deleteDeptPermanent(id: number) {
  return del('/system/dept/delete-permanent', { params: { id } })
}

export function getRecyclePostPage(params: { pageNo?: number; pageSize?: number; name?: string }) {
  return get<PageResult<PostVO>>('/system/post/recycle/page', params)
}

export function restorePost(id: number) {
  return put('/system/post/restore', null, { params: { id } })
}

export function deletePostPermanent(id: number) {
  return del('/system/post/delete-permanent', { params: { id } })
}
