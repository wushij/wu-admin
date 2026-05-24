/** 应用版本号（manifest versionName / 环境变量） */
export function getAppVersionName(): string {
  const fromEnv = import.meta.env.VITE_APP_VERSION
  if (fromEnv) return String(fromEnv)
  try {
    const info = uni.getSystemInfoSync() as UniApp.GetSystemInfoResult & { appVersion?: string }
    if (info.appVersion) return info.appVersion
  } catch {
    /* ignore */
  }
  return '1.0.0'
}
