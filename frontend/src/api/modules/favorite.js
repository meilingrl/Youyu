import service from '@/api/client'

export function listFavorites() {
  return service.get('/favorites')
}

export function toggleFavorite(productId) {
  return service.post(`/favorites/${productId}/toggle`)
}
