import type { MenuVO } from '@/types/system'

/** 扁平列表转树（parentId 为 0 或 null 为根） */
export function buildMenuTree<T extends MenuVO>(list: T[]): T[] {
  if (!list?.length) return []
  const map = new Map<number, T & { children: T[] }>()
  list.forEach((item) => {
    map.set(item.id, { ...item, children: [] })
  })
  const roots: T[] = []
  map.forEach((node) => {
    const pid = node.parentId ?? 0
    if (pid === 0 || !map.has(pid)) {
      roots.push(node as T)
    } else {
      map.get(pid)!.children.push(node as T)
    }
  })
  const prune = (nodes: Array<T & { children?: T[] }>) => {
    nodes.forEach((n) => {
      if (n.children?.length) {
        prune(n.children as Array<T & { children?: T[] }>)
      } else {
        delete n.children
      }
    })
  }
  prune(roots as Array<T & { children?: T[] }>)
  return roots
}

export function collectMenuNodeIds(node: MenuVO): number[] {
  const ids = [node.id]
  node.children?.forEach((child) => {
    ids.push(...collectMenuNodeIds(child))
  })
  return ids
}

export function walkMenuTree(nodes: MenuVO[], visitor: (node: MenuVO, level: number) => void, level = 0) {
  for (const node of nodes) {
    visitor(node, level)
    if (node.children?.length) {
      walkMenuTree(node.children, visitor, level + 1)
    }
  }
}
