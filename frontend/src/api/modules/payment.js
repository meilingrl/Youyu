import service from '@/api/client'

export function getPaymentGateway() {
  return service.get('/payments/gateway')
}

export function initiatePayment(orderId, paymentMethod) {
  return service.post(`/payments/orders/${orderId}/initiate`, null, {
    params: paymentMethod ? { paymentMethod } : undefined
  })
}

export function resumePayment(paymentNo) {
  return service.post(`/payments/${paymentNo}/resume`)
}

export function completeMockPayment(paymentNo) {
  return service.post(`/payments/${paymentNo}/mock-success`)
}
