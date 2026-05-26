import service from '@/api/client'

export function getNotificationList(params = {}) {
  return service.get('/notifications', { params })
}

export function getUnreadCount() {
  return service.get('/notifications/unread-count')
}

export function markNotificationRead(id) {
  return service.post(`/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return service.post('/notifications/read-all')
}
