export interface MenuNode {
  id: number
  name: string
  type?: number
  parentId?: number
  children?: MenuNode[]
  [key: string]: unknown
}

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
