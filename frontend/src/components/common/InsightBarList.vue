<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  emptyText: {
    type: String,
    default: '暂无可展示数据'
  }
})

const normalizedItems = computed(() => {
  const raw = Array.isArray(props.items) ? props.items : []
  const max = raw.reduce((currentMax, item) => Math.max(currentMax, Number(item?.value || 0)), 0)

  return raw.map((item, index) => {
    const value = Number(item?.value || 0)
    const width = max > 0 ? Math.max((value / max) * 100, value > 0 ? 8 : 0) : 0
    return {
      id: item?.id || item?.label || `metric-${index}`,
      label: item?.label || '--',
      helper: item?.helper || '',
      displayValue: item?.displayValue || String(value),
      tone: item?.tone || 'primary',
      width
    }
  })
})
</script>

<template>
  <div v-if="normalizedItems.length" class="insight-bar-list">
    <article v-for="item in normalizedItems" :key="item.id" class="insight-bar-list__item">
      <div class="insight-bar-list__row">
        <strong>{{ item.label }}</strong>
        <span>{{ item.displayValue }}</span>
      </div>
      <div class="insight-bar-list__track">
        <div class="insight-bar-list__fill" :class="`is-${item.tone}`" :style="{ width: `${item.width}%` }" />
      </div>
      <p v-if="item.helper" class="insight-bar-list__helper">{{ item.helper }}</p>
    </article>
  </div>
  <div v-else class="insight-bar-list__empty">{{ emptyText }}</div>
</template>

<style scoped>
.insight-bar-list {
  display: grid;
  gap: 14px;
}

.insight-bar-list__item {
  display: grid;
  gap: 8px;
}

.insight-bar-list__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.insight-bar-list__row strong {
  font-size: 14px;
}

.insight-bar-list__row span,
.insight-bar-list__helper,
.insight-bar-list__empty {
  color: var(--cm-text-secondary);
}

.insight-bar-list__track {
  overflow: hidden;
  height: 10px;
  border-radius: var(--cm-radius-pill);
  background: rgba(88, 62, 43, 0.08);
}

.insight-bar-list__fill {
  height: 100%;
  border-radius: inherit;
  background: var(--cm-gradient-primary);
}

.insight-bar-list__fill.is-success {
  background: linear-gradient(90deg, #4f8f69 0%, #7fb486 100%);
}

.insight-bar-list__fill.is-warning {
  background: linear-gradient(90deg, #c47a2c 0%, #e4a55a 100%);
}

.insight-bar-list__fill.is-info {
  background: linear-gradient(90deg, #7b6a5d 0%, #b09d8e 100%);
}

.insight-bar-list__helper {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
}

.insight-bar-list__empty {
  min-height: 84px;
  display: grid;
  place-items: center;
  border: 1px dashed var(--cm-border-strong);
  border-radius: var(--cm-radius-md);
  background: rgba(255, 255, 255, 0.52);
}
</style>
