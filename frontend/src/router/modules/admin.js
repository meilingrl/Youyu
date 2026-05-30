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
        title: '治理总览',
        navKey: '/admin/dashboard'
      }
    },
    {
      path: 'users',
      name: 'admin-users',
      component: () => import('@/views/admin/UserManageView.vue'),
      meta: {
        title: '用户与身份',
        navKey: '/admin/users'
      }
    },
    {
      path: 'verifications',
      name: 'admin-verifications',
      component: () => import('@/views/admin/VerificationManageView.vue'),
      meta: {
        title: '学生认证队列',
        navKey: '/admin/verifications'
      }
    },
    {
      path: 'products',
      name: 'admin-products',
      component: () => import('@/views/admin/ProductManageView.vue'),
      meta: {
        title: '商品治理',
        navKey: '/admin/products'
      }
    },
    {
      path: 'review-tasks',
      name: 'admin-review-tasks',
      component: () => import('@/views/admin/ReviewTaskManageView.vue'),
      meta: {
        title: '资料审核队列',
        navKey: '/admin/review-tasks'
      }
    },
    {
      path: 'shops',
      name: 'admin-shops',
      component: () => import('@/views/admin/ShopManageView.vue'),
      meta: {
        title: '店铺准入',
        navKey: '/admin/shops'
      }
    },
    {
      path: 'orders',
      name: 'admin-orders',
      component: () => import('@/views/admin/OrderManageView.vue'),
      meta: {
        title: '订单履约',
        navKey: '/admin/orders'
      }
    },
    {
      path: 'reports',
      name: 'admin-reports',
      component: () => import('@/views/admin/ReportManageView.vue'),
      meta: {
        title: '举报处置',
        navKey: '/admin/reports'
      }
    },
    {
      path: 'marketing',
      name: 'admin-marketing',
      component: () => import('@/views/admin/MarketingReviewView.vue'),
      meta: {
        title: '营销审核',
        navKey: '/admin/marketing'
      }
    },
    {
      path: 'mediation',
      name: 'admin-mediation',
      component: () => import('@/views/admin/MediationCaseListView.vue'),
      meta: {
        title: '调解案件',
        navKey: '/admin/mediation'
      }
    },
    {
      path: 'mediation/:id',
      name: 'admin-mediation-detail',
      component: () => import('@/views/admin/MediationCaseDetailView.vue'),
      meta: {
        title: '调解详情',
        navKey: '/admin/mediation',
        hiddenInNav: true
      }
    },
    {
      path: 'hot-search',
      name: 'admin-hot-search',
      component: () => import('@/views/admin/HotSearchGovernView.vue'),
      meta: {
        title: '搜索治理',
        navKey: '/admin/hot-search'
      }
    },
    {
      path: 'support',
      name: 'admin-support',
      component: () => import('@/views/admin/SupportView.vue'),
      meta: {
        title: '在线客服控制台',
        navKey: '/admin/support'
      }
    }
  ]
}
