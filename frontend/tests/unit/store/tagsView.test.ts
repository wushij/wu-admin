import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useTagsViewStore } from '@/store/tagsView'
import type { RouteLocationNormalized } from 'vue-router'

function routeOf(path: string, title: string, name?: string): RouteLocationNormalized {
  return {
    path,
    fullPath: path,
    name,
    meta: { title },
    matched: [],
    params: {},
    query: {},
    hash: '',
    redirectedFrom: undefined,
  } as RouteLocationNormalized
}

describe('useTagsViewStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
  })

  it('initTags keeps affix dashboard and restores cached views', () => {
    sessionStorage.setItem('tags-view:visited', JSON.stringify([
      { path: '/system/user', fullPath: '/system/user', title: '用户管理' },
    ]))
    const store = useTagsViewStore()
    store.initTags()

    expect(store.visitedViews.some((v) => v.path === '/dashboard' && v.affix)).toBe(true)
    expect(store.visitedViews.some((v) => v.path === '/system/user')).toBe(true)
  })

  it('addView skips login/register and appends new tag', () => {
    const store = useTagsViewStore()
    store.initTags()

    store.addView(routeOf('/login', '登录'))
    store.addView(routeOf('/system/role', '角色管理', 'Role'))

    expect(store.visitedViews.some((v) => v.path === '/login')).toBe(false)
    expect(store.visitedViews.some((v) => v.path === '/system/role')).toBe(true)
  })

  it('delView removes tag and returns next path when closing active tab', () => {
    const store = useTagsViewStore()
    store.initTags()
    store.addView(routeOf('/system/user', '用户管理'))
    const tag = store.visitedViews.find((v) => v.path === '/system/user')!

    const next = store.delView(tag, '/system/user')

    expect(store.visitedViews.some((v) => v.path === '/system/user')).toBe(false)
    expect(next).toBe('/dashboard')
  })

  it('delView keeps affix dashboard tag', () => {
    const store = useTagsViewStore()
    store.initTags()
    const dashboard = store.visitedViews.find((v) => v.path === '/dashboard')!

    const next = store.delView(dashboard, '/dashboard')

    expect(store.visitedViews.some((v) => v.path === '/dashboard')).toBe(true)
    expect(next).toBe('/dashboard')
  })

  it('resetTags clears session cache', () => {
    const store = useTagsViewStore()
    store.initTags()
    store.addView(routeOf('/system/menu', '菜单管理'))
    store.resetTags()

    expect(sessionStorage.getItem('tags-view:visited')).toBeNull()
    expect(store.visitedViews).toHaveLength(1)
    expect(store.visitedViews[0].path).toBe('/dashboard')
  })
})
