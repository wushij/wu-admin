import { describe, expect, it } from 'vitest'
import { hasMenuPerm, hasMonitorPerm } from '@/utils/hasMenuPerm'

const menus = [
  {
    permission: 'system:user:list',
    children: [
      { permission: 'system:user:create' },
    ],
  },
  {
    permission: 'system:role:list',
    children: [],
  },
]

describe('hasMenuPerm', () => {
  it('returns false for empty menus', () => {
    expect(hasMenuPerm(undefined, 'system:user:list')).toBe(false)
    expect(hasMenuPerm([], 'system:user:list')).toBe(false)
  })

  it('matches permission on root node', () => {
    expect(hasMenuPerm(menus, 'system:role:list')).toBe(true)
  })

  it('matches permission on nested child', () => {
    expect(hasMenuPerm(menus, 'system:user:create')).toBe(true)
  })

  it('returns false when permission not found', () => {
    expect(hasMenuPerm(menus, 'system:dept:list')).toBe(false)
  })
})

describe('hasMonitorPerm', () => {
  it('returns true when permission array contains perm', () => {
    expect(hasMonitorPerm(['monitor:cache:list'], [], 'monitor:cache:list')).toBe(true)
  })

  it('falls back to menu tree when not in permission array', () => {
    expect(hasMonitorPerm([], menus, 'system:user:list')).toBe(true)
  })

  it('returns false when neither source has perm', () => {
    expect(hasMonitorPerm([], menus, 'monitor:server:list')).toBe(false)
  })
})
