import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  claimCoupon,
  createOwnerActivity,
  createOwnerCoupon,
  getAvailableCoupons,
  getMyCoupons,
  getOwnerActivities,
  getOwnerCoupons,
  getShopActivities,
  updateOwnerActivityStatus,
  updateOwnerCouponStatus
} from '@/api/modules/marketing'

function camelKey(key) {
  return String(key).replace(/_([a-z])/g, (_, letter) => letter.toUpperCase())
}

function normalizeRecord(value) {
  if (Array.isArray(value)) {
    return value.map(normalizeRecord)
  }

  if (!value || typeof value !== 'object') {
    return value
  }

  return Object.entries(value).reduce((result, [key, item]) => {
    result[camelKey(key)] = normalizeRecord(item)
    return result
  }, {})
}

function readList(response) {
  const data = response?.data
  const items = Array.isArray(data) ? data : data?.items || data?.records || data?.list || []
  return items.map(normalizeRecord)
}

function upsertById(list, row) {
  const id = String(row?.id || row?.couponId || row?.activityId || '')
  if (!id) {
    return [row, ...list]
  }
  return [row, ...list.filter((item) => String(item.id || item.couponId || item.activityId) !== id)]
}

export function couponDiscountLabel(coupon) {
  const type = String(coupon?.type || coupon?.couponType || '').toLowerCase()
  const discount = Number(coupon?.discountAmount || coupon?.amount || 0)
  const threshold = Number(coupon?.thresholdAmount || coupon?.minimumSpendAmount || coupon?.minSpendAmount || coupon?.minOrderAmount || 0)

  if (type === 'threshold' || type === 'threshold_discount') {
    return `满 ${threshold.toFixed(2)} 减 ${discount.toFixed(2)}`
  }

  return `立减 ${discount.toFixed(2)}`
}

export function marketingStatusLabel(status) {
  const normalizedStatus = String(status || '').toLowerCase()
  return {
    active: '生效中',
    approved: '已通过',
    disabled: '已停用',
    draft: '草稿',
    expired: '已过期',
    pending: '待处理',
    pending_review: '待审核',
    rejected: '已驳回'
  }[normalizedStatus] || status || '待确认'
}

export const useMarketingStore = defineStore('marketing', () => {
  const myCoupons = ref([])
  const availableCouponsByShop = ref({})
  const ownerCoupons = ref([])
  const ownerActivities = ref([])
  const shopActivitiesByShop = ref({})

  const loadingMyCoupons = ref(false)
  const loadingAvailableCoupons = ref(false)
  const loadingOwnerCoupons = ref(false)
  const loadingOwnerActivities = ref(false)
  const loadingShopActivities = ref(false)
  const error = ref('')

  const ownedUsableCoupons = computed(() =>
    myCoupons.value.filter((item) => ['active', 'claimed', 'available'].includes(item.userCouponStatus || item.status))
  )

  async function loadMyCoupons(params) {
    loadingMyCoupons.value = true
    error.value = ''
    try {
      const response = await getMyCoupons(params)
      myCoupons.value = readList(response)
      return myCoupons.value
    } catch (err) {
      error.value = err?.response?.data?.message || err?.message || '优惠券加载失败'
      throw err
    } finally {
      loadingMyCoupons.value = false
    }
  }

  async function loadAvailableCoupons(shopId) {
    if (!shopId) {
      return []
    }
    loadingAvailableCoupons.value = true
    error.value = ''
    try {
      const response = await getAvailableCoupons({ shopId })
      const rows = readList(response)
      availableCouponsByShop.value = {
        ...availableCouponsByShop.value,
        [String(shopId)]: rows
      }
      return rows
    } catch (err) {
      error.value = err?.response?.data?.message || err?.message || '可领取优惠券加载失败'
      throw err
    } finally {
      loadingAvailableCoupons.value = false
    }
  }

  async function claimShopCoupon(couponId, shopId) {
    const response = await claimCoupon(couponId)
    await Promise.allSettled([
      shopId ? loadAvailableCoupons(shopId) : Promise.resolve([]),
      loadMyCoupons()
    ])
    return normalizeRecord(response?.data || {})
  }

  async function loadOwnerCoupons(params) {
    loadingOwnerCoupons.value = true
    error.value = ''
    try {
      const response = await getOwnerCoupons(params)
      ownerCoupons.value = readList(response)
      return ownerCoupons.value
    } catch (err) {
      error.value = err?.response?.data?.message || err?.message || '店铺优惠券加载失败'
      throw err
    } finally {
      loadingOwnerCoupons.value = false
    }
  }

  async function createCoupon(payload) {
    const response = await createOwnerCoupon(payload)
    const coupon = normalizeRecord(response?.data || {})
    if (coupon && Object.keys(coupon).length) {
      ownerCoupons.value = upsertById(ownerCoupons.value, coupon)
    }
    await loadOwnerCoupons()
    return coupon
  }

  async function changeOwnerCouponStatus(couponId, payload) {
    const response = await updateOwnerCouponStatus(couponId, payload)
    await loadOwnerCoupons()
    return normalizeRecord(response?.data || {})
  }

  async function loadOwnerActivities(params) {
    loadingOwnerActivities.value = true
    error.value = ''
    try {
      const response = await getOwnerActivities(params)
      ownerActivities.value = readList(response)
      return ownerActivities.value
    } catch (err) {
      error.value = err?.response?.data?.message || err?.message || '店铺活动加载失败'
      throw err
    } finally {
      loadingOwnerActivities.value = false
    }
  }

  async function createActivity(payload) {
    const response = await createOwnerActivity(payload)
    const activity = normalizeRecord(response?.data || {})
    if (activity && Object.keys(activity).length) {
      ownerActivities.value = upsertById(ownerActivities.value, activity)
    }
    await loadOwnerActivities()
    return activity
  }

  async function changeOwnerActivityStatus(activityId, payload) {
    const response = await updateOwnerActivityStatus(activityId, payload)
    await loadOwnerActivities()
    return normalizeRecord(response?.data || {})
  }

  async function loadShopActivities(shopId) {
    if (!shopId) {
      return []
    }
    loadingShopActivities.value = true
    error.value = ''
    try {
      const response = await getShopActivities(shopId)
      const rows = readList(response)
      shopActivitiesByShop.value = {
        ...shopActivitiesByShop.value,
        [String(shopId)]: rows
      }
      return rows
    } catch (err) {
      error.value = err?.response?.data?.message || err?.message || '店铺活动加载失败'
      throw err
    } finally {
      loadingShopActivities.value = false
    }
  }

  function getAvailableCouponsByShop(shopId) {
    return availableCouponsByShop.value[String(shopId)] || []
  }

  function getShopActivitiesByShop(shopId) {
    return shopActivitiesByShop.value[String(shopId)] || []
  }

  return {
    myCoupons,
    availableCouponsByShop,
    ownerCoupons,
    ownerActivities,
    shopActivitiesByShop,
    loadingMyCoupons,
    loadingAvailableCoupons,
    loadingOwnerCoupons,
    loadingOwnerActivities,
    loadingShopActivities,
    error,
    ownedUsableCoupons,
    loadMyCoupons,
    loadAvailableCoupons,
    claimShopCoupon,
    loadOwnerCoupons,
    createCoupon,
    changeOwnerCouponStatus,
    loadOwnerActivities,
    createActivity,
    changeOwnerActivityStatus,
    loadShopActivities,
    getAvailableCouponsByShop,
    getShopActivitiesByShop
  }
})
