<script setup>
defineProps({
  /** Number of skeleton cards to show in a grid-friendly block */
  count: {
    type: Number,
    default: 1
  }
})
</script>

<template>
  <div class="cm-skeleton-grid" aria-hidden="true">
    <div v-for="n in count" :key="n" class="cm-skeleton-card">
      <div class="cm-skeleton-card__image cm-skeleton-shimmer" />
      <div class="cm-skeleton-card__body">
        <div class="cm-skeleton-line cm-skeleton-line--short cm-skeleton-shimmer" />
        <div class="cm-skeleton-line cm-skeleton-shimmer" />
        <div class="cm-skeleton-line cm-skeleton-line--medium cm-skeleton-shimmer" />
        <div class="cm-skeleton-line cm-skeleton-line--btn cm-skeleton-shimmer" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.cm-skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}

.cm-skeleton-card {
  overflow: hidden;
  border-radius: var(--cm-radius-lg);
  border: 1px solid var(--cm-border);
  background: var(--cm-surface-strong);
  box-shadow: var(--cm-shadow-soft);
}

.cm-skeleton-card__image {
  aspect-ratio: 1 / 1;
  width: 100%;
}

.cm-skeleton-card__body {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.cm-skeleton-line {
  height: 14px;
  border-radius: 8px;
  max-width: 100%;
}

.cm-skeleton-line--short {
  max-width: 45%;
}

.cm-skeleton-line--medium {
  max-width: 70%;
}

.cm-skeleton-line--btn {
  height: 40px;
  max-width: 120px;
  border-radius: 999px;
  margin-top: 4px;
}

.cm-skeleton-shimmer {
  background: linear-gradient(
    90deg,
    var(--cm-bg-soft) 0%,
    rgba(255, 255, 255, 0.95) 50%,
    var(--cm-bg-soft) 100%
  );
  background-size: 200% 100%;
  animation: cm-shimmer 1.2s ease-in-out infinite;
}

@keyframes cm-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
