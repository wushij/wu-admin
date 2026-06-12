export const H5_FAVICON = '/static/favicon.svg?v=1'

export function ensureH5Favicon() {
  if (import.meta.env.UNI_PLATFORM !== 'h5' || typeof document === 'undefined') return

  let link = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
  if (!link) {
    link = document.createElement('link')
    link.rel = 'icon'
    link.type = 'image/svg+xml'
    document.head.appendChild(link)
  }
  link.href = H5_FAVICON

  let appleIcon = document.querySelector<HTMLLinkElement>('link[rel="apple-touch-icon"]')
  if (!appleIcon) {
    appleIcon = document.createElement('link')
    appleIcon.rel = 'apple-touch-icon'
    document.head.appendChild(appleIcon)
  }
  appleIcon.href = H5_FAVICON
}

export function setH5DocumentTitle(pageTitle: string, appName = 'Admin Platform') {
  if (import.meta.env.UNI_PLATFORM !== 'h5' || typeof document === 'undefined') return

  const suffix = appName.trim() || 'Admin Platform'
  document.title = pageTitle ? `${pageTitle} - ${suffix}` : suffix
  ensureH5Favicon()
}
