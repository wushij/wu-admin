import { get, post, put, del } from '@/utils/request'
import type { PageQuery } from '@/types/api'

// ========== 用户管理 ==========
export const getUserList = (params?: Record<string, unknown>) => {
  return get('/system/user/list', params)
}

export const getUserDetail = (id: number) => {
  return get(`/system/user/${id}`)
}

export const createUser = (data: Record<string, unknown>) => {
  return post('/system/user', data)
}

export const updateUser = (data: Record<string, unknown>) => {
  return put('/system/user', data)
}

export const deleteUser = (id: number) => {
  return del(`/system/user/${id}`)
}

// ========== 角色管理 ==========
export const getRoleList = (params?: Record<string, unknown>) => {
  return get('/system/role/list', params)
}

export const getRoleDetail = (id: number) => {
  return get(`/system/role/${id}`)
}

export const createRole = (data: Record<string, unknown>) => {
  return post('/system/role', data)
}

export const updateRole = (data: Record<string, unknown>) => {
  return put('/system/role', data)
}

export const deleteRole = (id: number) => {
  return del(`/system/role/${id}`)
}

// ========== 菜单管理 ==========
export const getMenuList = (params?: Record<string, unknown>) => {
  return get('/system/menu/list', params)
}

export const getMenuDetail = (id: number) => {
  return get(`/system/menu/${id}`)
}

export const createMenu = (data: Record<string, unknown>) => {
  return post('/system/menu', data)
}

export const updateMenu = (data: Record<string, unknown>) => {
  return put('/system/menu', data)
}

export const deleteMenu = (id: number) => {
  return del(`/system/menu/${id}`)
}

// ========== 部门管理 ==========
export const getDeptList = (params?: Record<string, unknown>) => {
  return get('/system/dept/list', params)
}

export const getDeptDetail = (id: number) => {
  return get(`/system/dept/${id}`)
}

export const createDept = (data: Record<string, unknown>) => {
  return post('/system/dept', data)
}

export const updateDept = (data: Record<string, unknown>) => {
  return put('/system/dept', data)
}

export const deleteDept = (id: number) => {
  return del(`/system/dept/${id}`)
}

// ========== 登录日志 ==========
export const getLoginLogList = (params: PageQuery) => {
  return get<import('@/types/api').PageResult<Record<string, unknown>>>('/system/login-log/list', params)
}

export const deleteLoginLog = (id: number) => {
  return del(`/system/login-log/${id}`)
}

export const clearLoginLog = () => {
  return del('/system/login-log/clear')
}
