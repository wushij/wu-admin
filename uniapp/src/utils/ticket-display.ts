import type { TicketVO } from '@/types/system'

/** 当前用户是否需要处理该工单（待处理 + 指定本人或全员通知） */
export function isTicketPendingForUser(
  item: Pick<TicketVO, 'status' | 'assigneeUserId'>,
  userId?: number | null,
): boolean {
  if (item.status !== 'OPEN' || userId == null) return false
  const assignee = item.assigneeUserId
  if (assignee === 0) return true
  return assignee === userId
}
