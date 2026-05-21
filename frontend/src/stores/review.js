import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  submitProductReview as submitProductReviewApi,
  submitShopReview as submitShopReviewApi,
  getPendingReviewItems,
  getMyReviews,
  getProductReviewList,
  getProductReviewSummary,
  getShopReviewList,
  getShopReviewSummary
} from '@/api/modules/review'

export const useReviewStore = defineStore('review', () => {
  // Pending review items
  const pendingItems = ref([])
  const loadingPending = ref(false)
  const pendingError = ref('')

  // My reviews
  const myProductReviews = ref([])
  const myShopReviews = ref([])
  const loadingMyReviews = ref(false)
  const myReviewsError = ref('')

  // Product reviews (public)
  const productReviews = ref([])
  const productReviewTotal = ref(0)
  const loadingProductReviews = ref(false)
  const productReviewError = ref('')

  // Product rating summary
  const productReviewSummary = ref(null)
  const loadingProductSummary = ref(false)
  const productSummaryError = ref('')

  // Shop reviews (public)
  const shopReviews = ref([])
  const shopReviewTotal = ref(0)
  const loadingShopReviews = ref(false)
  const shopReviewError = ref('')

  // Shop rating summary
  const shopReviewSummary = ref(null)
  const loadingShopSummary = ref(false)
  const shopSummaryError = ref('')

  /**
   * 加载当前用户待评价的订单商品列表。
   *
   * @returns {Promise<void>}
   * @sideEffects 更新 pendingItems 响应式数组，管理 loadingPending / pendingError 状态
   */
  async function loadPendingReviews() {
    loadingPending.value = true
    pendingError.value = ''
    try {
      const res = await getPendingReviewItems()
      if (!res?.success) throw new Error(res?.message || 'Failed to load pending reviews')
      pendingItems.value = res.data?.items ?? []
    } catch (e) {
      pendingError.value = e?.response?.data?.message || e.message || '加载待评价列表失败'
      throw e
    } finally {
      loadingPending.value = false
    }
  }

  /**
   * 加载当前用户已提交的评价历史（商品评价 + 店铺评价）。
   *
   * @returns {Promise<void>}
   * @sideEffects 更新 myProductReviews / myShopReviews 响应式数组，管理 loadingMyReviews / myReviewsError 状态
   */
  async function loadMyReviews() {
    loadingMyReviews.value = true
    myReviewsError.value = ''
    try {
      const res = await getMyReviews()
      if (!res?.success) throw new Error(res?.message || 'Failed to load my reviews')
      myProductReviews.value = res.data?.productReviews ?? []
      myShopReviews.value = res.data?.shopReviews ?? []
    } catch (e) {
      myReviewsError.value = e?.response?.data?.message || e.message || '加载我的评价失败'
      throw e
    } finally {
      loadingMyReviews.value = false
    }
  }

  /**
   * 提交商品评价。
   *
   * @param {object} payload - 评价内容（包含评分、文字评价等字段）
   * @returns {Promise<object>} 提交结果数据
   * @sideEffects 管理 submittingProduct / submitProductError 状态
   */
  async function doSubmitProductReview(payload) {
    const res = await submitProductReviewApi(payload)
    if (!res?.success) throw new Error(res?.message || '提交评价失败')
    return res.data
  }

  /**
   * 提交店铺评价。
   *
   * @param {object} payload - 评价内容（包含评分、文字评价等字段）
   * @returns {Promise<object>} 提交结果数据
   * @sideEffects 管理 submittingShop / submitShopError 状态
   */
  async function doSubmitShopReview(payload) {
    const res = await submitShopReviewApi(payload)
    if (!res?.success) throw new Error(res?.message || '提交评价失败')
    return res.data
  }

  /**
   * 分页加载指定商品的评价列表。
   *
   * @param {string|number} productId - 商品 ID
   * @param {number} [page=1] - 页码
   * @param {number} [pageSize=10] - 每页条数
   * @returns {Promise<void>}
   * @sideEffects 更新 productReviews / productReviewTotal，管理 loadingProductReviews / productReviewError 状态
   */
  async function loadProductReviews(productId, page = 1, pageSize = 10) {
    loadingProductReviews.value = true
    productReviewError.value = ''
    try {
      const res = await getProductReviewList(productId, { page, pageSize })
      if (!res?.success) throw new Error(res?.message || 'Failed to load reviews')
      productReviews.value = res.data?.items ?? []
      productReviewTotal.value = res.data?.total ?? 0
    } catch (e) {
      productReviewError.value = e?.response?.data?.message || e.message || '加载商品评价失败'
    } finally {
      loadingProductReviews.value = false
    }
  }

  /**
   * 加载指定商品的评分汇总（平均分、分布等）。
   *
   * @param {string|number} productId - 商品 ID
   * @returns {Promise<void>}
   * @sideEffects 更新 productReviewSummary，管理 loadingProductSummary / productSummaryError 状态
   */
  async function loadProductReviewSummary(productId) {
    loadingProductSummary.value = true
    productSummaryError.value = ''
    try {
      const res = await getProductReviewSummary(productId)
      if (!res?.success) throw new Error(res?.message || 'Failed to load summary')
      productReviewSummary.value = res.data
    } catch (e) {
      productSummaryError.value = e?.response?.data?.message || e.message || '加载评分汇总失败'
    } finally {
      loadingProductSummary.value = false
    }
  }

  /**
   * 分页加载指定店铺的评价列表。
   *
   * @param {string|number} shopId - 店铺 ID
   * @param {number} [page=1] - 页码
   * @param {number} [pageSize=10] - 每页条数
   * @returns {Promise<void>}
   * @sideEffects 更新 shopReviews / shopReviewTotal，管理 loadingShopReviews / shopReviewError 状态
   */
  async function loadShopReviews(shopId, page = 1, pageSize = 10) {
    loadingShopReviews.value = true
    shopReviewError.value = ''
    try {
      const res = await getShopReviewList(shopId, { page, pageSize })
      if (!res?.success) throw new Error(res?.message || 'Failed to load shop reviews')
      shopReviews.value = res.data?.items ?? []
      shopReviewTotal.value = res.data?.total ?? 0
    } catch (e) {
      shopReviewError.value = e?.response?.data?.message || e.message || '加载店铺评价失败'
    } finally {
      loadingShopReviews.value = false
    }
  }

  /**
   * 加载指定店铺的评分汇总（平均分、分布等）。
   *
   * @param {string|number} shopId - 店铺 ID
   * @returns {Promise<void>}
   * @sideEffects 更新 shopReviewSummary，管理 loadingShopSummary / shopSummaryError 状态
   */
  async function loadShopReviewSummary(shopId) {
    loadingShopSummary.value = true
    shopSummaryError.value = ''
    try {
      const res = await getShopReviewSummary(shopId)
      if (!res?.success) throw new Error(res?.message || 'Failed to load shop summary')
      shopReviewSummary.value = res.data
    } catch (e) {
      shopSummaryError.value = e?.response?.data?.message || e.message || '加载店铺评分失败'
    } finally {
      loadingShopSummary.value = false
    }
  }

  return {
    pendingItems, loadingPending, pendingError,
    myProductReviews, myShopReviews, loadingMyReviews, myReviewsError,
    productReviews, productReviewTotal, loadingProductReviews, productReviewError,
    productReviewSummary, loadingProductSummary, productSummaryError,
    shopReviews, shopReviewTotal, loadingShopReviews, shopReviewError,
    shopReviewSummary, loadingShopSummary, shopSummaryError,
    loadPendingReviews, loadMyReviews,
    doSubmitProductReview, doSubmitShopReview,
    loadProductReviews, loadProductReviewSummary,
    loadShopReviews, loadShopReviewSummary
  }
})
