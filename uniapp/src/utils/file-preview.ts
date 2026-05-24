import {
  getDownloadApiUrl,
  getPreviewApiUrl,
  getStreamPreviewUrl,
  getFileText,
  type FileRecord,
} from '@/api/system/file/index'
import { getToken } from '@/utils/auth'
import { isArchive, isAudio, isImage, isPdf, isText, isVideo } from '@/utils/file-type'

function isH5() {
  try {
    return uni.getSystemInfoSync().uniPlatform === 'web'
  } catch {
    return false
  }
}

function authHeader(): Record<string, string> {
  const token = getToken()
  return token ? { Authorization: token } : {}
}

function openH5Url(url: string) {
  window.open(url, '_blank')
}

async function downloadAndOpen(file: FileRecord) {
  const url = file.id ? getDownloadApiUrl(file.id) : getStreamPreviewUrl(file)
  if (!url) {
    uni.showToast({ title: '文件地址无效', icon: 'none' })
    return
  }
  uni.showLoading({ title: '打开中' })
  try {
    const res = await new Promise<UniApp.DownloadSuccessData>((resolve, reject) => {
      uni.downloadFile({
        url,
        header: authHeader(),
        success: (r) => {
          if (r.statusCode && r.statusCode >= 400) reject(new Error('download failed'))
          else resolve(r)
        },
        fail: reject,
      })
    })
    await new Promise<void>((resolve, reject) => {
      uni.openDocument({
        filePath: res.tempFilePath,
        showMenu: true,
        success: () => resolve(),
        fail: reject,
      })
    })
  } catch {
    uni.showToast({ title: '无法预览该文件', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

export async function previewFile(file: FileRecord) {
  if (!file.id && !file.url) {
    uni.showToast({ title: '文件信息无效', icon: 'none' })
    return
  }

  if (isArchive(file)) {
    uni.showModal({
      title: '压缩包',
      content: '压缩包暂不支持在线预览，请在 PC 端下载后查看。',
      showCancel: false,
    })
    return
  }

  if (isImage(file)) {
    const url = getStreamPreviewUrl(file)
    if (!url) return
    uni.previewImage({ urls: [url], current: url })
    return
  }

  if (isVideo(file) || isAudio(file)) {
    if (!file.id) {
      uni.showToast({ title: '文件信息无效', icon: 'none' })
      return
    }
    const type = isVideo(file) ? 'video' : 'audio'
    const name = encodeURIComponent(file.originalName || file.name || '媒体文件')
    uni.navigateTo({
      url: `/pages-sub/system/file/preview?type=${type}&id=${file.id}&name=${name}`,
    })
    return
  }

  if (isText(file) && file.id) {
    uni.showLoading({ title: '加载中' })
    try {
      const res = await getFileText(file.id)
      uni.hideLoading()
      uni.showModal({
        title: file.originalName || file.name || '文本预览',
        content: (res.data || '').slice(0, 2000) || '空文件',
        showCancel: false,
      })
    } catch {
      uni.hideLoading()
      uni.showToast({ title: '预览失败', icon: 'none' })
    }
    return
  }

  if (isPdf(file) && file.id) {
    const url = getPreviewApiUrl(file.id)
    if (isH5()) {
      openH5Url(url)
      return
    }
    await downloadAndOpen(file)
    return
  }

  if (isH5() && file.id) {
    openH5Url(getPreviewApiUrl(file.id))
    return
  }

  await downloadAndOpen(file)
}

export async function downloadFile(file: FileRecord) {
  if (!file.id) {
    uni.showToast({ title: '文件信息无效', icon: 'none' })
    return
  }
  const url = getDownloadApiUrl(file.id)
  const name = file.originalName || file.name || 'download'

  uni.showLoading({ title: '下载中' })
  try {
    if (isH5()) {
      const res = await fetch(url, { headers: authHeader() })
      if (res.status === 404) throw new Error('file not found')
      if (!res.ok) throw new Error('download failed')
      const blob = await res.blob()
      const blobUrl = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = blobUrl
      link.download = name
      link.click()
      URL.revokeObjectURL(blobUrl)
      uni.showToast({ title: '已开始下载', icon: 'success' })
      return
    }

    const res = await new Promise<UniApp.DownloadSuccessData>((resolve, reject) => {
      uni.downloadFile({
        url,
        header: authHeader(),
        success: (r) => {
          if (r.statusCode && r.statusCode >= 400) reject(new Error('download failed'))
          else resolve(r)
        },
        fail: reject,
      })
    })
    await new Promise<void>((resolve, reject) => {
      uni.openDocument({
        filePath: res.tempFilePath,
        showMenu: true,
        success: () => resolve(),
        fail: reject,
      })
    })
  } catch (err) {
    const msg = String((err as Error)?.message || '')
    uni.showToast({
      title: msg.includes('not found') ? '文件不存在或已删除' : '下载失败',
      icon: 'none',
    })
  } finally {
    uni.hideLoading()
  }
}

export function isUserCancelError(err: unknown) {
  const msg = String((err as { errMsg?: string })?.errMsg || (err as Error)?.message || err || '')
  return /cancel/i.test(msg)
}
