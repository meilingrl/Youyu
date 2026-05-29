import service from '@/api/client'

export function getAdminDashboard() {
  return service.get('/admin/dashboard')
}

export function getAdminUsers(params) {
  return service.get('/admin/users', { params })
}

export function getAdminUserDetail(id) {
  return service.get(`/admin/users/${id}`)
}

export function updateAdminUserStatus(id, payload) {
  return service.put(`/admin/users/${id}/status`, payload)
}

export function batchUpdateAdminUserStatus(payload) {
  return service.put('/admin/users/batch-status', payload)
}

export function getAdminVerifications(params) {
  return service.get('/admin/verifications', { params })
}

export function reviewAdminVerification(id, payload) {
  return service.put(`/admin/verifications/${id}/review`, payload)
}

export function batchReviewAdminVerifications(payload) {
  return service.put('/admin/verifications/batch-review', payload)
}

export function getAdminProducts(params) {
  return service.get('/admin/products', { params })
}

export function updateAdminProductStatus(id, payload) {
  return service.put(`/admin/products/${id}/status`, payload)
}

export function batchUpdateAdminProductStatus(payload) {
  return service.put('/admin/products/batch-status', payload)
}

export function getAdminReviewTasks(params) {
  return service.get('/admin/review-tasks', { params })
}

export function getAdminReviewTaskDetail(id) {
  return service.get(`/admin/review-tasks/${id}`)
}

export function reviewAdminTask(id, payload) {
  return service.put(`/admin/review-tasks/${id}/review`, payload)
}

export function batchReviewAdminTasks(payload) {
  return service.put('/admin/review-tasks/batch-review', payload)
}

export function getAdminShops(params) {
  return service.get('/admin/shops', { params })
}

export function getAdminShopDetail(id) {
  return service.get(`/admin/shops/${id}`)
}

export function updateAdminShopStatus(id, payload) {
  return service.put(`/admin/shops/${id}/status`, payload)
}

export function batchUpdateAdminShopStatus(payload) {
  return service.put('/admin/shops/batch-status', payload)
}

export function getAdminReports(params) {
  return service.get('/admin/reports', { params })
}

export function processAdminReport(id, payload) {
  return service.put(`/admin/reports/${id}/process`, payload)
}

export function batchProcessAdminReports(payload) {
  return service.put('/admin/reports/batch-process', payload)
}

export function escalateAdminReportToMediation(id, payload) {
  return service.post(`/admin/reports/${id}/escalate-to-mediation`, payload)
}

export function getAdminMediationCases(params) {
  return service.get('/admin/mediation-cases', { params })
}

export function getAdminMediationCaseDetail(id) {
  return service.get(`/admin/mediation-cases/${id}`)
}

export function updateAdminMediationStatus(id, payload) {
  return service.put(`/admin/mediation-cases/${id}/status`, payload)
}

export function recordAdminMediationDecision(id, payload) {
  return service.post(`/admin/mediation-cases/${id}/decision`, payload)
}

export function getAdminSearchGovernanceRules() {
  return service.get('/admin/search/governance-rules')
}

export function createAdminSearchGovernanceRule(payload) {
  return service.post('/admin/search/governance-rules', payload)
}

export function updateAdminSearchGovernanceRule(id, payload) {
  return service.put(`/admin/search/governance-rules/${id}`, payload)
}

export function deleteAdminSearchGovernanceRule(id) {
  return service.delete(`/admin/search/governance-rules/${id}`)
}

export function getAdminSearchLogs(params) {
  return service.get('/admin/search/logs', { params })
}

export function getAdminSupportTickets(params) {
  return service.get('/admin/support/tickets', { params })
}

export function getAdminSupportTicketDetail(ticketId) {
  return service.get(`/admin/support/tickets/${ticketId}`)
}

export function updateAdminSupportTicketStatus(ticketId, payload) {
  return service.put(`/admin/support/tickets/${ticketId}/status`, payload)
}

export function createAdminSupportTicketMessage(ticketId, payload) {
  return service.post(`/admin/support/tickets/${ticketId}/messages`, payload)
}
