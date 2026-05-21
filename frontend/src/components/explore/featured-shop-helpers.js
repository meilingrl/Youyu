export function buildFeaturedShops(rows = [], options = {}) {
  const {
    maxShops = 3,
    maxPreviewProducts = 3,
    fallbackCategoryLabel = '校园店铺',
    fallbackDescription = '把适合校园场景的商品整理成一条轻松好逛的店铺线索。'
  } = options

  const byShop = new Map()

  rows.forEach((product) => {
    const shopId = String(product?.shopId || '')
    const shopName = String(product?.shopName || '').trim()
    if (!shopId || !shopName) {
      return
    }

    if (!byShop.has(shopId)) {
      byShop.set(shopId, {
        id: shopId,
        name: shopName,
        initials: shopName.slice(0, 2).toUpperCase(),
        categoryLabel: product.categoryName || fallbackCategoryLabel,
        description: product.subtitle || fallbackDescription,
        favoriteCount: 0,
        productCount: 0,
        lowestPrice: Number(product.salePrice || product.price || 0),
        previewProducts: []
      })
    }

    const shop = byShop.get(shopId)
    shop.productCount += 1
    shop.favoriteCount += Number(product.favoriteCount || 0)
    shop.lowestPrice = Math.min(shop.lowestPrice, Number(product.salePrice || product.price || 0))
    if (shop.previewProducts.length < maxPreviewProducts) {
      shop.previewProducts.push(product)
    }
  })

  return [...byShop.values()]
    .map((shop) => ({
      ...shop,
      priceLabel: Number.isFinite(shop.lowestPrice) ? shop.lowestPrice.toFixed(2) : '0.00'
    }))
    .sort((a, b) => (b.favoriteCount + b.productCount * 3) - (a.favoriteCount + a.productCount * 3))
    .slice(0, maxShops)
}
