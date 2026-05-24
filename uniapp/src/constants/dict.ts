/** 与 PC constants/dict.ts 对齐 */
export const DICT_TYPE = {
  NORMAL_DISABLE: 'sys_normal_disable',
  USER_SEX: 'sys_user_sex',
  YES_NO: 'sys_yes_no',
  TICKET_STATUS: 'sys_ticket_status',
  TICKET_PRIORITY: 'sys_ticket_priority',
  APPROVAL_FORM_TYPE: 'sys_approval_form_type',
  APPROVAL_STATUS: 'sys_approval_status',
} as const

export type DictTypeCode = (typeof DICT_TYPE)[keyof typeof DICT_TYPE]

export const COMMON_DICT_TYPES: DictTypeCode[] = [
  DICT_TYPE.NORMAL_DISABLE,
  DICT_TYPE.TICKET_STATUS,
  DICT_TYPE.TICKET_PRIORITY,
  DICT_TYPE.APPROVAL_FORM_TYPE,
  DICT_TYPE.APPROVAL_STATUS,
]
