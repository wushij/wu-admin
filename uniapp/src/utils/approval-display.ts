export interface RegisterApprovalContent {
  userId?: number
  username?: string
  nickname?: string
  mobile?: string
}

export const APPROVAL_ACTION_LABELS: Record<string, string> = {
  SUBMIT: '提交审批',
  APPROVE: '审批通过',
  REJECT: '审批驳回',
  ARCHIVE: '归档单据',
}

export function parseRegisterApprovalContent(content?: string): RegisterApprovalContent {
  if (!content?.trim()) return {}
  try {
    const obj = JSON.parse(content) as RegisterApprovalContent
    return {
      userId: obj.userId,
      username: obj.username,
      nickname: obj.nickname,
      mobile: obj.mobile,
    }
  } catch {
    return {}
  }
}

export function approvalActionLabel(action?: string) {
  if (!action) return '—'
  return APPROVAL_ACTION_LABELS[action] || action
}

export function timelineActionClass(action?: string) {
  if (action === 'APPROVE') return 'timeline--approve'
  if (action === 'REJECT') return 'timeline--reject'
  if (action === 'ARCHIVE') return 'timeline--archive'
  if (action === 'SUBMIT') return 'timeline--submit'
  return ''
}

export function resolveApplicantDisplayName(approval?: {
  formType?: string
  applicantName?: string
  content?: string
}): string {
  const name = approval?.applicantName?.trim()
  if (name && name !== '-') return name
  if (approval?.formType === 'REGISTER') {
    const detail = parseRegisterApprovalContent(approval.content)
    return detail.nickname || detail.username || '—'
  }
  return '—'
}

export function resolveOperatorDisplayName(
  record: { action?: string; operatorName?: string },
  approval?: { formType?: string; content?: string },
): string {
  const name = record.operatorName?.trim()
  if (name && name !== '-') return name
  if (record.action === 'SUBMIT' && approval?.formType === 'REGISTER') {
    const detail = parseRegisterApprovalContent(approval.content)
    return detail.nickname || detail.username || '系统'
  }
  return '系统'
}
