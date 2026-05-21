<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  }
})
</script>

<template>
  <section v-if="items.length" class="trade-metric-strip">
    <article v-for="item in items" :key="item.label" class="trade-metric-strip__item shell-card">
      <span class="trade-metric-strip__label">{{ item.label }}</span>
      <strong class="trade-metric-strip__value">{{ item.value }}</strong>
      <p v-if="item.helper" class="trade-metric-strip__helper">{{ item.helper }}</p>
    </article>
  </section>
</template>

<style scoped>
.trade-metric-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.trade-metric-strip__item {
  display: grid;
  gap: 6px;
  padding: 20px;
}

.trade-metric-strip__label {
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.trade-metric-strip__value {
  font-size: clamp(22px, 2.4vw, 30px);
  line-height: 1.2;
  letter-spacing: -0.03em;
}

.trade-metric-strip__helper {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}
</style>
