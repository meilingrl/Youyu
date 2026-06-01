import service from '@/api/client'

export function listFavorites() {
  return service.get('/favorites')
}

export function addFavorite(productId) {
  return service.post('/favorites', { productId })
}

export function removeFavorite(productId) {
  return service.delete(`/favorites/${productId}`)
}
