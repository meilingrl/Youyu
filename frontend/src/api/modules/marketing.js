import service from '@/api/client'

export function getOwnerCoupons(params) {
  return service.get('/marketing/owner/coupons', { params })
}

export function createOwnerCoupon(payload) {
  return service.post('/marketing/owner/coupons', payload)
}

export function updateOwnerCoupon(couponId, payload) {
  return service.put(`/marketing/owner/coupons/${couponId}`, payload)
}

export function updateOwnerCouponStatus(couponId, payload) {
  return service.put(`/marketing/owner/coupons/${couponId}/status`, payload)
}

export function getAvailableCoupons(params) {
  return service.get('/marketing/coupons/available', { params })
}

export function claimCoupon(couponId) {
  return service.post(`/marketing/coupons/${couponId}/claim`)
}

export function getMyCoupons(params) {
  return service.get('/marketing/my-coupons', { params })
}

export function getOwnerActivities(params) {
  return service.get('/marketing/owner/activities', { params })
}

export function createOwnerActivity(payload) {
  return service.post('/marketing/owner/activities', payload)
}

export function updateOwnerActivity(activityId, payload) {
  return service.put(`/marketing/owner/activities/${activityId}`, payload)
}

export function updateOwnerActivityStatus(activityId, payload) {
  return service.put(`/marketing/owner/activities/${activityId}/status`, payload)
}

export function getShopActivities(shopId, params) {
  return service.get(`/marketing/shops/${shopId}/activities`, { params })
}

export function getAdminMarketingCoupons(params) {
  return service.get('/admin/marketing/coupons', { params })
}

export function reviewAdminMarketingCoupon(couponId, payload) {
  return service.put(`/admin/marketing/coupons/${couponId}/review`, payload)
}

export function disableAdminMarketingCoupon(couponId, payload) {
  return service.put(`/admin/marketing/coupons/${couponId}/disable`, payload)
}

export function getAdminMarketingActivities(params) {
  return service.get('/admin/marketing/activities', { params })
}

export function reviewAdminMarketingActivity(activityId, payload) {
  return service.put(`/admin/marketing/activities/${activityId}/review`, payload)
}

export function disableAdminMarketingActivity(activityId, payload) {
  return service.put(`/admin/marketing/activities/${activityId}/disable`, payload)
}
