<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import SearchSuggestInput from '@/components/search/SearchSuggestInput.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  categories: { type: Array, default: () => [] },
  productTypes: { type: Array, default: () => [] },
  selectedCategoryId: { type: String, default: '' },
  selectedProductType: { type: String, default: '' },
  suggestions: { type: Array, default: () => [] },
  loadingSuggestions: { type: Boolean, default: false },
  suggestionError: { type: String, default: '' },
  searchHistory: { type: Array, default: () => [] },
  hotKeywords: { type: Array, default: () => [] },
  loadingHotKeywords: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:modelValue',
  'change',
  'submit',
  'select-suggestion',
  'select-category',
  'select-product-type',
  'apply-history',
  'apply-hot',
  'clear'
])

const openPanel = ref(null)

const activeCount = computed(() => {
  let count = 0
  if (String(props.modelValue || '').trim()) count += 1
  if (props.selectedCategoryId) count += 1
  if (props.selectedProductType) count += 1
  return count
})

const hasDiscovery = computed(() => props.searchHistory.length || props.hotKeywords.length)

const currentCategoryLabel = computed(
  () => props.categories.find((c) => String(c.id || '') === props.selectedCategoryId)?.name || '全部分类'
)

const currentProductTypeLabel = computed(
  () => props.productTypes.find((t) => String(t.id || '') === props.selectedProductType)?.name || '全部类型'
)

function togglePanel(name) {
  openPanel.value = openPanel.value === name ? null : name
}

function closePanel() {
  openPanel.value = null
}

function selectCategory(id) {
  emit('select-category', id)
  closePanel()
}

function selectProductType(id) {
  emit('select-product-type', id)
  closePanel()
}

function onClickOutside(e) {
  if (!e.target.closest('.explore-search-shell')) closePanel()
}

onMounted(() => document.addEventListener('click', onClickOutside))
onUnmounted(() => document.removeEventListener('click', onClickOutside))
</script>

<template>
  <!-- 单层胶囊：展开时是圆角卡片，condensed 时是胶囊 -->
  <section
    class="explore-search-shell"
    :class="{ 'has-panel': openPanel }"
    aria-label="搜索与筛选"
  >
    <!-- 搜索栏主体 -->
    <div class="explore-search-shell__bar">
      <!-- 搜索段 -->
      <div class="explore-search-shell__seg explore-search-shell__seg--search">
        <span class="explore-search-shell__seg-label">搜什么</span>
        <SearchSuggestInput
          class="explore-search-shell__search-input"
          :model-value="modelValue"
          placeholder="搜索耳机、教材、宿舍好物…"
          button-label="搜索"
          :suggestions="suggestions"
          :loading="loadingSuggestions"
          :error="suggestionError"
          @update:model-value="emit('update:modelValue', $event)"
          @change="emit('change', $event)"
          @submit="emit('submit', $event)"
          @select-suggestion="emit('select-suggestion', $event)"
        />
      </div>

      <span class="explore-search-shell__divider" aria-hidden="true" />

      <!-- 分类段 -->
      <button
        type="button"
        class="explore-search-shell__seg explore-search-shell__seg--btn"
        :class="{ 'is-open': openPanel === 'category', 'is-active': selectedCategoryId }"
        @click.stop="togglePanel('category')"
      >
        <span class="explore-search-shell__seg-label">分类</span>
        <strong class="explore-search-shell__seg-value">{{ currentCategoryLabel }}</strong>
      </button>

      <span class="explore-search-shell__divider" aria-hidden="true" />

      <!-- 类型段 -->
      <button
        type="button"
        class="explore-search-shell__seg explore-search-shell__seg--btn"
        :class="{ 'is-open': openPanel === 'type', 'is-active': selectedProductType }"
        @click.stop="togglePanel('type')"
      >
        <span class="explore-search-shell__seg-label">类型</span>
        <strong class="explore-search-shell__seg-value">{{ currentProductTypeLabel }}</strong>
      </button>

      <!-- 搜索按钮 -->
      <button
        type="button"
        class="explore-search-shell__action"
        aria-label="提交搜索"
        @click="emit('submit', modelValue)"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M11 5a6 6 0 1 0 0 12a6 6 0 0 0 0-12m0-2a8 8 0 1 1 0 16a8 8 0 0 1 0-16m9.707 16.293l-3.4-3.4a1 1 0 1 0-1.414 1.414l3.4 3.4a1 1 0 0 0 1.414-1.414"
            fill="currentColor"
          />
        </svg>
      </button>
    </div>

    <!-- 分类 dropdown -->
    <Transition name="panel">
      <div
        v-if="openPanel === 'category'"
        class="explore-search-shell__panel"
        role="dialog"
        aria-label="选择分类"
        @click.stop
      >
        <p class="explore-search-shell__panel-title">选择分类</p>
        <div class="explore-search-shell__panel-chips" role="group">
          <button
            v-for="category in categories"
            :key="category.id || 'all'"
            type="button"
            class="explore-search-shell__chip"
            :class="{ 'is-active': selectedCategoryId === category.id }"
            @click="selectCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </div>
      </div>
    </Transition>

    <!-- 类型 dropdown -->
    <Transition name="panel">
      <div
        v-if="openPanel === 'type'"
        class="explore-search-shell__panel explore-search-shell__panel--type"
        role="dialog"
        aria-label="选择类型"
        @click.stop
      >
        <p class="explore-search-shell__panel-title">选择类型</p>
        <div class="explore-search-shell__panel-chips" role="group">
          <button
            v-for="productType in productTypes"
            :key="productType.id || 'all'"
            type="button"
            class="explore-search-shell__chip"
            :class="{ 'is-active': selectedProductType === productType.id }"
            @click="selectProductType(productType.id)"
          >
            {{ productType.name }}
          </button>
        </div>
      </div>
    </Transition>

    <!-- 热门/历史发现区（无 panel 时显示） -->
    <Transition name="discovery">
      <div v-if="hasDiscovery && !openPanel" class="explore-search-shell__discovery">
        <div v-if="searchHistory.length" class="explore-search-shell__discovery-group">
          <span class="explore-search-shell__meta-label">最近搜过</span>
          <div class="explore-search-shell__chips">
            <button
              v-for="item in searchHistory"
              :key="item"
              type="button"
              class="explore-search-shell__chip explore-search-shell__chip--soft"
              @click="emit('apply-history', item)"
            >
              {{ item }}
            </button>
          </div>
        </div>
        <div v-if="hotKeywords.length" class="explore-search-shell__discovery-group">
          <span class="explore-search-shell__meta-label">热门搜索</span>
          <div class="explore-search-shell__chips">
            <button
              v-for="kw in hotKeywords.slice(0, 6)"
              :key="kw.normalizedKeyword || kw.keyword"
              type="button"
              class="explore-search-shell__chip explore-search-shell__chip--soft"
              @click="emit('apply-hot', kw.keyword)"
            >
              {{ kw.keyword }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 激活筛选条 -->
    <div v-if="activeCount > 0 && !openPanel" class="explore-search-shell__active-bar">
      <span class="explore-search-shell__active-count">{{ activeCount }} 个条件生效</span>
      <button type="button" class="explore-search-shell__clear-btn" @click="emit('clear')">
        清空全部
      </button>
    </div>
  </section>
</template>

<style scoped>
/* ── 单层容器：展开态是圆角卡片，condensed 是胶囊 ── */
.explore-search-shell {
  position: relative;
  display: grid;
  gap: 0;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(88, 62, 43, 0.1);
  box-shadow: 0 8px 40px rgba(88, 62, 43, 0.1);
  overflow: visible;
  transition:
    border-radius var(--cm-transition-feature),
    box-shadow var(--cm-transition-feature),
    background var(--cm-transition-feature);
}

/* ── 搜索栏主体行 ── */
.explore-search-shell__bar {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) auto minmax(140px, 0.65fr) auto minmax(140px, 0.65fr) auto;
  align-items: center;
  padding: 8px 8px 8px 24px;
}

/* ── 段落 ── */
.explore-search-shell__seg {
  display: grid;
  gap: 2px;
  padding: 10px 20px;
  min-width: 0;
}

.explore-search-shell__seg--search {
  padding-left: 0;
}

.explore-search-shell__seg--btn {
  appearance: none;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  border-radius: 20px;
  padding: 10px 20px;
  transition: background var(--cm-transition-micro);
}

.explore-search-shell__seg--btn:hover {
  background: rgba(var(--cm-primary-rgb), 0.05);
}

.explore-search-shell__seg--btn.is-open {
  background: rgba(var(--cm-primary-rgb), 0.07);
}

.explore-search-shell__seg-label {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--cm-text);
}

.explore-search-shell__seg-value {
  font-size: 14px;
  font-weight: 400;
  color: var(--cm-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.explore-search-shell__seg--btn.is-active .explore-search-shell__seg-value {
  color: var(--cm-primary-deep);
  font-weight: 600;
}

/* ── 分隔线 ── */
.explore-search-shell__divider {
  width: 1px;
  height: 32px;
  background: rgba(88, 62, 43, 0.1);
  flex-shrink: 0;
}

/* ── 搜索按钮 ── */
.explore-search-shell__action {
  align-self: center;
  justify-self: end;
  width: 52px;
  height: 52px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #d9936d 0%, #b86c45 100%);
  color: #fff;
  display: grid;
  place-items: center;
  box-shadow: 0 10px 24px rgba(184, 108, 69, 0.24);
  cursor: pointer;
  transition:
    transform var(--cm-transition),
    box-shadow var(--cm-transition);
}

.explore-search-shell__action svg {
  width: 20px;
  height: 20px;
}

.explore-search-shell__action:hover {
  transform: scale(1.06);
  box-shadow: 0 14px 30px rgba(184, 108, 69, 0.32);
}

/* ── Dropdown panel（绝对定位，向下延伸） ── */
.explore-search-shell__panel {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  right: 0;
  z-index: 50;
  padding: 28px 32px;
  border-radius: 24px;
  background: rgba(255, 252, 249, 0.99);
  border: 1px solid rgba(88, 62, 43, 0.08);
  box-shadow: 0 20px 60px rgba(88, 62, 43, 0.14);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
}

.explore-search-shell__panel--type {
  left: auto;
  min-width: 300px;
}

.explore-search-shell__panel-title {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--cm-text-secondary);
  margin-bottom: 18px;
}

.explore-search-shell__panel-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* ── Panel 进出动画 ── */
.panel-enter-active {
  transition: opacity 200ms var(--cm-ease-enter), transform 200ms var(--cm-ease-enter);
}
.panel-leave-active {
  transition: opacity 160ms var(--cm-ease-exit), transform 160ms var(--cm-ease-exit);
}
.panel-enter-from,
.panel-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
}

/* ── Discovery 进出动画 ── */
.discovery-enter-active {
  transition: opacity 200ms var(--cm-ease-enter);
}
.discovery-leave-active {
  transition: opacity 140ms var(--cm-ease-exit);
}
.discovery-enter-from,
.discovery-leave-to {
  opacity: 0;
}

/* ── 发现区 ── */
.explore-search-shell__discovery {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 22px;
  padding: 16px 24px 20px;
  border-top: 1px solid rgba(88, 62, 43, 0.07);
}

.explore-search-shell__discovery-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.explore-search-shell__meta-label {
  color: var(--cm-text-secondary);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

/* ── 激活筛选条 ── */
.explore-search-shell__active-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 24px 16px;
  border-top: 1px solid rgba(88, 62, 43, 0.07);
}

.explore-search-shell__active-count {
  color: var(--cm-primary-deep);
  font-size: 13px;
  font-weight: 700;
}

.explore-search-shell__clear-btn {
  appearance: none;
  padding: 6px 14px;
  border: 1px solid rgba(88, 62, 43, 0.1);
  border-radius: 999px;
  background: transparent;
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition:
    color var(--cm-transition-micro),
    border-color var(--cm-transition-micro);
}

.explore-search-shell__clear-btn:hover {
  color: var(--cm-text);
  border-color: rgba(var(--cm-primary-rgb), 0.26);
}

/* ── Chips ── */
.explore-search-shell__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.explore-search-shell__chip {
  appearance: none;
  min-height: 36px;
  padding: 6px 14px;
  border: 1px solid rgba(88, 62, 43, 0.12);
  border-radius: 999px;
  background: rgba(248, 242, 234, 0.8);
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition:
    transform var(--cm-transition-micro),
    border-color var(--cm-transition-micro),
    color var(--cm-transition-micro),
    background var(--cm-transition-micro);
}

.explore-search-shell__chip:hover {
  transform: translateY(-1px);
  color: var(--cm-text);
  border-color: rgba(var(--cm-primary-rgb), 0.24);
  background: rgba(255, 248, 239, 0.98);
}

.explore-search-shell__chip.is-active {
  color: var(--cm-primary-deep);
  border-color: rgba(var(--cm-primary-rgb), 0.32);
  background: rgba(var(--cm-primary-rgb), 0.1);
}

.explore-search-shell__chip--soft {
  background: rgba(255, 255, 255, 0.78);
}

/* ── 搜索输入框覆盖 ── */
.explore-search-shell :deep(.search-suggest) {
  width: 100%;
}

.explore-search-shell :deep(.search-suggest .el-input-group__append) {
  display: none;
}

.explore-search-shell :deep(.search-suggest .el-input__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  background: rgba(246, 240, 232, 0.65) !important;
  box-shadow: none !important;
}

.explore-search-shell :deep(.search-suggest .el-input__inner) {
  font-size: 14px;
}

.explore-search-shell :deep(.search-suggest--active .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.96) !important;
}

.explore-search-shell :deep(.search-suggest__panel) {
  top: calc(100% + 8px);
  border-radius: 20px;
  background: rgba(255, 252, 247, 0.98);
}

/* ── condensed 状态：单层胶囊，无内容区 ── */
.explore-search-shell.is-condensed {
  border-radius: 999px;
  box-shadow: 0 4px 20px rgba(88, 62, 43, 0.09);
}

.explore-search-shell.is-condensed .explore-search-shell__bar {
  padding: 4px 4px 4px 18px;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, auto) auto minmax(0, auto) auto;
}

.explore-search-shell.is-condensed .explore-search-shell__seg {
  padding: 4px 14px;
  gap: 1px;
}

.explore-search-shell.is-condensed .explore-search-shell__seg--search {
  padding-left: 0;
}

.explore-search-shell.is-condensed .explore-search-shell__seg-label {
  display: none;
}

.explore-search-shell.is-condensed .explore-search-shell__seg-value {
  font-size: 13px;
  font-weight: 500;
  color: var(--cm-text);
  white-space: nowrap;
}

.explore-search-shell.is-condensed .explore-search-shell__divider {
  height: 20px;
}

.explore-search-shell.is-condensed .explore-search-shell__action {
  width: 42px;
  height: 42px;
  box-shadow: 0 6px 18px rgba(184, 108, 69, 0.2);
}

.explore-search-shell.is-condensed .explore-search-shell__action svg {
  width: 17px;
  height: 17px;
}

.explore-search-shell.is-condensed .explore-search-shell__discovery,
.explore-search-shell.is-condensed .explore-search-shell__active-bar {
  display: none;
}

.explore-search-shell.is-condensed :deep(.search-suggest .el-input__wrapper) {
  min-height: 34px;
  border-radius: 999px;
  background: transparent !important;
  box-shadow: none !important;
}

.explore-search-shell.is-condensed :deep(.search-suggest .el-input__inner) {
  font-size: 13px;
}

/* ── 响应式 ── */
@media (max-width: 1100px) {
  .explore-search-shell__bar {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
    padding: 14px 16px;
  }

  .explore-search-shell__divider {
    display: none;
  }

  .explore-search-shell__seg,
  .explore-search-shell__seg--search,
  .explore-search-shell__seg--btn {
    padding: 0;
    border-radius: 14px;
  }

  .explore-search-shell__action {
    justify-self: stretch;
    width: 100%;
    height: 50px;
  }

  /* condensed 在窄屏保持单行 */
  .explore-search-shell.is-condensed .explore-search-shell__bar {
    grid-template-columns: minmax(0, 1fr) auto minmax(0, auto) auto minmax(0, auto) auto;
    padding: 4px 4px 4px 14px;
    gap: 0;
  }

  .explore-search-shell.is-condensed .explore-search-shell__seg {
    padding: 4px 10px;
  }

  .explore-search-shell.is-condensed .explore-search-shell__action {
    width: 42px;
    height: 42px;
    justify-self: end;
  }
}

@media (max-width: 768px) {
  .explore-search-shell {
    border-radius: 22px;
  }

  .explore-search-shell__discovery {
    padding: 14px 16px 18px;
  }

  .explore-search-shell__discovery-group {
    width: 100%;
    overflow-x: auto;
    flex-wrap: nowrap;
    scrollbar-width: none;
  }

  .explore-search-shell__discovery-group::-webkit-scrollbar {
    display: none;
  }

  .explore-search-shell__chips {
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
    -webkit-overflow-scrolling: touch;
  }

  .explore-search-shell__chips::-webkit-scrollbar {
    display: none;
  }

  .explore-search-shell__panel {
    left: -16px;
    right: -16px;
    border-radius: 20px;
  }

  .explore-search-shell.is-condensed {
    border-radius: 999px;
  }
}
</style>
