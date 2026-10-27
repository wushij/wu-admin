import type { DeptVO } from '@/types/system'

export interface DeptTreeRow {
  id: number
  name: string
  level: number
  hasChildren: boolean
}

export function buildDeptNodeMaps(tree: DeptVO[]) {
  const idToNode = new Map<number, DeptVO>()
  const idToParent = new Map<number, number>()
  const walk = (nodes: DeptVO[], parentId?: number) => {
    for (const node of nodes) {
      idToNode.set(node.id, node)
      if (parentId != null) idToParent.set(node.id, parentId)
      if (node.children?.length) walk(node.children, node.id)
    }
  }
  walk(tree)
  return { idToNode, idToParent }
}

export function buildDeptPath(id: number, idToNode: Map<number, DeptVO>, idToParent: Map<number, number>) {
  const names: string[] = []
  let current: number | undefined = id
  while (current != null) {
    const node = idToNode.get(current)
    if (!node) break
    names.unshift(node.name)
    current = idToParent.get(current)
  }
  return names.join(' / ')
}

export function flattenDeptRows(tree: DeptVO[], expandedIds: Set<number>): DeptTreeRow[] {
  const rows: DeptTreeRow[] = []
  const walk = (nodes: DeptVO[], level: number) => {
    for (const node of nodes) {
      const hasChildren = !!(node.children?.length)
      rows.push({ id: node.id, name: node.name, level, hasChildren })
      if (hasChildren && expandedIds.has(node.id)) {
        walk(node.children!, level + 1)
      }
    }
  }
  walk(tree, 0)
  return rows
}

export function pruneDeptSubtree(tree: DeptVO[], excludeId?: number): DeptVO[] {
  if (!excludeId) return tree
  const result: DeptVO[] = []
  for (const node of tree) {
    if (node.id === excludeId) continue
    result.push({
      ...node,
      children: node.children?.length ? pruneDeptSubtree(node.children, excludeId) : node.children,
    })
  }
  return result
}

export function findDeptPathLabel(tree: DeptVO[], id: number | null | undefined) {
  if (id == null) return '未分配'
  const maps = buildDeptNodeMaps(tree)
  return buildDeptPath(id, maps.idToNode, maps.idToParent) || '未分配'
}

/** 收集目标节点的所有祖先 id，用于默认展开到选中部门 */
export function collectDeptAncestorIds(id: number, idToParent: Map<number, number>) {
  const ids: number[] = []
  let current = idToParent.get(id)
  while (current != null) {
    ids.push(current)
    current = idToParent.get(current)
  }
  return ids
}

/** 默认展开根节点；若已选中则再展开到目标节点 */
export function buildDeptExpandedIds(
  tree: DeptVO[],
  idToParent: Map<number, number>,
  selectedId: number | null,
) {
  const expanded = new Set<number>()
  for (const node of tree) {
    if (node.children?.length) expanded.add(node.id)
  }
  if (selectedId != null) {
    for (const ancestorId of collectDeptAncestorIds(selectedId, idToParent)) {
      expanded.add(ancestorId)
    }
  }
  return expanded
}

export function buildDeptExpandedIdsForMulti(
  tree: DeptVO[],
  idToParent: Map<number, number>,
  selectedIds: number[],
) {
  const expanded = new Set<number>()
  for (const node of tree) {
    if (node.children?.length) expanded.add(node.id)
  }
  for (const id of selectedIds) {
    for (const ancestorId of collectDeptAncestorIds(id, idToParent)) {
      expanded.add(ancestorId)
    }
  }
  return expanded
}

export function buildDeptLabels(ids: number[], idToNode: Map<number, DeptVO>) {
  return ids
    .map((id) => idToNode.get(id)?.name || '')
    .filter(Boolean)
}
