import service from '@/api/client'

export function getShopSkeleton() {
  return service.get('/shops/skeleton')
}

export function getMyShop() {
  return service.get('/shops/mine')
}

export function getShopDetail(shopId) {
  return service.get(`/shops/${shopId}`)
}

export function getShopInsightSnapshot(shopId) {
  return service.get(`/shops/${shopId}/insight-snapshot`)
}
