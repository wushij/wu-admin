let maskEl: HTMLElement | null = null
let hideTimer: ReturnType<typeof setTimeout> | null = null

/** H5 浅栈重建时遮住中间页，避免先闪一下父页再进目标页 */
export function showH5RebuildMask() {
  // #ifdef H5
  if (typeof document === 'undefined') return

  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }

  if (!maskEl) {
    maskEl = document.createElement('div')
    maskEl.className = 'wu-shallow-rebuild-mask'
    maskEl.style.cssText =
      'position:fixed;inset:0;background:#f3f5f8;z-index:99999;pointer-events:none;opacity:1;transition:opacity .12s ease;'
    document.body.appendChild(maskEl)
  }

  maskEl.style.display = 'block'
  maskEl.style.opacity = '1'

  hideTimer = setTimeout(() => hideH5RebuildMask(0), 600)
  // #endif
}

export function hideH5RebuildMask(delayMs = 80) {
  // #ifdef H5
  if (typeof document === 'undefined' || !maskEl) return

  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }

  hideTimer = setTimeout(() => {
    maskEl!.style.opacity = '0'
    hideTimer = setTimeout(() => {
      if (maskEl) maskEl.style.display = 'none'
      hideTimer = null
    }, 120)
  }, delayMs)
  // #endif
}
