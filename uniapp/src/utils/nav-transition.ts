/** H5 程序化跳转时抑制 popstate 误触发（redirectTo / switchTab 等） */
let suppressPopstateUntil = 0

export function suppressPopstate(ms = 450) {
  suppressPopstateUntil = Date.now() + ms
}

export function shouldSuppressPopstate(): boolean {
  return Date.now() < suppressPopstateUntil
}
