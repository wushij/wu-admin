/** 统一 API 响应结构（与后端 CommonResult 一致） */
export interface ApiResult<T = unknown> {
  code: number
  msg?: string
  message?: string
  data: T
}

/** 分页查询参数 */
export interface PageQuery {
  pageNo?: number
  pageSize?: number
}

/** 回收站分页（pageNo/pageSize 必填，便于列表翻页逻辑类型安全） */
export interface RecyclePageQuery {
  pageNo: number
  pageSize: number
}

/** 实体公共时间字段（与后端 BaseEntity 一致） */
export interface EntityTimestamps {
  createTime?: string
  updateTime?: string
  creator?: string
  updater?: string
  deleted?: number
}

/** 分页列表响应 */
export interface PageResult<T> {
  list: T[]
  total: number
}

/** 登录表单 */
export interface LoginForm {
  loginType?: 'account' | 'sms'
  username?: string
  password?: string
  phone?: string
  uuid?: string
  code?: string
  rememberMe?: boolean
}

/** 注册表单 */
export interface RegisterForm {
  username: string
  password: string
  nickname?: string
  mobile?: string
  uuid?: string
  code?: string
}

/** 登录响应（Token 由后端写入 httpOnly Cookie，响应体不再返回） */
export interface LoginResult {
  userId: number
  username: string
  nickname?: string
}

/** 用户信息（/auth/info） */
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

/** 字典项（后端 DictData） */
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

/** 字典下拉选项 */
export interface DictOption {
  label: string
  value: string | number
  raw?: DictDataItem
}
