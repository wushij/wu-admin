import { describe, it, expect, beforeEach } from 'vitest'
import {
  LOGIN_REMEMBER_STORAGE_KEY,
  loadLoginRemember,
  saveLoginRemember,
  clearLoginRemember,
} from './loginRemember'

describe('loginRemember', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('saves and loads account credentials', () => {
    saveLoginRemember({
      mode: 'account',
      rememberMe: true,
      username: 'admin',
      password: 'secret',
    })
    const loaded = loadLoginRemember()
    expect(loaded).toEqual({
      mode: 'account',
      rememberMe: true,
      username: 'admin',
      password: 'secret',
    })
  })

  it('clears stored credentials', () => {
    saveLoginRemember({ mode: 'account', rememberMe: true, username: 'a', password: 'b' })
    clearLoginRemember()
    expect(localStorage.getItem(LOGIN_REMEMBER_STORAGE_KEY)).toBeNull()
    expect(loadLoginRemember()).toBeNull()
  })

  it('returns null for invalid payload', () => {
    localStorage.setItem(LOGIN_REMEMBER_STORAGE_KEY, JSON.stringify({ rememberMe: false }))
    expect(loadLoginRemember()).toBeNull()
  })
})
