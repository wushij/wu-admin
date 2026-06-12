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

export function formatJobDuration(log: SysJobLog) {
  if (!log.startTime || !log.stopTime) return '—'
  const ms = new Date(log.stopTime).getTime() - new Date(log.startTime).getTime()
  if (Number.isNaN(ms) || ms < 0) return '—'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}
