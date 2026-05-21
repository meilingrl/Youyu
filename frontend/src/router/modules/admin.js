export default {
  path: '/admin',
  component: () => import('@/layouts/AdminLayout.vue'),
  redirect: '/admin/dashboard',
  meta: {
    section: 'admin',
    requiresAuth: true,
    role: 'admin'
  },
  children: [
    {
      path: 'dashboard',
      name: 'admin-dashboard',
      component: () => import('@/views/admin/DashboardView.vue'),
      meta: {
        title: '后台首页',
        navKey: '/admin/dashboard'
      }
    },
    {
      path: 'users',
      name: 'admin-users',
      component: () => import('@/views/admin/UserManageView.vue'),
      meta: {
        title: '用户管理',
        navKey: '/admin/users'
      }
    },
    {
      path: 'verifications',
      name: 'admin-verifications',
      component: () => import('@/views/admin/VerificationManageView.vue'),
      meta: {
        title: '认证管理',
        navKey: '/admin/verifications'
      }
    },
    {
      path: 'products',
      name: 'admin-products',
      component: () => import('@/views/admin/ProductManageView.vue'),
      meta: {
        title: '商品管理',
        navKey: '/admin/products'
      }
    },
    {
      path: 'review-tasks',
      name: 'admin-review-tasks',
      component: () => import('@/views/admin/ReviewTaskManageView.vue'),
      meta: {
        title: '资料审核',
        navKey: '/admin/review-tasks'
      }
    },
    {
      path: 'shops',
      name: 'admin-shops',
      component: () => import('@/views/admin/ShopManageView.vue'),
      meta: {
        title: '店铺管理',
        navKey: '/admin/shops'
      }
    },
    {
      path: 'orders',
      name: 'admin-orders',
      component: () => import('@/views/admin/OrderManageView.vue'),
      meta: {
        title: '订单管理',
        navKey: '/admin/orders'
      }
    },
    {
      path: 'reports',
      name: 'admin-reports',
      component: () => import('@/views/admin/ReportManageView.vue'),
      meta: {
        title: '举报处理',
        navKey: '/admin/reports'
      }
    },
    {
      path: 'hot-search',
      name: 'admin-hot-search',
      component: () => import('@/views/admin/HotSearchGovernView.vue'),
      meta: {
        title: '热搜治理',
        navKey: '/admin/hot-search'
      }
    },
    {
      path: 'support',
      name: 'admin-support',
      component: () => import('@/views/admin/SupportView.vue'),
      meta: {
        title: '客服与消息治理',
        navKey: '/admin/support'
      }
    }
  ]
}
