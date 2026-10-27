import type { MenuTreeNode } from '@/types/api'

export type MenuNode = MenuTreeNode

/** 扁平列表转树（parentId 为 0 或 null 为根） */
export function buildMenuTree<T extends MenuNode>(list: T[]): T[] {
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

/**
 * 侧栏菜单树：父级不在列表中时不提升为根节点（如父目录已禁用被过滤）
 */
export function buildSidebarMenuTree<T extends MenuNode>(list: T[]): T[] {
  if (!list?.length) return []
  const map = new Map<number, T & { children: T[] }>()
  list.forEach((item) => {
    map.set(item.id, { ...item, children: [] })
  })
  const roots: T[] = []
  map.forEach((node) => {
    const pid = node.parentId ?? 0
    if (pid === 0) {
      roots.push(node as T)
    } else if (map.has(pid)) {
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

/** 上级菜单选项：排除按钮(type=3)，根节点为「顶级菜单」 */
export function buildParentMenuOptions(tree: MenuNode[], rootLabel = '顶级菜单') {
  const convert = (menus: MenuNode[]): MenuNode[] =>
    menus
      .filter((m) => m.type !== 3)
      .map((m) => ({
        id: m.id,
        name: m.name,
        children: m.children?.length ? convert(m.children) : undefined
      }))

  return [{ id: 0, name: rootLabel, children: convert(tree) }]
}

/** 统计目录/菜单/按钮数量 */
export function countMenuTypes(tree: MenuNode[]) {
  let dir = 0
  let menu = 0
  let button = 0
  const walk = (nodes: MenuNode[]) => {
    nodes.forEach((n) => {
      if (n.type === 1) dir++
      else if (n.type === 2) menu++
      else if (n.type === 3) button++
      if (n.children?.length) walk(n.children)
    })
  }
  walk(tree)
  return { dir, menu, button, total: dir + menu + button }
}

export function flattenMenuTree(tree: MenuNode[]): MenuNode[] {
  const result: MenuNode[] = []
  const walk = (nodes: MenuNode[]) => {
    nodes.forEach((n) => {
      result.push(n)
      if (n.children?.length) walk(n.children)
    })
  }
  walk(tree)
  return result
}

export function isExternalMenuComponent(component?: string | null) {
  if (!component) return false
  const c = component.trim().toLowerCase()
  return c.startsWith('http://') || c.startsWith('https://')
}

function isMenuDisabled(node: MenuNode) {
  return node.status === 0
}

/** 菜单管理展示：禁用节点不展示子级，禁用祖先下的节点也不展示 */
export function applyMenuAdminDisplayTree<T extends MenuNode>(nodes: T[]): T[] {
  const walk = (list: T[], ancestorDisabled: boolean): T[] => {
    const result: T[] = []
    for (const node of list) {
      if (ancestorDisabled) {
        continue
      }
      const disabled = isMenuDisabled(node)
      const children = node.children?.length
        ? walk(node.children as T[], disabled)
        : undefined
      result.push({
        ...node,
        children: disabled ? undefined : children?.length ? children : undefined,
      } as T)
    }
    return result
  }
  return walk(nodes, false)
}

export function menuNodeHasChildren(node: MenuNode) {
  return Array.isArray(node.children) && node.children.length > 0
}
