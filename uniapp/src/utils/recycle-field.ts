import { formatDateTime } from '@/utils/format'
import type { RecycleDetailField } from '@/constants/recycle-modules'

export function formatRecycleFieldValue(
  item: Record<string, unknown>,
  field: RecycleDetailField,
): string {
  const raw = item[field.prop]
  if (raw == null || raw === '') return '—'
  if (field.format === 'datetime') return formatDateTime(String(raw), true)
  if (field.format === 'bytes') {
    const num = Number(raw)
    return Number.isFinite(num) ? num.toLocaleString() : String(raw)
  }
  return String(raw)
}

export function isRecycleTagField(field: RecycleDetailField) {
  return !!(field.tag && field.dictType)
}
