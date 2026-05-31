import { beforeEach, describe, expect, it, vi } from 'vitest'
import service from '@/api/client'
import {
  getCaptcha,
  login,
  register,
  resetPassword,
  sendEmailCode
} from '../auth'

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('auth API adapter', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('maps the registration form account field to the backend username contract', () => {
    register({
      account: 'new-user',
      nickname: 'New User',
      email: 'new@example.com',
      emailCode: '482913',
      password: 'pass123456',
      confirmPassword: 'pass123456'
    })

    expect(service.post).toHaveBeenCalledWith('/auth/register', {
      username: 'new-user',
      nickname: 'New User',
      email: 'new@example.com',
      emailCode: '482913',
      password: 'pass123456'
    })
  })

  it('omits CAPTCHA login fields until a challenge is present', () => {
    login({
      account: 'new-user',
      password: 'pass123456',
      captchaCode: 'AB12'
    })

    expect(service.post).toHaveBeenCalledWith('/auth/login', {
      loginId: 'new-user',
      password: 'pass123456'
    })
  })

  it('submits CAPTCHA fields together after escalation', () => {
    login({
      loginId: 'new-user',
      password: 'pass123456',
      captchaChallengeId: 'opaque-id',
      captchaCode: 'AB12'
    })

    expect(service.post).toHaveBeenCalledWith('/auth/login', {
      loginId: 'new-user',
      password: 'pass123456',
      captchaChallengeId: 'opaque-id',
      captchaCode: 'AB12'
    })
  })

  it('shapes email-code, CAPTCHA, and password-reset requests', () => {
    sendEmailCode({ email: 'new@example.com', purpose: 'reset_password' })
    getCaptcha()
    resetPassword({
      email: 'new@example.com',
      emailCode: '482913',
      newPassword: 'new-pass123456',
      confirmPassword: 'new-pass123456'
    })

    expect(service.post).toHaveBeenNthCalledWith(1, '/auth/email-codes', {
      email: 'new@example.com',
      purpose: 'reset_password'
    })
    expect(service.get).toHaveBeenCalledWith('/auth/captcha')
    expect(service.post).toHaveBeenNthCalledWith(2, '/auth/password-reset', {
      email: 'new@example.com',
      emailCode: '482913',
      newPassword: 'new-pass123456'
    })
  })
})
