import type { PostVO } from '@/types/system'

export interface PostTreeRow {
  id: number
  name: string
  level: number
  hasChildren: boolean
  extra?: string
}

export function prunePostSubtree(tree: PostVO[], excludeId?: number): PostVO[] {
  if (!excludeId) return tree
  const result: PostVO[] = []
  for (const node of tree) {
    if (node.id === excludeId) continue
    result.push({
      ...node,
      children: node.children?.length ? prunePostSubtree(node.children, excludeId) : node.children,
    })
  }
  return result
}

export function postDisplayName(node: PostVO) {
  return node.postName || node.name || node.postCode || node.code || '岗位'
}

export function buildPostNodeMaps(tree: PostVO[]) {
  const idToNode = new Map<number, PostVO>()
  const idToParent = new Map<number, number>()
  const walk = (nodes: PostVO[], parentId?: number) => {
    for (const node of nodes) {
      idToNode.set(node.id, node)
      if (parentId != null) idToParent.set(node.id, parentId)
      if (node.children?.length) walk(node.children, node.id)
    }
  }
  walk(tree)
  return { idToNode, idToParent }
}

export function flattenPostRows(tree: PostVO[], expandedIds: Set<number>): PostTreeRow[] {
  const rows: PostTreeRow[] = []
  const walk = (nodes: PostVO[], level: number) => {
    for (const node of nodes) {
      const hasChildren = !!(node.children?.length)
      rows.push({
        id: node.id,
        name: postDisplayName(node),
        level,
        hasChildren,
        extra: node.postCode || node.code,
      })
      if (hasChildren && expandedIds.has(node.id)) {
        walk(node.children!, level + 1)
      }
    }
  }
  walk(tree, 0)
  return rows
}

export function collectPostAncestorIds(id: number, idToParent: Map<number, number>) {
  const ids: number[] = []
  let current = idToParent.get(id)
  while (current != null) {
    ids.push(current)
    current = idToParent.get(current)
  }
  return ids
}

export function buildPostExpandedIds(
  tree: PostVO[],
  idToParent: Map<number, number>,
  selectedIds: number[],
) {
  const expanded = new Set<number>()
  for (const node of tree) {
    if (node.children?.length) expanded.add(node.id)
  }
  for (const id of selectedIds) {
    for (const ancestorId of collectPostAncestorIds(id, idToParent)) {
      expanded.add(ancestorId)
    }
  }
  return expanded
}

export function buildPostLabels(ids: number[], idToNode: Map<number, PostVO>) {
  return ids
    .map((id) => {
      const node = idToNode.get(id)
      return node ? postDisplayName(node) : ''
    })
    .filter(Boolean)
}
