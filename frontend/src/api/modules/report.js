import service from '@/api/client'

export function submitReport(payload) {
  return service.post('/reports', payload)
}
