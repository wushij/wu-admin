/** 聊天图片上传前压缩，降低上行体积 */
const CHAT_IMAGE_MAX_WIDTH = 1280
const CHAT_IMAGE_QUALITY = 80

function compressImageH5(src: string, maxWidth: number, quality: number): Promise<string> {
  return new Promise((resolve) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      let width = img.naturalWidth || img.width
      let height = img.naturalHeight || img.height
      if (!width || !height) {
        resolve(src)
        return
      }
      if (width > maxWidth) {
        height = Math.round((height * maxWidth) / width)
        width = maxWidth
      }
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        resolve(src)
        return
      }
      ctx.drawImage(img, 0, 0, width, height)
      canvas.toBlob(
        (blob) => {
          if (!blob) {
            resolve(src)
            return
          }
          resolve(URL.createObjectURL(blob))
        },
        'image/jpeg',
        quality / 100,
      )
    }
    img.onerror = () => resolve(src)
    img.src = src
  })
}

/** 将选中的图片压到适合聊天上传的尺寸，失败时回退原路径 */
export async function compressChatImage(filePath: string): Promise<string> {
  if (!filePath?.trim()) return filePath
  // #ifdef H5
  return compressImageH5(filePath, CHAT_IMAGE_MAX_WIDTH, CHAT_IMAGE_QUALITY)
  // #endif
  // #ifndef H5
  try {
    const res = await uni.compressImage({
      src: filePath,
      quality: CHAT_IMAGE_QUALITY,
      compressedWidth: CHAT_IMAGE_MAX_WIDTH,
    })
    return res.tempFilePath || filePath
  } catch {
    return filePath
  }
  // #endif
}
