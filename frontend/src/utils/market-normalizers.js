/**
 * 将 API 原始产品数据规范化为统一的产品对象，所有字段保证存在并提供合理默认值。
 *
 * @param {object} [raw={}] - API 原始产品数据
 * @returns {object} 规范化产品（所有字段保证存在）
 */
export function normalizeProduct(raw = {}) {
  const fulfillmentTypes = Array.isArray(raw.allowedFulfillmentTypes)
    ? raw.allowedFulfillmentTypes
    : [
        raw.supportsLogistics ? 'logistics' : null,
        raw.supportsOfflineDelivery ? 'offline' : null,
        raw.supportsDigitalDelivery ? 'digital' : null
      ].filter(Boolean)

  const productType = raw.productType || raw.type || ''
  const salePrice = Number(raw.salePrice ?? raw.price ?? 0)
  const coverUrl = raw.coverUrl || raw.cover || raw.media?.[0] || ''
  const media =
    Array.isArray(raw.media) && raw.media.length
      ? raw.media.map((item) => (typeof item === 'string' ? item : item?.url || '')).filter(Boolean)
      : coverUrl
        ? [coverUrl]
        : []

  return {
    ...raw,
    id: raw.id,
    title: raw.title || '',
    subtitle: raw.subtitle || raw.description || '',
    description: raw.description || raw.subtitle || '',
    categoryId: raw.categoryId || raw.categoryName || '',
    categoryName: raw.categoryName || '',
    productType,
    type: productType,
    salePrice,
    price: salePrice,
    coverUrl,
    cover: coverUrl,
    media,
    previewAssets: Array.isArray(raw.previewAssets) ? raw.previewAssets : [],
    sellerId: raw.sellerId || raw.sellerUserId || '',
    sellerName: raw.sellerName || '',
    shopId: raw.shopId || '',
    shopName: raw.shopName || raw.sellerName || '个人卖家',
    status: raw.status || '',
    reviewStatus: raw.reviewStatus || '',
    favoriteCount: Number(raw.favoriteCount || 0),
    viewCount: Number(raw.viewCount || 0),
    publishedAt: raw.publishedAt || raw.createdAt || raw.updatedAt || '',
    deliveryMethods: raw.deliveryMethods || fulfillmentTypes,
    allowedFulfillmentTypes: fulfillmentTypes,
    previewRuleText: raw.previewRuleText || '',
    fullAssetLocked: Boolean(raw.fullAssetLocked)
  }
}

/**
 * 从产品列表中提取所有分类，去重后返回 `[{id, name}]` 列表。
 *
 * @param {object[]} [products=[]] - 产品列表
 * @returns {object[]} 去重后的 `[{id, name}]` 分类列表
 */
export function categoriesFromProducts(products = []) {
  const seen = new Set()
  return products
    .map((item) => ({
      id: item.categoryId,
      name: item.categoryName
    }))
    .filter((item) => item.id && item.name)
    .filter((item) => {
      if (seen.has(String(item.id))) {
        return false
      }
      seen.add(String(item.id))
      return true
    })
}

/**
 * 将 API 原始店铺数据规范化为统一的店铺对象，所有字段保证存在并提供合理默认值。
 *
 * @param {object} [raw={}] - API 原始店铺数据
 * @returns {object} 规范化店铺对象
 */
export function normalizeShop(raw = {}) {
  return {
    ...raw,
    id: raw.id || '',
    ownerUserId: raw.ownerUserId || raw.ownerId || '',
    name: raw.name || '',
    slogan: raw.slogan || raw.description || '',
    description: raw.description || raw.slogan || '',
    status: raw.status || '',
    reviewStatus: raw.reviewStatus || '',
    coverUrl: raw.coverUrl || raw.cover || '',
    announcement: raw.announcement || raw.notice || '',
    rating: Number(raw.rating ?? raw.ratingScore ?? 0),
    followers: Number(raw.followers ?? raw.followerCount ?? 0),
    creditLevel: raw.creditLevel || raw.reviewStatus || '',
    responseRate: raw.responseRate || '',
    createdAt: raw.createdAt || ''
  }
}

function verificationStatusLabel(status) {
  switch (status) {
    case 'approved':
      return '已认证'
    case 'pending_review':
      return '审核中'
    case 'rejected':
      return '已驳回'
    case 'unverified':
    default:
      return '未认证'
  }
}

function normalizeAssetUrl(url) {
  if (!url || /^(https?:|data:|blob:)/i.test(url)) {
    return url || ''
  }
  const apiBase = import.meta.env.VITE_API_BASE_URL || ''
  if (!apiBase || !apiBase.startsWith('http')) {
    return url
  }

  try {
    return `${new URL(apiBase).origin}${url.startsWith('/') ? url : `/${url}`}`
  } catch {
    return url
  }
}

/**
 * 将 API 原始用户数据规范化为带嵌套 verification / privilege / addresses 的 profile 对象。
 *
 * @param {object} [raw={}] - API 原始用户数据（含 user、verification、privilege、addresses 等）
 * @param {object|null} [currentUser=null] - 当前登录用户数据，用于回退 ID 和昵称等基础字段
 * @returns {object} 带嵌套 verification / privilege / addresses 的规范化 profile
 */
export function normalizeProfile(raw = {}, currentUser = null) {
  const user = raw.user || raw
  const verification = raw.verification || {}
  const privilege = raw.privilege || raw.privileges || currentUser?.privilege || {}
  const rawAddresses = Array.isArray(raw.addresses) ? raw.addresses : []
  const verificationStatus = verification.verificationStatus || raw.verificationStatus || 'unverified'

  return {
    id: user.id || currentUser?.id || '',
    loginId: user.loginId || user.username || currentUser?.loginId || '',
    nickname: user.nickname || currentUser?.nickname || '用户',
    avatar: normalizeAssetUrl(user.avatar || ''),
    email: user.email || currentUser?.email || '',
    school: user.school || raw.school || '',
    campus: user.campus || raw.campus || '',
    major: user.major || verification.major || raw.major || '',
    grade: user.grade || verification.grade || raw.grade || '',
    bio: user.bio || raw.bio || '',
    creditLevel: user.creditLevel || raw.creditLevel || '暂无信用摘要',
    creditScoreText: user.creditScoreText || raw.creditScoreText || raw.creditSummary || '暂无信用记录',
    verification: {
      status: verificationStatus,
      label: verificationStatusLabel(verificationStatus),
      studentNo: verification.studentNoMasked || verification.studentNo || '',
      submittedAt: verification.submittedAt || raw.submittedAt || '',
      reviewedAt: verification.reviewedAt || raw.reviewedAt || '',
      rejectReason: verification.rejectReason || raw.rejectReason || ''
    },
    privilege: {
      canBuy: Boolean(privilege.canBuy ?? privilege.canPurchase ?? true),
      canPublish: Boolean(privilege.canPublish ?? false),
      canApplyShop: Boolean(privilege.canApplyShop ?? false),
      canReview: Boolean(privilege.canReview ?? false)
    },
    addresses: rawAddresses.map((address) => ({
      ...address,
      id: address.id || '',
      contactName: address.contactName || address.receiverName || '',
      phone: address.phone || address.receiverPhone || '',
      detail: address.detail || address.detailAddress || '',
      type: address.type || address.addressType || '',
      region:
        address.region ||
        [address.province, address.city, address.district].filter(Boolean).join(' '),
      isDefault: Boolean(address.isDefault ?? address.defaultAddress ?? false)
    }))
  }
}
