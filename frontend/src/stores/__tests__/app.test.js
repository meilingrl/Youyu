import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAppStore } from '../app'

function setupStore() {
  setActivePinia(createPinia())
  return useAppStore()
}

describe('app store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('hasKeyword is false when keyword is empty', () => {
    const store = setupStore()
    expect(store.keyword).toBe('')
    expect(store.hasKeyword).toBe(false)
  })

  it('hasKeyword is true after setKeyword with non-blank text', () => {
    const store = setupStore()
    store.setKeyword('calculus notes')
    expect(store.keyword).toBe('calculus notes')
    expect(store.hasKeyword).toBe(true)
  })

  it('setKeyword with whitespace-only value sets hasKeyword to false', () => {
    const store = setupStore()
    store.setKeyword('   ')
    expect(store.hasKeyword).toBe(false)
  })

  it('collapsed starts false and toggles correctly', () => {
    const store = setupStore()
    expect(store.collapsed).toBe(false)
    store.toggleCollapsed()
    expect(store.collapsed).toBe(true)
    store.toggleCollapsed()
    expect(store.collapsed).toBe(false)
  })

  it('keyword state updates via setKeyword', () => {
    const store = setupStore()
    store.setKeyword('dorm lamp')
    expect(store.keyword).toBe('dorm lamp')
    store.setKeyword('')
    expect(store.keyword).toBe('')
    expect(store.hasKeyword).toBe(false)
  })
})
