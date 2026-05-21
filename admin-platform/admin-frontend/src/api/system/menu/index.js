import request from '@/utils/request'

// 获取菜单树（管理页，支持 name/status/type 筛选）
export function getMenuList(params) {
  return request({
    url: '/system/menu/list',
    method: 'get',
    params
  })
}

// 全量扁平列表（角色分配菜单等）
export function getMenuSimpleList() {
  return request({
    url: '/system/menu/simple-list',
    method: 'get'
  })
}

// 获取菜单详情
export function getMenu(id) {
  return request({
    url: '/system/menu/get',
    method: 'get',
    params: { id }
  })
}

// 新增菜单
export function createMenu(data) {
  return request({
    url: '/system/menu/create',
    method: 'post',
    data
  })
}

// 修改菜单
export function updateMenu(data) {
  return request({
    url: '/system/menu/update',
    method: 'put',
    data
  })
}

// 删除菜单
export function deleteMenu(id) {
  return request({
    url: '/system/menu/delete',
    method: 'delete',
    params: { id }
  })
}

// 更新菜单状态
export function updateMenuStatus(id, status) {
  return request({
    url: '/system/menu/update-status',
    method: 'put',
    params: { id, status }
  })
}

// 菜单回收站分页
export function getRecycleMenuPage(params) {
  return request({
    url: '/system/menu/recycle/page',
    method: 'get',
    params
  })
}

// 恢复菜单
export function restoreMenu(id) {
  return request({
    url: '/system/menu/restore',
    method: 'put',
    params: { id }
  })
}

// 彻底删除菜单
export function deleteMenuPermanent(id) {
  return request({
    url: '/system/menu/delete-permanent',
    method: 'delete',
    params: { id }
  })
}
