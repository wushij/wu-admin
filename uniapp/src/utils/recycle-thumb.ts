import { fileDisplayUrl } from '@/api/system/file/index'
import { isImage } from '@/utils/file-type'
import type { RecycleModule } from '@/constants/recycle-modules'

export interface RecycleRowThumb {
  type: 'avatar' | 'image'
  src: string
  name?: string
}

export function resolveRecycleRowThumb(
  item: Record<string, unknown>,
  mod: RecycleModule | undefined,
): RecycleRowThumb | null {
  if (!mod?.thumb) return null
  const { type, srcField, nameField } = mod.thumb
  const raw = item[srcField]
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
