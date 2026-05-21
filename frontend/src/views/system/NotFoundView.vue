<template>
  <div class="not-found-page">
    <div class="shell-card not-found-page__card">
      <span class="eyebrow">404</span>
      <h1>页面不存在</h1>
      <p>当前页面尚未建立或路由地址不正确。</p>
      <div class="shell-inline-actions">
        <el-button plain @click="$router.push('/app/home')">前台首页</el-button>
        <el-button type="primary" @click="$router.push('/admin/dashboard')">后台首页</el-button>
      </div>
    </div>
  </div>
</template>
