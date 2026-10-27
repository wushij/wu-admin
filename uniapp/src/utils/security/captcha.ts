/** 将后端验证码 base64 转为可在 image 组件中显示的 data URL */
export function toCaptchaDataUrl(img?: string): string {
  if (!img) return ''
  if (img.startsWith('data:')) return img
  return `data:image/png;base64,${img}`
}
