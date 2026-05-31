import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import appRoutes from '../modules/app'
import adminRoutes from '../modules/admin'
import { setupRouterGuards } from '../guards'
import { useAuthStore } from '@/stores/auth'
import { getAuthStorage } from '@/utils/auth'
import { isAdminRole } from '@/utils/admin-permissions'

const AUTH_STORAGE_KEY = 'youyu-auth'

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
    component: { template: '<div />' },
    meta: {
      title: 'Login',
      public: true
    }
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: { template: '<div />' },
    meta: {
      title: 'Forgot password',
      public: true
    }
  },
  appRoutes,
  adminRoutes
]

function createTestRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes
  })

  setupRouterGuards(router)
  return router
}

async function navigate(router, path) {
  await router.push(path)
  await router.isReady()
}

function setSession(role) {
  const authStore = useAuthStore()
  authStore.setSession({
    token: `${role}.token`,
    role,
    user: {
      id: role === 'admin' ? '9001' : '1001',
      loginId: role
    }
  })
}

describe('router guards', () => {
  beforeEach(() => {
    window.localStorage.clear()
    document.title = ''
    setActivePinia(createPinia())
  })

  it('redirects an anonymous user from profile to login with the target path', async () => {
    const router = createTestRouter()

    await navigate(router, '/app/profile')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/app/profile')
  })

  it('allows anonymous users to visit public app pages', async () => {
    const router = createTestRouter()

    await navigate(router, '/app/home')
    expect(router.currentRoute.value.path).toBe('/app/home')

    await navigate(router, '/app/products')
    expect(router.currentRoute.value.path).toBe('/app/products')
  })

  it('redirects a regular user away from the admin dashboard', async () => {
    setSession('user')
    const router = createTestRouter()

    await navigate(router, '/admin/dashboard')

    expect(router.currentRoute.value.path).toBe('/app/home')
  })

  it('routes a regular user default entry to the app home', async () => {
    setSession('user')
    const router = createTestRouter()

    await navigate(router, '/')

    expect(router.currentRoute.value.path).toBe('/app/home')
  })

  it('allows an admin user to visit the admin dashboard', async () => {
    setSession('admin')
    const router = createTestRouter()

    await navigate(router, '/admin/dashboard')

    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('allows an admin user to visit mediation list and detail routes', async () => {
    setSession('admin')
    const router = createTestRouter()

    await navigate(router, '/admin/mediation')
    expect(router.currentRoute.value.name).toBe('admin-mediation')

    await navigate(router, '/admin/mediation/70001')
    expect(router.currentRoute.value.name).toBe('admin-mediation-detail')
    expect(router.currentRoute.value.meta.navKey).toBe('/admin/mediation')
  })

  it('routes an admin default entry to the admin dashboard', async () => {
    setSession('admin')
    const router = createTestRouter()

    await navigate(router, '/')

    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('routes a restored uppercase admin session default entry to the admin dashboard', async () => {
    window.localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({
        token: 'admin.token',
        role: 'ADMIN',
        user: {
          id: '9001',
          loginId: 'admin'
        }
      })
    )
    const router = createTestRouter()

    await navigate(router, '/')

    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('allows specialist admin roles to enter the dashboard', async () => {
    setSession('reviewer')
    const router = createTestRouter()

    await navigate(router, '/')

    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('redirects specialist admin roles away from unavailable admin pages', async () => {
    setSession('reviewer')
    const router = createTestRouter()

    await navigate(router, '/admin/orders')

    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('allows specialist admin roles to visit permitted admin pages', async () => {
    setSession('order_admin')
    const router = createTestRouter()

    await navigate(router, '/admin/orders')

    expect(router.currentRoute.value.path).toBe('/admin/orders')
  })

  it('redirects an admin away from user-only app pages to the admin dashboard', async () => {
    setSession('admin')
    const router = createTestRouter()

    await navigate(router, '/app/profile')

    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('keeps login public even when a regular user is already authenticated', async () => {
    setSession('user')
    const router = createTestRouter()

    await navigate(router, '/login')

    expect(router.currentRoute.value.name).toBe('login')
  })

  it('allows anonymous users to visit the forgot-password flow', async () => {
    const router = createTestRouter()

    await navigate(router, '/forgot-password')

    expect(router.currentRoute.value.name).toBe('forgot-password')
  })
})
