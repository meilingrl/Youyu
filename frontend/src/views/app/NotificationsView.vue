<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import { useNotificationStore } from '@/stores/notification'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false
  }
})

const router = useRouter()
const store = useNotificationStore()

const unreadText = computed(() => {
  if (store.unreadCount <= 0) return 'No unread notifications'
  return `${store.unreadCount} unread`
})

const typeMeta = {
  order_status: { label: 'Order', tone: 'success', icon: 'Order' },
  review_reminder: { label: 'Review', tone: 'warning', icon: 'Review' },
  system: { label: 'System', tone: 'info', icon: 'Info' }
}

onMounted(async () => {
  await Promise.all([store.loadNotifications(0, store.size), store.loadUnreadCount()])
})

function metaFor(type) {
  return typeMeta[type] || typeMeta.system
}

function formatTime(value) {
  if (!value) return ''
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return value
  const diffMs = Date.now() - date.getTime()
  const diffMinutes = Math.max(0, Math.floor(diffMs / 60000))
  if (diffMinutes < 1) return 'Just now'
  if (diffMinutes < 60) return `${diffMinutes} min ago`
  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) return `${diffHours} h ago`
  const diffDays = Math.floor(diffHours / 24)
  if (diffDays < 7) return `${diffDays} d ago`
  return date.toLocaleString()
}

async function openNotification(notification) {
  try {
    if (!notification.isRead) {
      await store.markRead(notification.id)
    }
    if (notification.actionUrl) {
      await router.push(notification.actionUrl)
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || 'Failed to open notification')
  }
}

async function markAllRead() {
  try {
    await store.markAllRead()
    ElMessage.success('All notifications marked as read')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || 'Failed to mark notifications')
  }
}

async function changePage(nextPage) {
  await store.loadNotifications(nextPage - 1, store.size)
}
</script>

<template>
  <section class="notifications-view" :class="{ 'notifications-view--embedded': props.embedded }">
    <section class="notifications-hero">
      <div>
        <p class="notifications-hero__eyebrow">Message Center</p>
        <h1>Notifications</h1>
        <p>{{ unreadText }}</p>
      </div>
      <div class="notifications-hero__actions">
        <el-button plain :loading="store.loading" @click="store.loadNotifications(store.page, store.size)">
          Refresh
        </el-button>
        <el-button
          type="primary"
          :loading="store.actionLoading"
          :disabled="store.unreadCount === 0"
          @click="markAllRead"
        >
          Mark All Read
        </el-button>
      </div>
    </section>

    <ErrorBlock v-if="store.error" :message="store.error" @retry="store.loadNotifications(0, store.size)" />

    <SkeletonCard v-else-if="store.loading" :count="3" />

    <EmptyState
      v-else-if="store.notifications.length === 0"
      emoji="i"
      title="No notifications yet"
      description="Order status updates and review reminders will appear here."
    >
      <el-button type="primary" @click="$router.push('/app/orders')">Go to Orders</el-button>
    </EmptyState>

    <template v-else>
      <section class="notification-list">
        <article
          v-for="item in store.notifications"
          :key="item.id"
          class="notification-item"
          :class="{ 'is-unread': !item.isRead }"
          role="button"
          tabindex="0"
          @click="openNotification(item)"
          @keydown.enter.prevent="openNotification(item)"
        >
          <div class="notification-item__icon" aria-hidden="true">
            {{ metaFor(item.type).icon }}
          </div>
          <div class="notification-item__body">
            <div class="notification-item__meta">
              <el-tag size="small" :type="metaFor(item.type).tone">{{ metaFor(item.type).label }}</el-tag>
              <span>{{ formatTime(item.createdAt) }}</span>
              <span v-if="!item.isRead" class="notification-item__unread">Unread</span>
            </div>
            <h2>{{ item.title }}</h2>
            <p>{{ item.body }}</p>
          </div>
        </article>
      </section>

      <div class="notification-pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="store.page + 1"
          :page-size="store.size"
          :total="store.total"
          @current-change="changePage"
        />
      </div>
    </template>
  </section>
</template>

<style scoped>
.notifications-view {
  display: grid;
  gap: 18px;
}

.notifications-view--embedded {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.notifications-view--embedded .notifications-hero {
  padding: 16px;
}

.notifications-view--embedded .notifications-hero h1 {
  font-size: 20px;
}

.notifications-hero,
.notifications-hero__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.notifications-hero {
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: var(--cm-surface-strong);
  padding: 20px;
}

.notifications-hero h1,
.notifications-hero p,
.notification-item h2,
.notification-item p {
  margin: 0;
}

.notifications-hero__eyebrow {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.notification-list {
  display: grid;
  gap: 12px;
}

.notification-item {
  display: grid;
  grid-template-columns: 52px 1fr;
  gap: 14px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: var(--cm-surface-strong);
  padding: 16px;
  cursor: pointer;
  transition:
    border-color var(--cm-transition),
    background-color var(--cm-transition),
    transform var(--cm-transition);
}

.notification-item:hover,
.notification-item:focus-visible {
  border-color: rgba(var(--cm-primary-rgb), 0.32);
  outline: none;
  transform: translateY(-1px);
}

.notification-item.is-unread {
  border-color: rgba(var(--cm-primary-rgb), 0.28);
  background: rgba(var(--cm-primary-rgb), 0.08);
}

.notification-item__icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--cm-primary);
  font-size: 12px;
  font-weight: 700;
}

.notification-item__body {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.notification-item__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--cm-text-tertiary);
  font-size: 12px;
}

.notification-item h2 {
  color: var(--cm-text);
  font-size: 17px;
  line-height: 1.35;
}

.notification-item p {
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.notification-item__unread {
  color: var(--cm-primary);
  font-weight: 700;
}

.notification-pagination {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .notifications-hero,
  .notifications-hero__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .notifications-hero__actions :deep(.el-button) {
    width: 100%;
  }

  .notification-item {
    grid-template-columns: 1fr;
  }
}
</style>
