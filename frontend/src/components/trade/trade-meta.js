export const TRADE_ROUTES = [
  {
    key: 'trade',
    title: '交易中心',
    eyebrow: 'Overview',
    path: '/app/trade',
    description: '回到交易总览，快速进入购物车、订单、售后与评价。'
  },
  {
    key: 'cart',
    title: '购物车',
    eyebrow: 'Cart',
    path: '/app/cart',
    description: '确认本次准备结算的商品。'
  },
  {
    key: 'checkout',
    title: '结算确认',
    eyebrow: 'Checkout',
    path: '/app/checkout',
    description: '补齐履约信息，生成订单。',
    flowOnly: true
  },
  {
    key: 'payment',
    title: '支付确认',
    eyebrow: 'Payment',
    path: '',
    description: '确认支付并返回订单继续跟进。',
    flowOnly: true
  },
  {
    key: 'orders',
    title: '订单与售后',
    eyebrow: 'Orders',
    path: '/app/orders',
    description: '查看进度、退款、举报与收货。'
  },
  {
    key: 'reviews-pending',
    title: '待评价',
    eyebrow: 'Review',
    path: '/app/reviews/pending',
    description: '完成交易后继续补齐评价。'
  },
  {
    key: 'reviews-mine',
    title: '我的评价',
    eyebrow: 'History',
    path: '/app/reviews/mine',
    description: '回看已经提交的商品与店铺评价。'
  }
]

const ORDER_STATUS_META = {
  pending_payment: {
    label: '待支付',
    tone: 'warning',
    description: '订单已创建，等待完成支付。'
  },
  pending_fulfillment: {
    label: '已支付',
    tone: 'info',
    description: '已付款，等待卖家履约。'
  },
  pending_receipt: {
    label: '待收货',
    tone: 'primary',
    description: '卖家已履约，等待确认收货。'
  },
  completed: {
    label: '已完成',
    tone: 'success',
    description: '交易已完成，可以继续评价。'
  },
  cancelled: {
    label: '已取消',
    tone: 'muted',
    description: '订单已关闭，不再继续流转。'
  },
  refunding: {
    label: '退款中',
    tone: 'danger',
    description: '退款申请处理中，请关注平台反馈。'
  },
  refund_in_progress: {
    label: '退款中',
    tone: 'danger',
    description: '退款申请处理中，请关注平台反馈。'
  },
  refunded: {
    label: '退款完成',
    tone: 'success',
    description: '退款流程已完成。'
  }
}

const PAYMENT_STATUS_META = {
  unpaid: {
    label: '待支付',
    tone: 'warning'
  },
  paid: {
    label: '已支付',
    tone: 'success'
  },
  refunded: {
    label: '已退款',
    tone: 'muted'
  },
  refunding: {
    label: '退款中',
    tone: 'danger'
  }
}

const FULFILLMENT_TYPE_META = {
  logistics: {
    label: '快递物流'
  },
  offline: {
    label: '线下交付'
  },
  digital: {
    label: '数字交付'
  }
}

export function getOrderStatusMeta(status) {
  const normalizedStatus = status === 'refund_in_progress' ? 'refunding' : status
  return (
    ORDER_STATUS_META[normalizedStatus] || {
      label: normalizedStatus || '未知状态',
      tone: 'muted',
      description: '当前交易状态暂未收录。'
    }
  )
}

export function getPaymentStatusMeta(status) {
  return (
    PAYMENT_STATUS_META[status] || {
      label: status || '未知状态',
      tone: 'muted'
    }
  )
}

export function getFulfillmentTypeMeta(type) {
  return (
    FULFILLMENT_TYPE_META[type] || {
      label: type || '未知方式'
    }
  )
}

export function formatCurrency(amount) {
  return `¥${Number(amount || 0).toFixed(2)}`
}

export function countOrdersByStatus(orders = []) {
  const counters = {
    all: orders.length,
    pending_payment: 0,
    pending_fulfillment: 0,
    pending_receipt: 0,
    completed: 0,
    refunding: 0,
    refund_in_progress: 0,
    refunded: 0
  }

  for (const order of orders) {
    const status = order?.orderStatus === 'refund_in_progress' ? 'refunding' : order?.orderStatus
    if (status && counters[status] !== undefined) {
      counters[status] += 1
      if (status === 'refunding') {
        counters.refund_in_progress += 1
      }
    }
  }

  return counters
}

export function getTradeNavRoutes(currentKey, routes = TRADE_ROUTES) {
  const isFlowPage = routes.some((route) => route.key === currentKey && route.flowOnly)
  return routes.filter((route) => {
    if (!route.flowOnly) {
      return true
    }
    return route.key === currentKey || isFlowPage
  })
}
