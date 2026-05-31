export const preferenceOptions = {
  themeMode: [
    { label: '跟随系统', value: 'system' },
    { label: '日间模式', value: 'light' },
    { label: '夜间模式', value: 'dark' }
  ],
  themeColor: [
    { label: '校园蓝', value: 'campus_blue', color: '#0071e3' },
    { label: '清新青', value: 'fresh_cyan', color: '#0891b2' },
    { label: '暖橙', value: 'warm_orange', color: '#f97316' }
  ],
  homeDisplayMode: [
    { label: '卡片展示', value: 'card' },
    { label: '列表展示', value: 'list' }
  ],
  defaultSortType: [
    { label: '综合排序', value: 'comprehensive' },
    { label: '最新发布', value: 'latest' },
    { label: '价格优先', value: 'price' },
    { label: '收藏优先', value: 'favorite' }
  ],
  defaultFulfillmentType: [
    { label: '不限', value: 'any' },
    { label: '线下交付', value: 'offline' },
    { label: '快递物流', value: 'logistics' },
    { label: '电子交付', value: 'digital' }
  ],
  defaultPaymentMethod: [
    { label: '平台支付', value: 'mock_payment' },
    { label: '后续选择', value: 'choose_later' }
  ]
}

export const defaultUserPreference = {
  themeMode: 'system',
  themeColor: 'campus_blue',
  homeDisplayMode: 'card',
  defaultSortType: 'comprehensive',
  defaultAddressId: '',
  defaultFulfillmentType: 'any',
  defaultPaymentMethod: 'mock_payment',
  notificationPreference: {
    orderReminder: true,
    reviewReminder: true
  }
}

export const userInsightMetricDefinitions = [
  {
    key: 'totalSpendAmount',
    label: '累计消费金额',
    scope: '已完成或已支付订单的用户侧金额汇总',
    dataSource: 'Order.payAmount'
  },
  {
    key: 'totalPurchasedItemCount',
    label: '已购件数',
    scope: '用户订单明细中的商品数量汇总',
    dataSource: 'OrderItem.quantity'
  },
  {
    key: 'recentBrowses',
    label: '最近浏览',
    scope: '按 viewedAt 倒序展示最近浏览商品',
    dataSource: 'BrowseHistory'
  },
  {
    key: 'favoritePreferenceSummary',
    label: '收藏偏好',
    scope: '按收藏商品分类聚合',
    dataSource: 'ProductFavorite + Product.categoryId'
  }
]

export const shopInsightMetricDefinitions = [
  {
    key: 'monthlySalesAmount',
    label: '本月销售额',
    scope: '自然月内已支付或已完成订单的店铺侧实付金额',
    dataSource: 'Order.payAmount'
  },
  {
    key: 'monthlyOrderCount',
    label: '本月订单数',
    scope: '自然月内店铺订单数量',
    dataSource: 'Order'
  },
  {
    key: 'viewCountSummary',
    label: '浏览量摘要',
    scope: '店铺及商品详情页的累计访问次数汇总',
    dataSource: 'ShopView / ProductView 埋点'
  },
  {
    key: 'favoriteCountSummary',
    label: '收藏量摘要',
    scope: '店铺内商品的累计收藏次数汇总',
    dataSource: 'ProductFavorite'
  },
  {
    key: 'hotProducts',
    label: '热销商品',
    scope: '按销量或订单明细数量排序的店铺商品',
    dataSource: 'OrderItem + Product'
  }
]
