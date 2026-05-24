import request, { get, post, put, del } from '@/utils/request'

export interface PostVO {
  id: number
  name: string
  code?: string
  parentId?: number
  sort?: number
  status?: number
  children?: PostVO[]
  [key: string]: unknown
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

export function createPost(data: Record<string, unknown>) {
  return post('/system/post', data)
}

export function updatePost(data: Record<string, unknown>) {
  return put('/system/post', data)
}

export function deletePost(id: number) {
  return del(`/system/post/${id}`)
}

export function movePost(id: number, parentId: number) {
  return request.post(`/system/post/${id}/move`, null, { params: { parentId } })
}
