import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../auth'
import { login as loginApi } from '@/api/modules/auth'

vi.mock('@/api/modules/auth', () => ({
  login: vi.fn(),
  register: vi.fn()
}))

const AUTH_STORAGE_KEY = 'campus-market-auth'

function setupStore() {
  setActivePinia(createPinia())
  return useAuthStore()
}

describe('auth store', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('stores token, normalized role, and user after successful login', async () => {
    const session = {
      token: 'test.jwt.token',
      role: 'USER',
      user: {
        id: '1001',
        loginId: 'zhangsan',
        nickname: 'Zhang San'
      }
    }
    loginApi.mockResolvedValue({ data: session })

    const store = setupStore()
    const result = await store.login({ loginId: 'zhangsan', password: 'user123' })

    const normalized = { ...session, role: 'user' }
    expect(result).toEqual(session)
    expect(loginApi).toHaveBeenCalledWith({ loginId: 'zhangsan', password: 'user123' })
    expect(store.session).toEqual(normalized)
    expect(store.currentRole).toBe('user')
    expect(store.currentUser).toEqual(session.user)
    expect(store.isLoggedIn).toBe(true)
    expect(JSON.parse(window.localStorage.getItem(AUTH_STORAGE_KEY))).toEqual(normalized)
  })

  it('restores an existing session from local storage with a normalized role', () => {
    const session = {
      token: 'restored.token',
      role: 'ADMIN',
      user: {
        id: '9001',
        loginId: 'admin'
      }
    }
    window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))

    const store = setupStore()

    expect(store.session).toEqual({ ...session, role: 'admin' })
    expect(store.currentRole).toBe('admin')
    expect(store.currentUser).toEqual(session.user)
    expect(store.isLoggedIn).toBe(true)
  })

  it('does not create a valid session when login fails', async () => {
    const error = new Error('Invalid credentials')
    loginApi.mockRejectedValue(error)

    const store = setupStore()

    await expect(store.login({ loginId: 'zhangsan', password: 'wrong' })).rejects.toThrow('Invalid credentials')
    expect(store.session).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(window.localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
  })

  it('clears session on logout', () => {
    const session = {
      token: 'test.jwt.token',
      role: 'USER',
      user: { id: '1001' }
    }
    window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))

    const store = setupStore()
    store.logout()

    expect(store.session).toBeNull()
    expect(store.currentRole).toBe('guest')
    expect(store.currentUser).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(window.localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
  })
})
