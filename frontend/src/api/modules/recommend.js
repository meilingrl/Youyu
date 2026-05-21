import service from '@/api/client'

export function getHomeRecommend(limit = 8) {
  return service.get('/recommend/home', { params: { limit } })
}

export function getAlsoBought(productId, limit = 6) {
  return service.get(`/recommend/also-bought/${productId}`, { params: { limit } })
}
