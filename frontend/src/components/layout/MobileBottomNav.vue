<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { mobileBottomNavigation } from '@/constants/navigation'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()

function isActive(item) {
  return route.meta?.navKey === item.navKey
}

function onTap(item) {
  if (item.auth && !authStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: item.path } })
    return
  }
  router.push(item.path)
}

function badgeForItem(item) {
  if (item.path !== '/app/messages') return ''
  const numericCount = Number(chatStore.unreadCount || 0)
  if (numericCount <= 0) return ''
  return numericCount > 99 ? '99+' : String(numericCount)
}
</script>

<template>
  <nav v-if="route.path.startsWith('/app')" class="mobile-bottom-nav" aria-label="底部快捷导航">
    <button
      v-for="item in mobileBottomNavigation"
      :key="item.path"
      type="button"
      class="mobile-bottom-nav__item"
      :class="{ 'is-active': isActive(item) }"
      @click="onTap(item)"
    >
      <span class="mobile-bottom-nav__icon-wrap">
        <span class="mobile-bottom-nav__icon" aria-hidden="true">{{ item.icon }}</span>
        <span v-if="badgeForItem(item)" class="mobile-bottom-nav__badge">
          {{ badgeForItem(item) }}
        </span>
      </span>
      <span class="mobile-bottom-nav__label">{{ item.label }}</span>
    </button>
  </nav>
</template>

<style scoped>
.mobile-bottom-nav {
  display: none;
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: 10px;
  z-index: 40;
  padding: 8px 10px calc(8px + env(safe-area-inset-bottom, 0));
  background: var(--cm-surface-glass);
  border: 1px solid var(--cm-border);
  border-radius: 24px;
  backdrop-filter: blur(var(--cm-blur-strong));
  -webkit-backdrop-filter: blur(var(--cm-blur-strong));
  box-shadow: 0 14px 42px rgba(88, 62, 43, 0.16);
}

.mobile-bottom-nav__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-height: 54px;
  padding: 7px 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--cm-text-secondary);
  font-size: 11px;
  font-weight: 600;
  border-radius: 18px;
  transition:
    color var(--cm-transition-micro),
    background-color var(--cm-transition-micro),
    transform var(--cm-transition-micro);
}

.mobile-bottom-nav__item:hover {
  color: var(--cm-text);
  background: rgba(var(--cm-primary-rgb), 0.08);
}

.mobile-bottom-nav__item.is-active {
  color: var(--cm-primary);
  background: var(--cm-primary-soft);
  transform: translateY(-1px);
}

.mobile-bottom-nav__icon {
  font-size: 20px;
  line-height: 1;
}

.mobile-bottom-nav__icon-wrap {
  position: relative;
  display: inline-flex;
}

.mobile-bottom-nav__badge {
  position: absolute;
  top: -8px;
  right: -14px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #DC2626;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

@media (max-width: 768px) {
  .mobile-bottom-nav {
    display: flex;
  }
}
</style>
