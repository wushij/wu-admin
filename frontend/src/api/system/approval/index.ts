import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult, RecyclePageQuery } from '@/types/api'

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

export interface ApprovalPageQuery extends PageQuery {
  title?: string
  formType?: string
  status?: string
}

export interface ApprovalCreateDTO {
  formType: string
  title: string
  approverUserId: number | null
  content: string
}

export interface ApprovalActionDTO {
  id: number
  action: string
  remark?: string
}

export interface ApprovalArchiveDTO {
  id: number
  remark?: string
}

export function getApprovalPage(params: ApprovalPageQuery) {
  return get<PageResult<ApprovalVO>>('/system/approval/page', params)
}

export function getApproval(id: number) {
  return get<ApprovalVO>('/system/approval/get', { id })
}

export function getApprovalRecords(formId: number) {
  return get<ApprovalRecordVO[]>('/system/approval/record/list', { formId })
}

export function createApproval(data: ApprovalCreateDTO) {
  return post('/system/approval/create', data)
}

export function approveApproval(data: ApprovalActionDTO) {
  return put('/system/approval/approve', data)
}

export function archiveApproval(data: ApprovalArchiveDTO) {
  return put('/system/approval/archive', data)
}

export function deleteApproval(id: number) {
  return del('/system/approval/delete', { params: { id } })
}

export function getApprovalRecyclePage(params: RecyclePageQuery) {
  return get<PageResult<ApprovalVO>>('/system/approval/recycle/page', params)
}

export function restoreApproval(id: number) {
  return put('/system/approval/restore', null, { params: { id } })
}

export function deleteApprovalPermanent(id: number) {
  return del('/system/approval/delete-permanent', { params: { id } })
}
