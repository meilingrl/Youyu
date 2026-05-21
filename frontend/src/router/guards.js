import { useAuthStore } from '@/stores/auth'

function hasRolePermission(routeRole, currentRole) {
  if (!routeRole) {
    return true
  }

  if (Array.isArray(routeRole)) {
    return routeRole.includes(currentRole)
  }

  return routeRole === currentRole
}

export function setupRouterGuards(router) {
  router.beforeEach((to) => {
    const authStore = useAuthStore()

    if (to.meta?.title) {
      document.title = `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE || 'CampusMarket'}`
    }

    if (to.meta?.public) {
      return true
    }

    if (to.meta?.requiresAuth && !authStore.isLoggedIn) {
      return {
        name: 'login',
        query: {
          redirect: to.fullPath
        }
      }
    }

    if (to.meta?.role && !hasRolePermission(to.meta.role, authStore.currentRole)) {
      return authStore.currentRole === 'admin' ? { path: '/admin' } : { path: '/app' }
    }

    return true
  })
}
