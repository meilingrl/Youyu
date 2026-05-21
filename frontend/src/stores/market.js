import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { defaultUserPreference } from '@/constants/insightMetrics'
import {
  createProduct,
  getMyProductList,
  getProductDetail,
  getProductList
} from '@/api/modules/product'
import { listFavorites, toggleFavorite as toggleFavoriteApi } from '@/api/modules/favorite'
import {
  getUserAddresses,
  getUserInsightSnapshot,
  getUserPreference,
  getUserProfile,
  updateUserPreference as updateUserPreferenceApi
} from '@/api/modules/user'
import { getMyShop, getShopDetail, getShopInsightSnapshot } from '@/api/modules/shop'
import { getStorage, setStorage } from '@/utils/storage'
import {
  categoriesFromProducts,
  normalizeProduct,
  normalizeProfile,
  normalizeShop
} from '@/utils/market-normalizers'

const USER_PREFERENCE_KEY = 'campus-market-user-preference'

const DEFAULT_CATEGORIES = [
  { id: 1, name: '学习资料' },
  { id: 2, name: '学习工具' },
  { id: 3, name: '宿舍生活' },
  { id: 4, name: '数字配件' }
]

const reservedUserInsightSnapshot = {
  userId: '',
  totalSpendAmount: null,
  totalPurchasedItemCount: null,
  recentBrowses: [],
  favoritePreferenceSummary: [],
  lastCalculatedAt: null,
  metricSource: 'reserved_query'
}

const verificationTemplate = {
  realName: '',
  studentNo: '',
  college: '',
  major: '',
  grade: '',
  campusEmail: '',
  verifyMethod: 'campus_email',
  note: ''
}

function emptyOwnedShopState() {
  return {
    shop: null,
    capability: {},
    products: []
  }
}

function ensureSuccessArrayResponse(response, fallbackMessage) {
  if (!response || response.success !== true || !Array.isArray(response.data)) {
    throw new Error(response?.message || fallbackMessage)
  }
}

function ensureSuccessDataResponse(response, fallbackMessage) {
  if (!response || response.success !== true || !response.data) {
    throw new Error(response?.message || fallbackMessage)
  }
}

export const useMarketStore = defineStore('market', () => {
  const categories = ref([...DEFAULT_CATEGORIES])
  const shops = ref([])
  const ownedShop = ref(emptyOwnedShopState())
  const products = ref([])
  const myProducts = ref([])
  const favoriteIds = ref([])
  const profile = ref(normalizeProfile())

  const searchTotal = ref(0)
  const searchPage = ref(1)
  const searchPageSize = ref(12)
  const loadingProducts = ref(false)
  const productError = ref('')
  const loadingProfile = ref(false)
  const profileError = ref('')
  const loadingPreference = ref(false)
  const savingPreference = ref(false)
  const preferenceError = ref('')
  const preferenceSource = ref('local_fallback')
  const userPreference = ref(getStorage(USER_PREFERENCE_KEY, { ...defaultUserPreference }))
  const userInsightSnapshot = ref({ ...reservedUserInsightSnapshot })
  const loadingUserInsight = ref(false)
  const userInsightError = ref('')
  const userInsightStatus = ref('reserved')
  const shopInsightById = ref({})
  const loadingShopInsight = ref(false)
  const shopInsightError = ref('')

  const favoriteProducts = computed(() =>
    products.value.filter((item) => favoriteIds.value.includes(String(item.id)))
  )

  /**
   * 判断某个商品是否已被当前用户收藏。
   *
   * @param {string|number} productId - 商品 ID
   * @returns {boolean} 已收藏返回 true，否则返回 false
   * @sideEffects 无
   */
  function isFavorite(productId) {
    return favoriteIds.value.includes(String(productId))
  }

  /**
   * 直接替换本地 products 列表（对每项执行 normalizeProduct 规范化）。
   *
   * @param {object[]} [list=[]] - 待设置的原始商品数据数组
   * @returns {void}
   * @sideEffects 替换 products ref
   */
  function setProducts(list = []) {
    products.value = list.map(normalizeProduct)
  }

  function setFavoriteIds(ids = []) {
    favoriteIds.value = [...new Set(ids.map((id) => String(id)).filter(Boolean))]
  }

  /**
   * 从本地 products 列表中按 ID 查找商品。
   *
   * @param {string|number} productId - 商品 ID
   * @returns {object|null} 匹配的商品对象，不存在则返回 null
   * @sideEffects 无
   */
  function getProductById(productId) {
    return products.value.find((item) => String(item.id) === String(productId)) || null
  }

  /**
   * 从本地 shops 列表中按 ID 查找店铺。
   *
   * @param {string|number} shopId - 店铺 ID
   * @returns {object|null} 匹配的店铺对象，不存在则返回 null
   * @sideEffects 无
   */
  function getShopById(shopId) {
    return shops.value.find((item) => String(item.id) === String(shopId)) || null
  }

  /**
   * 从本地 products 列表中筛选属于指定店铺的商品。
   *
   * @param {string|number} shopId - 店铺 ID
   * @returns {object[]} 属于该店铺的商品数组
   * @sideEffects 无
   */
  function getProductsByShopId(shopId) {
    return products.value.filter((item) => String(item.shopId) === String(shopId))
  }

  /**
   * 获取当前用户发布的商品列表（纯本地读取）。
   *
   * @returns {object[]} 当前用户的商品数组
   * @sideEffects 无
   */
  function getMyProducts() {
    return myProducts.value
  }

  /**
   * 按店铺 ID 获取已缓存的店铺统计快照。
   *
   * @param {string|number} shopId - 店铺 ID
   * @returns {object|null} 店铺统计快照对象，未缓存则返回 null
   * @sideEffects 无
   */
  function getShopInsightById(shopId) {
    return shopInsightById.value[String(shopId)] || null
  }

  /**
   * 从服务端加载商品列表（支持分页、筛选、搜索）。
   *
   * @param {object} [params] - 查询参数（分页、筛选条件等）
   * @returns {Promise<object[]>} 加载完成后返回当前 products 数组
   * @sideEffects 更新 products、categories、searchTotal、searchPage、searchPageSize、loadingProducts、productError
   */
  async function loadProducts(params) {
    loadingProducts.value = true
    productError.value = ''
    try {
      const response = await getProductList(params)
      if (!response || response.success !== true) {
        throw new Error(response?.message || '商品列表接口返回异常')
      }
      const data = response.data
      if (Array.isArray(data.items)) {
        setProducts(data.items)
        searchTotal.value = data.total ?? data.items.length
        searchPage.value = data.page ?? 1
        searchPageSize.value = data.pageSize ?? data.items.length
      } else if (Array.isArray(data)) {
        setProducts(data)
        searchTotal.value = data.length
        searchPage.value = 1
        searchPageSize.value = data.length
      } else {
        throw new Error(response?.message || '商品列表接口返回异常')
      }
      categories.value = categoriesFromProducts(products.value)
      return products.value
    } catch (error) {
      productError.value = error?.response?.data?.message || error?.message || '商品列表加载失败'
      throw error
    } finally {
      loadingProducts.value = false
    }
  }

  /**
   * 从服务端加载单个商品详情，并 upsert 到本地 products 列表。
   *
   * @param {string|number} productId - 商品 ID
   * @returns {Promise<object>} 规范化后的商品详情对象
   * @sideEffects 向 products 列表更新或插入该商品，必要时重新派生 categories
   */
  async function loadProductDetail(productId) {
    const response = await getProductDetail(productId)
    ensureSuccessDataResponse(response, '商品详情加载失败')

    const detail = normalizeProduct(response.data)
    const index = products.value.findIndex((item) => String(item.id) === String(productId))
    if (index >= 0) {
      products.value.splice(index, 1, detail)
    } else {
      products.value = [detail, ...products.value]
      categories.value = categoriesFromProducts(products.value)
    }
    return detail
  }

  /**
   * 从服务端加载当前用户发布的商品列表。
   *
   * @returns {Promise<object[]>} 规范化后的用户商品数组
   * @sideEffects 填充 myProducts ref
   */
  async function loadMyProducts() {
    const response = await getMyProductList()
    ensureSuccessArrayResponse(response, '我的发布列表加载失败')
    myProducts.value = response.data.map(normalizeProduct)
    return myProducts.value
  }

  /**
   * 创建并发布一个新商品。
   *
   * @param {object} payload - 创建商品所需的参数
   * @returns {Promise<object>} 规范化后的新商品对象
   * @sideEffects 向 myProducts 和 products 头部插入新商品
   */
  async function publishProduct(payload) {
    const response = await createProduct(payload)
    ensureSuccessDataResponse(response, '商品发布失败')

    const product = normalizeProduct(response.data)
    myProducts.value = [product, ...myProducts.value.filter((item) => String(item.id) !== String(product.id))]
    if (product.status === 'on_sale') {
      products.value = [product, ...products.value.filter((item) => String(item.id) !== String(product.id))]
    }
    return product
  }

  /**
   * 从服务端加载收藏列表，同步到本地 favoriteIds 和 products。
   *
   * @returns {Promise<object[]>} 规范化后的收藏商品数组
   * @sideEffects 填充 favoriteIds ref，扩展 products ref 中的收藏商品
   */
  async function loadFavorites() {
    const response = await listFavorites()
    if (!response || response.success !== true) {
      throw new Error(response?.message || '收藏列表加载失败')
    }

    const data = response.data
    if (Array.isArray(data?.productIds)) {
      setFavoriteIds(data.productIds)
      return favoriteProducts.value
    }

    const rows = Array.isArray(data) ? data : data?.items || data?.products || []
    const normalized = rows.map((item) => normalizeProduct(item.product || item))
    if (normalized.length) {
      const knownIds = new Set(products.value.map((item) => String(item.id)))
      products.value = [
        ...products.value,
        ...normalized.filter((item) => !knownIds.has(String(item.id)))
      ]
    }
    setFavoriteIds(normalized.map((item) => item.id))
    return normalized
  }

  /**
   * 通过 API 切换收藏状态，完成后刷新收藏列表。
   *
   * @param {string|number} productId - 商品 ID
   * @returns {Promise<void>} 收藏状态切换完成后刷新收藏列表
   * @sideEffects 调用远程 API，刷新 favoriteIds 和 products
   */
  async function toggleFavoriteRemote(productId) {
    const response = await toggleFavoriteApi(productId)
    if (!response || response.success !== true) {
      throw new Error(response?.message || '收藏状态更新失败')
    }
    await loadFavorites()
  }

  /**
   * 切换商品的收藏状态（委托远程 API 调用）。
   *
   * @param {string|number} productId - 商品 ID
   * @returns {Promise<void>} 收藏状态切换完成后刷新收藏列表
   * @sideEffects 调用远程 API，刷新 favoriteIds 和 products
   */
  function toggleFavorite(productId) {
    return toggleFavoriteRemote(productId)
  }

  /**
   * 从服务端加载指定店铺的详情及其商品列表。
   *
   * @param {string|number} shopId - 店铺 ID
   * @returns {Promise<object>} 规范化后的店铺对象（含 capability 信息）
   * @sideEffects 扩展 shops ref 和 products ref
   */
  async function loadShopDetail(shopId) {
    const response = await getShopDetail(shopId)
    ensureSuccessDataResponse(response, '店铺详情加载失败')

    const data = response.data
    const rawShop = data.shop || data
    const capability = data.capability || {}
    const shop = {
      ...normalizeShop(rawShop),
      ownerName: rawShop.ownerName || '',
      cover: rawShop.coverUrl || rawShop.cover || '',
      avatarUrl: rawShop.avatarUrl || '',
      capability,
      capabilityLevel: capability.capabilityLevel || ''
    }
    const shopProducts = data.products || data.productList || []

    shops.value = [shop, ...shops.value.filter((item) => String(item.id) !== String(shop.id))]

    if (Array.isArray(shopProducts) && shopProducts.length) {
      const normalized = shopProducts.map(normalizeProduct)
      const knownIds = new Set(products.value.map((item) => String(item.id)))
      products.value = [
        ...products.value,
        ...normalized.filter((item) => !knownIds.has(String(item.id)))
      ]
    }

    return shop
  }

  /**
   * 从服务端加载当前用户拥有的店铺信息。
   *
   * @returns {Promise<object>} 包含 shop、capability、products 的 ownedShop 对象
   * @sideEffects 填充 ownedShop ref，扩展 shops 和 products ref
   */
  async function loadMyShop() {
    try {
      const response = await getMyShop()
      ensureSuccessDataResponse(response, '我的店铺加载失败')

      const data = response.data || {}
      const capability = data.capability || {}
      const shop =
        data.shop && Object.keys(data.shop).length
          ? {
              ...normalizeShop(data.shop),
              ownerName: data.shop.ownerName || '',
              cover: data.shop.coverUrl || data.shop.cover || '',
              avatarUrl: data.shop.avatarUrl || '',
              capability,
              capabilityLevel: capability.capabilityLevel || ''
            }
          : null
      const normalizedProducts = Array.isArray(data.products)
        ? data.products.map(normalizeProduct)
        : []

      ownedShop.value = {
        shop,
        capability,
        products: normalizedProducts
      }

      if (shop?.id) {
        shops.value = [shop, ...shops.value.filter((item) => String(item.id) !== String(shop.id))]
      }

      if (normalizedProducts.length) {
        const knownIds = new Set(products.value.map((item) => String(item.id)))
        products.value = [
          ...products.value,
          ...normalizedProducts.filter((item) => !knownIds.has(String(item.id)))
        ]
      }

      return ownedShop.value
    } catch (error) {
      ownedShop.value = emptyOwnedShopState()
      throw error
    }
  }

  /**
   * 从服务端加载当前用户的个人资料。
   *
   * @param {object} [currentUser] - 回退用的当前用户数据，API 失败时作为降级填充
   * @returns {Promise<object>} 规范化后的用户 profile 对象
   * @sideEffects 填充 profile ref，管理 loadingProfile / profileError
   */
  async function loadProfile(currentUser) {
    loadingProfile.value = true
    profileError.value = ''
    try {
      const response = await getUserProfile()
      ensureSuccessDataResponse(response, '个人资料加载失败')
      const normalized = normalizeProfile(response.data, currentUser)
      profile.value = {
        ...normalized,
        addresses: normalized.addresses.length ? normalized.addresses : profile.value.addresses
      }
      return profile.value
    } catch (error) {
      profile.value = normalizeProfile({}, currentUser)
      profileError.value = error?.response?.data?.message || error?.message || '个人资料加载失败'
      throw error
    } finally {
      loadingProfile.value = false
    }
  }

  /**
   * 从服务端加载用户地址列表，合并到当前 profile 中。
   *
   * @returns {Promise<object[]>} 用户地址数组
   * @sideEffects 合并地址到 profile.addresses
   */
  async function loadUserAddresses() {
    const response = await getUserAddresses()
    ensureSuccessArrayResponse(response, '地址列表加载失败')
    profile.value = {
      ...profile.value,
      addresses: normalizeProfile({ addresses: response.data }).addresses
    }
    return profile.value.addresses
  }

  /**
   * 在本地应用用户偏好设置（同步写入 localStorage）。
   *
   * @param {object} payload - 要应用的偏好字段
   * @param {string} [source] - 来源标签（如 'api'、'local_fallback'）
   * @returns {void}
   * @sideEffects 写入 localStorage，更新 userPreference ref 和 preferenceSource ref
   */
  function applyUserPreference(payload = {}, source = preferenceSource.value) {
    userPreference.value = {
      ...userPreference.value,
      ...payload,
      notificationPreference: {
        ...userPreference.value.notificationPreference,
        ...(payload.notificationPreference || {})
      }
    }

    setStorage(USER_PREFERENCE_KEY, userPreference.value)
    preferenceSource.value = source
  }

  /**
   * 从服务端加载用户偏好设置，失败时降级到本地缓存。
   *
   * @returns {Promise<object>} 当前用户偏好对象
   * @sideEffects 从 API 填充 userPreference ref，管理 loadingPreference / preferenceError
   */
  async function loadUserPreference() {
    loadingPreference.value = true
    preferenceError.value = ''
    try {
      const response = await getUserPreference()
      ensureSuccessDataResponse(response, '偏好设置加载失败')
      applyUserPreference(response.data, 'api')
      return userPreference.value
    } catch (error) {
      preferenceError.value = error?.response?.data?.message || error?.message || '偏好设置加载失败'
      preferenceSource.value = 'local_fallback'
      throw error
    } finally {
      loadingPreference.value = false
    }
  }

  /**
   * 将用户偏好设置持久化到服务端并同步应用到本地。
   *
   * @param {object} payload - 要更新的偏好字段
   * @returns {Promise<object>} 更新后的用户偏好对象
   * @sideEffects API 持久化后本地应用，管理 savingPreference / preferenceError
   */
  async function updateUserPreference(payload) {
    savingPreference.value = true
    preferenceError.value = ''
    try {
      const response = await updateUserPreferenceApi(payload)
      ensureSuccessDataResponse(response, '偏好设置保存失败')
      applyUserPreference(response.data, 'api')
      return userPreference.value
    } catch (error) {
      preferenceError.value = error?.response?.data?.message || error?.message || '偏好设置保存失败'
      throw error
    } finally {
      savingPreference.value = false
    }
  }

  /**
   * 从服务端加载用户统计快照（消费总额、购买件数、浏览偏好等）。
   *
   * @returns {Promise<object>} 用户统计快照对象
   * @sideEffects 填充 userInsightSnapshot ref，管理 loadingUserInsight / userInsightError / userInsightStatus
   */
  async function loadUserInsightSnapshot() {
    loadingUserInsight.value = true
    userInsightError.value = ''
    try {
      const response = await getUserInsightSnapshot()
      ensureSuccessDataResponse(response, '用户统计快照加载失败')
      userInsightSnapshot.value = {
        ...reservedUserInsightSnapshot,
        ...response.data
      }
      userInsightStatus.value =
        response.data.metricSource === 'reserved_query' ? 'reserved' : 'connected'
      return userInsightSnapshot.value
    } catch (error) {
      userInsightSnapshot.value = { ...reservedUserInsightSnapshot }
      userInsightStatus.value = 'failed'
      userInsightError.value = error?.response?.data?.message || error?.message || '用户统计快照加载失败'
      throw error
    } finally {
      loadingUserInsight.value = false
    }
  }

  /**
   * 从服务端加载指定店铺的统计快照，失败时写入降级占位数据。
   *
   * @param {string|number} shopId - 店铺 ID
   * @returns {Promise<object>} 店铺统计快照对象
   * @sideEffects 填充 shopInsightById[shopId] ref，管理 loadingShopInsight / shopInsightError
   */
  async function loadShopInsightSnapshot(shopId) {
    loadingShopInsight.value = true
    shopInsightError.value = ''
    try {
      const response = await getShopInsightSnapshot(shopId)
      ensureSuccessDataResponse(response, '店铺统计快照加载失败')
      shopInsightById.value = {
        ...shopInsightById.value,
        [String(shopId)]: response.data
      }
      return response.data
    } catch (error) {
      shopInsightById.value = {
        ...shopInsightById.value,
        [String(shopId)]: {
          shopId,
          monthlySalesAmount: null,
          monthlyOrderCount: null,
          hotProducts: [],
          viewCountSummary: null,
          favoriteCountSummary: null,
          repeatBuyerCount: null,
          lastCalculatedAt: null,
          metricSource: 'unavailable'
        }
      }
      shopInsightError.value = error?.response?.data?.message || error?.message || '店铺统计快照加载失败'
      throw error
    } finally {
      loadingShopInsight.value = false
    }
  }

  /**
   * 基于当前 profile 计算学生认证表单的预填默认值。
   *
   * @returns {object} 包含 realName、major、grade 等预填字段的认证草案对象
   * @sideEffects 无
   */
  function getVerificationDraft() {
    return {
      ...verificationTemplate,
      realName: profile.value.nickname || '',
      major: profile.value.major || '',
      grade: profile.value.grade || ''
    }
  }

  return {
    categories,
    shops,
    ownedShop,
    products,
    myProducts,
    profile,
    userPreference,
    userInsightSnapshot,
    shopInsightById,
    favoriteIds,
    searchTotal,
    searchPage,
    searchPageSize,
    loadingProducts,
    productError,
    loadingProfile,
    profileError,
    loadingPreference,
    savingPreference,
    preferenceError,
    preferenceSource,
    loadingUserInsight,
    userInsightError,
    userInsightStatus,
    loadingShopInsight,
    shopInsightError,
    favoriteProducts,
    isFavorite,
    toggleFavorite,
    getProductById,
    getShopById,
    getProductsByShopId,
    getMyProducts,
    getShopInsightById,
    setProducts,
    loadProducts,
    loadProductDetail,
    loadMyProducts,
    publishProduct,
    loadProfile,
    loadFavorites,
    toggleFavoriteRemote,
    loadMyShop,
    loadShopDetail,
    loadUserAddresses,
    loadUserPreference,
    updateUserPreference,
    loadUserInsightSnapshot,
    loadShopInsightSnapshot,
    getVerificationDraft
  }
})
