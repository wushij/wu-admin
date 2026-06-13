import type { Component } from 'vue'
import {
  User,
  UserFilled,
  Menu as MenuIcon,
  OfficeBuilding,
  Briefcase,
  Tickets,
  Checked,
  Collection,
  Bell,
  List,
  Timer,
  Folder,
  DocumentCopy,
} from '@element-plus/icons-vue'
import { getRecycleUserPage, restoreUser, deleteUserPermanent } from '@/api/system/user'
import { getRecycleRolePage, restoreRole, deleteRolePermanent } from '@/api/system/role'
import { getRecycleMenuPage, restoreMenu, deleteMenuPermanent } from '@/api/system/menu'
import { getRecycleDeptPage, restoreDept, deleteDeptPermanent } from '@/api/system/dept'
import { getRecyclePostPage, restorePost, deletePostPermanent } from '@/api/system/post'
import { getRecycleTicketPage, restoreTicket, deleteTicketPermanent } from '@/api/system/ticket'
import { getApprovalRecyclePage, restoreApproval, deleteApprovalPermanent } from '@/api/system/approval'
import {
  getRecycleDictTypePage,
  restoreDictType,
  deleteDictTypePermanent,
  getRecycleDictDataPage,
  restoreDictData,
  deleteDictDataPermanent,
} from '@/api/system/dict'
import {
  getRecycleAnnouncePage,
  restoreAnnounce,
  deleteAnnouncePermanent,
} from '@/api/message/index'
import { getRecycleJobPage, restoreJob, deleteJobPermanent, getRecycleJobLogPage, restoreJobLog, deleteJobLogPermanent } from '@/api/monitor/job'
import { getRecycleFilePage, restoreFile, deleteFilePermanent } from '@/api/system/file'
import { getRecycleGenPage, restoreGenTable, deleteGenTablePermanent } from '@/api/tool/gen'
import type { ApiResult, PageResult } from '@/types/api'
import type { RecycleSummary } from '@/api/system/recycle'

export type RecycleTypeKey = keyof Pick<
  RecycleSummary,
  | 'user'
  | 'role'
  | 'menu'
  | 'dept'
  | 'post'
  | 'ticket'
  | 'approval'
  | 'dict'
  | 'dictData'
  | 'announce'
  | 'job'
  | 'jobLog'
  | 'file'
  | 'gen'
>

export interface RecycleColumn {
  prop: string
  label: string
  width?: number
  minWidth?: number
  dictType?: string
  tag?: boolean
}

/** 类型卡片图标渐变色（与工作台 StatCard 色系一致） */
export type RecycleAccent =
  | 'user'
  | 'role'
  | 'menu'
  | 'dept'
  | 'post'
  | 'ticket'
  | 'approval'
  | 'dict'
  | 'dictData'
  | 'announce'
  | 'job'
  | 'jobLog'
  | 'file'
  | 'gen'

export interface RecycleTypeConfig {
  key: RecycleTypeKey
  label: string
  icon: Component
  accent: RecycleAccent
  permission: string
  deletePermission: string
  hint?: string
  columns: RecycleColumn[]
  searchFields?: Array<{ key: string; label: string; placeholder: string }>
  fetchPage: (params: Record<string, unknown>) => Promise<ApiResult<PageResult<Record<string, unknown>>>>
  restore: (id: number) => Promise<unknown>
  deletePermanent: (id: number) => Promise<unknown>
}

export const RECYCLE_TYPES: RecycleTypeConfig[] = [
  {
    key: 'user',
    label: '用户',
    icon: User,
    accent: 'user',
    permission: 'system:user:list',
    deletePermission: 'system:user:delete',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'username', label: '用户名', width: 120 },
      { prop: 'nickname', label: '昵称', width: 120 },
      { prop: 'mobile', label: '手机号', width: 130 },
      { prop: 'deptName', label: '部门', minWidth: 120 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'username', label: '用户名', placeholder: '请输入用户名' }],
    fetchPage: (p) => getRecycleUserPage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreUser,
    deletePermanent: deleteUserPermanent,
  },
  {
    key: 'role',
    label: '角色',
    icon: UserFilled,
    accent: 'role',
    permission: 'system:role:list',
    deletePermission: 'system:role:delete',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'name', label: '角色名称', minWidth: 140 },
      { prop: 'code', label: '角色编码', width: 140 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    fetchPage: (p) => getRecycleRolePage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreRole,
    deletePermanent: deleteRolePermanent,
  },
  {
    key: 'menu',
    label: '菜单',
    icon: MenuIcon,
    accent: 'menu',
    permission: 'system:menu:list',
    deletePermission: 'system:menu:delete',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'name', label: '菜单名称', minWidth: 160 },
      { prop: 'path', label: '路由', width: 160 },
      { prop: 'permission', label: '权限标识', minWidth: 160 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'name', label: '菜单名', placeholder: '请输入菜单名称' }],
    fetchPage: (p) => getRecycleMenuPage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreMenu,
    deletePermanent: deleteMenuPermanent,
  },
  {
    key: 'dept',
    label: '部门',
    icon: OfficeBuilding,
    accent: 'dept',
    permission: 'system:dept:list',
    deletePermission: 'system:dept:delete',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'name', label: '部门名称', minWidth: 160 },
      { prop: 'leaderName', label: '负责人', width: 120 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'name', label: '部门名', placeholder: '请输入部门名称' }],
    fetchPage: (p) => getRecycleDeptPage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreDept,
    deletePermanent: deleteDeptPermanent,
  },
  {
    key: 'post',
    label: '岗位',
    icon: Briefcase,
    accent: 'post',
    // 岗位无独立 list 菜单，与组织管理一致用 query（后端 hasRead 亦将 list 映射到 query）
    permission: 'system:post:query',
    deletePermission: 'system:post:delete',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'postName', label: '岗位名称', minWidth: 140 },
      { prop: 'postCode', label: '岗位编码', width: 140 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'postName', label: '岗位名', placeholder: '请输入岗位名称' }],
    fetchPage: (p) => getRecyclePostPage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restorePost,
    deletePermanent: deletePostPermanent,
  },
  {
    key: 'ticket',
    label: '工单',
    icon: Tickets,
    accent: 'ticket',
    permission: 'system:ticket:list',
    deletePermission: 'system:ticket:delete',
    columns: [
      { prop: 'ticketNo', label: '工单号', width: 180 },
      { prop: 'title', label: '标题', minWidth: 160 },
      { prop: 'status', label: '状态', width: 100, dictType: 'sys_ticket_status', tag: true },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'title', label: '标题', placeholder: '请输入工单标题' }],
    fetchPage: (p) => getRecycleTicketPage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreTicket,
    deletePermanent: deleteTicketPermanent,
  },
  {
    key: 'approval',
    label: '审批',
    icon: Checked,
    accent: 'approval',
    permission: 'system:approval:list',
    deletePermission: 'system:approval:delete',
    columns: [
      { prop: 'formNo', label: '单号', width: 190 },
      { prop: 'title', label: '标题', minWidth: 160 },
      { prop: 'formType', label: '类型', width: 110, dictType: 'sys_approval_form_type', tag: true },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'title', label: '标题', placeholder: '请输入审批标题' }],
    fetchPage: (p) => getApprovalRecyclePage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreApproval,
    deletePermanent: deleteApprovalPermanent,
  },
  {
    key: 'dict',
    label: '字典类型',
    icon: Collection,
    accent: 'dict',
    permission: 'system:dict:list',
    deletePermission: 'system:dict:delete',
    hint: '恢复字典类型不会还原已级联删除的字典数据，需重新维护数据项。',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'dictName', label: '字典名称', minWidth: 140 },
      { prop: 'dictType', label: '字典类型', width: 160 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [
      { key: 'dictName', label: '名称', placeholder: '字典名称' },
      { key: 'dictType', label: '类型', placeholder: '字典类型编码' },
    ],
    fetchPage: (p) => getRecycleDictTypePage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreDictType,
    deletePermanent: deleteDictTypePermanent,
  },
  {
    key: 'dictData',
    label: '字典数据',
    icon: List,
    accent: 'dictData',
    permission: 'system:dict:list',
    deletePermission: 'system:dict:delete',
    hint: '恢复后字典缓存会自动刷新；若所属字典类型已删除，请先恢复字典类型。',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'dictType', label: '字典类型', width: 160 },
      { prop: 'dictLabel', label: '标签', minWidth: 120 },
      { prop: 'dictValue', label: '键值', width: 120 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [
      { key: 'dictType', label: '类型', placeholder: '字典类型编码' },
      { key: 'dictLabel', label: '标签', placeholder: '字典标签' },
    ],
    fetchPage: (p) => getRecycleDictDataPage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreDictData,
    deletePermanent: deleteDictDataPermanent,
  },
  {
    key: 'announce',
    label: '系统通知',
    icon: Bell,
    accent: 'announce',
    permission: 'system:announce:list',
    deletePermission: 'system:announce:delete',
    hint: '恢复后用户收件记录保留；已发布通知不会自动重新推送。',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'title', label: '标题', minWidth: 180 },
      { prop: 'createName', label: '创建人', width: 110 },
      { prop: 'status', label: '状态', width: 90 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'title', label: '标题', placeholder: '请输入通知标题' }],
    fetchPage: (p) => getRecycleAnnouncePage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreAnnounce,
    deletePermanent: deleteAnnouncePermanent,
  },
  {
    key: 'job',
    label: '定时任务',
    icon: Timer,
    accent: 'job',
    permission: 'monitor:job:delete',
    deletePermission: 'monitor:job:delete',
    hint: '恢复后将重新注册 Quartz 调度；暂停状态的任务会以暂停方式恢复。',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'jobName', label: '任务名称', minWidth: 160 },
      { prop: 'jobGroup', label: '任务组', width: 110 },
      { prop: 'cronExpression', label: 'Cron', minWidth: 140 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [
      { key: 'jobName', label: '任务名', placeholder: '请输入任务名称' },
      { key: 'jobGroup', label: '任务组', placeholder: '如 SYSTEM' },
    ],
    fetchPage: (p) => getRecycleJobPage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreJob,
    deletePermanent: deleteJobPermanent,
  },
  {
    key: 'jobLog',
    label: '调度日志',
    icon: List,
    accent: 'job',
    permission: 'monitor:job:delete',
    deletePermission: 'monitor:job:delete',
    hint: '清空调度日志后会进入此处，可恢复后在调度日志中重新查看。',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'jobName', label: '任务名称', minWidth: 160 },
      { prop: 'jobGroup', label: '任务组', width: 110 },
      { prop: 'startTime', label: '开始时间', minWidth: 170 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'jobName', label: '任务名', placeholder: '请输入任务名称' }],
    fetchPage: (p) => getRecycleJobLogPage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreJobLog,
    deletePermanent: deleteJobLogPermanent,
  },
  {
    key: 'file',
    label: '文件',
    icon: Folder,
    accent: 'file',
    permission: 'sys:file:list',
    deletePermission: 'sys:file:delete',
    hint: '删除后文件仍保留在磁盘；恢复前会校验磁盘文件是否存在。超过 30 天未处理的回收文件可由定时任务自动清盘。',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'originalName', label: '文件名', minWidth: 180 },
      { prop: 'fileSuffix', label: '后缀', width: 80 },
      { prop: 'fileSize', label: '大小(B)', width: 100 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'originalName', label: '文件名', placeholder: '请输入文件名' }],
    fetchPage: (p) => getRecycleFilePage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreFile,
    deletePermanent: deleteFilePermanent,
  },
  {
    key: 'gen',
    label: '代码生成',
    icon: DocumentCopy,
    accent: 'gen',
    permission: 'tool:gen:list',
    deletePermission: 'tool:gen:remove',
    hint: '恢复后将还原表配置与字段元数据；若同名表已重新导入，需先删除或彻底清理后再恢复。',
    columns: [
      { prop: 'id', label: 'ID', width: 70 },
      { prop: 'tableName', label: '表名', minWidth: 160 },
      { prop: 'tableComment', label: '表描述', minWidth: 140 },
      { prop: 'className', label: '类名', width: 140 },
      { prop: 'updateTime', label: '删除时间', width: 180 },
    ],
    searchFields: [{ key: 'tableName', label: '表名', placeholder: '请输入表名' }],
    fetchPage: (p) => getRecycleGenPage(p as never) as unknown as Promise<ApiResult<PageResult<Record<string, unknown>>>>,
    restore: restoreGenTable,
    deletePermanent: deleteGenTablePermanent,
  },
]

export function getRecycleType(key: string): RecycleTypeConfig | undefined {
  return RECYCLE_TYPES.find((t) => t.key === key)
}
