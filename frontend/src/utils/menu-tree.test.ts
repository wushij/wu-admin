import { describe, expect, it } from 'vitest'
import {
  buildMenuTree,
  buildParentMenuOptions,
  countMenuTypes,
  flattenMenuTree,
  isExternalMenuComponent,
} from '@/utils/menu-tree'
import type { MenuNode } from '@/utils/menu-tree'

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
