import { describe, expect, it } from 'vitest'
import {
  isValidEntityId,
  normalizeEntityId,
  parseOptionalEntityId,
  sanitizeRequestParams
} from '../id-utils'

describe('id-utils', () => {
  it('accepts positive integer ids', () => {
    expect(isValidEntityId(3001)).toBe(true)
    expect(isValidEntityId('3001')).toBe(true)
    expect(normalizeEntityId(' 42 ')).toBe('42')
  })

  it('rejects invalid literals and non-numeric values', () => {
    expect(isValidEntityId('NaN')).toBe(false)
    expect(isValidEntityId('undefined')).toBe(false)
    expect(isValidEntityId('null')).toBe(false)
    expect(isValidEntityId('abc')).toBe(false)
    expect(isValidEntityId('')).toBe(false)
    expect(isValidEntityId(0)).toBe(false)
    expect(isValidEntityId(-1)).toBe(false)
    expect(normalizeEntityId('NaN')).toBeNull()
  })

  it('parses optional ids only when valid', () => {
    expect(parseOptionalEntityId('12')).toBe(12)
    expect(parseOptionalEntityId('NaN')).toBeUndefined()
    expect(parseOptionalEntityId('')).toBeUndefined()
  })

  it('removes invalid numeric query params', () => {
    expect(
      sanitizeRequestParams({
        keyword: 'math',
        categoryId: NaN,
        page: 2,
        shopId: 'NaN'
      })
    ).toEqual({
      keyword: 'math',
      page: 2
    })
  })
})
