import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getHomeRecommend, getAlsoBought } from '@/api/modules/recommend'
import { normalizeProduct } from '@/utils/market-normalizers'

export const useRecommendStore = defineStore('recommend', () => {
  const homeRecommendList = ref([])
  const alsoBoughtList = ref([])
  const loadingHome = ref(false)
  const homeError = ref('')
  const loadingAlsoBought = ref(false)
  const alsoBoughtError = ref('')

  function ensureSuccessArray(response, fallbackMessage) {
    if (!response || response.success !== true || !Array.isArray(response.data)) {
      throw new Error(response?.message || fallbackMessage)
    }
  }

  /**
   * 加载首页推荐商品列表（按热度或个性化排序）。
   *
   * @param {number} [limit=8] - 返回商品数量上限
   * @returns {Promise<void>}
   * @sideEffects 更新 homeRecommendList 响应式数组，管理 loadingHome / homeError 状态
   */
  async function loadHomeRecommend(limit = 8) {
    loadingHome.value = true
    homeError.value = ''
    try {
      const response = await getHomeRecommend(limit)
      ensureSuccessArray(response, '推荐数据加载失败')
      homeRecommendList.value = response.data.map(normalizeProduct)
      return homeRecommendList.value
    } catch (error) {
      homeError.value =
        error?.response?.data?.message || error?.message || '首页推荐加载失败'
      throw error
    } finally {
      loadingHome.value = false
    }
  }

  /**
   * 加载与指定商品关联购买的推荐商品列表。
   *
   * @param {string|number} productId - 当前商品 ID
   * @param {number} [limit=6] - 返回商品数量上限
   * @returns {Promise<void>}
   * @sideEffects 更新 alsoBoughtList 响应式数组，管理 loadingAlsoBought / alsoBoughtError 状态
   */
  async function loadAlsoBought(productId, limit = 6) {
    loadingAlsoBought.value = true
    alsoBoughtError.value = ''
    try {
      const response = await getAlsoBought(productId, limit)
      ensureSuccessArray(response, '关联推荐数据加载失败')
      alsoBoughtList.value = response.data.map(normalizeProduct)
      return alsoBoughtList.value
    } catch (error) {
      alsoBoughtError.value =
        error?.response?.data?.message || error?.message || '关联推荐加载失败'
      throw error
    } finally {
      loadingAlsoBought.value = false
    }
  }

  return {
    homeRecommendList,
    alsoBoughtList,
    loadingHome,
    homeError,
    loadingAlsoBought,
    alsoBoughtError,
    loadHomeRecommend,
    loadAlsoBought
  }
})
