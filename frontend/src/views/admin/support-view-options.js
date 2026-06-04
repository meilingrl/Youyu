export const POLL_INTERVAL = 8000

export const workspaces = [
  {
    key: 'chat',
    title: '在线客服',
    eyebrow: '实时接待',
    description: '用于处理升级到人工的在线客服会话，由 /api/admin/support/chat/** 提供支持。'
  },
  {
    key: 'tickets',
    title: '支持工单',
    eyebrow: '异步工单',
    description: '用于承接用户创建的客服工单，由 /api/admin/support/tickets/** 提供支持。'
  }
]

export const workspaceDisplayMeta = {
  chat: {
    title: '在线客服',
    eyebrow: '实时接待',
    description: '接待需要人工跟进的在线咨询，快速响应当前用户会话。'
  },
  tickets: {
    title: '支持工单',
    eyebrow: '持续跟进',
    description: '处理用户提交的客服工单，统一跟进补充材料、进度更新和处理结论。'
  }
}

export const chatFilterOptions = [
  { value: 'pending', label: '待接入' },
  { value: 'active', label: '处理中' },
  { value: 'mine', label: '我负责的' },
  { value: 'closed', label: '已结束' }
]

export const ticketStatusOptions = [
  { value: '', label: '全部状态' },
  { value: 'open', label: '待受理' },
  { value: 'in_progress', label: '处理中' },
  { value: 'waiting_user', label: '待用户补充' },
  { value: 'resolved', label: '已解决' },
  { value: 'closed', label: '已关闭' }
]

export const ticketCategoryOptions = [
  { value: '', label: '全部分类' },
  { value: 'account', label: '账号' },
  { value: 'order', label: '订单' },
  { value: 'product', label: '商品' },
  { value: 'shop', label: '店铺' },
  { value: 'payment', label: '支付' },
  { value: 'report', label: '举报' },
  { value: 'other', label: '其他' }
]

export const ticketMessageTypeOptions = [
  { value: 'public_reply', label: '公开回复' },
  { value: 'internal_note', label: '内部备注' }
]

export const statusMeta = {
  ai: { label: '智能客服', type: 'info' },
  pending: { label: '待接入', type: 'warning' },
  human: { label: '进行中', type: 'primary' },
  closed: { label: '已结束', type: 'info' },
  open: { label: '待受理', type: 'warning' },
  in_progress: { label: '处理中', type: 'primary' },
  waiting_user: { label: '待用户补充', type: 'danger' },
  resolved: { label: '已解决', type: 'success' }
}
