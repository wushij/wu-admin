import type { PageResult, ApiResult } from '@/types/api'
import type { IconName } from '@/constants/iconfont'
import { getRecycleUserPage, restoreUser, deleteUserPermanent } from '@/api/system/user'
import { getRecycleRolePage, restoreRole, deleteRolePermanent } from '@/api/system/role'
import { getRecycleMenuPage, restoreMenu, deleteMenuPermanent } from '@/api/system/menu'
import {
  getRecycleDeptPage,
  restoreDept,
  deleteDeptPermanent,
  getRecyclePostPage,
  restorePost,
  deletePostPermanent,
} from '@/api/system/dept'
import { getRecycleTicketPage, restoreTicket, deleteTicketPermanent } from '@/api/system/ticket'
import { getRecycleApprovalPage, restoreApproval, deleteApprovalPermanent } from '@/api/system/approval'
import { getRecycleAnnouncePage, restoreAnnounce, deleteAnnouncePermanent } from '@/api/message'
import { getRecycleJobPage, restoreJob, deleteJobPermanent, getRecycleJobLogPage, restoreJobLog, deleteJobLogPermanent } from '@/api/monitor/job'
import {
  getRecycleDictTypePage,
  restoreDictType,
  deleteDictTypePermanent,
  getRecycleDictDataPage,
  restoreDictData,
  deleteDictDataPermanent,
} from '@/api/system/dict'
import { getRecycleFilePage, restoreFile, deleteFilePermanent } from '@/api/system/file'

export interface RecycleSearchField {
  key: string
  label: string
  placeholder: string
}

export interface RecycleDetailField {
  prop: string
  label: string
  dictType?: string
  tag?: boolean
  format?: 'datetime' | 'bytes'
}

export interface RecycleThumbConfig {
  type: 'avatar' | 'image' | 'file'
  srcField?: string
  nameField?: string
}

export interface RecycleModule {
  key: string
  label: string
  theme: string
  icon: IconName
  listPerm: string
  deletePerm: string
  titleField: string
  subField?: string
  subFieldLabel?: string
  hint?: string
  searchFields?: RecycleSearchField[]
  detailFields: RecycleDetailField[]
  thumb?: RecycleThumbConfig
  fetchPage: (params: Record<string, unknown>) => Promise<ApiResult<PageResult<Record<string, unknown>>>>
  restore: (id: number) => Promise<unknown>
  deletePermanent: (id: number) => Promise<unknown>
}

export const RECYCLE_MODULES: RecycleModule[] = [
  {
    key: 'user',
    label: '用户',
    theme: 'user',
    icon: 'user-o',
    listPerm: 'system:user:list',
    deletePerm: 'system:user:delete',
    titleField: 'nickname',
    subField: 'username',
    subFieldLabel: '用户名',
    searchFields: [{ key: 'username', label: '用户名', placeholder: '请输入用户名' }],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'username', label: '用户名' },
      { prop: 'nickname', label: '昵称' },
      { prop: 'mobile', label: '手机号' },
      { prop: 'deptName', label: '部门' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    thumb: { type: 'avatar', srcField: 'avatar', nameField: 'nickname' },
    fetchPage: (p) => getRecycleUserPage(p as never) as never,
    restore: restoreUser,
    deletePermanent: deleteUserPermanent,
  },
  {
    key: 'role',
    label: '角色',
    theme: 'role',
    icon: 'shield-o',
    listPerm: 'system:role:list',
    deletePerm: 'system:role:delete',
    titleField: 'name',
    subField: 'code',
    subFieldLabel: '编码',
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'name', label: '角色名称' },
      { prop: 'code', label: '角色编码' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleRolePage(p as never) as never,
    restore: restoreRole,
    deletePermanent: deleteRolePermanent,
  },
  {
    key: 'menu',
    label: '菜单',
    theme: 'menu',
    icon: 'apps-o',
    listPerm: 'system:menu:list',
    deletePerm: 'system:menu:delete',
    titleField: 'name',
    subField: 'path',
    subFieldLabel: '路由',
    searchFields: [{ key: 'name', label: '菜单名', placeholder: '请输入菜单名称' }],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'name', label: '菜单名称' },
      { prop: 'path', label: '路由' },
      { prop: 'permission', label: '权限标识' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleMenuPage(p as never) as never,
    restore: restoreMenu,
    deletePermanent: deleteMenuPermanent,
  },
  {
    key: 'dept',
    label: '部门',
    theme: 'dept',
    icon: 'cluster-o',
    listPerm: 'system:dept:list',
    deletePerm: 'system:dept:delete',
    titleField: 'name',
    subField: 'leaderName',
    subFieldLabel: '负责人',
    searchFields: [{ key: 'name', label: '部门名', placeholder: '请输入部门名称' }],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'name', label: '部门名称' },
      { prop: 'leaderName', label: '负责人' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleDeptPage(p as never) as never,
    restore: restoreDept,
    deletePermanent: deleteDeptPermanent,
  },
  {
    key: 'post',
    label: '岗位',
    theme: 'post',
    icon: 'manager-o',
    listPerm: 'system:post:query',
    deletePerm: 'system:post:delete',
    titleField: 'postName',
    subField: 'postCode',
    subFieldLabel: '编码',
    searchFields: [{ key: 'postName', label: '岗位名', placeholder: '请输入岗位名称' }],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'postName', label: '岗位名称' },
      { prop: 'postCode', label: '岗位编码' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecyclePostPage(p as never) as never,
    restore: restorePost,
    deletePermanent: deletePostPermanent,
  },
  {
    key: 'ticket',
    label: '工单',
    theme: 'ticket',
    icon: 'notes-o',
    listPerm: 'system:ticket:list',
    deletePerm: 'system:ticket:delete',
    titleField: 'title',
    subField: 'ticketNo',
    subFieldLabel: '工单号',
    searchFields: [{ key: 'title', label: '标题', placeholder: '请输入工单标题' }],
    detailFields: [
      { prop: 'ticketNo', label: '工单号' },
      { prop: 'title', label: '标题' },
      { prop: 'status', label: '状态', dictType: 'sys_ticket_status', tag: true },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleTicketPage(p as never) as never,
    restore: restoreTicket,
    deletePermanent: deleteTicketPermanent,
  },
  {
    key: 'approval',
    label: '审批',
    theme: 'approval',
    icon: 'completed',
    listPerm: 'system:approval:list',
    deletePerm: 'system:approval:delete',
    titleField: 'title',
    subField: 'formNo',
    subFieldLabel: '单号',
    searchFields: [{ key: 'title', label: '标题', placeholder: '请输入审批标题' }],
    detailFields: [
      { prop: 'formNo', label: '单号' },
      { prop: 'title', label: '标题' },
      { prop: 'formType', label: '类型', dictType: 'sys_approval_form_type', tag: true },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleApprovalPage(p as never) as never,
    restore: restoreApproval,
    deletePermanent: deleteApprovalPermanent,
  },
  {
    key: 'dict',
    label: '字典类型',
    theme: 'dict',
    icon: 'balance-list-o',
    listPerm: 'system:dict:list',
    deletePerm: 'system:dict:delete',
    titleField: 'dictName',
    subField: 'dictType',
    hint: '恢复字典类型不会还原已级联删除的字典数据，需重新维护数据项。',
    searchFields: [
      { key: 'dictName', label: '名称', placeholder: '字典名称' },
      { key: 'dictType', label: '类型', placeholder: '字典类型编码' },
    ],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'dictName', label: '字典名称' },
      { prop: 'dictType', label: '字典类型' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleDictTypePage(p as never) as never,
    restore: restoreDictType,
    deletePermanent: deleteDictTypePermanent,
  },
  {
    key: 'dictData',
    label: '字典数据',
    theme: 'dict',
    icon: 'records-o',
    listPerm: 'system:dict:list',
    deletePerm: 'system:dict:delete',
    titleField: 'dictLabel',
    subField: 'dictType',
    hint: '恢复后字典缓存会自动刷新；若所属字典类型已删除，请先恢复字典类型。',
    searchFields: [
      { key: 'dictType', label: '类型', placeholder: '字典类型编码' },
      { key: 'dictLabel', label: '标签', placeholder: '字典标签' },
    ],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'dictType', label: '字典类型' },
      { prop: 'dictLabel', label: '标签' },
      { prop: 'dictValue', label: '键值' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleDictDataPage(p as never) as never,
    restore: restoreDictData,
    deletePermanent: deleteDictDataPermanent,
  },
  {
    key: 'announce',
    label: '系统通知',
    theme: 'notice',
    icon: 'bell',
    listPerm: 'system:announce:list',
    deletePerm: 'system:announce:delete',
    titleField: 'title',
    subField: 'createName',
    subFieldLabel: '创建人',
    searchFields: [{ key: 'title', label: '标题', placeholder: '请输入通知标题' }],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'title', label: '标题' },
      { prop: 'createName', label: '创建人' },
      { prop: 'status', label: '状态' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleAnnouncePage(p as never) as never,
    restore: restoreAnnounce,
    deletePermanent: deleteAnnouncePermanent,
  },
  {
    key: 'job',
    label: '定时任务',
    theme: 'job',
    icon: 'clock-o',
    listPerm: 'monitor:job:delete',
    deletePerm: 'monitor:job:delete',
    titleField: 'jobName',
    subField: 'jobGroup',
    subFieldLabel: '任务组',
    searchFields: [{ key: 'jobName', label: '任务名', placeholder: '请输入任务名称' }],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'jobName', label: '任务名称' },
      { prop: 'jobGroup', label: '任务组' },
      { prop: 'cronExpression', label: 'Cron' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleJobPage(p as never) as never,
    restore: restoreJob,
    deletePermanent: deleteJobPermanent,
  },
  {
    key: 'jobLog',
    label: '调度日志',
    theme: 'job',
    icon: 'records-o',
    listPerm: 'monitor:job:delete',
    deletePerm: 'monitor:job:delete',
    titleField: 'jobName',
    subField: 'jobGroup',
    subFieldLabel: '任务组',
    hint: '清空调度日志后会进入此处，恢复后可在定时任务日志中重新查看。',
    searchFields: [{ key: 'jobName', label: '任务名', placeholder: '请输入任务名称' }],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'jobName', label: '任务名称' },
      { prop: 'jobGroup', label: '任务组' },
      { prop: 'startTime', label: '开始时间', format: 'datetime' },
      { prop: 'durationMs', label: '耗时(ms)' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    fetchPage: (p) => getRecycleJobLogPage(p as never) as never,
    restore: restoreJobLog,
    deletePermanent: deleteJobLogPermanent,
  },
  {
    key: 'file',
    label: '文件',
    theme: 'file-store',
    icon: 'coupon-o',
    listPerm: 'sys:file:list',
    deletePerm: 'sys:file:delete',
    titleField: 'originalName',
    subField: 'fileSuffix',
    subFieldLabel: '后缀',
    searchFields: [{ key: 'originalName', label: '文件名', placeholder: '请输入文件名' }],
    detailFields: [
      { prop: 'id', label: 'ID' },
      { prop: 'originalName', label: '文件名' },
      { prop: 'fileSuffix', label: '后缀' },
      { prop: 'fileSize', label: '大小(B)', format: 'bytes' },
      { prop: 'updateTime', label: '删除时间', format: 'datetime' },
    ],
    thumb: { type: 'file' },
    fetchPage: (p) => getRecycleFilePage(p as never) as never,
    restore: restoreFile,
    deletePermanent: deleteFilePermanent,
  },
]
