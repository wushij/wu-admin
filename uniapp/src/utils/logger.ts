/** 统一日志工具：生产环境自动静默 console 输出 */
const isDev = import.meta.env.DEV

export const logger = {
  /** 调试日志（仅开发环境） */
  debug(...args: unknown[]) {
    if (isDev) console.debug(...args)
  },
  /** 普通信息（仅开发环境） */
  info(...args: unknown[]) {
    if (isDev) console.info(...args)
  },
  /** 警告（仅开发环境） */
  warn(...args: unknown[]) {
    if (isDev) console.warn(...args)
  },
  /** 错误（所有环境，便于排查线上问题） */
  error(...args: unknown[]) {
    console.error(...args)
  },
}
