/** H5/小程序：确保下拉刷新动画结束 */
export function stopPullDownRefreshSafe() {
  try {
    uni.stopPullDownRefresh()
  } catch {
    /* ignore */
  }
  setTimeout(() => {
    try {
      uni.stopPullDownRefresh()
    } catch {
      /* ignore */
    }
  }, 100)
}
