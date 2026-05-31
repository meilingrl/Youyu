import service from '@/api/client'

export function getUserProfile() {
  return service.get('/users/profile')
}

export function updateUserProfile(payload) {
  return service.patch('/users/profile', payload)
}

export function uploadUserAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return service.post('/users/me/avatar', formData)
}

export function bindUserEmail(payload) {
  return service.put('/users/me/email', payload)
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

export function createUserAddress(payload) {
  return service.post('/users/addresses', payload)
}

export function updateUserAddress(addressId, payload) {
  return service.put(`/users/addresses/${addressId}`, payload)
}

export function deleteUserAddress(addressId) {
  return service.delete(`/users/addresses/${addressId}`)
}

export function setDefaultUserAddress(addressId) {
  return service.put(`/users/addresses/${addressId}/default`)
}
