const LABELS = {
  active: '正常',
  disabled: '已禁用',
  locked: '已锁定',
  draft: '草稿',
  on_sale: '在售',
  off_sale: '已下架',
  closed: '已关闭',
  pending_review: '待审核',
  approved: '已通过',
  rejected: '已驳回',
  not_required: '无需审核',
  inactive: '未启用',
  pending: '待处理',
  processing: '处理中',
  resolved: '已解决',
  opened: '已开启',
  evidence_review: '证据核查',
  decision_pending: '待决策',
  cancelled: '已取消',
  paid: '已支付',
  unpaid: '未支付',
  completed: '已完成',
  failed: '失败',
  refunded: '已退款',
  pending_fulfillment: '待履约',
  pending_receipt: '待收货',
  refunding: '退款中',
  logistics: '物流配送',
  offline: '线下交付',
  digital: '数字商品',
  digital_fulfillment: '数字交付',
  physical: '实物商品',
  service: '服务商品',
  digital_product: '资料商品',
  user: '用户',
  admin: '管理员',
  super_admin: '超级管理员',
  support_agent: '客服专员',
  reviewer: '审核员',
  operator: '运营员',
  order_admin: '订单管理员',
  product: '商品',
  shop: '店铺',
  order: '订单',
  digital_order: '数字订单',
  refund_full_to_buyer: '全额退款给买家',
  refund_rejected_release_to_seller: '驳回退款并放款给卖家',
  order_completion_required: '要求完成订单',
  platform_governance_action: '平台治理处置',
  no_action_invalid_or_duplicate: '无效或重复申请',
  SENSITIVE_WORD: '敏感词',
  STOP_WORD: '停用词',
  HIDE_KEYWORD: '隐藏关键词',
  PIN_KEYWORD: '置顶关键词'
}

const TAG_TYPES = {
  active: 'success',
  on_sale: 'success',
  approved: 'success',
  resolved: 'success',
  completed: 'success',
  paid: 'success',
  pending_review: 'warning',
  pending: 'warning',
  processing: 'warning',
  opened: 'warning',
  evidence_review: 'warning',
  decision_pending: 'warning',
  pending_fulfillment: 'warning',
  pending_receipt: 'warning',
  refunding: 'warning',
  disabled: 'danger',
  locked: 'danger',
  rejected: 'danger',
  failed: 'danger',
  cancelled: 'info',
  off_sale: 'info',
  inactive: 'info',
  closed: 'info'
}

export function adminLabel(value, fallback = '未填写') {
  if (value === null || value === undefined || value === '') return fallback
  const raw = String(value)
  return LABELS[raw] || LABELS[raw.toLowerCase()] || raw
}

export function adminTagType(value) {
  return TAG_TYPES[value] || TAG_TYPES[String(value || '').toLowerCase()] || 'info'
}

export function optionLabel(value) {
  return adminLabel(value, value)
}
