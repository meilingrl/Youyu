import service from '@/api/client'

export function getPaymentGateway() {
  return service.get('/payments/gateway')
}

export function initiatePayment(orderId) {
  return service.post(`/payments/orders/${orderId}/initiate`)
}

export function completeMockPayment(paymentNo) {
  return service.post(`/payments/${paymentNo}/mock-success`)
}
