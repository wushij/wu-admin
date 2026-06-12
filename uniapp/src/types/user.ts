export interface UserVO {
  id: number
  username: string
  nickname?: string
  mobile?: string
  email?: string
  avatar?: string
  status?: number
  deptId?: number
  deptName?: string
  postIds?: number[]
  postNames?: string
  roleIds?: number[]
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface UserPageQuery {
  pageNo?: number
  pageSize?: number
  keyword?: string
  username?: string
  nickname?: string
  mobile?: string
  status?: number | null
  deptId?: number | null
}

export interface UserSaveDTO {
  id?: number | null
  username: string
  nickname: string
  password?: string
  mobile?: string
  email?: string
  deptId?: number | null
  status?: number
  roleId?: number | null
  postIds?: number[]
  remark?: string
}
