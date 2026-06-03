import service from '@/api/client'
import { isValidEntityId } from '@/utils/id-utils'

export function getHomeRecommend(limit = 8) {
  return service.get('/recommend/home', { params: { limit } })
}

export function getAlsoBought(productId, limit = 6) {
  if (!isValidEntityId(productId)) {
    return Promise.reject(new Error('无效的商品 ID'))
  }
  return service.get(`/recommend/also-bought/${String(productId).trim()}`, { params: { limit } })
}
