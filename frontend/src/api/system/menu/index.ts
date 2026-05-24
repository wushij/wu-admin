import { get, post, put, del } from '@/utils/request'
import type { PageResult, RecyclePageQuery } from '@/types/api'

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
  isFrame?: number
  children?: MenuVO[]
  createTime?: string
  updateTime?: string
}

export interface MenuListQuery {
  name?: string
  status?: number | null
  type?: number | null
}

export type MenuRecycleQuery = RecyclePageQuery & Pick<MenuListQuery, 'name' | 'status'>

export interface MenuSaveDTO {
  id?: number | null
  parentId?: number
  name: string
  type: number
  path?: string
  component?: string
  permission?: string
  sort?: number
  icon?: string
  status?: number
  isFrame?: number
}

export function getMenuList(params?: MenuListQuery) {
  return get<MenuVO[]>('/system/menu/list', params)
}

export function getMenuSimpleList() {
  return get<MenuVO[]>('/system/menu/simple-list')
}

export function getMenu(id: number) {
  return get<MenuVO>('/system/menu/get', { id })
}

export function createMenu(data: MenuSaveDTO) {
  return post('/system/menu/create', data)
}

export function updateMenu(data: MenuSaveDTO) {
  return put('/system/menu/update', data)
}

export function deleteMenu(id: number) {
  return del('/system/menu/delete', { params: { id } })
}

export function updateMenuStatus(id: number, status: number) {
  return put('/system/menu/update-status', null, { params: { id, status } })
}

export function getRecycleMenuPage(params: MenuRecycleQuery) {
  return get<PageResult<MenuVO>>('/system/menu/recycle/page', params)
}

export function restoreMenu(id: number) {
  return put('/system/menu/restore', null, { params: { id } })
}

export function deleteMenuPermanent(id: number) {
  return del('/system/menu/delete-permanent', { params: { id } })
}
