import type { DeptVO, PostVO } from '@/types/system'

export interface FlatTreeNode {
  id: number
  label: string
  level: number
  status?: number
  extra?: string
  parentId?: number
  entity: 'dept' | 'post'
}

function flattenDept(nodes: DeptVO[], level = 0): FlatTreeNode[] {
  const rows: FlatTreeNode[] = []
  for (const node of nodes) {
    rows.push({
      id: node.id,
      label: node.name,
      level,
      status: node.status,
      extra: node.leaderName ? `负责人：${node.leaderName}` : undefined,
      parentId: node.parentId,
      entity: 'dept',
    })
    if (node.children?.length) {
      rows.push(...flattenDept(node.children, level + 1))
    }
  }
  return rows
}

function flattenPost(nodes: PostVO[], level = 0): FlatTreeNode[] {
  const rows: FlatTreeNode[] = []
  for (const node of nodes) {
    const label = node.postName || node.name || node.postCode || '岗位'
    rows.push({
      id: node.id,
      label,
      level,
      status: node.status,
      extra: node.postCode || node.code,
      parentId: node.parentId,
      entity: 'post',
    })
    if (node.children?.length) {
      rows.push(...flattenPost(node.children, level + 1))
    }
  }
  return rows
}

export function flattenDeptTree(nodes: DeptVO[]): FlatTreeNode[] {
  return flattenDept(nodes || [])
}

export function flattenPostTree(nodes: PostVO[]): FlatTreeNode[] {
  return flattenPost(nodes || [])
}
