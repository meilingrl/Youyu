<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import MobileBottomNav from '@/components/layout/MobileBottomNav.vue'

const route = useRoute()
const isHome = computed(() => route.meta?.navKey === '/app/home')
</script>

<template>
  <div class="market-layout">
    <AppHeader v-if="!isHome" />
    <main class="market-layout__main">
      <router-view />
    </main>
    <AppFooter v-if="!isHome" />
    <MobileBottomNav v-if="!isHome" />
  </div>
</template>
