import type { RiskSubjectVO } from '@/api/system/security'

/** 格式化事件类型名称 */
export function formatEventTypeName(type: string): string {
  switch (type) {
    case 'UA_BLOCK':
      return '爬虫工具拦截'
    case 'ANTI_REPLAY':
      return '重放攻击拦截'
    case 'SIGN_FAIL':
      return '数字签名篡改'
    case 'TIMESTAMP_INVALID':
      return '时间戳超窗'
    case 'BODY_TOO_LARGE':
      return '超大请求体'
    case 'RISK_BLOCK':
      return '风控累计封禁'
    case 'HONEYPOT':
      return '蜜罐诱捕触发'
    case 'LOGIN_FAIL':
      return '登录失败'
    case 'CAPTCHA_FAIL':
      return '滑块验证失败'
    case 'CAPTCHA_ABUSE':
      return '滑块连续失败'
    case 'PERMISSION_DENIED':
      return '越权探测'
    case 'PAGE_SCAN':
      return '翻页遍历'
    case 'BULK_EXPORT':
      return '批量导出'
    case 'RISK_CHALLENGE':
      return '风控挑战'
    default:
      return type || '未知事件'
  }
}

/** 事件类型标签样式 */
export function getEventTypeTagType(
  type: string,
): 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined {
  switch (type) {
    case 'UA_BLOCK':
    case 'CAPTCHA_FAIL':
    case 'LOGIN_FAIL':
      return 'warning'
    case 'ANTI_REPLAY':
    case 'SIGN_FAIL':
    case 'RISK_BLOCK':
    case 'HONEYPOT':
    case 'CAPTCHA_ABUSE':
    case 'PERMISSION_DENIED':
    case 'PAGE_SCAN':
    case 'BULK_EXPORT':
      return 'danger'
    case 'TIMESTAMP_INVALID':
    case 'BODY_TOO_LARGE':
    case 'RISK_CHALLENGE':
      return 'info'
    default:
      return undefined
  }
}

/** 处置动作文案 */
export function formatAction(action: string): string {
  switch (action) {
    case 'FORBIDDEN':
      return '触发封禁'
    case 'CHALLENGE':
      return '要求验证'
    case 'WARNING':
      return '安全告警'
    case 'BLOCKED':
      return '已阻断(403)'
    default:
      return action || '未知'
  }
}

/** 处置动作标签样式 */
export function getActionTagType(action: string): 'danger' | 'warning' | 'info' | 'success' {
  switch (action) {
    case 'FORBIDDEN':
      return 'danger'
    case 'CHALLENGE':
      return 'warning'
    case 'WARNING':
      return 'warning'
    default:
      return 'info'
  }
}

/** 格式化剩余秒数 */
export function formatRemainingSeconds(sec: number): string {
  if (!sec || sec <= 0) return '已到期'
  if (sec < 60) return `${sec} 秒`
  const minutes = Math.floor(sec / 60)
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const remMinutes = minutes % 60
  if (hours < 24) return `${hours} 小时 ${remMinutes} 分`
  const days = Math.floor(hours / 24)
  const remHours = hours % 24
  return `${days} 天 ${remHours} 小时`
}

/** 格式化风险主体友好展示名称 */
export function formatPrincipalDisplayName(subject?: RiskSubjectVO | null): string {
  if (!subject) return ''
  if (subject.principalType === 'user') {
    if (subject.username || subject.nickname) {
      const name = subject.nickname
        ? `${subject.nickname} (${subject.username || ''})`
        : subject.username
      return `${name} · ID: ${subject.principalId}`
    }
  }
  return subject.principal || ''
}

