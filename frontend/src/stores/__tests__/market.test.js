import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useMarketStore } from '../market'
import { getProductList } from '@/api/modules/product'
import { addFavorite, listFavorites, removeFavorite } from '@/api/modules/favorite'

vi.mock('@/api/modules/product', () => ({
  createProduct: vi.fn(),
  getMyProductList: vi.fn(),
  getProductDetail: vi.fn(),
  getProductList: vi.fn()
}))

vi.mock('@/api/modules/favorite', () => ({
  addFavorite: vi.fn(),
  listFavorites: vi.fn(),
  removeFavorite: vi.fn()
}))

vi.mock('@/api/modules/user', () => ({
  bindUserEmail: vi.fn(),
  createUserAddress: vi.fn(),
  getUserAddresses: vi.fn(),
  getUserInsightSnapshot: vi.fn(),
  getUserPreference: vi.fn(),
  getUserProfile: vi.fn(),
  setDefaultUserAddress: vi.fn(),
  updateUserProfile: vi.fn(),
  uploadUserAvatar: vi.fn(),
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

  it('loads products without replacing the stable category options from page results', async () => {
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
      { id: 1, name: '学习资料' },
      { id: 2, name: '学习工具' },
      { id: 3, name: '宿舍生活' },
      { id: 4, name: '数字配件' }
    ])
    expect(store.productError).toBe('')
    expect(store.loadingProducts).toBe(false)
  })

  it('passes product list sort params through the store boundary', async () => {
    getProductList.mockResolvedValue({
      success: true,
      data: {
        items: [],
        total: 0,
        page: 1,
        pageSize: 12,
        sort: 'price_asc'
      }
    })

    const store = setupStore()
    await store.loadProducts({ keyword: 'notes', sort: 'price_asc' })

    expect(getProductList).toHaveBeenCalledWith({ keyword: 'notes', sort: 'price_asc' })
    expect(store.searchTotal).toBe(0)
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

  it('loads favorite products from the frozen favorites contract', async () => {
    listFavorites.mockResolvedValue({
      success: true,
      data: [
        {
          id: 301,
          title: 'Mock Favorite Product',
          salePrice: 25,
          categoryId: 9,
          categoryName: 'Favorites'
        }
      ]
    })

    const store = setupStore()
    const result = await store.loadFavorites()

    expect(listFavorites).toHaveBeenCalledTimes(1)
    expect(result).toHaveLength(1)
    expect(store.favoriteIds).toEqual(['301'])
    expect(store.favoriteProducts[0]).toMatchObject({
      id: 301,
      title: 'Mock Favorite Product'
    })
  })

  it('uses POST and DELETE favorites endpoints for remote favorite changes', async () => {
    addFavorite.mockResolvedValue({
      success: true,
      data: {
        productId: 401,
        favorite: true
      }
    })
    removeFavorite.mockResolvedValue({
      success: true,
      data: {
        productId: 401,
        favorite: false
      }
    })

    const store = setupStore()

    await store.toggleFavoriteRemote(401)
    expect(addFavorite).toHaveBeenCalledWith(401)
    expect(store.favoriteIds).toEqual(['401'])

    await store.toggleFavoriteRemote(401)
    expect(removeFavorite).toHaveBeenCalledWith(401)
    expect(store.favoriteIds).toEqual([])
  })
})
