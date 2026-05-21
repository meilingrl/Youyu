import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import appRoutes from '../modules/app'
import adminRoutes from '../modules/admin'
import { setupRouterGuards } from '../guards'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: { template: '<div />' },
    meta: {
      title: 'Login',
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

  it('allows an admin user to visit the admin dashboard', async () => {
    setSession('admin')
    const router = createTestRouter()

    await navigate(router, '/admin/dashboard')

    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('keeps login public even when a regular user is already authenticated', async () => {
    setSession('user')
    const router = createTestRouter()

    await navigate(router, '/login')

    expect(router.currentRoute.value.name).toBe('login')
  })
})
