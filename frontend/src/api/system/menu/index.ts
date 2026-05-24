import request from '@/utils/request'
import type { ApiResult, PageQuery, PageResult } from '@/types/api'

export interface MenuVO {
  id: number
  name: string
  permission?: string
  type?: number
  sort?: number
  parentId?: number
  path?: string
  icon?: string
  component?: string
  status?: number
  children?: MenuVO[]
  [key: string]: unknown
}

export function getMenuList(params?: Record<string, unknown>) {
  return request.get('/system/menu/list', { params }) as Promise<ApiResult<MenuVO[]>>
}

export function getMenuSimpleList() {
  return request.get('/system/menu/simple-list') as Promise<ApiResult<MenuVO[]>>
}

export function getMenu(id: number) {
  return request.get('/system/menu/get', { params: { id } }) as Promise<ApiResult<MenuVO>>
}

export function createMenu(data: Record<string, unknown>) {
  return request.post('/system/menu/create', data)
}

export function updateMenu(data: Record<string, unknown>) {
  return request.put('/system/menu/update', data)
}

export function deleteMenu(id: number) {
  return request.delete('/system/menu/delete', { params: { id } })
}

export function updateMenuStatus(id: number, status: number) {
  return request.put('/system/menu/update-status', null, { params: { id, status } })
}

export function getRecycleMenuPage(params: PageQuery) {
  return request.get('/system/menu/recycle/page', { params }) as Promise<ApiResult<PageResult<MenuVO>>>
}

export function restoreMenu(id: number) {
  return request.put('/system/menu/restore', null, { params: { id } })
}

export function deleteMenuPermanent(id: number) {
  return request.delete('/system/menu/delete-permanent', { params: { id } })
}
