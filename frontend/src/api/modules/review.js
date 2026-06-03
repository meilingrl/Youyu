import service from '@/api/client'
import { isValidEntityId } from '@/utils/id-utils'

export function submitProductReview(payload) {
  return service.post('/reviews/products', payload)
}

export function submitShopReview(payload) {
  return service.post('/reviews/shops', payload)
}

export function getPendingReviewItems() {
  return service.get('/reviews/pending')
}

export function getMyReviews() {
  return service.get('/reviews/mine')
}

export function getProductReviewList(productId, params = {}) {
  if (!isValidEntityId(productId)) {
    return Promise.reject(new Error('无效的商品 ID'))
  }
  return service.get(`/products/${String(productId).trim()}/reviews`, { params })
}

export function getProductReviewSummary(productId) {
  if (!isValidEntityId(productId)) {
    return Promise.reject(new Error('无效的商品 ID'))
  }
  return service.get(`/products/${String(productId).trim()}/review-summary`)
}

export function getShopReviewList(shopId, params = {}) {
  if (!isValidEntityId(shopId)) {
    return Promise.reject(new Error('无效的店铺 ID'))
  }
  return service.get(`/shops/${String(shopId).trim()}/reviews`, { params })
}

export function getShopReviewSummary(shopId) {
  if (!isValidEntityId(shopId)) {
    return Promise.reject(new Error('无效的店铺 ID'))
  }
  return service.get(`/shops/${String(shopId).trim()}/review-summary`)
}
