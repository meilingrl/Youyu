import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useRecommendStore } from '../recommend'
import { getHomeRecommend, getAlsoBought } from '@/api/modules/recommend'

vi.mock('@/api/modules/recommend', () => ({
  getHomeRecommend: vi.fn(),
  getAlsoBought: vi.fn()
}))

function setupStore() {
  setActivePinia(createPinia())
  return useRecommendStore()
}

describe('recommend store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads home recommendations successfully', async () => {
    getHomeRecommend.mockResolvedValue({
      success: true,
      data: [
        { id: 3001, title: 'Advanced Math', salePrice: 45, source: 'popularity' },
        { id: 3002, title: 'Linear Algebra', salePrice: 30, source: 'popularity' }
      ]
    })

    const store = setupStore()
    const result = await store.loadHomeRecommend(4)

    expect(result).toHaveLength(2)
    expect(store.homeRecommendList).toHaveLength(2)
    expect(store.homeError).toBe('')
    expect(store.loadingHome).toBe(false)
  })

  it('sets error when home recommend fails', async () => {
    getHomeRecommend.mockResolvedValue({
      success: false,
      message: '服务异常'
    })

    const store = setupStore()
    await expect(store.loadHomeRecommend()).rejects.toThrow('服务异常')
    expect(store.homeError).toBe('服务异常')
  })

  it('loads also-bought recommendations successfully', async () => {
    getAlsoBought.mockResolvedValue({
      success: true,
      data: [
        { id: 3003, title: 'Calculus Notes', salePrice: 25, coPurchaseCount: 3, source: 'also-bought' }
      ]
    })

    const store = setupStore()
    const result = await store.loadAlsoBought(3001, 6)

    expect(result).toHaveLength(1)
    expect(store.alsoBoughtList).toHaveLength(1)
    expect(store.alsoBoughtError).toBe('')
  })

  it('sets error when also-bought fails', async () => {
    getAlsoBought.mockRejectedValue(new Error('网络错误'))

    const store = setupStore()
    await expect(store.loadAlsoBought(9999)).rejects.toThrow('网络错误')
    expect(store.alsoBoughtError).toBe('网络错误')
  })
})
