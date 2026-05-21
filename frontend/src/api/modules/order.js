import service from '@/api/client'

export function getCart() {
  return service.get('/cart')
}

export function addCartItem(payload) {
  return service.post('/cart/items', payload)
}

export function updateCartItem(cartItemId, payload) {
  return service.patch(`/cart/items/${cartItemId}`, payload)
}

export function removeCartItem(cartItemId) {
  return service.delete(`/cart/items/${cartItemId}`)
}

export function previewOrder(payload) {
  return service.post('/orders/preview', payload)
}

export function createOrder(payload) {
  return service.post('/orders', payload)
}

export function getOrderList() {
  return service.get('/orders')
}

export function getOrderDetail(orderId) {
  return service.get(`/orders/${orderId}`)
}

export function cancelOrder(orderId) {
  return service.post(`/orders/${orderId}/cancel`)
}

export function confirmReceipt(orderId) {
  return service.post(`/orders/${orderId}/confirm-receipt`)
}

export function buyerConfirmOffline(orderId) {
  return service.post(`/orders/${orderId}/offline/buyer-confirm`)
}

export function applyRefund(orderId, payload) {
  return service.post(`/orders/${orderId}/refunds`, payload)
}

export function getAdminOrderList() {
  return service.get('/admin/orders')
}

export function getAdminOrderDetail(orderId) {
  return service.get(`/admin/orders/${orderId}`)
}

export function shipOrder(orderId, payload) {
  return service.post(`/admin/orders/${orderId}/ship`, payload)
}

export function sellerConfirmOffline(orderId) {
  return service.post(`/admin/orders/${orderId}/offline/seller-confirm`)
}

export function completeRefund(orderId, refundId) {
  return service.post(`/admin/orders/${orderId}/refunds/${refundId}/complete`)
}

export function accessDigitalAsset(orderId, assetId) {
  return service.get(`/orders/${orderId}/assets/${assetId}/access`)
}
