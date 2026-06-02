const ADMIN_ROLES = new Set([
  'admin',
  'super_admin',
  'support_agent',
  'reviewer',
  'operator',
  'order_admin'
])

const ALL_PERMISSIONS = [
  'ADMIN_DASHBOARD_VIEW',
  'ADMIN_SUPPORT_CONTEXT_VIEW',
  'ADMIN_SUPPORT_TICKETS_HANDLE',
  'ADMIN_USERS_VIEW',
  'ADMIN_USERS_MANAGE',
  'ADMIN_VERIFICATIONS_REVIEW',
  'ADMIN_PRODUCTS_VIEW',
  'ADMIN_PRODUCTS_REVIEW',
  'ADMIN_SHOPS_VIEW',
  'ADMIN_SHOPS_MANAGE',
  'ADMIN_MARKETING_REVIEW',
  'ADMIN_REPORTS_HANDLE',
  'ADMIN_SEARCH_GOVERN',
  'ADMIN_SEARCH_LOGS_VIEW',
  'ADMIN_ORDERS_READ',
  'ADMIN_ORDERS_MANAGE',
  'ADMIN_MEDIATION_HANDLE',
  'ADMIN_MEDIATION_DECIDE',
  'ADMIN_NOTIFICATIONS_PUBLISH',
  'ADMIN_AUDIT_VIEW'
]

const PERMISSIONS_BY_ROLE = {
  admin: ALL_PERMISSIONS,
  super_admin: ALL_PERMISSIONS,
  support_agent: [
    'ADMIN_DASHBOARD_VIEW',
    'ADMIN_SUPPORT_CONTEXT_VIEW',
    'ADMIN_USERS_VIEW',
    'ADMIN_PRODUCTS_VIEW',
    'ADMIN_SHOPS_VIEW',
    'ADMIN_REPORTS_HANDLE',
    'ADMIN_SEARCH_LOGS_VIEW',
    'ADMIN_ORDERS_READ',
    'ADMIN_MEDIATION_HANDLE',
    'ADMIN_SUPPORT_TICKETS_HANDLE'
  ],
  reviewer: [
    'ADMIN_DASHBOARD_VIEW',
    'ADMIN_VERIFICATIONS_REVIEW',
    'ADMIN_PRODUCTS_VIEW',
    'ADMIN_PRODUCTS_REVIEW',
    'ADMIN_SHOPS_VIEW',
    'ADMIN_SHOPS_MANAGE',
    'ADMIN_MARKETING_REVIEW'
  ],
  operator: [
    'ADMIN_DASHBOARD_VIEW',
    'ADMIN_PRODUCTS_VIEW',
    'ADMIN_SEARCH_GOVERN',
    'ADMIN_SEARCH_LOGS_VIEW'
  ],
  order_admin: [
    'ADMIN_DASHBOARD_VIEW',
    'ADMIN_ORDERS_READ',
    'ADMIN_ORDERS_MANAGE',
    'ADMIN_MEDIATION_HANDLE'
  ]
}

export const ADMIN_PATH_PERMISSIONS = {
  '/admin/dashboard': ['ADMIN_DASHBOARD_VIEW'],
  '/admin/users': ['ADMIN_USERS_VIEW'],
  '/admin/verifications': ['ADMIN_VERIFICATIONS_REVIEW'],
  '/admin/products': ['ADMIN_PRODUCTS_VIEW'],
  '/admin/review-tasks': ['ADMIN_PRODUCTS_REVIEW'],
  '/admin/shops': ['ADMIN_SHOPS_VIEW'],
  '/admin/orders': ['ADMIN_ORDERS_READ'],
  '/admin/reports': ['ADMIN_REPORTS_HANDLE'],
  '/admin/marketing': ['ADMIN_MARKETING_REVIEW'],
  '/admin/mediation': ['ADMIN_MEDIATION_HANDLE'],
  '/admin/hot-search': ['ADMIN_SEARCH_GOVERN'],
  '/admin/notifications': ['ADMIN_NOTIFICATIONS_PUBLISH'],
  '/admin/support': ['ADMIN_SUPPORT_TICKETS_HANDLE']
}

export function normalizeRole(role) {
  return String(role || '').trim().toLowerCase()
}

export function isAdminRole(role) {
  return ADMIN_ROLES.has(normalizeRole(role))
}

export function hasAnyAdminPermission(role, permissions = []) {
  if (!permissions.length) {
    return true
  }

  const granted = new Set(PERMISSIONS_BY_ROLE[normalizeRole(role)] || [])
  return permissions.some((permission) => granted.has(permission))
}

export function permissionsForAdminPath(path = '') {
  const normalizedPath = String(path || '')
  if (normalizedPath.startsWith('/admin/mediation/')) {
    return ADMIN_PATH_PERMISSIONS['/admin/mediation']
  }
  return ADMIN_PATH_PERMISSIONS[normalizedPath] || []
}
