import { test, expect } from '@playwright/test'

const USER_TOKEN = 'Bearer mock-1001-USER'
const ADMIN_TOKEN = 'Bearer mock-9001-ADMIN'

// ─────────────────────────────────────────────
// Group 1: Offline Order + Refund Flow
// ─────────────────────────────────────────────
test.describe('Offline Order & Refund Flow', () => {
  test('full offline flow: cart → order → pay → double confirm → completed', async ({ request }) => {
    const userHeaders = { Authorization: USER_TOKEN }
    const adminHeaders = { Authorization: ADMIN_TOKEN }

    // Add to cart
    const cartRes = await request.post('/api/cart/items', {
      headers: userHeaders,
      data: { productId: 3003, quantity: 1 }
    })
    expect(cartRes.status()).toBe(200)
    let body = await cartRes.json()
    expect(body.success).toBe(true)
    const cartItemId = body.data.items[0].id

    // Create offline order
    const orderRes = await request.post('/api/orders', {
      headers: userHeaders,
      data: {
        cartItemIds: [cartItemId],
        fulfillmentType: 'offline',
        offlineMeetTime: '2026-07-01 12:00',
        offlineMeetLocation: 'NEU Library Gate'
      }
    })
    expect(orderRes.status()).toBe(200)
    body = await orderRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('pending_payment')
    const orderId = body.data.id

    // Initiate payment
    const payRes = await request.post(`/api/payments/orders/${orderId}/initiate`, { headers: userHeaders })
    expect(payRes.status()).toBe(200)
    body = await payRes.json()
    expect(body.success).toBe(true)
    expect(body.data.payment.paymentStatus).toBe('initiated')
    const paymentNo = body.data.payment.paymentNo

    // Mock payment success
    const mockRes = await request.post(`/api/payments/${paymentNo}/mock-success`, { headers: userHeaders })
    expect(mockRes.status()).toBe(200)
    body = await mockRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('pending_fulfillment')

    // Seller confirm (admin acts as seller)
    const sellerConfirmRes = await request.post(`/api/admin/orders/${orderId}/offline/seller-confirm`, { headers: adminHeaders })
    expect(sellerConfirmRes.status()).toBe(200)
    body = await sellerConfirmRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('pending_receipt')

    // Buyer confirm
    const buyerConfirmRes = await request.post(`/api/orders/${orderId}/offline/buyer-confirm`, { headers: userHeaders })
    expect(buyerConfirmRes.status()).toBe(200)
    body = await buyerConfirmRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('completed')

    // Apply refund
    const refundRes = await request.post(`/api/orders/${orderId}/refunds`, {
      headers: userHeaders,
      data: { refundReason: '商品与描述不符（E2E test）' }
    })
    expect(refundRes.status()).toBe(200)
    body = await refundRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('refunding')

    // Get refund ID from order detail
    const detailRes = await request.get(`/api/orders/${orderId}`, { headers: userHeaders })
    expect(detailRes.status()).toBe(200)
    body = await detailRes.json()
    const refundId = body.data.refunds[0].id

    // Admin complete refund
    const completeRes = await request.post(`/api/admin/orders/${orderId}/refunds/${refundId}/complete`, { headers: adminHeaders })
    expect(completeRes.status()).toBe(200)
    body = await completeRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('refunded')
  })
})
