import service from '@/api/client'
import { isValidEntityId } from '@/utils/id-utils'

function rejectInvalidOrderId(orderId) {
  if (!isValidEntityId(orderId)) {
    return Promise.reject(new Error('无效的订单 ID'))
  }
  return null
}

function normalizeOrderId(orderId) {
  return String(orderId).trim()
}

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
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  return service.get(`/orders/${normalizeOrderId(orderId)}`)
}

export function cancelOrder(orderId) {
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  return service.post(`/orders/${normalizeOrderId(orderId)}/cancel`)
}

export function confirmReceipt(orderId) {
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  return service.post(`/orders/${normalizeOrderId(orderId)}/confirm-receipt`)
}

export function buyerConfirmOffline(orderId) {
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  return service.post(`/orders/${normalizeOrderId(orderId)}/offline/buyer-confirm`)
}

export function applyRefund(orderId, payload) {
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  return service.post(`/orders/${normalizeOrderId(orderId)}/refunds`, payload)
}

export function getAdminOrderList() {
  return service.get('/admin/orders')
}

export function getAdminOrderDetail(orderId) {
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  return service.get(`/admin/orders/${normalizeOrderId(orderId)}`)
}

export function shipOrder(orderId, payload) {
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  return service.post(`/admin/orders/${normalizeOrderId(orderId)}/ship`, payload)
}

export function sellerConfirmOffline(orderId) {
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  return service.post(`/admin/orders/${normalizeOrderId(orderId)}/offline/seller-confirm`)
}

export function completeRefund(orderId, refundId) {
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  if (!isValidEntityId(refundId)) {
    return Promise.reject(new Error('无效的退款 ID'))
  }
  return service.post(
    `/admin/orders/${normalizeOrderId(orderId)}/refunds/${String(refundId).trim()}/complete`
  )
}

export function accessDigitalAsset(orderId, assetId) {
  const invalid = rejectInvalidOrderId(orderId)
  if (invalid) return invalid
  if (!isValidEntityId(assetId)) {
    return Promise.reject(new Error('无效的数字资产 ID'))
  }
  return service.get(`/orders/${normalizeOrderId(orderId)}/assets/${String(assetId).trim()}/access`)
}
