import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../auth'
import {
  getCaptcha as getCaptchaApi,
  login as loginApi,
  register as registerApi,
  resetPassword as resetPasswordApi,
  sendEmailCode as sendEmailCodeApi
} from '@/api/modules/auth'

vi.mock('@/api/modules/auth', () => ({
  getCaptcha: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  resetPassword: vi.fn(),
  sendEmailCode: vi.fn()
}))

const AUTH_STORAGE_KEY = 'youyu-auth'

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
    expect(store.isAdmin).toBe(false)
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
    expect(store.isAdmin).toBe(true)
    expect(store.currentUser).toEqual(session.user)
    expect(store.isLoggedIn).toBe(true)
  })

  it('treats specialist admin roles as admin sessions', async () => {
    const session = {
      token: 'super.token',
      role: 'SUPER_ADMIN',
      user: {
        id: '9101',
        loginId: 'superadmin'
      }
    }
    loginApi.mockResolvedValue({ data: session })

    const store = setupStore()
    await store.login({ loginId: 'superadmin', password: 'admin123' })

    expect(store.currentRole).toBe('super_admin')
    expect(store.isAdmin).toBe(true)
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

  it('requests a CAPTCHA only after the backend reports that it is required', async () => {
    const error = {
      response: {
        data: {
          code: 'CAPTCHA_REQUIRED',
          message: 'Captcha is required'
        }
      }
    }
    const captcha = {
      challengeId: 'captcha-1',
      imageDataUrl: 'data:image/png;base64,abc',
      expiresInSeconds: 300
    }
    loginApi.mockRejectedValue(error)
    getCaptchaApi.mockResolvedValue({ data: captcha })

    const store = setupStore()

    await expect(store.login({ loginId: 'zhangsan', password: 'wrong' })).rejects.toBe(error)
    expect(getCaptchaApi).toHaveBeenCalledTimes(1)
    expect(store.captchaRequired).toBe(true)
    expect(store.captcha).toEqual(captcha)
  })

  it('does not request a CAPTCHA for an ordinary failed login', async () => {
    const error = new Error('Invalid credentials')
    loginApi.mockRejectedValue(error)

    const store = setupStore()

    await expect(store.login({ loginId: 'zhangsan', password: 'wrong' })).rejects.toThrow('Invalid credentials')
    expect(getCaptchaApi).not.toHaveBeenCalled()
    expect(store.captchaRequired).toBe(false)
    expect(store.captcha).toBeNull()
  })

  it('submits the current CAPTCHA challenge and clears CAPTCHA state after login', async () => {
    const session = {
      token: 'test.jwt.token',
      role: 'USER',
      user: { id: '1001' }
    }
    loginApi
      .mockRejectedValueOnce({
        response: {
          data: {
            code: 'CAPTCHA_REQUIRED',
            message: 'Captcha is required'
          }
        }
      })
      .mockResolvedValueOnce({ data: session })
    getCaptchaApi.mockResolvedValue({
      data: {
        challengeId: 'captcha-1',
        imageDataUrl: 'data:image/png;base64,abc'
      }
    })

    const store = setupStore()
    await expect(store.login({ loginId: 'zhangsan', password: 'wrong' })).rejects.toBeTruthy()
    await store.login({ loginId: 'zhangsan', password: 'user123', captchaCode: 'AB12' })

    expect(loginApi).toHaveBeenLastCalledWith({
      loginId: 'zhangsan',
      password: 'user123',
      captchaChallengeId: 'captcha-1',
      captchaCode: 'AB12'
    })
    expect(store.captchaRequired).toBe(false)
    expect(store.captcha).toBeNull()
  })

  it('keeps an existing session unchanged after registration', async () => {
    const session = {
      token: 'existing.token',
      role: 'USER',
      user: { id: '1001' }
    }
    registerApi.mockResolvedValue({
      data: {
        user: {
          id: '1002',
          loginId: 'new-user'
        }
      }
    })

    const store = setupStore()
    store.setSession(session)
    await store.registerAsUser({
      account: 'new-user',
      email: 'new@example.com',
      emailCode: '123456',
      password: 'pass123456',
      nickname: 'New User'
    })

    expect(store.session).toEqual({ ...session, role: 'user' })
    expect(JSON.parse(window.localStorage.getItem(AUTH_STORAGE_KEY))).toEqual({ ...session, role: 'user' })
  })

  it('returns email-code and password-reset responses without creating a session', async () => {
    sendEmailCodeApi.mockResolvedValue({ data: { cooldownSeconds: 60, expiresInSeconds: 600 } })
    resetPasswordApi.mockResolvedValue({ data: { reset: true } })

    const store = setupStore()

    await expect(store.sendEmailCode('new@example.com', 'register')).resolves.toEqual({
      cooldownSeconds: 60,
      expiresInSeconds: 600
    })
    await store.resetPassword({
      email: 'new@example.com',
      emailCode: '123456',
      newPassword: 'new-pass123456'
    })

    expect(sendEmailCodeApi).toHaveBeenCalledWith({ email: 'new@example.com', purpose: 'register' })
    expect(resetPasswordApi).toHaveBeenCalledWith({
      email: 'new@example.com',
      emailCode: '123456',
      newPassword: 'new-pass123456'
    })
    expect(store.session).toBeNull()
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
