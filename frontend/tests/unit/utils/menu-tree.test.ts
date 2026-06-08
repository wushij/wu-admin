import { describe, expect, it } from 'vitest'
import {
  applyMenuAdminDisplayTree,
  buildMenuTree,
  buildParentMenuOptions,
  buildSidebarMenuTree,
  countMenuTypes,
  flattenMenuTree,
  isExternalMenuComponent,
  type MenuNode,
} from '@/utils/menu-tree'

const flatMenus: MenuNode[] = [
  { id: 1, parentId: 0, name: '系统', type: 1, path: '/system' },
  { id: 2, parentId: 1, name: '用户管理', type: 2, path: 'user', component: 'system/user/index' },
  { id: 3, parentId: 2, name: '新增', type: 3, path: '', permission: 'system:user:create' },
  { id: 4, parentId: 0, name: '外链', type: 2, path: 'doc', component: 'https://example.com' },
]

describe('buildMenuTree', () => {
  it('builds nested tree and prunes empty children', () => {
    const tree = buildMenuTree(flatMenus)
    expect(tree).toHaveLength(2)
    expect(tree[0].children).toHaveLength(1)
    expect(tree[0].children![0].children).toHaveLength(1)
    expect(tree[0].children![0].children![0].children).toBeUndefined()
  })

  it('returns empty array for empty input', () => {
    expect(buildMenuTree([])).toEqual([])
  })

  it('promotes orphan when parent is missing (admin tree)', () => {
    const orphanOnly = [{ id: 2, parentId: 1, name: '孤儿', type: 2, path: 'orphan' }]
    expect(buildMenuTree(orphanOnly)).toHaveLength(1)
    expect(buildMenuTree(orphanOnly)[0].name).toBe('孤儿')
  })
})

describe('buildSidebarMenuTree', () => {
  it('does not promote child when parent is absent from list', () => {
    const orphanOnly: MenuNode[] = [
      { id: 2, parentId: 1, name: '子菜单', type: 2, path: 'child' },
    ]
    expect(buildSidebarMenuTree(orphanOnly)).toEqual([])
  })

  it('builds tree when parent exists in list', () => {
    const menus: MenuNode[] = [
      { id: 1, parentId: 0, name: '系统', type: 1, path: '/system' },
      { id: 2, parentId: 1, name: '用户', type: 2, path: 'user' },
    ]
    const tree = buildSidebarMenuTree(menus)
    expect(tree).toHaveLength(1)
    expect(tree[0].children).toHaveLength(1)
    expect(tree[0].children![0].name).toBe('用户')
  })
})

describe('applyMenuAdminDisplayTree', () => {
  it('hides children under disabled node', () => {
    const tree: MenuNode[] = [
      {
        id: 1,
        parentId: 0,
        name: '系统',
        type: 1,
        path: '/sys',
        status: 0,
        children: [{ id: 2, parentId: 1, name: '用户', type: 2, path: 'user', status: 1 }],
      },
    ]
    const result = applyMenuAdminDisplayTree(tree)
    expect(result).toHaveLength(1)
    expect(result[0].children).toBeUndefined()
  })

  it('hides descendants when ancestor is disabled', () => {
    const tree: MenuNode[] = [
      {
        id: 1,
        parentId: 0,
        name: '系统',
        type: 1,
        path: '/sys',
        status: 1,
        children: [
          {
            id: 2,
            parentId: 1,
            name: '已禁用',
            type: 2,
            path: 'off',
            status: 0,
            children: [{ id: 3, parentId: 2, name: '孙级', type: 2, path: 'grand', status: 1 }],
          },
        ],
      },
    ]
    const result = applyMenuAdminDisplayTree(tree)
    expect(result[0].children).toHaveLength(1)
    expect(result[0].children![0].children).toBeUndefined()
  })
})

describe('buildParentMenuOptions', () => {
  it('excludes button nodes and adds root option', () => {
    const tree = buildMenuTree(flatMenus)
    const options = buildParentMenuOptions(tree, '根菜单')
    expect(options[0].name).toBe('根菜单')
    expect(options[0].children?.some((n) => n.name === '新增')).toBe(false)
  })
})

describe('countMenuTypes', () => {
  it('counts dir, menu and button nodes', () => {
    const tree = buildMenuTree(flatMenus)
    expect(countMenuTypes(tree)).toEqual({ dir: 1, menu: 2, button: 1, total: 4 })
  })
})

describe('flattenMenuTree', () => {
  it('returns depth-first flat list', () => {
    const tree = buildMenuTree(flatMenus)
    const flat = flattenMenuTree(tree)
    expect(flat.map((n) => n.id)).toEqual([1, 2, 3, 4])
  })
})

describe('isExternalMenuComponent', () => {
  it('detects http(s) component paths', () => {
    expect(isExternalMenuComponent('https://a.com')).toBe(true)
    expect(isExternalMenuComponent('system/user/index')).toBe(false)
    expect(isExternalMenuComponent(null)).toBe(false)
  })
})
