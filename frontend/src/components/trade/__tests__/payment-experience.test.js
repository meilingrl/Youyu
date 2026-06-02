import { describe, expect, it } from 'vitest'
import {
  availablePaymentMethods,
  getPaymentAttemptMeta,
  getPaymentMethodMeta,
  latestPayment
} from '@/components/trade/payment-experience'

describe('payment experience helpers', () => {
  it('maps available methods to buyer-facing descriptions', () => {
    const localMethod = getPaymentMethodMeta('mock')
    const alipayMethod = getPaymentMethodMeta('alipay_sandbox')
    expect(localMethod).toMatchObject({
      label: '快捷支付',
      confirmLocally: true
    })
    expect(alipayMethod).toMatchObject({
      label: '支付宝',
      confirmLocally: false
    })
    expect(`${localMethod.label}${localMethod.description}${alipayMethod.label}${alipayMethod.description}`).not.toMatch(
      /mock|test|sandbox|gateway|internal/i
    )
  })

  it.each(['failed', 'cancelled', 'timed_out'])('allows retry after %s attempts', (status) => {
    expect(getPaymentAttemptMeta(status).retryable).toBe(true)
  })

  it('uses the latest attempt and guards malformed gateway payloads', () => {
    expect(latestPayment([{ paymentNo: 'PAY-1' }, { paymentNo: 'PAY-2' }])).toEqual({ paymentNo: 'PAY-2' })
    expect(latestPayment()).toBeNull()
    expect(availablePaymentMethods()).toEqual([])
  })
})
