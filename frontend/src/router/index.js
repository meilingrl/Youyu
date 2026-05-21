import { createRouter, createWebHistory } from 'vue-router'
import appRoutes from './modules/app'
import adminRoutes from './modules/admin'
import { setupRouterGuards } from './guards'

const routes = [
  {
    path: '/',
    redirect: '/app'
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
