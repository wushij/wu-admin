import request from '@/utils/request'

// 获取角色列表
export function getRoleList(params) {
  return request({
    url: '/system/role/list',
    method: 'get',
    params
  })
}

// 获取角色分页
export function getRolePage(params) {
  return request({
    url: '/system/role/page',
    method: 'get',
    params
  })
}

// 获取角色详情
export function getRole(id) {
  return request({
    url: '/system/role/get',
    method: 'get',
    params: { id }
  })
}

// 新增角色
export function createRole(data) {
  return request({
    url: '/system/role/create',
    method: 'post',
    data
  })
}

// 修改角色
export function updateRole(data) {
  return request({
    url: '/system/role/update',
    method: 'put',
    data
  })
}

// 删除角色
export function deleteRole(id) {
  return request({
    url: '/system/role/delete',
    method: 'delete',
    params: { id }
  })
}

// 获取角色菜单列表
export function getRoleMenuIds(roleId) {
  return request({
    url: '/system/role/get-menu-ids',
    method: 'get',
    params: { roleId }
  })
}

// 分配角色菜单
export function assignRoleMenu(data) {
  return request({
    url: '/system/role/assign-menu',
    method: 'post',
    data
  })
}

// 更新角色状态
export function updateRoleStatus(id, status) {
  return request({
    url: '/system/role/update-status',
    method: 'put',
    params: { id, status }
  })
}

// 角色回收站分页
export function getRecycleRolePage(params) {
  return request({
    url: '/system/role/recycle/page',
    method: 'get',
    params
  })
}

// 恢复角色
export function restoreRole(id) {
  return request({
    url: '/system/role/restore',
    method: 'put',
    params: { id }
  })
}

// 彻底删除角色
export function deleteRolePermanent(id) {
  return request({
    url: '/system/role/delete-permanent',
    method: 'delete',
    params: { id }
  })
}
