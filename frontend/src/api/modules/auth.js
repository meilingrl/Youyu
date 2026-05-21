import service from '@/api/client'

export function login(payload) {
  return service.post('/auth/login', {
    loginId: payload.loginId || payload.account || '',
    password: payload.password || ''
  })
}

export function register(payload) {
  return service.post('/auth/register', payload)
}

export function getCurrentUser() {
  return service.get('/auth/me')
}
