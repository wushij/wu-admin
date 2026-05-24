import { get, post, put, del } from '@/utils/request'

export interface PostVO {
  id: number
  name: string
  postName?: string
  postCode?: string
  code?: string
  parentId?: number
  sort?: number
  status?: number
  remark?: string
  children?: PostVO[]
  createTime?: string
  updateTime?: string
}

export interface PostSaveDTO {
  id?: number
  parentId?: number
  postCode?: string
  postName?: string
  sort?: number
  status?: number
  remark?: string
}

export function getPostTree() {
  return get<PostVO[]>('/system/post/tree')
}

export function getPostList() {
  return get<PostVO[]>('/system/post/list')
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

export function movePost(id: number, parentId: number) {
  return post(`/system/post/${id}/move`, null, { params: { parentId } })
}

export function getRecyclePostPage(params: { pageNo: number; pageSize: number; postName?: string; status?: number | null }) {
  return get<import('@/types/api').PageResult<PostVO>>('/system/post/recycle/page', params)
}

export function restorePost(id: number) {
  return put('/system/post/restore', null, { params: { id } })
}

export function deletePostPermanent(id: number) {
  return del('/system/post/delete-permanent', { params: { id } })
}
