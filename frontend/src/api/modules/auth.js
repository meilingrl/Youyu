import service from '@/api/client'

export function login(payload) {
  const body = {
    loginId: payload.loginId || payload.account || '',
    password: payload.password || ''
  }

  if (payload.captchaChallengeId && payload.captchaCode) {
    body.captchaChallengeId = payload.captchaChallengeId
    body.captchaCode = payload.captchaCode
  }

  return service.post('/auth/login', body)
}

export function register(payload) {
  const body = {
    username: payload.username || payload.account || '',
    password: payload.password || '',
    nickname: payload.nickname || '',
    email: payload.email || '',
    emailCode: payload.emailCode || ''
  }

  if (payload.phone) {
    body.phone = payload.phone
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'agreedToTerms')) {
    body.agreedToTerms = payload.agreedToTerms === true
  }

  return service.post('/auth/register', body)
}

export function sendEmailCode(payload) {
  return service.post('/auth/email-codes', {
    email: payload.email || '',
    purpose: payload.purpose || ''
  })
}

export function getCaptcha() {
  return service.get('/auth/captcha')
}

export function resetPassword(payload) {
  return service.post('/auth/password-reset', {
    email: payload.email || '',
    emailCode: payload.emailCode || '',
    newPassword: payload.newPassword || ''
  })
}

export function getCurrentUser() {
  return service.get('/auth/me')
}
