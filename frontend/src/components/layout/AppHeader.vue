<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { appNavigation } from '@/constants/navigation'
import MobileNav from '@/components/layout/MobileNav.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const mobileMenuOpen = ref(false)

const visibleNavigation = computed(() =>
  appNavigation.filter((item) => !item.auth || authStore.isLoggedIn)
)

const isHome = computed(() => route.meta?.navKey === '/app/home')
const isExplore = computed(() => route.meta?.navKey === '/app/explore')
const isCondensed = computed(() => isExplore.value && appStore.isHeaderCondensed)

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

function goRegister() {
  router.push({ name: 'register', query: { mode: 'register', redirect: route.fullPath } })
}

function handleLogout() {
  authStore.logout()
  router.push('/app/home')
}

function isActive(item) {
  return route.meta?.navKey === item.path
}

function focusExploreSearch() {
  if (!isExplore.value) {
    router.push('/app/explore')
    return
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<template>
  <header
    class="app-header shell-card"
    :class="{ 'app-header--home': isHome, 'app-header--condensed': isCondensed }"
  >
    <button type="button" class="app-header__brand" @click="router.push('/app/home')">
      <span class="app-header__badge">CM</span>
      <div class="app-header__brand-text">
        <div class="app-header__title">Youyu</div>
        <div class="app-header__subtitle">校园里的可信交易</div>
      </div>
    </button>

    <button
      v-if="isCondensed"
      type="button"
      class="app-header__search-pill"
      aria-label="搜索商品"
      @click="focusExploreSearch"
    >
      <svg class="app-header__search-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M11 5a6 6 0 1 0 0 12a6 6 0 0 0 0-12m0-2a8 8 0 1 1 0 16a8 8 0 0 1 0-16m9.707 16.293l-3.4-3.4a1 1 0 1 0-1.414 1.414l3.4 3.4a1 1 0 0 0 1.414-1.414"
          fill="currentColor"
        />
      </svg>
      <span class="app-header__search-pill-text">
        {{ appStore.keyword?.trim() || '搜索耳机、教材、宿舍好物…' }}
      </span>
      <span class="app-header__search-pill-divider" aria-hidden="true" />
      <span class="app-header__search-pill-action">搜索</span>
    </button>

    <nav v-else class="app-header__nav app-header__nav--desktop" aria-label="主导航">
      <router-link
        v-for="item in visibleNavigation"
        :key="item.path"
        :to="item.path"
        class="app-header__link"
        :class="{ 'is-active': isActive(item) }"
      >
        {{ item.label }}
      </router-link>
    </nav>

    <div class="app-header__actions app-header__actions--desktop">
      <template v-if="authStore.isLoggedIn">
        <span class="app-header__user">{{ authStore.currentUser?.nickname }}</span>
        <el-button class="app-header__publish" plain @click="$router.push('/app/shop/manage/publish')">发布商品</el-button>
        <el-button type="primary" @click="handleLogout">退出登录</el-button>
      </template>
      <template v-else>
        <el-button plain @click="goRegister">注册</el-button>
        <el-button type="primary" @click="goLogin">登录</el-button>
      </template>
    </div>

    <button
      type="button"
      class="app-header__menu-btn"
      aria-label="打开菜单"
      @click="mobileMenuOpen = true"
    >
      ☰
    </button>
  </header>

  <MobileNav v-model="mobileMenuOpen" />
</template>
