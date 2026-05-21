<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { appNavigation } from '@/constants/navigation'
import MobileNav from '@/components/layout/MobileNav.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const mobileMenuOpen = ref(false)

const visibleNavigation = computed(() =>
  appNavigation.filter((item) => !item.auth || authStore.isLoggedIn)
)

const isHome = computed(() => route.meta?.navKey === '/app/home')

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
</script>

<template>
  <header class="app-header shell-card" :class="{ 'app-header--home': isHome }">
    <button type="button" class="app-header__brand" @click="router.push('/app/home')">
      <span class="app-header__badge">CM</span>
      <div>
        <div class="app-header__title">CampusMarket</div>
        <div class="app-header__subtitle">校园里的可信交易</div>
      </div>
    </button>

    <nav class="app-header__nav app-header__nav--desktop" aria-label="主导航">
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
