import type { SysJobLog } from '@/types/system'

/** 调度日志：0 成功 / 1 失败（与后端 SysJobLogDO 一致） */
export function isJobLogSuccess(status?: number | null) {
  return status === 0
}

export function jobLogStatusLabel(status?: number | null) {
  return isJobLogSuccess(status) ? '成功' : '失败'
}

export function jobLogStatusEffect(status?: number | null): 'success' | 'danger' {
  return isJobLogSuccess(status) ? 'success' : 'danger'
}

export function formatDurationMs(ms: number) {
  if (Number.isNaN(ms) || ms < 0) return '—'
  if (ms === 0) return '<1s'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60_000) return `${(ms / 1000).toFixed(ms < 10_000 ? 1 : 0)}s`
  const minutes = Math.floor(ms / 60_000)
  const seconds = Math.round((ms % 60_000) / 1000)
  return seconds > 0 ? `${minutes}分${seconds}秒` : `${minutes}分`
}

export function formatJobDuration(log: SysJobLog) {
  if (log.durationMs != null && log.durationMs >= 0) {
    return formatDurationMs(log.durationMs)
  }
  if (!log.startTime || !log.stopTime) return '—'
  const ms = new Date(log.stopTime).getTime() - new Date(log.startTime).getTime()
  return formatDurationMs(ms)
}
