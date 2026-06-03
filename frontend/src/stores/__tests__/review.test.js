import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useReviewStore } from '@/stores/review'

vi.mock('@/api/modules/review', () => ({
  submitProductReview: vi.fn(),
  submitShopReview: vi.fn(),
  getPendingReviewItems: vi.fn(),
  getMyReviews: vi.fn(),
  getProductReviewList: vi.fn(),
  getProductReviewSummary: vi.fn(),
  getShopReviewList: vi.fn(),
  getShopReviewSummary: vi.fn()
}))

import {
  submitProductReview,
  submitShopReview,
  getPendingReviewItems,
  getMyReviews,
  getProductReviewList,
  getProductReviewSummary,
  getShopReviewList,
  getShopReviewSummary
} from '@/api/modules/review'

function setupStore() {
  setActivePinia(createPinia())
  return useReviewStore()
}

describe('review store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('loadPendingReviews', () => {
    it('loads pending review items successfully', async () => {
      getPendingReviewItems.mockResolvedValue({
        success: true,
        data: { items: [{ id: 1, productId: 100, titleSnapshot: 'Test' }] }
      })
      const store = setupStore()
      await store.loadPendingReviews()

      expect(store.pendingItems).toHaveLength(1)
      expect(store.pendingItems[0]).toMatchObject({ id: 1, productId: 100 })
      expect(store.pendingError).toBe('')
    })

    it('sets error on failure', async () => {
      getPendingReviewItems.mockResolvedValue({ success: false, message: 'API error' })
      const store = setupStore()
      try { await store.loadPendingReviews() } catch (e) { /* expected */ }

      expect(store.pendingError).toBeTruthy()
    })
  })

  describe('loadMyReviews', () => {
    it('loads both product and shop reviews', async () => {
      getMyReviews.mockResolvedValue({
        success: true,
        data: {
          productReviews: [{ id: 1, score: 5, images: [{ media_url: 'data:image/png;base64,abc' }] }],
          shopReviews: [{ id: 2, score: 4, images: [{ mediaUrl: 'data:image/webp;base64,def' }] }]
        }
      })
      const store = setupStore()
      await store.loadMyReviews()

      expect(store.myProductReviews).toHaveLength(1)
      expect(store.myShopReviews).toHaveLength(1)
      expect(store.myProductReviews[0].images[0].mediaUrl).toBe('data:image/png;base64,abc')
      expect(store.myShopReviews[0].images[0].mediaUrl).toBe('data:image/webp;base64,def')
      expect(store.myReviewsError).toBe('')
    })
  })

  describe('loadProductReviews', () => {
    it('loads product reviews with pagination', async () => {
      getProductReviewList.mockResolvedValue({
        success: true,
        data: { items: [{ id: 1, score: 5, content: 'Great', images: [{ file_name: 'review.png', media_url: 'data:image/png;base64,abc' }] }], total: 1 }
      })
      const store = setupStore()
      await store.loadProductReviews(100, 1, 10)

      expect(getProductReviewList).toHaveBeenCalledWith(100, { page: 1, pageSize: 10 })
      expect(store.productReviews).toHaveLength(1)
      expect(store.productReviews[0].images[0]).toMatchObject({
        fileName: 'review.png',
        mediaUrl: 'data:image/png;base64,abc'
      })
      expect(store.productReviewTotal).toBe(1)
    })
  })

  describe('loadProductReviewSummary', () => {
    it('loads product rating summary', async () => {
      getProductReviewSummary.mockResolvedValue({
        success: true,
        data: { avgScore: 4.5, reviewCount: 10, distribution: [] }
      })
      const store = setupStore()
      await store.loadProductReviewSummary(100)

      expect(store.productReviewSummary).toEqual({ avgScore: 4.5, reviewCount: 10, distribution: [] })
    })
  })

  describe('loadShopReviewSummary', () => {
    it('loads shop rating summary', async () => {
      getShopReviewSummary.mockResolvedValue({
        success: true,
        data: { avgScore: 4.0, reviewCount: 5, distribution: [] }
      })
      const store = setupStore()
      await store.loadShopReviewSummary(200)

      expect(store.shopReviewSummary).toEqual({ avgScore: 4.0, reviewCount: 5, distribution: [] })
    })
  })
})
