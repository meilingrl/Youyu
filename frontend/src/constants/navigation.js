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
  { label: '治理总览', path: '/admin/dashboard' },
  { label: '用户与身份', path: '/admin/users' },
  { label: '学生认证队列', path: '/admin/verifications' },
  { label: '商品治理', path: '/admin/products' },
  { label: '资料审核队列', path: '/admin/review-tasks' },
  { label: '店铺准入', path: '/admin/shops' },
  { label: '订单履约', path: '/admin/orders' },
  { label: '举报处置', path: '/admin/reports' },
  { label: '搜索治理', path: '/admin/hot-search' },
  { label: '支持上下文', path: '/admin/support' }
]
