export const mockCategories = [
  { id: 'c1', name: '教材教辅', icon: 'Reading' },
  { id: 'c2', name: '数码设备', icon: 'Monitor' },
  { id: 'c3', name: '资料文档', icon: 'Document' },
  { id: 'c4', name: '宿舍生活', icon: 'House' },
  { id: 'c5', name: '竞赛服务', icon: 'Opportunity' }
]

export const mockShops = [
  {
    id: 's1',
    ownerId: 'u1001',
    name: '研途资料小站',
    slogan: '整理过的课程资料，给复习留出更多时间。',
    description:
      '主营课程笔记、真题整理和答辩模板，当前保持轻量店铺展示，不涉及店铺运营深度配置。',
    status: 'active',
    rating: 4.8,
    followers: 126,
    creditLevel: 'L3 优质卖家',
    responseRate: '92%',
    createdAt: '2026-03-02',
    announcement: '资料类商品默认先通过平台审核后展示完整下载权益。'
  },
  {
    id: 's2',
    ownerId: 'u2001',
    name: '宿舍数码交换角',
    slogan: '二手设备快速流转，线下验货更安心。',
    description: '偏向数码和宿舍电器，支持校内见面交付与物流寄送。',
    status: 'active',
    rating: 4.6,
    followers: 81,
    creditLevel: 'L2 稳定交易者',
    responseRate: '88%',
    createdAt: '2025-11-15',
    announcement: '工作日晚上 7 点后回复更快。'
  }
]

export const mockProducts = [
  {
    id: 'p1001',
    sellerId: 'u1001',
    shopId: 's1',
    title: '高数期末复习资料包',
    subtitle: '笔记梳理 + 高频题型 + 重点公式清单',
    description:
      '适合期末冲刺使用，资料包含章节速记、历年题型归纳与错题整理建议。资料类商品需审核后上架，当前保留预览入口。',
    detail:
      '资料按照章节拆分，支持局部预览。购买后完整下载链路由订单与支付模块接管，当前前台仅展示入口与状态位。',
    categoryId: 'c3',
    categoryName: '资料文档',
    type: 'digital',
    status: 'on_sale',
    reviewStatus: 'approved',
    price: 19.9,
    originalPrice: 29.9,
    stock: 99,
    cover:
      'https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=900&q=80',
    media: [
      'https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80'
    ],
    scenarioTags: ['期末复习', '数学基础'],
    deliveryMethods: ['digital_delivery'],
    allowPreview: true,
    previewLabel: '预览前 15 页',
    previewHint: '当前仅开放局部预览，完整下载需待订单支付链路接入。',
    sales: 87,
    favoriteCount: 136,
    publishedAt: '2026-05-06',
    sellerName: '林学姐',
    shopName: '研途资料小站'
  },
  {
    id: 'p1002',
    sellerId: 'u2001',
    shopId: 's2',
    title: '二手显示器 24 寸',
    subtitle: '宿舍自提优先，附 HDMI 线',
    description: '成色良好，支持线下验货，也可以走校内物流。',
    detail: '更适合宿舍、实验室或临时工位使用，出手原因是换了更大的屏幕。',
    categoryId: 'c2',
    categoryName: '数码设备',
    type: 'physical',
    status: 'on_sale',
    reviewStatus: 'not_required',
    price: 320,
    originalPrice: 499,
    stock: 1,
    cover:
      'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=900&q=80',
    media: [
      'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1496171367470-9ed9a91ea931?auto=format&fit=crop&w=900&q=80'
    ],
    scenarioTags: ['宿舍桌搭', '数码换新'],
    deliveryMethods: ['logistics', 'offline_face_to_face'],
    allowPreview: false,
    previewLabel: '',
    previewHint: '',
    sales: 12,
    favoriteCount: 28,
    publishedAt: '2026-05-08',
    sellerName: '赵同学',
    shopName: '宿舍数码交换角'
  },
  {
    id: 'p1003',
    sellerId: 'u1001',
    shopId: 's1',
    title: '毕业答辩 PPT 模板合集',
    subtitle: '简洁学术风，附答辩结构建议',
    description: '适合本科毕设答辩前快速改稿，含封面、目录、方法、实验结果等常用页面。',
    detail: '支持预览缩略页，完整源文件下载能力后续由支付 Agent 接入。',
    categoryId: 'c3',
    categoryName: '资料文档',
    type: 'digital',
    status: 'on_sale',
    reviewStatus: 'approved',
    price: 12.8,
    originalPrice: 18,
    stock: 999,
    cover:
      'https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=900&q=80',
    media: [
      'https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=900&q=80'
    ],
    scenarioTags: ['毕业答辩', 'PPT 模板'],
    deliveryMethods: ['digital_delivery'],
    allowPreview: true,
    previewLabel: '预览封面与目录页',
    previewHint: '完整源文件将在支付完成后开放。',
    sales: 41,
    favoriteCount: 73,
    publishedAt: '2026-05-03',
    sellerName: '林学姐',
    shopName: '研途资料小站'
  },
  {
    id: 'p1004',
    sellerId: 'u1001',
    shopId: 's1',
    title: '校园摄影约拍基础服务',
    subtitle: '毕业季单人简拍，支持线下面交确认',
    description: '属于服务增强型商品，当前仅展示服务说明与交付方式占位。',
    detail: '服务时间、地点与确认机制需要订单 Agent 后续接管。',
    categoryId: 'c5',
    categoryName: '竞赛服务',
    type: 'service',
    status: 'on_sale',
    reviewStatus: 'not_required',
    price: 88,
    originalPrice: 128,
    stock: 8,
    cover:
      'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=900&q=80',
    media: [
      'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=900&q=80'
    ],
    scenarioTags: ['毕业季', '约拍'],
    deliveryMethods: ['offline_face_to_face', 'digital_delivery'],
    allowPreview: false,
    previewLabel: '',
    previewHint: '',
    sales: 5,
    favoriteCount: 18,
    publishedAt: '2026-05-04',
    sellerName: '林学姐',
    shopName: '研途资料小站'
  }
]

export const mockUserProfile = {
  id: 'u1001',
  loginId: 'demo-user',
  nickname: '校园用户',
  avatar:
    'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=400&q=80',
  school: '东北大学',
  campus: '浑南校区',
  major: '软件工程',
  grade: '2023 级',
  bio: '平时会整理课程资料，也会发布少量宿舍闲置。',
  creditLevel: 'L2 稳定交易者',
  creditScoreText: '近 90 天无违规，累计完成 6 笔有效交易',
  verification: {
    status: 'approved',
    label: '已认证学生',
    studentNo: '2023123456',
    submittedAt: '2026-04-01',
    reviewedAt: '2026-04-03',
    rejectReason: ''
  },
  privilege: {
    canBuy: true,
    canPublish: true,
    canApplyShop: true,
    canReview: true
  },
  addresses: [
    {
      id: 'a1',
      contactName: '校园用户',
      phone: '13800000000',
      region: '辽宁省 沈阳市',
      detail: '浑南校区 2 号宿舍楼 402',
      type: '宿舍收货',
      isDefault: true
    }
  ]
}

export const mockFavorites = ['p1001', 'p1003']

export const mockVerificationTemplate = {
  realName: '',
  studentNo: '',
  college: '',
  major: '',
  grade: '',
  campusEmail: '',
  verifyMethod: 'campus_email',
  note: ''
}
