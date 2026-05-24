/** 读取设备底部安全区高度（px）。H5 由 CSS env() 处理，此处返回 0。 */
export function getSafeAreaBottom(): number {
  // #ifdef H5
  return 0
  // #endif

  try {
    const info = uni.getSystemInfoSync()
    const insetBottom = Number(info.safeAreaInsets?.bottom)
    if (!Number.isNaN(insetBottom)) {
      return insetBottom > 0 ? insetBottom : 0
    }
    const screenHeight = Number(info.screenHeight)
    const areaBottom = Number(info.safeArea?.bottom)
    if (!Number.isNaN(screenHeight) && !Number.isNaN(areaBottom)) {
      const gap = screenHeight - areaBottom
      return gap > 0 ? gap : 0
    }
  } catch {
    // ignore
  }
  return 0
}
