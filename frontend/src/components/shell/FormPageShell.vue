<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <div class="page-stack">
    <div class="shell-card shell-hero shell-hero--compact">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <div class="shell-hero__meta">
        <slot name="summary" />
      </div>
    </div>

    <div class="shell-card">
      <div class="form-shell">
        <slot />
      </div>
    </div>
  </div>
</template>
