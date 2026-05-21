<script setup>
import AdminSidebar from '@/components/layout/AdminSidebar.vue'
import AdminTopbar from '@/components/layout/AdminTopbar.vue'
</script>

<template>
  <div class="admin-layout">
    <AdminSidebar />
    <div class="admin-layout__content">
      <AdminTopbar />
      <main class="admin-layout__main">
        <router-view />
      </main>
    </div>
  </div>
</template>
