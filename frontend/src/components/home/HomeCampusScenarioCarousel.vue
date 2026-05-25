<script setup>
import { computed, nextTick, ref } from 'vue'

const activeIndex = ref(0)
const cardElements = ref([])
const flipStyles = ref(new Map())
const transitionSuppressedIndexes = ref(new Set())
let animationFrameId = 0

const scenarios = [
  {
    title: '宿舍用品',
    desc: '台灯、收纳、床品和小电器集中流转，适合楼下自提和同楼栋交接。',
    image: 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1400&q=80',
    tags: ['楼下自提', '同校面交', '收纳小件', '夜间照明']
  },
  {
    title: '教材书籍',
    desc: '课程教材、考研资料和学长学姐笔记按学期更新，少走弯路先看同校版本。',
    image: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=1400&q=80',
    tags: ['课程教材', '考研资料', '笔记讲义']
  },
  {
    title: '数码设备',
    desc: '耳机、键盘、显示器和桌面设备支持当面验机，成色和配件一次看清。',
    image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=1400&q=80',
    tags: ['当面验机', '桌面设备', '配件齐全']
  },
  {
    title: '毕业季闲置',
    desc: '离校前的整包转让、家具清仓和生活用品交接，围绕宿舍区快速完成。',
    image: 'https://images.unsplash.com/photo-1523580846011-d3a5bc25702b?auto=format&fit=crop&w=1400&q=80',
    tags: ['整包转让', '低价清仓', '近距离交接']
  },
  {
    title: '社团摊位',
    desc: '活动周边、手作物料和社团摊位货品集中上架，方便成员和路过同学找到。',
    image: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=1400&q=80',
    tags: ['活动周边', '摊位物料', '校园社群']
  },
  {
    title: '校园服务',
    desc: '打印装订、跑腿代取、技能互助等轻服务，保留校园半径内的效率。',
    image: 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1400&q=80',
    tags: ['打印装订', '跑腿代取', '技能互助']
  }
]

function mod(index, length) {
  return ((index % length) + length) % length
}

const cardSlots = {
  '-2': { left: -7, width: 5, state: 'edge' },
  '-1': { left: 0, width: 10, state: 'previous' },
  0: { left: 12, width: 56, state: 'active' },
  1: { left: 70, width: 15, state: 'next' },
  2: { left: 86.5, width: 6.5, state: 'after' },
  3: { left: 94.5, width: 2.4, state: 'edge' }
}

function getRelativeOffsetForActive(index, active) {
  let offset = index - active

  if (offset < -2) {
    offset += scenarios.length
  }

  if (offset > 3) {
    offset -= scenarios.length
  }

  return offset
}

function getRelativeOffset(index) {
  return getRelativeOffsetForActive(index, activeIndex.value)
}

function getSlotForOffset(offset) {
  return cardSlots[offset] || { left: 101, width: 2, state: 'hidden' }
}

function getWrapSuppressedIndexes(nextIndex) {
  return scenarios
    .map((_, index) => {
      const currentSlot = getSlotForOffset(getRelativeOffsetForActive(index, activeIndex.value))
      const nextSlot = getSlotForOffset(getRelativeOffsetForActive(index, nextIndex))
      return Math.abs(nextSlot.left - currentSlot.left) > 60 ? index : null
    })
    .filter((index) => index !== null)
}

const positionedScenarios = computed(() => {
  return scenarios.map((scenario, index) => {
    const offset = getRelativeOffset(index)
    const slot = getSlotForOffset(offset)

    return {
      ...scenario,
      index,
      state: slot.state,
      style: {
        '--scenario-card-x': `${slot.left}cqw`,
        '--scenario-card-width': `${slot.width}cqw`,
        ...(flipStyles.value.get(index) || {})
      }
    }
  })
})

function setCardElement(element, index) {
  if (element) {
    cardElements.value[index] = element
  }
}

function measureCards() {
  return cardElements.value.map((element) => {
    if (!element) {
      return null
    }

    const rect = element.getBoundingClientRect()
    return {
      left: rect.left,
      width: rect.width
    }
  })
}

async function moveToScenario(index) {
  const nextIndex = mod(index, scenarios.length)

  if (nextIndex === activeIndex.value) {
    return
  }

  if (animationFrameId) {
    window.cancelAnimationFrame(animationFrameId)
    animationFrameId = 0
  }

  const firstRects = measureCards()
  const suppressedIndexes = getWrapSuppressedIndexes(nextIndex)
  transitionSuppressedIndexes.value = new Set(scenarios.map((_, scenarioIndex) => scenarioIndex))
  activeIndex.value = nextIndex

  await nextTick()

  const lastRects = measureCards()
  const suppressedSet = new Set(suppressedIndexes)
  const nextFlipStyles = new Map()

  lastRects.forEach((lastRect, cardIndex) => {
    const firstRect = firstRects[cardIndex]

    if (!firstRect || !lastRect || suppressedSet.has(cardIndex) || lastRect.width === 0) {
      return
    }

    nextFlipStyles.set(cardIndex, {
      '--flip-dx': `${firstRect.left - lastRect.left}px`,
      '--flip-scale-x': firstRect.width / lastRect.width
    })
  })

  flipStyles.value = nextFlipStyles
  await nextTick()

  cardElements.value[0]?.getBoundingClientRect()

  animationFrameId = window.requestAnimationFrame(() => {
    flipStyles.value = new Map()
    transitionSuppressedIndexes.value = new Set()
    animationFrameId = 0
  })
}

function step(direction) {
  moveToScenario(activeIndex.value + direction)
}

function selectScenario(index) {
  moveToScenario(index)
}
</script>

<template>
  <section class="campus-scenarios" aria-labelledby="campus-scenarios-title">
    <div class="campus-scenarios__inner shell-container">
      <header class="campus-scenarios__header">
        <div class="campus-scenarios__heading">
          <span class="eyebrow">校园场景</span>
          <h2 id="campus-scenarios-title">常见的校园交易，先从场景看。</h2>
        </div>

        <div class="campus-scenarios__controls" aria-label="校园场景轮播控制">
          <button
            type="button"
            class="campus-scenarios__arrow"
            aria-label="上一个校园场景"
            @click="step(-1)"
          >
            <span aria-hidden="true">&larr;</span>
          </button>
          <button
            type="button"
            class="campus-scenarios__arrow"
            aria-label="下一个校园场景"
            @click="step(1)"
          >
            <span aria-hidden="true">&rarr;</span>
          </button>
        </div>
      </header>

      <div class="campus-scenarios__track" aria-live="polite">
        <button
          v-for="scenario in positionedScenarios"
          :key="scenario.index"
          type="button"
          class="campus-scenario-card"
          :class="[
            `is-${scenario.state}`,
            { 'is-transition-suppressed': transitionSuppressedIndexes.has(scenario.index) }
          ]"
          :style="scenario.style"
          :aria-current="scenario.index === activeIndex ? 'true' : undefined"
          :aria-hidden="scenario.state === 'hidden' ? 'true' : undefined"
          :aria-label="`查看${scenario.title}场景`"
          :ref="(element) => setCardElement(element, scenario.index)"
          @click="selectScenario(scenario.index)"
        >
          <span class="campus-scenario-card__image-wrap">
            <img
              class="campus-scenario-card__image"
              :src="scenario.image"
              :alt="scenario.title"
              loading="lazy"
              decoding="async"
            >
          </span>

          <span class="campus-scenario-card__preview-title">{{ scenario.title }}</span>

          <span class="campus-scenario-card__body">
            <span class="campus-scenario-card__title">{{ scenario.title }}</span>
            <span class="campus-scenario-card__desc">{{ scenario.desc }}</span>
            <span class="campus-scenario-card__tags">
              <span
                v-for="tag in scenario.tags"
                :key="tag"
                class="campus-scenario-card__tag"
              >
                {{ tag }}
              </span>
            </span>
          </span>
        </button>
      </div>

      <div class="campus-scenarios__dots" aria-label="选择校园场景">
        <button
          v-for="(scenario, index) in scenarios"
          :key="scenario.title"
          type="button"
          class="campus-scenarios__dot"
          :class="{ 'is-active': index === activeIndex }"
          :aria-label="`切换到${scenario.title}场景`"
          :aria-current="index === activeIndex ? 'true' : undefined"
          @click="selectScenario(index)"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.campus-scenarios {
  position: relative;
  padding: 18px 0 28px;
  background:
    linear-gradient(180deg, rgba(255, 250, 243, 0.18) 0%, #ffffff 24%, #ffffff 82%, rgba(255, 250, 243, 0.28) 100%);
}

.campus-scenarios__inner {
  display: grid;
  gap: 18px;
}

.campus-scenarios__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.campus-scenarios__heading {
  display: grid;
  gap: 10px;
  max-width: 640px;
}

.campus-scenarios__heading h2 {
  margin: 0;
  color: var(--cm-text);
  font-size: clamp(24px, 2.2vw, 34px);
  line-height: 1.12;
  letter-spacing: 0;
}

.campus-scenarios__controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.campus-scenarios__arrow {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 1px solid rgba(126, 87, 194, 0.18);
  border-radius: 12px;
  background: rgba(243, 235, 255, 0.86);
  color: #5f3ca1;
  cursor: pointer;
  font-size: 18px;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(92, 62, 146, 0.1);
  transition:
    transform var(--cm-transition-micro),
    border-color var(--cm-transition-micro),
    background-color var(--cm-transition-micro),
    box-shadow var(--cm-transition-micro);
}

.campus-scenarios__arrow:focus-visible {
  border-color: rgba(126, 87, 194, 0.34);
  background: #f7f1ff;
  box-shadow: 0 14px 30px rgba(92, 62, 146, 0.14);
}

.campus-scenarios__arrow:focus-visible,
.campus-scenario-card:focus-visible,
.campus-scenarios__dot:focus-visible {
  outline: 2px solid rgba(126, 87, 194, 0.36);
  outline-offset: 3px;
}

.campus-scenarios__track {
  position: relative;
  container-type: inline-size;
  overflow: hidden;
  height: clamp(390px, 42vw, 500px);
  --scenario-transition: 680ms cubic-bezier(0.16, 1, 0.3, 1);
  --flip-dx: 0px;
  --flip-scale-x: 1;
}

.campus-scenario-card {
  position: absolute;
  inset-block: 0;
  left: 0;
  display: grid;
  width: var(--scenario-card-width);
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(36, 25, 20, 0.08);
  border-radius: 8px;
  background: #fff;
  color: var(--cm-text);
  cursor: pointer;
  text-align: left;
  box-shadow: 0 18px 44px rgba(36, 25, 20, 0.08);
  contain: layout paint;
  transform: translate3d(var(--scenario-card-x), 0, 0) translate3d(var(--flip-dx), 0, 0) scaleX(var(--flip-scale-x));
  transform-origin: left center;
  backface-visibility: hidden;
  will-change: transform, opacity;
  transition:
    transform var(--scenario-transition),
    box-shadow var(--cm-transition-feature),
    opacity var(--scenario-transition),
    border-color var(--cm-transition-feature);
}

.campus-scenario-card.is-active {
  grid-template-rows: minmax(0, 1fr) auto;
  z-index: 4;
}

.campus-scenario-card:not(.is-active) {
  grid-template-rows: minmax(0, 1fr);
  opacity: 0.76;
  z-index: 2;
}

.campus-scenario-card.is-after {
  opacity: 0.56;
  z-index: 1;
}

.campus-scenario-card.is-edge {
  opacity: 0.84;
  z-index: 0;
}

.campus-scenario-card.is-hidden {
  opacity: 0;
  pointer-events: none;
  z-index: 0;
}

.campus-scenario-card.is-transition-suppressed {
  transition: none;
}

.campus-scenario-card__image-wrap {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.campus-scenario-card__image-wrap::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 42%;
  background: linear-gradient(0deg, rgba(12, 10, 9, 0.46), transparent);
  opacity: 0;
  transition: opacity var(--cm-transition-feature);
}

.campus-scenario-card:not(.is-active) .campus-scenario-card__image-wrap::after {
  opacity: 1;
}

.campus-scenario-card__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--cm-transition-feature);
}

.campus-scenario-card__preview-title {
  position: absolute;
  right: 12px;
  bottom: 12px;
  left: 12px;
  z-index: 1;
  display: none;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.3;
  text-shadow: 0 1px 12px rgba(0, 0, 0, 0.35);
}

.campus-scenario-card:not(.is-active) .campus-scenario-card__preview-title {
  display: block;
}

.campus-scenario-card__body {
  display: grid;
  gap: 12px;
  padding: 22px;
}

.campus-scenario-card:not(.is-active) .campus-scenario-card__body {
  display: none;
}

.campus-scenario-card__title {
  color: var(--cm-text);
  font-size: clamp(24px, 2.7vw, 40px);
  font-weight: 750;
  line-height: 1.08;
  letter-spacing: 0;
}

.campus-scenario-card__desc {
  max-width: 560px;
  color: var(--cm-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.campus-scenario-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.campus-scenario-card__tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 5px 10px;
  border: 1px solid rgba(36, 25, 20, 0.08);
  border-radius: 999px;
  background: rgba(247, 241, 255, 0.68);
  color: #5f3ca1;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
}

.campus-scenarios__dots {
  display: flex;
  justify-content: center;
  gap: 9px;
  padding-top: 2px;
}

.campus-scenarios__dot {
  width: 8px;
  height: 8px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgba(36, 25, 20, 0.18);
  cursor: pointer;
  transition:
    width var(--cm-transition-micro),
    background-color var(--cm-transition-micro);
}

.campus-scenarios__dot.is-active {
  width: 26px;
  background: #5f3ca1;
}

@media (max-width: 900px) {
  .campus-scenarios {
    padding: 12px 0 18px;
  }

  .campus-scenarios__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .campus-scenarios__controls {
    align-self: flex-end;
  }

  .campus-scenarios__track {
    height: clamp(390px, 78vw, 430px);
  }

  .campus-scenario-card {
    left: 0;
    width: 100%;
    min-height: 0;
    transform: none;
  }

  .campus-scenario-card.is-previous,
  .campus-scenario-card.is-next,
  .campus-scenario-card.is-after,
  .campus-scenario-card.is-edge,
  .campus-scenario-card.is-hidden {
    display: none;
  }

  .campus-scenario-card__body {
    padding: 18px;
  }

  .campus-scenario-card__title {
    font-size: 30px;
  }
}

@media (max-width: 520px) {
  .campus-scenarios__track {
    height: clamp(380px, 116vw, 410px);
  }

  .campus-scenarios__heading h2 {
    font-size: 28px;
  }

  .campus-scenarios__controls {
    width: 100%;
    justify-content: flex-end;
  }

  .campus-scenario-card__desc {
    font-size: 13px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .campus-scenarios__arrow,
  .campus-scenario-card,
  .campus-scenario-card__image,
  .campus-scenario-card__image-wrap::after,
  .campus-scenarios__dot {
    transition: none;
  }
}
</style>
