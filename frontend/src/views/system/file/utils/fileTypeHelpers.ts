import {
  Document,
  DocumentCopy,
  Picture,
  VideoCamera,
  Headset,
} from '@element-plus/icons-vue'
import type { FileRecord } from '@/api/system/file'

export function formatSize(bytes: number | undefined) {
  if (!bytes) return '0 B'
  const k = 1024
  const s = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${s[i]}`
}

export function isImage(file: FileRecord | null | undefined) {
  return file?.fileType?.startsWith('image/') || false
}

export function isVideo(file: FileRecord | null | undefined) {
  return file?.fileType?.startsWith('video/') || false
}

export function isAudio(file: FileRecord | null | undefined) {
  return file?.fileType?.startsWith('audio/') || false
}

export function isPdf(file: FileRecord | null | undefined) {
  return file?.fileType === 'application/pdf' || file?.fileSuffix?.toLowerCase() === '.pdf'
}

export function isOffice(file: FileRecord | null | undefined) {
  const s = file?.fileSuffix?.toLowerCase() || ''
  return ['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'].includes(s)
}

export function isText(file: FileRecord | null | undefined) {
  if (!file) return false
  const textTypes = ['text/', 'application/json', 'application/xml', 'application/javascript']
  const s = file.fileSuffix?.toLowerCase() || ''
  const textSuffixes = [
    '.txt', '.md', '.json', '.xml', '.yaml', '.yml', '.ini', '.conf', '.cfg', '.properties',
    '.js', '.ts', '.vue', '.jsx', '.tsx', '.css', '.scss', '.less', '.html', '.htm', '.java',
    '.py', '.go', '.rs', '.c', '.cpp', '.h', '.hpp', '.cs', '.php', '.rb', '.swift', '.kt',
    '.sql', '.sh', '.bat', '.ps1', '.log', '.csv',
  ]
  return textTypes.some((t) => file.fileType?.startsWith(t)) || textSuffixes.includes(s)
}

export function isPreviewable(file: FileRecord | null | undefined) {
  return isImage(file) || isVideo(file) || isAudio(file) || isPdf(file) || isText(file) || isOffice(file)
}

export function useStreamPreview(file: FileRecord): boolean {
  return isVideo(file) || isAudio(file) || isImage(file) || isPdf(file)
}

export function getFileIcon(file: FileRecord | null | undefined) {
  const s = file?.fileSuffix?.toLowerCase() || ''
  if (['.doc', '.docx', '.xls', '.xlsx', '.pdf', '.txt', '.md'].includes(s)) return DocumentCopy
  if (file?.fileType?.startsWith('image/')) return Picture
  if (file?.fileType?.startsWith('video/')) return VideoCamera
  if (file?.fileType?.startsWith('audio/')) return Headset
  return Document
}

export function getFileIconColor(file: FileRecord | null | undefined) {
  const s = file?.fileSuffix?.toLowerCase() || ''
  if (['.doc', '.docx'].includes(s)) return '#2b579a'
  if (['.xls', '.xlsx'].includes(s)) return '#217346'
  if (s === '.pdf') return '#f40f02'
  return '#9ca3af'
}
