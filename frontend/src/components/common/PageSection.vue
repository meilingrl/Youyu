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
  <section class="page-section shell-card">
    <header class="page-section__header">
      <div>
        <h2>{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <div class="page-section__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="page-section__body">
      <slot />
    </div>
  </section>
</template>
