import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getStorage, setStorage } from '@/utils/storage'
import { getHotSearchList, getSearchSuggestions } from '@/api/modules/search'

const SEARCH_HISTORY_KEY = 'campus-market-search-history'
const HISTORY_LIMIT = 8
const SUGGESTION_LIMIT = 8

function normalizeKeyword(keyword) {
  const cleaned = String(keyword || '').trim().replaceAll(/\s+/g, ' ')
  if (!cleaned) {
    return ''
  }
  if (!Array.from(cleaned).some((char) => /[\p{L}\p{N}]/u.test(char))) {
    return ''
  }
  return cleaned
}

export const useSearchStore = defineStore('search', () => {
  const hotKeywords = ref([])
  const loadingHotKeywords = ref(false)
  const hotKeywordError = ref('')
  const searchHistory = ref(getStorage(SEARCH_HISTORY_KEY, []))
  const suggestions = ref([])
  const loadingSuggestions = ref(false)
  const suggestionError = ref('')
  let suggestionRequestId = 0

  async function loadHotKeywords() {
    loadingHotKeywords.value = true
    hotKeywordError.value = ''
    try {
      const response = await getHotSearchList()
      if (!response || response.success !== true || !Array.isArray(response.data)) {
        throw new Error(response?.message || '热搜数据加载失败')
      }
      hotKeywords.value = response.data
      return hotKeywords.value
    } catch (error) {
      hotKeywordError.value =
        error?.response?.data?.message || error?.message || '热搜数据加载失败'
      throw error
    } finally {
      loadingHotKeywords.value = false
    }
  }

  function rememberKeyword(keyword) {
    const normalized = normalizeKeyword(keyword)
    if (!normalized) {
      return searchHistory.value
    }
    searchHistory.value = [
      normalized,
      ...searchHistory.value.filter((item) => item !== normalized)
    ].slice(0, HISTORY_LIMIT)
    setStorage(SEARCH_HISTORY_KEY, searchHistory.value)
    return searchHistory.value
  }

  async function loadSuggestions(keyword, options = {}) {
    const normalized = normalizeKeyword(keyword)
    const limit = Math.min(
      SUGGESTION_LIMIT,
      Math.max(1, Number(options.limit) || SUGGESTION_LIMIT)
    )

    if (!normalized) {
      clearSuggestions()
      return []
    }

    const requestId = ++suggestionRequestId
    loadingSuggestions.value = true
    suggestionError.value = ''

    try {
      const response = await getSearchSuggestions({ q: normalized, limit })
      if (!response || response.success !== true || !Array.isArray(response.data)) {
        throw new Error(response?.message || '搜索建议加载失败')
      }
      if (requestId !== suggestionRequestId) {
        return suggestions.value
      }
      suggestions.value = response.data
      return suggestions.value
    } catch (error) {
      if (requestId === suggestionRequestId) {
        suggestions.value = []
        suggestionError.value =
          error?.response?.data?.message || error?.message || '搜索建议加载失败'
      }
      throw error
    } finally {
      if (requestId === suggestionRequestId) {
        loadingSuggestions.value = false
      }
    }
  }

  function clearSuggestions() {
    suggestionRequestId += 1
    suggestions.value = []
    loadingSuggestions.value = false
    suggestionError.value = ''
  }

  return {
    hotKeywords,
    loadingHotKeywords,
    hotKeywordError,
    searchHistory,
    suggestions,
    loadingSuggestions,
    suggestionError,
    loadHotKeywords,
    rememberKeyword,
    loadSuggestions,
    clearSuggestions
  }
})
