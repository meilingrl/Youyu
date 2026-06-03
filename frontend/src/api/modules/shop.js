import service from '@/api/client'
import { isValidEntityId } from '@/utils/id-utils'

export function getShopSkeleton() {
  return service.get('/shops/skeleton')
}

export function getMyShop() {
  return service.get('/shops/mine')
}

export function getShopDetail(shopId) {
  if (!isValidEntityId(shopId)) {
    return Promise.reject(new Error('无效的店铺 ID'))
  }
  return service.get(`/shops/${String(shopId).trim()}`)
}

export function getShopInsightSnapshot(shopId) {
  if (!isValidEntityId(shopId)) {
    return Promise.reject(new Error('无效的店铺 ID'))
  }
  return service.get(`/shops/${String(shopId).trim()}/insight-snapshot`)
}
