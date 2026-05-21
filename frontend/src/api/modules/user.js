import service from '@/api/client'

export function getUserProfile() {
  return service.get('/users/profile')
}

export function submitVerificationApplication(payload) {
  return service.post('/users/verification', {
    ...payload,
    verificationMethod: payload.verificationMethod || payload.verifyMethod
  })
}

export function getUserPreference() {
  return service.get('/users/me/preference')
}

export function updateUserPreference(payload) {
  return service.put('/users/me/preference', payload)
}

export function getUserInsightSnapshot() {
  return service.get('/users/me/insight-snapshot')
}

export function getUserAddresses() {
  return service.get('/users/addresses')
}
