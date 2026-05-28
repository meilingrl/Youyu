<script setup>
import { getCurrentInstance, onBeforeMount, ref } from 'vue'
import AdminSidebar from '@/components/layout/AdminSidebar.vue'
import AdminTopbar from '@/components/layout/AdminTopbar.vue'

const adminElementPlusReady = ref(false)
const app = getCurrentInstance().appContext.app

onBeforeMount(async () => {
  const { installElementPlusAdmin } = await import('@/plugins/element-plus-admin')
  installElementPlusAdmin(app)
  adminElementPlusReady.value = true
})
</script>

<template>
  <div class="admin-layout">
    <AdminSidebar />
    <div class="admin-layout__content">
      <AdminTopbar />
      <main class="admin-layout__main">
        <router-view v-if="adminElementPlusReady" />
      </main>
    </div>
  </div>
</template>
