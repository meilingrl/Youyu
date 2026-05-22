<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

const trackRef = ref(null)
const activeIndex = ref(0)
const activeVirtualIndex = ref(0)
const isAnimating = ref(false)

let wheelLockTimer = null

const props = defineProps({
  products: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['open-product'])

const stageProducts = computed(() => props.products.slice(0, 6))
const canBrowse = computed(() => stageProducts.value.length > 1)
const loopedProducts = computed(() => {
  if (!canBrowse.value) {
    return stageProducts.value
  }

  return [...stageProducts.value, ...stageProducts.value, ...stageProducts.value]
})

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

function getCardNodes() {
  return trackRef.value ? Array.from(trackRef.value.querySelectorAll('.featured-stage__card')) : []
}

function mod(index, length) {
  return ((index % length) + length) % length
}

function clearWheelLockTimer() {
  if (wheelLockTimer) {
    clearTimeout(wheelLockTimer)
    wheelLockTimer = null
  }
}

function syncLogicalIndex() {
  const length = stageProducts.value.length
  activeIndex.value = length ? mod(activeVirtualIndex.value, length) : 0
}

function centerVirtualIndexForLogical(logicalIndex) {
  const length = stageProducts.value.length
  if (!length) {
    return 0
  }

  return canBrowse.value ? length + mod(logicalIndex, length) : 0
}

function scrollToVirtualIndex(index, options = {}) {
  const track = trackRef.value
  const cards = getCardNodes()
  const target = cards[index]

  if (!track || !target) {
    return
  }

  const left = target.offsetLeft - (track.clientWidth - target.clientWidth) / 2
  track.scrollTo({
    left,
    behavior: options.behavior ?? 'smooth'
  })
}

function normalizeLoopPosition() {
  const length = stageProducts.value.length
  if (!trackRef.value || !canBrowse.value || !length) {
    return
  }

  const normalizedVirtualIndex = centerVirtualIndexForLogical(activeIndex.value)
  if (normalizedVirtualIndex === activeVirtualIndex.value) {
    return
  }

  activeVirtualIndex.value = normalizedVirtualIndex
  scrollToVirtualIndex(normalizedVirtualIndex, { behavior: 'auto' })
}

function finishStep() {
  syncLogicalIndex()
  normalizeLoopPosition()
  isAnimating.value = false
  clearWheelLockTimer()
}

function scheduleStepCompletion() {
  clearWheelLockTimer()
  wheelLockTimer = setTimeout(() => {
    finishStep()
  }, 430)
}

function stepBy(direction) {
  const length = stageProducts.value.length
  if (!canBrowse.value || !length || isAnimating.value) {
    return
  }

  isAnimating.value = true
  activeVirtualIndex.value += direction
  syncLogicalIndex()
  scrollToVirtualIndex(activeVirtualIndex.value)
  scheduleStepCompletion()
}

function scrollByDirection(direction) {
  stepBy(direction)
}

function jumpToIndex(index) {
  if (!stageProducts.value.length || isAnimating.value) {
    return
  }

  const length = stageProducts.value.length
  const current = activeIndex.value
  let delta = mod(index - current, length)
  if (delta > length / 2) {
    delta -= length
  }

  if (!delta) {
    return
  }

  stepBy(delta)
}

function handleWheelScroll(event) {
  if (!canBrowse.value) {
    return
  }

  if (Math.abs(event.deltaY) <= Math.abs(event.deltaX)) {
    return
  }

  event.preventDefault()
  stepBy(event.deltaY > 0 ? 1 : -1)
}

watch(
  () => stageProducts.value.length,
  async () => {
    clearWheelLockTimer()
    isAnimating.value = false
    activeIndex.value = 0
    activeVirtualIndex.value = centerVirtualIndexForLogical(0)

    await nextTick()
    scrollToVirtualIndex(activeVirtualIndex.value, { behavior: 'auto' })
  },
  { immediate: true }
)

watch(
  () => props.loading,
  async (loading) => {
    if (loading || !stageProducts.value.length) {
      return
    }

    await nextTick()
    activeVirtualIndex.value = centerVirtualIndexForLogical(activeIndex.value)
    scrollToVirtualIndex(activeVirtualIndex.value, { behavior: 'auto' })
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  clearWheelLockTimer()
})
</script>

<template>
  <section class="featured-stage" aria-label="精选好物">
    <div class="featured-stage__header shell-container">
      <div class="featured-stage__heading">
        <span class="eyebrow">Featured Picks</span>
        <h2>精选好物</h2>
        <p>先看这一组最值得打开的校园好物，再决定下一步去逛什么。</p>
      </div>

      <div class="featured-stage__actions">
        <button
          v-if="canBrowse"
          type="button"
          class="featured-stage__nav"
          aria-label="上一张"
          @click="scrollByDirection(-1)"
        >
          ←
        </button>
        <button
          v-if="canBrowse"
          type="button"
          class="featured-stage__nav"
          aria-label="下一张"
          @click="scrollByDirection(1)"
        >
          →
        </button>
        <router-link to="/app/explore" class="featured-stage__more">查看全部 →</router-link>
      </div>
    </div>

    <div
      v-if="loading"
      ref="trackRef"
      class="featured-stage__viewport featured-stage__viewport--skeleton"
      @wheel="handleWheelScroll"
    >
      <div class="featured-stage__track">
        <div v-for="index in 5" :key="index" class="featured-stage__skeleton" />
      </div>
    </div>

    <template v-else-if="stageProducts.length">
      <div
        ref="trackRef"
        class="featured-stage__viewport"
        @wheel="handleWheelScroll"
      >
        <div class="featured-stage__track">
          <article
            v-for="(product, index) in loopedProducts"
            :key="`${product.id}-${index}`"
            class="featured-stage__card"
            :class="{ 'is-active': index === activeVirtualIndex }"
            tabindex="0"
            @click="emit('open-product', product)"
            @keyup.enter="emit('open-product', product)"
          >
            <img
              class="featured-stage__image"
              :src="product.coverUrl || product.cover"
              :alt="product.title"
              loading="lazy"
              decoding="async"
            />

            <div class="featured-stage__vignette" />

            <div class="featured-stage__topline">
              <span class="featured-stage__brand">CampusMarket</span>
              <span class="featured-stage__category">{{ product.categoryName || '校园精选' }}</span>
            </div>

            <div class="featured-stage__content">
              <div class="featured-stage__copy">
                <h3>{{ product.title }}</h3>
                <p>{{ product.shopName || '校园卖家' }} · 今天值得优先打开的一件好物。</p>
              </div>

              <div class="featured-stage__footer">
                <span class="featured-stage__cta">立即查看</span>
                <span class="featured-stage__price">¥{{ formatPrice(product.salePrice ?? product.price) }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div v-if="canBrowse" class="featured-stage__dots" aria-label="分页">
        <button
          v-for="(_, index) in stageProducts"
          :key="index"
          type="button"
          class="featured-stage__dot"
          :class="{ 'is-active': index === activeIndex }"
          :aria-label="`跳转到第 ${index + 1} 张`"
          @click="jumpToIndex(index)"
        />
      </div>
    </template>
  </section>
</template>

<style scoped>
.featured-stage {
  display: grid;
  gap: 20px;
}

.featured-stage__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.featured-stage__heading {
  display: grid;
  gap: 10px;
}

.featured-stage__heading h2 {
  margin: 0;
  font-size: clamp(24px, 2.2vw, 32px);
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--cm-text);
}

.featured-stage__heading p {
  max-width: 680px;
  color: var(--cm-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.featured-stage__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.featured-stage__nav {
  width: 42px;
  height: 42px;
  border: 1px solid rgba(36, 25, 20, 0.14);
  border-radius: 999px;
  background: rgba(255, 250, 243, 0.88);
  color: var(--cm-text);
  cursor: pointer;
  transition:
    transform var(--cm-transition-micro),
    background var(--cm-transition-micro),
    border-color var(--cm-transition-micro);
}

.featured-stage__nav:hover {
  transform: translateY(-1px);
  background: #fff;
  border-color: rgba(36, 25, 20, 0.22);
}

.featured-stage__more {
  color: var(--cm-primary-deep);
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.featured-stage__viewport {
  overflow: hidden;
  user-select: none;
  overscroll-behavior-x: contain;
}

.featured-stage__track {
  display: flex;
  gap: 18px;
  padding-inline: calc(50vw - min(1120px, 78vw) / 2);
  padding-bottom: 12px;
}

.featured-stage__card,
.featured-stage__skeleton {
  position: relative;
  flex: 0 0 min(1120px, 78vw);
  height: clamp(420px, 55vw, 620px);
  border-radius: 28px;
  overflow: hidden;
}

.featured-stage__card {
  cursor: pointer;
  background: #e9dfd0;
  box-shadow: 0 24px 60px rgba(40, 28, 20, 0.14);
  transform: scale(0.94);
  opacity: 0.54;
  transition:
    transform 360ms var(--cm-ease-enter),
    opacity 360ms var(--cm-ease-enter),
    box-shadow 360ms var(--cm-ease-enter);
}

.featured-stage__card.is-active {
  transform: scale(1);
  opacity: 1;
  box-shadow: 0 36px 80px rgba(40, 28, 20, 0.18);
}

.featured-stage__card:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.88);
  outline-offset: -8px;
}

.featured-stage__image,
.featured-stage__vignette {
  position: absolute;
  inset: 0;
}

.featured-stage__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-stage__vignette {
  background:
    linear-gradient(180deg, rgba(11, 10, 10, 0.08) 0%, rgba(11, 10, 10, 0.02) 32%, rgba(11, 10, 10, 0.68) 100%),
    linear-gradient(90deg, rgba(11, 10, 10, 0.42) 0%, rgba(11, 10, 10, 0.08) 40%, rgba(11, 10, 10, 0.18) 100%);
}

.featured-stage__topline {
  position: absolute;
  inset: 22px 24px auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  z-index: 1;
}

.featured-stage__brand,
.featured-stage__category {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  backdrop-filter: blur(12px);
}

.featured-stage__brand {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.featured-stage__category {
  background: rgba(255, 250, 243, 0.88);
  color: var(--cm-text);
  font-size: 12px;
  font-weight: 700;
}

.featured-stage__content {
  position: absolute;
  inset: auto 28px 28px;
  z-index: 1;
  display: grid;
  gap: 18px;
}

.featured-stage__copy {
  display: grid;
  gap: 8px;
  max-width: 520px;
}

.featured-stage__copy h3 {
  margin: 0;
  color: #fff;
  font-size: clamp(38px, 6.2vw, 84px);
  line-height: 0.95;
  letter-spacing: -0.05em;
  font-weight: 800;
}

.featured-stage__copy p {
  color: rgba(255, 255, 255, 0.84);
  font-size: 16px;
  line-height: 1.55;
}

.featured-stage__footer {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.featured-stage__cta {
  display: inline-flex;
  align-items: center;
  min-height: 54px;
  padding: 0 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  color: #1f1f1f;
  font-size: 15px;
  font-weight: 700;
}

.featured-stage__price {
  color: rgba(255, 255, 255, 0.92);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.featured-stage__dots {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.featured-stage__dot {
  width: 10px;
  height: 10px;
  border: 0;
  border-radius: 999px;
  background: rgba(88, 62, 43, 0.22);
  cursor: pointer;
  transition:
    transform var(--cm-transition-micro),
    background var(--cm-transition-micro);
}

.featured-stage__dot.is-active {
  transform: scale(1.18);
  background: rgba(36, 25, 20, 0.78);
}

.featured-stage__skeleton {
  background: linear-gradient(
    90deg,
    rgba(240, 232, 224, 0.8) 25%,
    rgba(248, 243, 236, 0.95) 50%,
    rgba(240, 232, 224, 0.8) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.6s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
}

@media (max-width: 900px) {
  .featured-stage__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .featured-stage__actions {
    width: 100%;
    justify-content: space-between;
  }

  .featured-stage__track {
    gap: 14px;
    padding-inline: calc(50vw - min(88vw, 680px) / 2);
  }

  .featured-stage__card,
  .featured-stage__skeleton {
    flex-basis: min(88vw, 680px);
    height: clamp(380px, 112vw, 560px);
  }

  .featured-stage__copy h3 {
    font-size: clamp(30px, 13vw, 58px);
  }

  .featured-stage__copy p {
    font-size: 14px;
  }
}
</style>
