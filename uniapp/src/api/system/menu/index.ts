import { get, post, put, del } from '@/utils/request'

import type { PageResult } from '@/types/api'

import type { MenuSaveDTO, MenuVO } from '@/types/system'



export function getMenuList(params?: { name?: string; status?: number | null; type?: number | null }) {

  return get<MenuVO[]>('/system/menu/list', params)

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



export function getRecycleMenuPage(params: { pageNo?: number; pageSize?: number; name?: string }) {

  return get<PageResult<MenuVO>>('/system/menu/recycle/page', params)

}



export function restoreMenu(id: number) {

  return put('/system/menu/restore', null, { params: { id } })

}



export function deleteMenuPermanent(id: number) {

  return del('/system/menu/delete-permanent', { params: { id } })

}


