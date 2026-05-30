/** 系统内置字典类型编码（与 sys_dict_type.dict_type 一致） */
export const DICT_TYPE = {
  /** 通用启用/停用，键值 1/0 */
  NORMAL_DISABLE: 'sys_normal_disable',
  /** 用户性别 */
  USER_SEX: 'sys_user_sex',
  /** 是/否，键值 Y/N */
  YES_NO: 'sys_yes_no',
  /** 工单状态 */
  TICKET_STATUS: 'sys_ticket_status',
  /** 工单优先级 */
  TICKET_PRIORITY: 'sys_ticket_priority',
  /** 审批单类型 */
  APPROVAL_FORM_TYPE: 'sys_approval_form_type',
  /** 审批单状态 */
  APPROVAL_STATUS: 'sys_approval_status',
} as const

export type DictTypeCode = (typeof DICT_TYPE)[keyof typeof DICT_TYPE]

/** 登录后预加载的常用字典 */
export const COMMON_DICT_TYPES: DictTypeCode[] = [
  DICT_TYPE.NORMAL_DISABLE,
  DICT_TYPE.USER_SEX,
  DICT_TYPE.YES_NO,
  DICT_TYPE.TICKET_STATUS,
  DICT_TYPE.TICKET_PRIORITY,
  DICT_TYPE.APPROVAL_FORM_TYPE,
  DICT_TYPE.APPROVAL_STATUS,
]
