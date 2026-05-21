import { test, expect } from '@playwright/test'

const ADMIN_TOKEN = 'Bearer mock-9001-ADMIN'
const USER_TOKEN = 'Bearer mock-1001-USER'

// ─────────────────────────────────────────────
// Group 1: Dashboard & User Management
// ─────────────────────────────────────────────
test.describe('Admin Dashboard & User Management', () => {
  test('admin dashboard returns metrics', async ({ request }) => {
    const res = await request.get('/api/admin/dashboard', {
      headers: { Authorization: ADMIN_TOKEN }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data).toHaveProperty('cards')
    expect(body.data).toHaveProperty('todo')
  })

  test('admin dashboard denied for regular user', async ({ request }) => {
    const res = await request.get('/api/admin/dashboard', {
      headers: { Authorization: USER_TOKEN }
    })
    expect(res.status()).toBe(403)
  })

  test('list users returns results', async ({ request }) => {
    const res = await request.get('/api/admin/users', {
      headers: { Authorization: ADMIN_TOKEN }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.length).toBeGreaterThan(0)
    expect(body.data.items[0]).toHaveProperty('username')
  })

  test('list users filter by keyword', async ({ request }) => {
    const res = await request.get('/api/admin/users', {
      headers: { Authorization: ADMIN_TOKEN },
      params: { keyword: 'zhang' }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    const found = body.data.items.some(u =>
      u.username && u.username.toLowerCase().includes('zhang')
    )
    expect(found).toBe(true)
  })

  test('get user detail returns full profile', async ({ request }) => {
    const res = await request.get('/api/admin/users/1001', {
      headers: { Authorization: ADMIN_TOKEN }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.user.username).toBe('zhangsan')
    expect(body.data).toHaveProperty('verifications')
  })

  test('update user status to disabled and back', async ({ request }) => {
    const disableRes = await request.put('/api/admin/users/1002/status', {
      headers: { Authorization: ADMIN_TOKEN },
      data: { status: 'disabled', restrictionReason: 'e2e test' }
    })
    expect(disableRes.status()).toBe(200)
    let body = await disableRes.json()
    expect(body.success).toBe(true)
    expect(body.data.user.status).toBe('disabled')

    const enableRes = await request.put('/api/admin/users/1002/status', {
      headers: { Authorization: ADMIN_TOKEN },
      data: { status: 'active', restrictionReason: '' }
    })
    expect(enableRes.status()).toBe(200)
    body = await enableRes.json()
    expect(body.data.user.status).toBe('active')
  })
})

// ─────────────────────────────────────────────
// Group 2: Verification Review
// ─────────────────────────────────────────────
test.describe('Admin Verification Review', () => {
  test('list verifications returns results', async ({ request }) => {
    const res = await request.get('/api/admin/verifications', {
      headers: { Authorization: ADMIN_TOKEN }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.length).toBeGreaterThan(0)
  })

  test('list verifications filter by pending status', async ({ request }) => {
    const res = await request.get('/api/admin/verifications', {
      headers: { Authorization: ADMIN_TOKEN },
      params: { status: 'pending_review' }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    for (const item of body.data.items) {
      expect(item.verificationStatus).toBe('pending_review')
    }
  })

  test('approve a pending verification', async ({ request }) => {
    // Use verification 2006 (seed user 1012's pending verification)
    const res = await request.put('/api/admin/verifications/2006/review', {
      headers: { Authorization: ADMIN_TOKEN },
      data: { action: 'approve', reviewNote: 'e2e approve' }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.verification.verificationStatus).toBe('approved')
  })
})

// ─────────────────────────────────────────────
// Group 3: Admin Shop & Product Management
// ─────────────────────────────────────────────
test.describe('Admin Shop & Product Management', () => {
  test('list shops returns results', async ({ request }) => {
    const res = await request.get('/api/admin/shops', {
      headers: { Authorization: ADMIN_TOKEN }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.length).toBeGreaterThan(0)
  })

  test('get shop detail', async ({ request }) => {
    const res = await request.get('/api/admin/shops/4001', {
      headers: { Authorization: ADMIN_TOKEN }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.shop.name).toBe('Course Notes Shop')
  })

  test('list admin products', async ({ request }) => {
    const res = await request.get('/api/admin/products', {
      headers: { Authorization: ADMIN_TOKEN }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    expect(body.data.items.length).toBeGreaterThan(0)
  })

  test('filter admin products by status', async ({ request }) => {
    const res = await request.get('/api/admin/products', {
      headers: { Authorization: ADMIN_TOKEN },
      params: { status: 'on_sale' }
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.success).toBe(true)
    for (const item of body.data.items) {
      expect(item.status).toBe('on_sale')
    }
  })
})

// ─────────────────────────────────────────────
// Group 4: Admin Search Governance
// ─────────────────────────────────────────────
test.describe('Admin Search Governance', () => {
  test('CRUD search governance rules', async ({ request }) => {
    const headers = { Authorization: ADMIN_TOKEN }

    // Create
    const createRes = await request.post('/api/admin/search/governance-rules', {
      headers,
      data: { ruleType: 'SENSITIVE_WORD', keyword: 'e2e_spam_test' }
    })
    expect(createRes.status()).toBe(200)
    let body = await createRes.json()
    expect(body.success).toBe(true)
    expect(body.data.ruleType).toBe('SENSITIVE_WORD')
    expect(body.data.isActive).toBe(true)
    const ruleId = body.data.id

    // List
    const listRes = await request.get('/api/admin/search/governance-rules', { headers })
    expect(listRes.status()).toBe(200)
    body = await listRes.json()
    expect(body.success).toBe(true)

    // Update (toggle)
    const updateRes = await request.put(`/api/admin/search/governance-rules/${ruleId}`, {
      headers,
      data: { isActive: false }
    })
    expect(updateRes.status()).toBe(200)
    body = await updateRes.json()
    expect(body.data.isActive).toBe(false)

    // Delete
    const deleteRes = await request.delete(`/api/admin/search/governance-rules/${ruleId}`, { headers })
    expect(deleteRes.status()).toBe(200)
    body = await deleteRes.json()
    expect(body.success).toBe(true)
  })
})

// ─────────────────────────────────────────────
// Group 5: Admin Authorization
// ─────────────────────────────────────────────
test.describe('Admin Authorization', () => {
  test('user token rejected from admin endpoints', async ({ request }) => {
    const endpoints = ['/api/admin/dashboard', '/api/admin/users', '/api/admin/shops']
    for (const path of endpoints) {
      const res = await request.get(path, {
        headers: { Authorization: USER_TOKEN }
      })
      expect(res.status()).toBe(403)
    }
  })

  test('unauthenticated rejected from admin endpoints', async ({ request }) => {
    const endpoints = ['/api/admin/dashboard', '/api/admin/users', '/api/admin/verifications']
    for (const path of endpoints) {
      const res = await request.get(path)
      expect(res.status()).toBe(401)
    }
  })
})
