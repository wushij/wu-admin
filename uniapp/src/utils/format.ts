import dayjs from 'dayjs'

/** 列表时间：今天显示 HH:mm，否则 MM-DD */
export function formatListTime(time?: string | number) {
  if (!time) return ''
  const d = dayjs(time)
  if (!d.isValid()) return String(time)
  if (d.isSame(dayjs(), 'day')) return d.format('HH:mm')
  if (d.isSame(dayjs(), 'year')) return d.format('MM-DD')
  return d.format('YYYY-MM-DD')
}

export function formatDate(time?: string | number | Date) {
  if (!time) return ''
  const d = dayjs(time)
  return d.isValid() ? d.format('YYYY-MM-DD') : String(time)
}

export function formatDateTime(time?: string | number, withSeconds = false) {
  if (!time) return ''
  const d = dayjs(time)
  return d.isValid() ? d.format(withSeconds ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD HH:mm') : String(time)
}

/** 摘要：去标签、截断 */
export function summarizeText(text?: string, max = 48) {
  if (!text) return ''
  const plain = text.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
  return plain.length > max ? `${plain.slice(0, max)}…` : plain
}

export function formatBytes(bytes?: number) {
  if (bytes == null || Number.isNaN(bytes)) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1073741824) return `${(bytes / 1048576).toFixed(1)} MB`
  return `${(bytes / 1073741824).toFixed(1)} GB`
}

export function formatPercent(value?: number | null, digits = 1) {
  if (value == null || Number.isNaN(value)) return '—'
  return `${Number(value).toFixed(digits)}%`
}

const USER_STATUS_LABELS: Record<number, string> = {
  0: '已停用',
  1: '正常',
  2: '待审核',
  3: '审核驳回',
}

/** 用户账号状态文案，与 PC 个人中心一致 */
export function formatUserStatus(status?: number | null) {
  if (status == null) return '—'
  return USER_STATUS_LABELS[status] ?? '未知'
}
