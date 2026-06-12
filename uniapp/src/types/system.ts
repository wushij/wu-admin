export interface DeptVO {
  id: number
  name: string
  parentId?: number
  sort?: number
  status?: number
  leaderName?: string
  leaderUserId?: number | null
  phone?: string
  email?: string
  userCount?: number
  children?: DeptVO[]
  createTime?: string
}

export interface DeptSaveDTO {
  id?: number | null
  parentId?: number | null
  name: string
  leaderName?: string
  leaderUserId?: number | null
  phone?: string
  email?: string
  sort?: number
  status?: number
}

export interface PostVO {
  id: number
  name?: string
  postName?: string
  postCode?: string
  code?: string
  parentId?: number
  sort?: number
  status?: number
  remark?: string
  children?: PostVO[]
}

export interface PostSaveDTO {
  id?: number | null
  parentId?: number | null
  postCode?: string
  postName?: string
  sort?: number
  status?: number
  remark?: string
}

export interface TicketVO {
  id: number
  ticketNo?: string
  title?: string
  description?: string
  priority?: string
  status?: string
  creatorUserId?: number
  assigneeUserId?: number
  creatorName?: string
  assigneeName?: string
  deadline?: string
  closedTime?: string
  createTime?: string
  updateTime?: string
}

export interface TicketCommentVO {
  id: number
  ticketId?: number
  userId?: number
  username?: string
  content?: string
  createTime?: string
}

export interface ApprovalVO {
  id: number
  formNo?: string
  formType?: string
  title?: string
  content?: string
  status?: string
  applicantUserId?: number
  approverUserId?: number
  applicantName?: string
  approverName?: string
  resultRemark?: string
  createTime?: string
  updateTime?: string
}

export interface ApprovalRecordVO {
  id: number
  formId?: number
  operatorUserId?: number
  operatorName?: string
  action?: string
  remark?: string
  createTime?: string
}

export interface TicketSaveDTO {
  id?: number | null
  title: string
  description?: string
  priority?: string
  assigneeUserId?: number | null
  deadline?: string | null
}

export interface AssigneeOptionVO {
  id: number
  username?: string
  nickname?: string
}

export interface TicketPageQuery {
  pageNo?: number
  pageSize?: number
  title?: string
  status?: string
  priority?: string
}

export interface ApprovalCreateDTO {
  formType: string
  title: string
  approverUserId: number | null
  content: string
}

export interface TicketCommentCreateDTO {
  ticketId: number
  content: string
}

export interface ApprovalPageQuery {
  pageNo?: number
  pageSize?: number
  title?: string
  formType?: string
  status?: string
}

export interface RoleVO {
  id: number
  name: string
  code: string
  status?: number
  sort?: number
  remark?: string
  dataScope?: number
  createTime?: string
}

export interface RoleSaveDTO {
  id?: number | null
  name: string
  code: string
  sort?: number
  status?: number
  remark?: string
}

export interface MenuVO {
  id: number
  name: string
  permission?: string
  type?: number
  sort?: number
  parentId?: number
  path?: string
  icon?: string
  component?: string
  status?: number
  isFrame?: number
  children?: MenuVO[]
}

export interface MenuSaveDTO {
  id?: number | null
  parentId?: number
  name: string
  type: number
  path?: string
  component?: string
  permission?: string
  sort?: number
  icon?: string
  status?: number
  isFrame?: number
}

export interface DictTypeSaveDTO {
  id?: number
  dictName: string
  dictType: string
  status?: number
  remark?: string
}

export interface DictTypeVO {
  id: number
  dictName: string
  dictType: string
  status?: number
  remark?: string
  dataCount?: number
  createTime?: string
}

export interface ConfigGroup {
  groupCode: string
  groupName?: string
  configValue?: string
  remark?: string
  updateTime?: string
}

export interface OperLogVO {
  id: number
  title?: string
  businessType?: number
  method?: string
  requestMethod?: string
  operName?: string
  operUrl?: string
  operIp?: string
  operParam?: string
  jsonResult?: string
  status?: number
  errorMsg?: string
  operTime?: string
  costTime?: number
}

export interface OperLogPageQuery {
  pageNo?: number
  pageSize?: number
  title?: string
  operName?: string
  status?: number | null
  beginTime?: string
  endTime?: string
}

export interface LoginLogVO {
  id: number
  username?: string
  ipaddr?: string
  loginLocation?: string
  status?: number
  msg?: string
  loginTime?: string
  browser?: string
  os?: string
}

export interface LoginLogPageQuery {
  pageNo?: number
  pageSize?: number
  username?: string
  ipaddr?: string
  status?: number | null
  beginTime?: string
  endTime?: string
}

export interface OnlineUser {
  userId: number
  loginName?: string
  deptName?: string
  avatar?: string
  username?: string
  nickname?: string
  ipaddr?: string
  loginLocation?: string
  loginTime?: string
  browser?: string
  os?: string
}

export interface SysJob {
  id?: number
  jobName: string
  jobGroup?: string
  invokeTarget: string
  cronExpression: string
  misfirePolicy?: number
  concurrent?: number
  status?: number
  remark?: string
  createTime?: string
  nextFireTime?: string
  cronHint?: string
}

export interface SysJobLog {
  id: number
  jobName?: string
  jobGroup?: string
  jobMessage?: string
  status?: number
  exceptionInfo?: string
  startTime?: string
  stopTime?: string
}

export interface JobOverview {
  totalJobs?: number
  runningJobs?: number
  pausedJobs?: number
  totalCount?: number
  successCount?: number
  failCount?: number
  successRate?: number
}

export interface JobPageQuery {
  pageNo?: number
  pageSize?: number
  jobName?: string
  jobGroup?: string
  status?: number | null
}

export interface ServerInfo {
  cpu?: {
    name?: string
    arch?: string
    availableProcessors?: number
    systemLoadAverage?: number | null
    systemCpuPercent?: number | null
    processCpuPercent?: number | null
  }
  memory?: {
    heapInit?: string
    heapUsed?: string
    heapMax?: string
    heapCommitted?: string
    nonHeapUsed?: string
    heapUsedPercent?: number
    physicalUsedPercent?: number | null
    physicalTotal?: string
    physicalFree?: string
    physicalUsed?: string
  }
  jvm?: {
    name?: string
    vendor?: string
    version?: string
    specVersion?: string
    startTime?: string
    uptime?: string
    uptimeMillis?: number
  }
  sys?: {
    hostName?: string
    hostAddress?: string
    osName?: string
    osVersion?: string
    userDir?: string
    javaVersion?: string
  }
  disks?: Array<{ path?: string; usedPercent?: number; used?: string; total?: string; free?: string }>
}

export interface CacheStats {
  usedMemoryHuman?: string
  hitRate?: number | null
  cumulativeHitRate?: number | null
  connectedClients?: number
  ops?: number
  keyspaceHits?: number
  keyspaceMisses?: number
}

export interface CacheInfo {
  redisVersion?: string
  redisMode?: string
  os?: string
  tcpPort?: number
  uptimeInDays?: number
  connectedClients?: number
  dbSize?: number
  usedMemoryHuman?: string
  usedMemoryPeakHuman?: string
  totalCommandsProcessed?: number
  instantaneousOpsPerSec?: number
}

export interface CacheKeyItem {
  key: string
  type?: string
  ttl?: number
}

export interface CacheKeysResult {
  items: CacheKeyItem[]
  count: number
  limit?: number
  truncated?: boolean
}

export interface CacheValueDetail {
  key: string
  type: string
  ttl: number
  value: unknown
}

export interface ApiAccessLogRow {
  id?: number
  userId?: number
  username?: string
  apiPath?: string
  method?: string
  success?: number
  statusCode?: number
  createTime?: string
  costTime?: number
  ip?: string
}

export interface ApiAccessPageQuery {
  pageNo?: number
  pageSize?: number
  userId?: number | null
  apiPath?: string
  method?: string | null
  success?: number | null
  startTime?: string
  endTime?: string
}

export interface ApiAccessStatistics {
  totalCount?: number
  successCount?: number
  failCount?: number
  dailyStats?: Record<string, ApiAccessDailyStat>
  topPaths?: Array<{ apiPath?: string; count?: number }>
  topUsers?: Array<{ userId: number; username?: string; count?: number }>
  methodCount?: Record<string, number>
}

export interface ApiAccessDailyStat {
  total?: number
  success?: number
  fail?: number
}

export interface FilePageQuery {
  pageNo?: number
  pageSize?: number
  originalName?: string
  fileCategory?: string
}
