import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getNotificationList,
  getUnreadCount,
  markAllNotificationsRead,
  markNotificationRead
} from '@/api/modules/notification'
import { resolveErrorMessage } from '@/utils/error-utils'

function normalizeNotification(item = {}) {
  return {
    id: item.id,
    userId: item.userId ?? item.user_id,
    type: item.type || 'system',
    title: item.title || '',
    body: item.body || '',
    actionUrl: item.actionUrl ?? item.action_url ?? '',
    isRead: Boolean(item.isRead ?? item.is_read),
    createdAt: item.createdAt ?? item.created_at ?? ''
  }
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const page = ref(0)
  const size = ref(20)
  const total = ref(0)
  const totalPages = ref(0)
  const unreadCount = ref(0)
  const loading = ref(false)
  const unreadLoading = ref(false)
  const actionLoading = ref(false)
  const error = ref('')
  const unreadError = ref('')
  const pollingTimer = ref(null)

  const hasUnread = computed(() => unreadCount.value > 0)

  async function loadNotifications(nextPage = page.value, nextSize = size.value) {
    loading.value = true
    error.value = ''
    try {
      const response = await getNotificationList({ page: nextPage, size: nextSize })
      if (!response?.success) throw new Error(response?.message || 'Failed to load notifications')
      const data = response.data || {}
      notifications.value = (data.content || []).map(normalizeNotification)
      page.value = data.page ?? nextPage
      size.value = data.size ?? nextSize
      total.value = data.total ?? notifications.value.length
      totalPages.value = data.totalPages ?? 0
    } catch (err) {
      error.value = resolveErrorMessage(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function loadUnreadCount() {
    unreadLoading.value = true
    unreadError.value = ''
    try {
      const response = await getUnreadCount()
      if (!response?.success) throw new Error(response?.message || 'Failed to load unread count')
      unreadCount.value = Number(response.data?.count || 0)
    } catch (err) {
      unreadError.value = resolveErrorMessage(err)
    } finally {
      unreadLoading.value = false
    }
  }

  async function markRead(id) {
    actionLoading.value = true
    try {
      await markNotificationRead(id)
      notifications.value = notifications.value.map((item) =>
        item.id === id ? { ...item, isRead: true } : item
      )
      if (unreadCount.value > 0) unreadCount.value -= 1
    } finally {
      actionLoading.value = false
    }
  }

  async function markAllRead() {
    actionLoading.value = true
    try {
      await markAllNotificationsRead()
      notifications.value = notifications.value.map((item) => ({ ...item, isRead: true }))
      unreadCount.value = 0
    } finally {
      actionLoading.value = false
    }
  }

  function startUnreadPolling(intervalMs = 60000) {
    stopUnreadPolling()
    loadUnreadCount()
    pollingTimer.value = window.setInterval(loadUnreadCount, intervalMs)
  }

  function stopUnreadPolling() {
    if (pollingTimer.value) {
      window.clearInterval(pollingTimer.value)
      pollingTimer.value = null
    }
  }

  return {
    notifications,
    page,
    size,
    total,
    totalPages,
    unreadCount,
    loading,
    unreadLoading,
    actionLoading,
    error,
    unreadError,
    hasUnread,
    loadNotifications,
    loadUnreadCount,
    markRead,
    markAllRead,
    startUnreadPolling,
    stopUnreadPolling
  }
})
