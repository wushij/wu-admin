import type { FileRecord } from '@/api/system/file/index'

export function isImage(file: FileRecord | null | undefined) {
  if (file?.fileType?.startsWith('image/')) return true
  const ext = fileSuffix(file)
  return ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.svg'].includes(ext)
}

export function isVideo(file: FileRecord | null | undefined) {
  if (file?.fileType?.startsWith('video/')) return true
  const ext = fileSuffix(file)
  return ['.mp4', '.mov', '.avi', '.mkv', '.webm', '.m4v'].includes(ext)
}

export function isAudio(file: FileRecord | null | undefined) {
  if (file?.fileType?.startsWith('audio/')) return true
  const ext = fileSuffix(file)
  return ['.mp3', '.wav', '.aac', '.m4a', '.ogg'].includes(ext)
}

export function isPdf(file: FileRecord | null | undefined) {
  return file?.fileType === 'application/pdf' || fileSuffix(file) === '.pdf'
}

export function isText(file: FileRecord | null | undefined) {
  if (!file) return false
  const ext = fileSuffix(file)
  const textSuffixes = [
    '.txt', '.md', '.json', '.xml', '.yaml', '.yml', '.ini', '.log', '.csv', '.js', '.ts', '.vue', '.html', '.css',
  ]
  return file.fileType?.startsWith('text/') || textSuffixes.includes(ext)
}

export function isArchive(file: FileRecord | null | undefined) {
  const ext = fileSuffix(file)
  return ['.zip', '.rar', '.7z', '.tar', '.gz'].includes(ext)
}

export function isPreviewable(file: FileRecord | null | undefined) {
  return isImage(file) || isVideo(file) || isAudio(file) || isPdf(file) || isText(file)
}

export function fileSuffix(file: FileRecord | null | undefined) {
  if (file?.fileSuffix) {
    const s = file.fileSuffix.toLowerCase()
    return s.startsWith('.') ? s : `.${s}`
  }
  const name = file?.originalName || file?.name || ''
  const idx = name.lastIndexOf('.')
  return idx >= 0 ? name.slice(idx).toLowerCase() : ''
}

export function fileExtLabel(file: FileRecord) {
  const ext = fileSuffix(file).replace('.', '').toUpperCase()
  return ext || 'FILE'
}

export function fileIconTheme(file: FileRecord): 'image' | 'video' | 'audio' | 'pdf' | 'archive' | 'doc' | 'default' {
  if (isImage(file)) return 'image'
  if (isVideo(file)) return 'video'
  if (isAudio(file)) return 'audio'
  if (isPdf(file)) return 'pdf'
  if (isArchive(file)) return 'archive'
  const ext = fileSuffix(file)
  if (['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'].includes(ext)) return 'doc'
  return 'default'
}
