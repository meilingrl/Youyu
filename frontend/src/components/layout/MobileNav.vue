<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useNotificationStore } from '@/stores/notification'
import { appNavigation } from '@/constants/navigation'

defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

/** EP Drawer 的 size 用 calc，避免部分环境下对 min() 字符串解析异常 */
const viewportW = ref(typeof window !== 'undefined' ? window.innerWidth : 390)
function updateW() {
  viewportW.value = window.innerWidth
}
onMounted(() => window.addEventListener('resize', updateW))
onUnmounted(() => window.removeEventListener('resize', updateW))

const drawerWidth = computed(() => `${Math.min(viewportW.value * 0.88, 320)}px`)

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const notificationStore = useNotificationStore()

const visibleNavigation = computed(() =>
  appNavigation.filter((item) => !item.auth || authStore.isLoggedIn)
)

function close() {
  emit('update:modelValue', false)
}

function navigate(path) {
  router.push(path)
  close()
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
  close()
}

function goRegister() {
  router.push({ name: 'register', query: { mode: 'register', redirect: route.fullPath } })
  close()
}

function handleLogout() {
  authStore.logout()
  router.push('/app/home')
  close()
}

function isActive(item) {
  return route.meta?.navKey === item.path
}

function formatBadge(count) {
  const numericCount = Number(count || 0)
  if (numericCount <= 0) return ''
  return numericCount > 99 ? '99+' : String(numericCount)
}

function badgeForPath(path) {
  if (path === '/app/messages') return formatBadge(chatStore.unreadCount)
  if (path === '/app/notifications') return formatBadge(notificationStore.unreadCount)
  return ''
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    direction="ltr"
    append-to-body
    :size="drawerWidth"
    title="菜单"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <nav class="mobile-nav" aria-label="主导航">
      <router-link
        v-for="item in visibleNavigation"
        :key="item.path"
        :to="item.path"
        class="mobile-nav__link"
        :class="{ 'is-active': isActive(item) }"
        @click="close"
      >
        <span>{{ item.label }}</span>
        <span v-if="badgeForPath(item.path)" class="mobile-nav__badge">
          {{ badgeForPath(item.path) }}
        </span>
      </router-link>

      <div class="mobile-nav__divider" />

      <template v-if="authStore.isLoggedIn">
        <el-button class="mobile-nav__btn" plain @click="navigate('/app/shop/manage/publish')">
          发布商品
        </el-button>
        <el-button class="mobile-nav__btn" type="primary" @click="handleLogout">退出登录</el-button>
      </template>
      <template v-else>
        <el-button class="mobile-nav__btn" plain @click="goRegister">注册</el-button>
        <el-button class="mobile-nav__btn" type="primary" @click="goLogin">登录</el-button>
      </template>
    </nav>
  </el-drawer>
</template>

<style scoped>
.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom: 24px;
}

.mobile-nav__link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 48px;
  padding: 13px 16px;
  border-radius: var(--cm-radius-sm);
  color: var(--cm-text-secondary);
  font-weight: 600;
  border: 1px solid transparent;
  transition:
    color var(--cm-transition-micro),
    border-color var(--cm-transition-micro),
    background-color var(--cm-transition-micro),
    transform var(--cm-transition-micro);
}

.mobile-nav__badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #DC2626;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.mobile-nav__link:hover {
  color: var(--cm-text);
  background: rgba(var(--cm-primary-rgb), 0.08);
  transform: translateX(2px);
}

.mobile-nav__link.is-active {
  color: var(--cm-text);
  border-color: rgba(var(--cm-primary-rgb), 0.18);
  background: var(--cm-primary-soft);
}

.mobile-nav__divider {
  height: 1px;
  margin: 12px 0;
  background: var(--cm-border);
}

.mobile-nav__btn {
  width: 100%;
  margin: 0;
}
</style>
