import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useSearchStore } from '../search'
import { getHotSearchList, getSearchSuggestions } from '@/api/modules/search'

vi.mock('@/api/modules/search', () => ({
  getHotSearchList: vi.fn(),
  getSearchSuggestions: vi.fn()
}))

function setupStore() {
  setActivePinia(createPinia())
  return useSearchStore()
}

describe('search store', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('loads hot keywords from the API', async () => {
    getHotSearchList.mockResolvedValue({
      success: true,
      data: [{ keyword: 'calculus', searchCount: 3, score: 2.55 }]
    })

    const store = setupStore()
    const result = await store.loadHotKeywords()

    expect(result).toEqual([{ keyword: 'calculus', searchCount: 3, score: 2.55 }])
    expect(store.hotKeywords).toHaveLength(1)
    expect(store.hotKeywordError).toBe('')
  })

  it('stores recent meaningful keywords without duplicates', () => {
    const store = setupStore()

    store.rememberKeyword('  Calculus Notes ')
    store.rememberKeyword('!!!')
    store.rememberKeyword('Calculus Notes')
    store.rememberKeyword('Dorm lamp')

    expect(store.searchHistory).toEqual(['Dorm lamp', 'Calculus Notes'])
  })

  it('loads suggestions and clears them for blank input', async () => {
    getSearchSuggestions.mockResolvedValue({
      success: true,
      data: [{ keyword: 'Advanced Math', normalizedKeyword: 'advanced math', searchCount: 3, score: 2.55 }]
    })

    const store = setupStore()
    const result = await store.loadSuggestions('  Advanced ')

    expect(result).toHaveLength(1)
    expect(getSearchSuggestions).toHaveBeenCalledWith({ q: 'Advanced', limit: 8 })
    expect(store.suggestions[0].normalizedKeyword).toBe('advanced math')

    const emptyResult = await store.loadSuggestions('!!!')
    expect(emptyResult).toEqual([])
    expect(store.suggestions).toEqual([])
  })

  it('keeps only the latest suggestion response', async () => {
    let resolveFirst
    let resolveSecond
    getSearchSuggestions
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveFirst = resolve
          })
      )
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveSecond = resolve
          })
      )

    const store = setupStore()
    const firstPromise = store.loadSuggestions('Adv')
    const secondPromise = store.loadSuggestions('Dorm')

    resolveSecond({
      success: true,
      data: [{ keyword: 'Dorm Lamp', normalizedKeyword: 'dorm lamp', searchCount: 2, score: 1.5 }]
    })
    await secondPromise

    resolveFirst({
      success: true,
      data: [{ keyword: 'Advanced Math', normalizedKeyword: 'advanced math', searchCount: 5, score: 4 }]
    })
    await firstPromise

    expect(store.suggestions).toEqual([
      { keyword: 'Dorm Lamp', normalizedKeyword: 'dorm lamp', searchCount: 2, score: 1.5 }
    ])
  })
})
