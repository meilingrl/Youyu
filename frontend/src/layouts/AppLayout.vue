<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import MobileBottomNav from '@/components/layout/MobileBottomNav.vue'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()
const isHome = computed(() => route.meta?.navKey === '/app/home')

let rafId = null

function onScroll() {
  if (rafId) return
  rafId = requestAnimationFrame(() => {
    appStore.setScrollY(window.scrollY)
    rafId = null
  })
}

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  if (rafId) cancelAnimationFrame(rafId)
})
</script>

<template>
  <div class="market-layout">
    <AppHeader :reveal-on-hover="isHome" />
    <main class="market-layout__main">
      <router-view />
    </main>
    <AppFooter v-if="!isHome" />
    <MobileBottomNav v-if="!isHome" />
  </div>
</template>
