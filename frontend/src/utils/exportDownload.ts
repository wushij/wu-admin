import axios from 'axios'
import { ElMessage } from 'element-plus'

const EXPORT_MAX_ROWS = 10_000

/** 从 Content-Disposition 解析文件名 */
function parseFilename(disposition: string | undefined): string | null {
  if (!disposition) return null
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      return utf8Match[1]
    }
  }
  const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] ?? null
}

function triggerBrowserDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

/** 判断 blob 是否为后端 JSON 错误体 */
async function tryParseErrorBlob(blob: Blob): Promise<string | null> {
  if (!blob.type.includes('json') && blob.size > 512) {
    return null
  }
  try {
    const text = await blob.text()
    const json = JSON.parse(text) as { code?: number; msg?: string; message?: string }
    if (json && typeof json === 'object' && json.code !== undefined && json.code !== 200) {
      return json.msg || json.message || '导出失败'
    }
  } catch {
    // 非 JSON，视为正常文件
  }
  return null
}

export type ExportFormat = 'xlsx' | 'csv'
export type ExportScope = 'filtered' | 'page'

export interface ListExportOptions {
  url: string
  params?: Record<string, string | number | boolean | null | undefined>
  format?: ExportFormat
  scope?: ExportScope
  defaultFilename?: string
}

/**
 * 带鉴权下载导出文件（不走 JSON 拦截器）
 */
export async function downloadListExport(options: ListExportOptions): Promise<void> {
  const { url, params = {}, format = 'xlsx', scope = 'filtered', defaultFilename = 'export.xlsx' } = options
  const query: Record<string, string | number> = {
    format,
    scope,
  }
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined || value === '') continue
    query[key] = value as string | number
  }

  const res = await axios.get(`/api${url}`, {
    params: query,
    responseType: 'blob',
    withCredentials: true,
    timeout: 120_000,
  })

  const blob = res.data as Blob
  const errMsg = await tryParseErrorBlob(blob)
  if (errMsg) {
    ElMessage.error(errMsg)
    throw new Error(errMsg)
  }

  const filename = parseFilename(res.headers['content-disposition']) || defaultFilename
  triggerBrowserDownload(blob, filename)
}

export { EXPORT_MAX_ROWS }
