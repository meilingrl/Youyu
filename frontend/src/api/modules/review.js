import service from '@/api/client'

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
  return service.get(`/products/${productId}/reviews`, { params })
}

export function getProductReviewSummary(productId) {
  return service.get(`/products/${productId}/review-summary`)
}

export function getShopReviewList(shopId, params = {}) {
  return service.get(`/shops/${shopId}/reviews`, { params })
}

export function getShopReviewSummary(shopId) {
  return service.get(`/shops/${shopId}/review-summary`)
}
