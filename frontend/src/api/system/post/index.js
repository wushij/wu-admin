import request, { get, post, put, del } from '@/utils/request'

export function getPostTree() {
  return get('/system/post/tree')
}

export function getPostList() {
  return get('/system/post/list')
}

export function getPost(id) {
  return get(`/system/post/${id}`)
}

export function createPost(data) {
  return post('/system/post', data)
}

export function updatePost(data) {
  return put('/system/post', data)
}

export function deletePost(id) {
  return del(`/system/post/${id}`)
}

export function movePost(id, parentId) {
  return request.post(`/system/post/${id}/move`, null, { params: { parentId } })
}
