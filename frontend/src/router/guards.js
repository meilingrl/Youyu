import { useAuthStore } from '@/stores/auth'
import { adminNavigation } from '@/constants/navigation'
import {
  hasAnyAdminPermission,
  isAdminRole,
  permissionsForAdminPath
} from '@/utils/admin-permissions'

function hasRolePermission(routeRole, currentRole) {
  if (!routeRole) {
    return true
  }

  if (Array.isArray(routeRole)) {
    return routeRole.includes(currentRole) || (routeRole.includes('admin') && isAdminRole(currentRole))
  }

  if (routeRole === 'admin') {
    return isAdminRole(currentRole)
  }

  return routeRole === currentRole
}

function firstAllowedAdminPath(role) {
  return adminNavigation.find((item) =>
    hasAnyAdminPermission(role, permissionsForAdminPath(item.path))
  )?.path || '/app/home'
}

export function setupRouterGuards(router) {
  router.beforeEach((to) => {
    const authStore = useAuthStore()

    if (to.meta?.title) {
      document.title = `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE || 'Youyu'}`
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
      return isAdminRole(authStore.currentRole) ? { path: firstAllowedAdminPath(authStore.currentRole) } : { path: '/app/home' }
    }

    if (to.path.startsWith('/admin')) {
      const permissions = permissionsForAdminPath(to.meta?.navKey || to.path)
      if (!hasAnyAdminPermission(authStore.currentRole, permissions)) {
        return { path: firstAllowedAdminPath(authStore.currentRole) }
      }
    }

    return true
  })
}
