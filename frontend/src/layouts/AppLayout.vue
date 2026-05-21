<script setup>
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import MobileBottomNav from '@/components/layout/MobileBottomNav.vue'
</script>

<template>
  <div class="market-layout">
    <AppHeader />
    <main class="market-layout__main">
      <router-view />
    </main>
    <AppFooter />
    <MobileBottomNav />
  </div>
</template>
