export const appNavigation = [
  { label: '首页', path: '/app/home' },
  { label: '探索', path: '/app/explore' },
  { label: '消息', path: '/app/messages', auth: true },
  { label: '交易', path: '/app/trade', auth: true },
  { label: '我的', path: '/app/me', auth: true }
]

export const mobileBottomNavigation = [
  { label: '首页', path: '/app/home', navKey: '/app/home', icon: '⌂' },
  { label: '探索', path: '/app/explore', navKey: '/app/explore', icon: '◇' },
  { label: '消息', path: '/app/messages', navKey: '/app/messages', icon: '✉', auth: true },
  { label: '交易', path: '/app/trade', navKey: '/app/trade', icon: '▣', auth: true },
  { label: '我的', path: '/app/me', navKey: '/app/me', icon: '○', auth: true }
]

export const adminNavigation = [
  { label: '后台首页', path: '/admin/dashboard' },
  { label: '用户管理', path: '/admin/users' },
  { label: '认证管理', path: '/admin/verifications' },
  { label: '商品管理', path: '/admin/products' },
  { label: '资料审核', path: '/admin/review-tasks' },
  { label: '店铺管理', path: '/admin/shops' },
  { label: '订单管理', path: '/admin/orders' },
  { label: '举报处理', path: '/admin/reports' },
  { label: '热搜治理', path: '/admin/hot-search' },
  { label: '客服治理', path: '/admin/support' }
]
