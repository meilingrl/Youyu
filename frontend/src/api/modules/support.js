import service from '@/api/client'

export function createSupportTicket(payload) {
  return service.post('/support/tickets', payload)
}

export function getSupportTickets(params) {
  return service.get('/support/tickets', { params })
}

export function getSupportTicketDetail(ticketId) {
  return service.get(`/support/tickets/${ticketId}`)
}

export function replySupportTicket(ticketId, payload) {
  return service.post(`/support/tickets/${ticketId}/messages`, payload)
}
