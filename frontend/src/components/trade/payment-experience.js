const PAYMENT_METHODS = {
  mock: {
    label: '快捷支付',
    description: '确认后即可完成付款，适合当前交易流程。',
    confirmLocally: true
  },
  alipay_sandbox: {
    label: '支付宝',
    description: '创建支付单后，请使用支付宝扫码或打开付款页面完成支付。',
    confirmLocally: false
  }
}

const ATTEMPT_STATES = {
  initiated: {
    label: '等待付款',
    tone: 'warning',
    retryable: false,
    description: '支付单已创建，请完成付款。'
  },
  success: {
    label: '支付成功',
    tone: 'success',
    retryable: false,
    description: '付款已确认，可以返回订单继续查看履约进度。'
  },
  failed: {
    label: '支付失败',
    tone: 'danger',
    retryable: true,
    description: '本次付款没有完成，请重新发起支付。'
  },
  cancelled: {
    label: '支付已取消',
    tone: 'muted',
    retryable: true,
    description: '本次付款已取消，如仍需购买可以重新发起支付。'
  },
  timed_out: {
    label: '支付已超时',
    tone: 'muted',
    retryable: true,
    description: '支付单已过期，请重新发起支付。'
  }
}

export function getPaymentMethodMeta(paymentMethod) {
  return (
    PAYMENT_METHODS[paymentMethod] || {
      label: '在线支付',
      description: '创建支付单后，请按照页面提示完成付款。',
      confirmLocally: false
    }
  )
}

export function getPaymentAttemptMeta(paymentStatus) {
  return (
    ATTEMPT_STATES[paymentStatus] || {
      label: paymentStatus ? '状态更新中' : '尚未创建',
      tone: 'info',
      retryable: false,
      description: paymentStatus ? '正在同步付款结果，请稍后刷新。' : '选择支付方式后创建支付单。'
    }
  )
}

export function latestPayment(payments = []) {
  return payments.length ? payments[payments.length - 1] : null
}

export function availablePaymentMethods(gateway) {
  return Array.isArray(gateway?.availableMethods) ? gateway.availableMethods : []
}
