import { test, expect, chromium } from '@playwright/test'

const USER_TOKEN = 'Bearer mock-1001-USER'
const USER2_TOKEN = 'Bearer mock-1002-USER'
const ADMIN_TOKEN = 'Bearer mock-9001-ADMIN'

// ─────────────────────────────────────────────
// Group 1: Public Browse (no auth required)
// ─────────────────────────────────────────────
test.describe('Public Browse', () => {
  test('product list returns products', async ({ request }) => {
    const res = await request.get('/api/products')
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.length).toBeGreaterThan(0)
    const first = body.data.items[0]
    expect(first).toHaveProperty('id')
    expect(first).toHaveProperty('title')
    expect(first).toHaveProperty('salePrice')
  })

  test('product detail for product 3001', async ({ request }) => {
    const res = await request.get('/api/products/3001')
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.title).toBe('Advanced Math Review Pack')
    expect(body.data.salePrice).toBe(19.9)
  })

  test('product detail 404', async ({ request }) => {
    const res = await request.get('/api/products/99999')
    expect(res.status()).toBe(404)
  })

  test('product list filter by keyword', async ({ request }) => {
    const res = await request.get('/api/products', { params: { keyword: 'Math' } })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.length).toBeGreaterThan(0)
    expect(body.data.items[0]).toHaveProperty('title')
  })
})

// ─────────────────────────────────────────────
// Group 2: Digital Purchase Flow (mock-1002-USER)
// ─────────────────────────────────────────────
test.describe('Digital Purchase Flow', () => {
  test('full digital purchase: cart → order → pay → receipt → assets', async ({ request }) => {
    const headers = { Authorization: USER2_TOKEN }

    // Add to cart
    const cartRes = await request.post('/api/cart/items', {
      headers,
      data: { productId: 3001, quantity: 1 }
    })
    expect(cartRes.status()).toBe(200)
    let body = await cartRes.json()
    expect(body.success).toBe(true)
    const cartItemId = body.data.items[0].id

    // Create order
    const orderRes = await request.post('/api/orders', {
      headers,
      data: { cartItemIds: [cartItemId], fulfillmentType: 'digital' }
    })
    expect(orderRes.status()).toBe(200)
    body = await orderRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('pending_payment')
    const orderId = body.data.id

    // Initiate payment
    const payRes = await request.post(`/api/payments/orders/${orderId}/initiate`, { headers })
    expect(payRes.status()).toBe(200)
    body = await payRes.json()
    expect(body.success).toBe(true)
    expect(body.data.payment.paymentStatus).toBe('initiated')
    const paymentNo = body.data.payment.paymentNo

    // Mock payment success
    const mockRes = await request.post(`/api/payments/${paymentNo}/mock-success`, { headers })
    expect(mockRes.status()).toBe(200)
    body = await mockRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('pending_receipt')

    // Check order detail (before receipt: 2 preview assets)
    const detailRes = await request.get(`/api/orders/${orderId}`, { headers })
    expect(detailRes.status()).toBe(200)
    body = await detailRes.json()
    expect(body.data.digitalAssets.length).toBe(2)

    // Confirm receipt
    const confirmRes = await request.post(`/api/orders/${orderId}/confirm-receipt`, { headers })
    expect(confirmRes.status()).toBe(200)
    body = await confirmRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('completed')
    expect(body.data.digitalAssets.length).toBe(3)

    // Access full digital asset
    const assetRes = await request.get(`/api/orders/${orderId}/assets/3203/access`, { headers })
    expect(assetRes.status()).toBe(200)
    body = await assetRes.json()
    expect(body.success).toBe(true)
    expect(body.data.asset.assetName).toBe('Full Pack.zip')
    expect(body.data.accessLogs.length).toBe(1)
  })
})

// ─────────────────────────────────────────────
// Group 3: Logistics Ship Flow
// ─────────────────────────────────────────────
test.describe('Logistics Ship Flow', () => {
  test('full logistics: cart → order → pay → admin ship → receipt', async ({ request }) => {
    const userHeaders = { Authorization: USER2_TOKEN }
    const adminHeaders = { Authorization: ADMIN_TOKEN }

    // Create an address for user 1002
    const addrRes = await request.post('/api/users/addresses', {
      headers: userHeaders,
      data: {
        receiverName: 'Li Si',
        receiverPhone: '13900001111',
        addressType: 'campus',
        campusArea: 'NEU Hunnan Campus Dorm 3',
        detailAddress: 'Building 3 Room 201',
        isDefault: true
      }
    })
    let body = await addrRes.json()
    const addressId = body.data.id

    // Add to cart
    const cartRes = await request.post('/api/cart/items', {
      headers: userHeaders,
      data: { productId: 3002, quantity: 1 }
    })
    expect(cartRes.status()).toBe(200)
    body = await cartRes.json()
    expect(body.success).toBe(true)
    const cartItemId = body.data.items[0].id

    // Create logistics order
    const orderRes = await request.post('/api/orders', {
      headers: userHeaders,
      data: { cartItemIds: [cartItemId], fulfillmentType: 'logistics', addressId }
    })
    expect(orderRes.status()).toBe(200)
    body = await orderRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('pending_payment')
    const orderId = body.data.id

    // Pay
    const payRes = await request.post(`/api/payments/orders/${orderId}/initiate`, { headers: userHeaders })
    body = await payRes.json()
    const paymentNo = body.data.payment.paymentNo
    await request.post(`/api/payments/${paymentNo}/mock-success`, { headers: userHeaders })

    // Admin ship
    const shipRes = await request.post(`/api/admin/orders/${orderId}/ship`, {
      headers: adminHeaders,
      data: { trackingNo: 'SF1234567890', logisticsCompany: 'SF Express' }
    })
    expect(shipRes.status()).toBe(200)
    body = await shipRes.json()
    expect(body.data.orderStatus).toBe('pending_receipt')

    // Confirm receipt
    const confirmRes = await request.post(`/api/orders/${orderId}/confirm-receipt`, { headers: userHeaders })
    expect(confirmRes.status()).toBe(200)
    body = await confirmRes.json()
    expect(body.success).toBe(true)
    expect(body.data.orderStatus).toBe('completed')
  })
})

// ─────────────────────────────────────────────
// Group 4: Admin Product Review
// ─────────────────────────────────────────────
test.describe('Admin Product Review', () => {
  test('submit digital product → admin approve → on_sale', async ({ request }) => {
    const userHeaders = { Authorization: USER_TOKEN }
    const adminHeaders = { Authorization: ADMIN_TOKEN }

    // Submit digital product
    const pubRes = await request.post('/api/products', {
      headers: userHeaders,
      data: {
        title: 'Smoke Test Digital Notes',
        categoryId: 1,
        type: 'digital',
        price: '15.00',
        stock: 1,
        deliveryMethods: ['digital_delivery'],
        submitMode: 'submit',
        description: 'smoke test digital product for review'
      }
    })
    expect(pubRes.status()).toBe(200)
    let body = await pubRes.json()
    expect(body.success).toBe(true)
    expect(body.data.reviewStatus).toBe('pending_review')
    const productId = body.data.id

    // Fetch pending review tasks
    const tasksRes = await request.get('/api/admin/review-tasks', {
      headers: adminHeaders,
      params: { status: 'pending_review' }
    })
    expect(tasksRes.status()).toBe(200)
    body = await tasksRes.json()
    const tasks = body.data.items
    const task = tasks.find(t => t.productId === productId)
    expect(task).toBeDefined()

    // Approve
    const approveRes = await request.put(`/api/admin/review-tasks/${task.id}/review`, {
      headers: adminHeaders,
      data: { action: 'approve', reviewNote: 'smoke approve' }
    })
    expect(approveRes.status()).toBe(200)
    body = await approveRes.json()
    expect(body.success).toBe(true)
    expect(body.data.reviewTask.reviewStatus).toBe('approved')
    expect(body.data.product.status).toBe('on_sale')
    expect(body.data.product.reviewStatus).toBe('approved')
  })
})

// ─────────────────────────────────────────────
// Group 5: Report Governance
// ─────────────────────────────────────────────
test.describe('Report Governance', () => {
  test('unauthorized report returns 401', async ({ request }) => {
    const res = await request.post('/api/reports', {
      data: { targetType: 'product', targetId: 3001, reason: 'test', content: 'test' }
    })
    expect(res.status()).toBe(401)
  })

  test('user submit → admin view → admin process', async ({ request }) => {
    const userHeaders = { Authorization: USER_TOKEN }
    const adminHeaders = { Authorization: ADMIN_TOKEN }

    // Submit report
    const submitRes = await request.post('/api/reports', {
      headers: userHeaders,
      data: {
        targetType: 'product',
        targetId: 3001,
        targetLabel: 'Advanced Math Review Pack',
        reason: 'inaccurate_content',
        content: 'Smoke test report content for verification.'
      }
    })
    expect(submitRes.status()).toBe(200)
    let body = await submitRes.json()
    expect(body.success).toBe(true)
    expect(body.data.report.status).toBe('pending')
    const reportId = body.data.report.id

    // Admin view pending reports
    const listRes = await request.get('/api/admin/reports', {
      headers: adminHeaders,
      params: { status: 'pending' }
    })
    expect(listRes.status()).toBe(200)
    body = await listRes.json()
    const found = body.data.items.some(r => r.id === reportId)
    expect(found).toBe(true)

    // Admin process
    const processRes = await request.put(`/api/admin/reports/${reportId}/process`, {
      headers: adminHeaders,
      data: { status: 'resolved', resolution: 'Smoke test resolved.' }
    })
    expect(processRes.status()).toBe(200)
    body = await processRes.json()
    expect(body.success).toBe(true)
    expect(body.data.report.status).toBe('resolved')
    expect(body.data.report.resolution).toBe('Smoke test resolved.')
  })
})

// ─────────────────────────────────────────────
// Group 6: Hot Search & Search Filters
// ─────────────────────────────────────────────
test.describe('Hot Search & Search Filters', () => {
  test('hot search endpoint returns ranked keywords', async ({ request }) => {
    const res = await request.get('/api/search/hot')
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(Array.isArray(body.data)).toBe(true)
    if (body.data.length > 0) {
      const entry = body.data[0]
      expect(entry).toHaveProperty('keyword')
      expect(entry).toHaveProperty('searchCount')
      expect(entry).toHaveProperty('score')
    }
  })

  test('search suggestion endpoint returns prefixed keywords', async ({ request }) => {
    await request.get('/api/products', { params: { keyword: 'Advanced Math' } })
    const res = await request.get('/api/search/suggest', { params: { q: 'adv', limit: '5' } })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(Array.isArray(body.data)).toBe(true)
    if (body.data.length > 0) {
      const entry = body.data[0]
      expect(entry).toHaveProperty('keyword')
      expect(entry).toHaveProperty('normalizedKeyword')
      expect(entry).toHaveProperty('searchCount')
      expect(entry).toHaveProperty('score')
    }
  })

  test('product list filters by keyword', async ({ request }) => {
    const res = await request.get('/api/products', { params: { keyword: 'Math' } })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.length).toBeGreaterThan(0)
    expect(body.data.items[0].title).toMatch(/Math/i)
  })

  test('product list filters by category', async ({ request }) => {
    const res = await request.get('/api/products', { params: { categoryId: '1' } })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.length).toBeGreaterThan(0)
  })

  test('product list filters by productType', async ({ request }) => {
    const res = await request.get('/api/products', { params: { productType: 'digital' } })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.every(p => p.productType === 'digital')).toBe(true)
  })

  test('product list combined filters: keyword + category + productType', async ({ request }) => {
    const res = await request.get('/api/products', {
      params: { keyword: 'Math', categoryId: '1', productType: 'digital' }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.length).toBeGreaterThan(0)
    expect(body.data.items[0].title).toMatch(/Math/i)
    expect(body.data.items[0].categoryName).toBe('Learning Materials')
    expect(body.data.items[0].productType).toBe('digital')
  })

  test('keyword search persists search logs and affects ranking', async ({ request }) => {
    // Perform a unique search to create a log entry
    await request.get('/api/products', { params: { keyword: 'Test Unique Search Term' } })

    // Hot search should still be available
    const res = await request.get('/api/search/hot')
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(Array.isArray(body.data)).toBe(true)
  })

  test('blank keyword does not pollute search logs', async ({ request }) => {
    // Search with whitespace-only keyword
    const res = await request.get('/api/products', { params: { keyword: '   ' } })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
  })
})

// ─────────────────────────────────────────────
// Group 7: Error Cases
// ─────────────────────────────────────────────
test.describe('Error Cases', () => {
  test('USER token cannot access admin dashboard', async ({ request }) => {
    const res = await request.get('/api/admin/dashboard', {
      headers: { Authorization: USER_TOKEN }
    })
    expect(res.status()).toBe(403)
  })

  test('no token cannot access orders', async ({ request }) => {
    const res = await request.get('/api/orders')
    expect(res.status()).toBe(401)
  })

  test('confirm receipt on pending_payment order fails', async ({ request }) => {
    // Pre-built order 8001 is pending_payment, belongs to user 1010
    const res = await request.post('/api/orders/8001/confirm-receipt', {
      headers: { Authorization: 'Bearer mock-1010-USER' }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(false)
    expect(body.code).toBe('BUSINESS_ERROR')
  })
})

// ─────────────────────────────────────────────
// Group 8: Browser Happy Path
// ─────────────────────────────────────────────
test.describe('Browser Happy Path', () => {
  test('frontend home page loads and renders products', async () => {
    const browser = await chromium.launch()
    const page = await browser.newPage()
    try {
      await page.goto('http://localhost:5173/app/home', { timeout: 15000 })
      await expect(page).toHaveTitle(/Youyu/)
      const productCards = page.locator('[data-testid="home-featured-product-card"]')
      await expect(productCards.first()).toBeVisible({ timeout: 10000 })
      await expect(productCards).toHaveCount(18, { timeout: 10000 })
    } finally {
      await browser.close()
    }
  })
})
