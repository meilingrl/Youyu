<script setup>
import { computed } from 'vue'
import {
  getFulfillmentTypeMeta,
  getOrderStatusMeta,
  getPaymentStatusMeta
} from '@/components/trade/trade-meta'

const props = defineProps({
  kind: {
    type: String,
    default: 'order'
  },
  value: {
    type: String,
    default: ''
  }
})

const meta = computed(() => {
  if (props.kind === 'payment') {
    return getPaymentStatusMeta(props.value)
  }
  if (props.kind === 'fulfillment') {
    return getFulfillmentTypeMeta(props.value)
  }
  return getOrderStatusMeta(props.value)
})
</script>

<template>
  <span class="trade-status-tag" :class="`trade-status-tag--${meta.tone || 'muted'}`">
    {{ meta.label }}
  </span>
</template>

<style scoped>
.trade-status-tag {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.trade-status-tag--primary {
  background: rgba(var(--cm-primary-rgb), 0.1);
  border-color: rgba(var(--cm-primary-rgb), 0.18);
  color: var(--cm-primary);
}

.trade-status-tag--info {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.18);
  color: #2858a9;
}

.trade-status-tag--warning {
  background: rgba(245, 158, 11, 0.14);
  border-color: rgba(245, 158, 11, 0.22);
  color: #9a5b07;
}

.trade-status-tag--success {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.18);
  color: #0f7a57;
}

.trade-status-tag--danger {
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.18);
  color: #b33434;
}

.trade-status-tag--muted {
  background: rgba(115, 102, 91, 0.1);
  border-color: rgba(115, 102, 91, 0.14);
  color: var(--cm-text-secondary);
}
</style>
