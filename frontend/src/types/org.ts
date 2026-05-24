import type { DeptVO } from '@/api/system/dept'
import type { PostVO } from '@/api/system/post'

/** Element Plus 树拖拽节点上下文 */
export interface OrgTreeDragNode<T> {
  data: T
}

export function unwrapOrgTreeNode<T>(node: unknown): T {
  return (node as OrgTreeDragNode<T>).data
}

export function isPostVO(data: DeptVO | PostVO): data is PostVO {
  return 'postCode' in data || 'postName' in data
}

export function orgNodeLabel(data: DeptVO | PostVO, tab: 'dept' | 'post'): string {
  if (tab === 'dept') {
    return (data as DeptVO).name
  }
  const post = data as PostVO
  return String(post.postName ?? post.name ?? '')
}
