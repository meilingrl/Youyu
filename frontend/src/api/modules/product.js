import service from '@/api/client'
import { isValidEntityId } from '@/utils/id-utils'

export function getProductList(params) {
  return service.get('/products', { params })
}

export function getProductDetail(id) {
  if (!isValidEntityId(id)) {
    return Promise.reject(new Error('无效的商品 ID'))
  }
  return service.get(`/products/${String(id).trim()}`)
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
