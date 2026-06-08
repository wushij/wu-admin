import { del, get, post, put } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface DatabaseTable {
  tableName: string
  tableComment: string
  createTime?: string
  updateTime?: string
}

export interface GenTableColumn {
  id?: number
  tableId?: number
  columnName: string
  columnComment: string
  columnType: string
  javaType: string
  javaField: string
  isPk: number
  isIncrement: number
  isRequired: number
  isInsert: number
  isEdit: number
  isList: number
  isQuery: number
  queryType: string
  htmlType: string
  dictType?: string
  sort: number
}

export interface GenTable {
  id?: number
  tableName: string
  tableComment: string
  className: string
  packageName: string
  moduleName: string
  businessName: string
  functionName: string
  author: string
  genType: string
  genPath?: string
  frontType: string
  formLayout?: string
  parentMenuId?: number
  remark?: string
  createTime?: string
  updateTime?: string
  columns?: GenTableColumn[]
}

export interface GenPageQuery extends PageQuery {
  tableName?: string
}

export function dbTableList(params: { pageNo: number; pageSize: number; tableName?: string }) {
  return get<PageResult<DatabaseTable>>('/tool/gen/db/list', params)
}

export function importGenTables(tableNames: string[]) {
  return post<boolean>('/tool/gen/import', tableNames)
}

export function pageGenTable(params: GenPageQuery) {
  return get<PageResult<GenTable>>('/tool/gen/page', params)
}

export function getGenTable(id: number) {
  return get<GenTable>(`/tool/gen/${id}`)
}

export function updateGenTable(data: GenTable) {
  return put<boolean>('/tool/gen', data)
}

export function deleteGenTable(ids: number[]) {
  return del<boolean>(`/tool/gen/${ids.join(',')}`)
}

export function previewGenCode(id: number) {
  return get<Record<string, string>>(`/tool/gen/preview/${id}`)
}

export function previewGenerateFiles(id: number) {
  return get<string[]>(`/tool/gen/preview-generate/${id}`)
}

export function generateToProject(id: number, overwrite = false) {
  return post<string[]>(`/tool/gen/generate/${id}?overwrite=${overwrite}`)
}

export function previewRemoveFiles(id: number) {
  return get<string[]>(`/tool/gen/preview-remove/${id}`)
}

export function removeGeneratedCode(id: number) {
  return del<string[]>(`/tool/gen/remove-code/${id}`)
}

export function syncGenTable(id: number) {
  return post<boolean>(`/tool/gen/sync/${id}`)
}

export function downloadGenCodeUrl(ids: number[]) {
  return `/api/tool/gen/download?ids=${ids.join(',')}`
}
