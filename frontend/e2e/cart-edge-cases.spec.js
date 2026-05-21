import { test, expect } from '@playwright/test'

const USER_TOKEN = 'Bearer mock-1002-USER'
const OTHER_TOKEN = 'Bearer mock-1001-USER'

// ─────────────────────────────────────────────
// Group 1: Cart Edge Cases
// ─────────────────────────────────────────────
test.describe('Cart Edge Cases', () => {
  test('duplicate add merges quantity', async ({ request }) => {
    const headers = { Authorization: USER_TOKEN }

    // Add product twice
    await request.post('/api/cart/items', {
      headers,
      data: { productId: 3001, quantity: 1 }
    })
    const addRes = await request.post('/api/cart/items', {
      headers,
      data: { productId: 3001, quantity: 1 }
    })
    expect(addRes.status()).toBe(200)
    const body = await addRes.json()
    expect(body.success).toBe(true)

    // Verify cart has only one item (merged quantity)
    const cartRes = await request.get('/api/cart', { headers })
    expect(cartRes.status()).toBe(200)
    const cartBody = await cartRes.json()
    const matchingItems = cartBody.data.items.filter(i => i.productId === 3001)
    expect(matchingItems.length).toBe(1)
  })

  test('update cart item quantity', async ({ request }) => {
    const headers = { Authorization: USER_TOKEN }

    const cartRes = await request.post('/api/cart/items', {
      headers,
      data: { productId: 3001, quantity: 1 }
    })
    const body = await cartRes.json()
    const cartItemId = body.data.items[0].id

    const updateRes = await request.patch(`/api/cart/items/${cartItemId}`, {
      headers,
      data: { quantity: 5 }
    })
    expect(updateRes.status()).toBe(200)
    const updateBody = await updateRes.json()
    expect(updateBody.success).toBe(true)
  })

  test('remove cart item', async ({ request }) => {
    const headers = { Authorization: USER_TOKEN }

    const cartRes = await request.post('/api/cart/items', {
      headers,
      data: { productId: 3001, quantity: 1 }
    })
    const body = await cartRes.json()
    const cartItemId = body.data.items[0].id

    const deleteRes = await request.delete(`/api/cart/items/${cartItemId}`, { headers })
    expect(deleteRes.status()).toBe(200)
    const deleteBody = await deleteRes.json()
    expect(deleteBody.success).toBe(true)
    expect(deleteBody.data.removed).toBe(true)
  })

  test('unauthorized cart access returns 401', async ({ request }) => {
    const paths = ['/api/cart', '/api/cart/items']
    for (const path of paths) {
      const res = await (path === '/api/cart'
        ? request.get(path)
        : request.post(path, { data: { productId: 3001, quantity: 1 } }))
      expect(res.status()).toBe(401)
    }
  })
})
