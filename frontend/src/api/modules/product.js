import service from '@/api/client'
import { isValidEntityId } from '@/utils/id-utils'

const PRODUCT_LIST_SORTS = new Set(['price_asc', 'price_desc', 'sales_desc', 'newest'])

function normalizeProductListParams(params = {}) {
  const next = { ...params }
  if (next.sort && !PRODUCT_LIST_SORTS.has(next.sort)) {
    next.sort = 'newest'
  }
  return next
}

export function getProductList(params) {
  return service.get('/products', { params: normalizeProductListParams(params) })
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
