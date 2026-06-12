/** 统一 API 响应结构（与后端 CommonResult 一致） */
export interface ApiResult<T = unknown> {
  code: number
  msg?: string
  message?: string
  data: T
}

export interface PageQuery {
  pageNo?: number
  pageSize?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}

export interface LoginForm {
  loginType?: 'account' | 'sms'
  username?: string
  password?: string
  phone?: string
  uuid?: string
  code?: string
  rememberMe?: boolean
}

export interface RegisterForm {
  username: string
  password: string
  nickname?: string
  mobile?: string
  uuid?: string
  code?: string
}

/** 登录响应（移动端须读取 token 字段） */
export interface LoginResult {
  userId: number
  username: string
  nickname?: string
  token?: string
}

export interface AuthInfo {
  userId: number
  username: string
  nickname?: string
  avatar?: string
  roles?: string[]
  permissions?: string[]
  menus?: MenuTreeNode[]
}

export interface MenuTreeNode {
  id: number
  name: string
  permission?: string
  type?: number
  sort?: number
  parentId?: number
  path?: string
  icon?: string
  status?: number
  component?: string
  children?: MenuTreeNode[]
}

export interface DictDataItem {
  id?: number
  dictType?: string
  dictLabel: string
  dictValue: string
  listClass?: string
  status?: number
  sort?: number
  isDefault?: number
  remark?: string
}

export interface DictOption {
  label: string
  value: string | number
  raw?: DictDataItem
}
