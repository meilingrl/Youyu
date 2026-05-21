import service from '@/api/client'

export function getProductList(params) {
  return service.get('/products', { params })
}

export function getProductDetail(id) {
  return service.get(`/products/${id}`)
}

export function getMyProductList() {
  return service.get('/products/mine')
}

export function createProduct(payload) {
  return service.post('/products', payload)
}

export function getProductSkeleton() {
  return service.get('/products/skeleton')
}
