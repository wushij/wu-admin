/** 系统内置字典类型编码（与 sys_dict_type.dict_type 一致） */
export const DICT_TYPE = {
  /** 通用启用/停用，键值 1/0 */
  NORMAL_DISABLE: 'sys_normal_disable',
  /** 用户性别 */
  USER_SEX: 'sys_user_sex',
  /** 是/否，键值 Y/N */
  YES_NO: 'sys_yes_no'
}

/** 登录后预加载的常用字典 */
export const COMMON_DICT_TYPES = [
  DICT_TYPE.NORMAL_DISABLE,
  DICT_TYPE.USER_SEX,
  DICT_TYPE.YES_NO
]
