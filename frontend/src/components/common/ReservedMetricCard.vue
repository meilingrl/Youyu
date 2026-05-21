<script setup>
defineProps({
  label: {
    type: String,
    required: true
  },
  scope: {
    type: String,
    required: true
  },
  dataSource: {
    type: String,
    default: ''
  },
  unit: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <div class="reserved-metric-card">
    <span class="reserved-metric-card__label">{{ label }}</span>
    <span class="reserved-metric-card__badge">待接入</span>
    <p class="reserved-metric-card__scope">{{ scope }}</p>
    <p v-if="dataSource" class="reserved-metric-card__source">数据源：{{ dataSource }}</p>
  </div>
</template>

<style scoped>
.reserved-metric-card {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: var(--cm-radius-sm);
  border: 1px dashed var(--cm-border-strong);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.7), rgba(244, 247, 251, 0.55));
}

.reserved-metric-card__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--cm-text-secondary);
}

.reserved-metric-card__badge {
  justify-self: start;
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--cm-accent);
  background: var(--cm-accent-soft);
}

.reserved-metric-card__scope {
  margin: 0;
  font-size: 12px;
  line-height: 1.55;
  color: var(--cm-text-tertiary);
}

.reserved-metric-card__source {
  margin: 0;
  font-size: 11px;
  font-family: "SF Mono", "Cascadia Code", "Consolas", monospace;
  color: var(--cm-text-tertiary);
  opacity: 0.75;
}
</style>
