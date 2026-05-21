<script setup>
defineProps({
  emoji: {
    type: String,
    default: '📦'
  },
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
  <div class="cm-empty-state shell-card" role="status">
    <div class="cm-empty-state__emoji" aria-hidden="true">{{ emoji }}</div>
    <h2 class="cm-empty-state__title">{{ title }}</h2>
    <p v-if="description" class="cm-empty-state__desc">{{ description }}</p>
    <div v-if="$slots.default" class="cm-empty-state__actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.cm-empty-state {
  display: grid;
  gap: 12px;
  justify-items: start;
  padding: 28px 24px;
  text-align: left;
}

.cm-empty-state__emoji {
  font-size: 40px;
  line-height: 1;
}

.cm-empty-state__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.cm-empty-state__desc {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.65;
  max-width: 420px;
}

.cm-empty-state__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 4px;
}
</style>
