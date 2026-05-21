import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useMarketStore } from '../market'
import { getProductList } from '@/api/modules/product'

vi.mock('@/api/modules/product', () => ({
  createProduct: vi.fn(),
  getMyProductList: vi.fn(),
  getProductDetail: vi.fn(),
  getProductList: vi.fn()
}))

vi.mock('@/api/modules/favorite', () => ({
  listFavorites: vi.fn(),
  toggleFavorite: vi.fn()
}))

vi.mock('@/api/modules/user', () => ({
  getUserInsightSnapshot: vi.fn(),
  getUserPreference: vi.fn(),
  getUserProfile: vi.fn(),
  updateUserPreference: vi.fn()
}))

vi.mock('@/api/modules/shop', () => ({
  getShopDetail: vi.fn(),
  getShopInsightSnapshot: vi.fn()
}))

function setupStore() {
  setActivePinia(createPinia())
  return useMarketStore()
}

describe('market store', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('loads products and derives categories from loaded data', async () => {
    getProductList.mockResolvedValue({
      success: true,
      data: [
        {
          id: 101,
          title: 'Calculus Notes',
          price: 18,
          categoryId: 1,
          categoryName: 'Learning Materials'
        }
      ]
    })

    const store = setupStore()
    const result = await store.loadProducts({ keyword: 'notes' })

    expect(getProductList).toHaveBeenCalledWith({ keyword: 'notes' })
    expect(result).toHaveLength(1)
    expect(store.products).toHaveLength(1)
    expect(store.products[0]).toMatchObject({
      id: 101,
      title: 'Calculus Notes',
      categoryName: 'Learning Materials'
    })
    expect(store.categories).toEqual([
      { id: 1, name: 'Learning Materials' }
    ])
    expect(store.productError).toBe('')
    expect(store.loadingProducts).toBe(false)
  })

  it('sets productError and stops loading when product loading fails', async () => {
    getProductList.mockRejectedValue({
      response: {
        data: {
          message: 'Product service unavailable'
        }
      }
    })

    const store = setupStore()

    await expect(store.loadProducts()).rejects.toEqual({
      response: {
        data: {
          message: 'Product service unavailable'
        }
      }
    })
    expect(store.products).toEqual([])
    expect(store.productError).toBe('Product service unavailable')
    expect(store.loadingProducts).toBe(false)
  })
})
