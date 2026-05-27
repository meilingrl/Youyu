<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { getTradeNavRoutes, TRADE_ROUTES } from '@/components/trade/trade-meta'

const props = defineProps({
  eyebrow: {
    type: String,
    default: 'Trade Center'
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  currentKey: {
    type: String,
    default: 'trade'
  },
  routes: {
    type: Array,
    default: () => TRADE_ROUTES
  }
})

const navRoutes = computed(() => getTradeNavRoutes(props.currentKey, props.routes))

function isCurrent(routeKey) {
  return props.currentKey === routeKey
}

function isNavigable(route) {
  return Boolean(route?.path)
}
</script>

<template>
  <div class="shell-container page-stack trade-page-shell">
    <section class="shell-card shell-hero shell-hero--compact trade-page-shell__hero">
      <div class="trade-page-shell__copy">
        <span class="eyebrow">{{ eyebrow }}</span>
        <h1>{{ title }}</h1>
        <p v-if="description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="trade-page-shell__actions">
        <slot name="actions" />
      </div>
    </section>

    <nav class="trade-route-nav shell-card" aria-label="交易中心导航">
      <component
        v-for="route in navRoutes"
        :key="route.key"
        :is="isNavigable(route) ? RouterLink : 'div'"
        :to="isNavigable(route) ? route.path : undefined"
        class="trade-route-nav__item"
        :class="{
          'is-active': isCurrent(route.key),
          'is-disabled': !isNavigable(route)
        }"
      >
        <span class="trade-route-nav__eyebrow">{{ route.eyebrow }}</span>
        <strong>{{ route.title }}</strong>
        <p>{{ route.description }}</p>
      </component>
    </nav>

    <slot name="metrics" />
    <div class="trade-page-shell__body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.trade-page-shell__hero {
  gap: 18px;
}

.trade-page-shell__copy {
  display: grid;
  gap: 12px;
}

.trade-page-shell__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
  align-items: center;
}

.trade-page-shell__body {
  display: grid;
  gap: 28px;
  min-height: clamp(320px, 40vh, 480px);
  align-content: start;
}

.trade-route-nav {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  padding: 14px;
}

.trade-route-nav__item {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.4);
  transition:
    transform var(--cm-transition),
    border-color var(--cm-transition),
    background-color var(--cm-transition),
    box-shadow var(--cm-transition);
}

.trade-route-nav__item:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--cm-primary-rgb), 0.14);
  box-shadow: var(--cm-shadow-soft);
}

.trade-route-nav__item.is-active {
  border-color: rgba(var(--cm-primary-rgb), 0.22);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(255, 246, 238, 0.88)),
    rgba(var(--cm-primary-rgb), 0.06);
  box-shadow: var(--cm-shadow-soft);
}

.trade-route-nav__item.is-disabled {
  cursor: default;
  opacity: 0.72;
}

.trade-route-nav__item.is-disabled:hover {
  transform: none;
  border-color: transparent;
  box-shadow: none;
}

.trade-route-nav__eyebrow {
  color: var(--cm-text-tertiary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.trade-route-nav strong {
  font-size: 15px;
  line-height: 1.35;
}

.trade-route-nav p {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 1120px) {
  .trade-route-nav {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .trade-page-shell__body {
    min-height: 260px;
  }

  .trade-page-shell__actions {
    justify-content: stretch;
  }

  .trade-page-shell__actions :deep(.el-button) {
    width: 100%;
  }

  .trade-route-nav {
    grid-template-columns: 1fr;
  }
}
</style>
