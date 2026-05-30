export default {
  path: '/app',
  component: () => import('@/layouts/AppLayout.vue'),
  redirect: '/app/home',
  meta: {
    section: 'app',
    role: ['guest', 'user', 'admin']
  },
  children: [
    {
      path: 'home',
      name: 'app-home',
      component: () => import('@/views/app/HomeView.vue'),
      meta: {
        title: '首页',
        public: true,
        navKey: '/app/home'
      }
    },
    {
      path: 'explore',
      name: 'app-explore',
      component: () => import('@/views/app/ProductListView.vue'),
      meta: {
        title: '探索',
        public: true,
        navKey: '/app/explore'
      }
    },
    {
      path: 'products',
      name: 'app-products',
      component: () => import('@/views/app/ProductListView.vue'),
      meta: {
        title: '探索',
        public: true,
        navKey: '/app/explore'
      }
    },
    {
      path: 'products/:id',
      name: 'app-product-detail',
      component: () => import('@/views/app/ProductDetailView.vue'),
      props: true,
      meta: {
        title: '商品详情',
        public: true,
        hiddenInNav: true
      }
    },
    {
      path: 'cart',
      name: 'app-cart',
      component: () => import('@/views/app/CartView.vue'),
      meta: {
        title: '购物车',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/trade'
      }
    },
    {
      path: 'checkout',
      name: 'app-checkout',
      component: () => import('@/views/app/CheckoutView.vue'),
      meta: {
        title: '下单确认',
        requiresAuth: true,
        role: 'user',
        hiddenInNav: true
      }
    },
    {
      path: 'payments/:orderId',
      name: 'app-payment',
      component: () => import('@/views/app/PaymentView.vue'),
      meta: {
        title: '模拟支付',
        requiresAuth: true,
        role: 'user',
        hiddenInNav: true
      }
    },
    {
      path: 'favorites',
      name: 'app-favorites',
      component: () => import('@/views/app/FavoritesView.vue'),
      meta: {
        title: '我的收藏',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/me'
      }
    },
    {
      path: 'coupons',
      name: 'app-coupons',
      component: () => import('@/views/app/BuyerCouponsView.vue'),
      meta: {
        title: '我的优惠券',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/me'
      }
    },
    {
      path: 'shops/:id',
      name: 'app-shop',
      component: () => import('@/views/app/ShopView.vue'),
      props: true,
      meta: {
        title: '店铺主页',
        public: true,
        hiddenInNav: true
      }
    },
    {
      path: 'shop/manage/products',
      name: 'shop-manage-products',
      component: () => import('@/views/app/SellerProductsView.vue'),
      meta: {
        title: '店铺商品管理',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/me'
      }
    },
    {
      path: 'shop/manage/publish',
      name: 'shop-manage-publish',
      component: () => import('@/views/app/SellerPublishView.vue'),
      meta: {
        title: '发布商品',
        requiresAuth: true,
        role: 'user',
        hiddenInNav: true
      }
    },
    {
      path: 'seller/products',
      name: 'seller-products',
      component: () => import('@/views/app/SellerProductsView.vue'),
      meta: {
        title: '我的发布',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/me'
      }
    },
    {
      path: 'seller/publish',
      name: 'seller-publish',
      component: () => import('@/views/app/SellerPublishView.vue'),
      meta: {
        title: '发布商品',
        requiresAuth: true,
        role: 'user',
        hiddenInNav: true
      }
    },
    {
      path: 'seller/marketing',
      name: 'seller-marketing',
      component: () => import('@/views/app/SellerMarketingView.vue'),
      meta: {
        title: '店铺营销管理',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/me'
      }
    },
    {
      path: 'orders',
      name: 'app-orders',
      component: () => import('@/views/app/OrdersView.vue'),
      meta: {
        title: '我的订单',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/trade'
      }
    },
    {
      path: 'orders/:orderId',
      name: 'app-order-detail',
      component: () => import('@/views/app/OrderDetailView.vue'),
      meta: {
        title: '订单详情',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/trade',
        hiddenInNav: true
      }
    },
    {
      path: 'reviews/pending',
      name: 'app-reviews-pending',
      component: () => import('@/views/app/PendingReviewsView.vue'),
      meta: {
        title: '待评价',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/trade'
      }
    },
    {
      path: 'reviews/mine',
      name: 'app-reviews-mine',
      component: () => import('@/views/app/MyReviewsView.vue'),
      meta: {
        title: '我的评价',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/trade'
      }
    },
    {
      path: 'trade',
      name: 'app-trade',
      component: () => import('@/views/app/TradeView.vue'),
      meta: {
        title: '交易中心',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/trade'
      }
    },
    {
      path: 'messages',
      name: 'app-messages',
      component: () => import('@/views/app/MessagesView.vue'),
      meta: {
        title: '消息中心',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/messages'
      }
    },
    {
      path: 'support',
      name: 'app-support',
      component: () => import('@/views/app/SupportTicketsView.vue'),
      meta: {
        title: '平台客服工单',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/messages'
      }
    },
    {
      path: 'messages/:conversationId',
      name: 'app-message-detail',
      component: () => import('@/views/app/MessagesView.vue'),
      props: true,
      meta: {
        title: '消息会话',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/messages'
      }
    },
    {
      path: 'notifications',
      name: 'app-notifications',
      component: () => import('@/views/app/NotificationsView.vue'),
      meta: {
        title: '通知中心',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/messages'
      }
    },
    {
      path: 'me',
      name: 'app-me',
      component: () => import('@/views/app/ProfileView.vue'),
      meta: {
        title: '我的',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/me'
      }
    },
    {
      path: 'profile',
      name: 'app-profile',
      component: () => import('@/views/app/ProfileView.vue'),
      meta: {
        title: '个人中心',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/me'
      }
    },
    {
      path: 'settings',
      name: 'app-settings',
      component: () => import('@/views/app/SettingsView.vue'),
      meta: {
        title: '设置中心',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/settings'
      }
    },
    {
      path: 'settings/preferences',
      name: 'app-settings-preferences',
      component: () => import('@/views/app/PreferenceSettingsView.vue'),
      meta: {
        title: '偏好设置',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/settings'
      }
    },
    {
      path: 'settings/auto-reply',
      name: 'app-settings-auto-reply',
      component: () => import('@/views/app/SettingsAutoReplyView.vue'),
      meta: {
        title: '自动回复设置',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/settings'
      }
    },
    {
      path: 'preferences',
      name: 'app-preferences',
      component: () => import('@/views/app/PreferenceSettingsView.vue'),
      meta: {
        title: '偏好设置',
        requiresAuth: true,
        role: 'user',
        navKey: '/app/settings'
      }
    },
    {
      path: 'verification',
      name: 'app-verification',
      component: () => import('@/views/app/VerificationView.vue'),
      meta: {
        title: '学生认证申请',
        requiresAuth: true,
        role: 'user',
        hiddenInNav: true
      }
    }
  ]
}
