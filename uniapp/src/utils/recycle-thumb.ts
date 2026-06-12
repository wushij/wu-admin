import { fileDisplayUrl, type FileRecord } from '@/api/system/file/index'
import { isImage } from '@/utils/file-type'
import type { RecycleModule } from '@/constants/recycle-modules'

export interface RecycleRowThumb {
  type: 'avatar' | 'image' | 'file'
  src?: string
  name?: string
  file?: FileRecord
}

function toFileRecord(item: Record<string, unknown>): FileRecord | null {
  const url = item.url != null ? String(item.url).trim() : ''
  const id = item.id != null ? Number(item.id) : undefined
  if (!url && !id) return null
  return {
    id: id && !Number.isNaN(id) ? id : undefined,
    url: url || undefined,
    originalName: String(item.originalName || item.name || ''),
    name: item.name != null ? String(item.name) : undefined,
    fileSuffix: item.fileSuffix != null ? String(item.fileSuffix) : undefined,
    fileType: item.fileType != null ? String(item.fileType) : undefined,
    fileSize: typeof item.fileSize === 'number' ? item.fileSize : Number(item.fileSize) || undefined,
  }
}

export function resolveRecycleRowThumb(
  item: Record<string, unknown>,
  mod: RecycleModule | undefined,
): RecycleRowThumb | null {
  if (!mod?.thumb) return null
  const { type, srcField, nameField } = mod.thumb

  if (type === 'file') {
    const file = toFileRecord(item)
    return file ? { type: 'file', file } : null
  }

  const raw = item[srcField!]
  if (raw == null || !String(raw).trim()) return null

  if (type === 'image') {
    const fileLike = {
      url: String(raw),
      fileSuffix: item.fileSuffix as string | undefined,
      fileType: item.fileType as string | undefined,
    }
    if (!isImage(fileLike)) return null
    return { type: 'image', src: fileDisplayUrl(String(raw)) }
  }

  const nameRaw = nameField ? item[nameField] : null
  const fallback = item[mod.titleField]
  return {
    type: 'avatar',
    src: fileDisplayUrl(String(raw)),
    name: String(nameRaw || fallback || ''),
  }
}
