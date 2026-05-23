import request from '@/utils/request'

// 获取用户列表
export function getUserList() {
  return request({
    url: '/system/user/list',
    method: 'get'
  })
}

// 获取用户分页
export function getUserPage(params) {
  return request({
    url: '/system/user/page',
    method: 'get',
    params
  })
}

// 获取用户详情
export function getUser(id) {
  return request({
    url: '/system/user/get',
    method: 'get',
    params: { id }
  })
}

// 新增用户
export function createUser(data) {
  return request({
    url: '/system/user/create',
    method: 'post',
    data
  })
}

// 修改用户
export function updateUser(data) {
  return request({
    url: '/system/user/update',
    method: 'put',
    data
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: '/system/user/delete',
    method: 'delete',
    params: { id }
  })
}

// 获取用户角色列表
export function getUserRoleIds(userId) {
  return request({
    url: '/system/user/get-role-ids',
    method: 'get',
    params: { userId }
  })
}

// 分配用户角色
export function assignUserRole(data) {
  return request({
    url: '/system/user/assign-role',
    method: 'post',
    data
  })
}

// 更新用户状态
export function updateUserStatus(id, status) {
  return request({
    url: '/system/user/update-status',
    method: 'put',
    params: { id, status }
  })
}

// 重置用户密码
export function resetUserPassword(id, password) {
  return request({
    url: '/system/user/reset-password',
    method: 'put',
    params: { id, password }
  })
}

// 用户回收站分页
export function getRecycleUserPage(params) {
  return request({
    url: '/system/user/recycle/page',
    method: 'get',
    params
  })
}

// 恢复用户
export function restoreUser(id) {
  return request({
    url: '/system/user/restore',
    method: 'put',
    params: { id }
  })
}

// 彻底删除用户
export function deleteUserPermanent(id) {
  return request({
    url: '/system/user/delete-permanent',
    method: 'delete',
    params: { id }
  })
}
