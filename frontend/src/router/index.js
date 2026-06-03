import { createRouter, createWebHistory } from 'vue-router'
import appRoutes from './modules/app'
import adminRoutes from './modules/admin'
import { setupRouterGuards } from './guards'
import { getAuthStorage } from '@/utils/auth'
import { isAdminRole } from '@/utils/admin-permissions'

function resolveRootEntry() {
  return isAdminRole(getAuthStorage()?.role) ? '/admin/dashboard' : '/app/home'
}

const routes = [
  {
    path: '/',
    redirect: resolveRootEntry
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: {
      title: '登录',
      public: true
    }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: {
      title: '注册',
      public: true
    }
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('@/views/auth/ForgotPasswordView.vue'),
    meta: {
      title: '找回密码',
      public: true
    }
  },
  {
    path: '/legal/privacy-policy',
    name: 'legal-privacy-policy',
    component: () => import('@/views/legal/PrivacyPolicyView.vue'),
    meta: {
      title: '隐私政策',
      public: true
    }
  },
  {
    path: '/legal/user-agreement',
    name: 'legal-user-agreement',
    component: () => import('@/views/legal/UserAgreementView.vue'),
    meta: {
      title: '用户协议',
      public: true
    }
  },
  {
    path: '/legal/cookie-policy',
    name: 'legal-cookie-policy',
    component: () => import('@/views/legal/CookiePolicyView.vue'),
    meta: {
      title: 'Cookie 政策',
      public: true
    }
  },
  appRoutes,
  adminRoutes,
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/system/NotFoundView.vue'),
    meta: {
      title: '页面不存在',
      public: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

setupRouterGuards(router)

export default router
