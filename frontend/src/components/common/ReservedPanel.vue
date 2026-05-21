<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  },
  icon: {
    type: String,
    default: '📊'
  }
})
</script>

<template>
  <div class="reserved-panel" role="status">
    <div class="reserved-panel__icon" aria-hidden="true">{{ icon }}</div>
    <h3 class="reserved-panel__title">{{ title }}</h3>
    <p class="reserved-panel__desc">{{ description }}</p>
  </div>
</template>

<style scoped>
.reserved-panel {
  min-height: 160px;
  display: grid;
  gap: 10px;
  justify-items: center;
  align-content: center;
  padding: 28px 24px;
  border-radius: var(--cm-radius-lg);
  border: 1px dashed var(--cm-border-strong);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.7), rgba(244, 247, 251, 0.55));
  text-align: center;
}

.reserved-panel__icon {
  font-size: 32px;
  line-height: 1;
  opacity: 0.6;
}

.reserved-panel__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--cm-text-secondary);
}

.reserved-panel__desc {
  margin: 0;
  max-width: 360px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--cm-text-tertiary);
}
</style>
