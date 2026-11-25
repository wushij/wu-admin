export type FileDocKind =
  | 'pdf'
  | 'word'
  | 'excel'
  | 'ppt'
  | 'archive'
  | 'markdown'
  | 'text'
  | 'code'
  | 'video'
  | 'audio'
  | 'image'
  | 'default'

/**
 * 从文件名或扩展名 / MIME 类型提取规范化后缀
 */
export function normalizeFileExt(fileName?: string, fileType?: string): string {
  const cleanName = (fileName || '').split(/[?#]/)[0].trim().toLowerCase()
  const extMatch = cleanName.match(/\.([a-z0-9]{1,8})$/)
  if (extMatch) {
    return extMatch[1]
  }

  const rawType = (fileType || '').split(';')[0].trim().toLowerCase().replace(/^\./, '')
  if (rawType.includes('/')) {
    if (rawType.includes('pdf')) return 'pdf'
    if (rawType.includes('wordprocessingml') || rawType.includes('msword')) return 'docx'
    if (rawType.includes('spreadsheetml') || rawType.includes('excel')) return 'xlsx'
    if (rawType.includes('presentationml') || rawType.includes('powerpoint')) return 'pptx'
    if (rawType.includes('zip') || rawType.includes('compressed') || rawType.includes('rar')) return 'zip'
    if (rawType.includes('markdown')) return 'md'
    if (rawType.includes('plain')) return 'txt'
    if (rawType.startsWith('image/')) return 'png'
    if (rawType.startsWith('video/')) return 'mp4'
    if (rawType.startsWith('audio/')) return 'mp3'
  }
  return rawType
}

/**
 * 解析文档图标语义分类
 */
export function resolveFileDocKind(fileName?: string, fileType?: string): FileDocKind {
  const ext = normalizeFileExt(fileName, fileType)
  if (ext === 'pdf') return 'pdf'
  if (['doc', 'docx', 'rtf', 'wps', 'word'].includes(ext)) return 'word'
  if (['xls', 'xlsx', 'csv', 'et', 'excel'].includes(ext)) return 'excel'
  if (['ppt', 'pptx', 'dps'].includes(ext)) return 'ppt'
  if (['zip', 'rar', '7z', 'tar', 'gz', 'bz2'].includes(ext)) return 'archive'
  if (['md', 'markdown'].includes(ext)) return 'markdown'
  if (['txt', 'log', 'ini', 'conf', 'cfg', 'properties'].includes(ext)) return 'text'
  if (['mp4', 'avi', 'mkv', 'webm', 'mov', 'flv', 'wmv', 'video'].includes(ext)) return 'video'
  if (['mp3', 'wav', 'flac', 'aac', 'ogg', 'm4a', 'audio'].includes(ext)) return 'audio'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp', 'ico', 'avif', 'image'].includes(ext)) return 'image'
  if (
    [
      'java', 'py', 'js', 'ts', 'jsx', 'tsx', 'vue', 'json', 'sql', 'cpp', 'c', 'h',
      'hpp', 'cs', 'go', 'rs', 'php', 'rb', 'swift', 'kt', 'html', 'htm', 'css',
      'scss', 'less', 'xml', 'yaml', 'yml', 'sh', 'ps1', 'bat',
    ].includes(ext)
  ) {
    return 'code'
  }
  return 'default'
}

/**
 * 获取折角文档中心标识文字（PDF / W / X / P / MD / TXT）
 */
export function resolveFileBadgeText(kind: FileDocKind): string {
  switch (kind) {
    case 'pdf':
      return 'PDF'
    case 'word':
      return 'W'
    case 'excel':
      return 'X'
    case 'ppt':
      return 'P'
    case 'markdown':
      return 'MD'
    case 'text':
      return 'TXT'
    default:
      return ''
  }
}
