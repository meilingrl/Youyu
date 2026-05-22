<script setup>
import { computed } from 'vue'
import SearchSuggestInput from '@/components/search/SearchSuggestInput.vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  categories: {
    type: Array,
    default: () => []
  },
  productTypes: {
    type: Array,
    default: () => []
  },
  selectedCategoryId: {
    type: String,
    default: ''
  },
  selectedProductType: {
    type: String,
    default: ''
  },
  suggestions: {
    type: Array,
    default: () => []
  },
  loadingSuggestions: {
    type: Boolean,
    default: false
  },
  suggestionError: {
    type: String,
    default: ''
  },
  searchHistory: {
    type: Array,
    default: () => []
  },
  hotKeywords: {
    type: Array,
    default: () => []
  },
  loadingHotKeywords: {
    type: Boolean,
    default: false
  }
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

const activeCount = computed(() => {
  let count = 0
  if (String(props.modelValue || '').trim()) count += 1
  if (props.selectedCategoryId) count += 1
  if (props.selectedProductType) count += 1
  return count
})

const hasDiscovery = computed(() => props.searchHistory.length || props.hotKeywords.length)
const currentCategoryLabel = computed(
  () =>
    props.categories.find((category) => String(category.id || '') === props.selectedCategoryId)?.name ||
    '全部分类'
)
const currentProductTypeLabel = computed(
  () =>
    props.productTypes.find((productType) => String(productType.id || '') === props.selectedProductType)
      ?.name || '全部类型'
)
</script>

<template>
  <section class="explore-search-shell shell-card" aria-label="搜索与筛选">
    <div class="explore-search-shell__mode-row" aria-hidden="true">
      <span class="explore-search-shell__mode-pill is-active">搜索商品</span>
      <span class="explore-search-shell__mode-pill">按分类细分</span>
      <span class="explore-search-shell__mode-pill">按类型收窄</span>
    </div>

    <div class="explore-search-shell__hero-pill">
      <div class="explore-search-shell__segment explore-search-shell__segment--search">
        <div class="explore-search-shell__segment-copy">
          <span class="explore-search-shell__segment-label">搜什么</span>
          <strong class="explore-search-shell__segment-value">关键词、商品、服务</strong>
        </div>
        <SearchSuggestInput
          class="explore-search-shell__search-control"
          :model-value="modelValue"
          placeholder="搜索耳机、教材、宿舍好物或校园服务"
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

      <div class="explore-search-shell__segment">
        <div class="explore-search-shell__segment-copy">
          <span class="explore-search-shell__segment-label">分类</span>
          <strong class="explore-search-shell__segment-value">{{ currentCategoryLabel }}</strong>
        </div>
        <div class="explore-search-shell__chips" role="group" aria-label="分类筛选">
          <button
            v-for="category in categories"
            :key="category.id || 'all-category'"
            type="button"
            class="explore-search-shell__chip"
            :class="{ 'is-active': selectedCategoryId === category.id }"
            @click="emit('select-category', category.id)"
          >
            {{ category.name }}
          </button>
        </div>
      </div>

      <span class="explore-search-shell__divider" aria-hidden="true" />

      <div class="explore-search-shell__segment">
        <div class="explore-search-shell__segment-copy">
          <span class="explore-search-shell__segment-label">类型</span>
          <strong class="explore-search-shell__segment-value">{{ currentProductTypeLabel }}</strong>
        </div>
        <div class="explore-search-shell__chips" role="group" aria-label="类型筛选">
          <button
            v-for="productType in productTypes"
            :key="productType.id || 'all-type'"
            type="button"
            class="explore-search-shell__chip"
            :class="{ 'is-active': selectedProductType === productType.id }"
            @click="emit('select-product-type', productType.id)"
          >
            {{ productType.name }}
          </button>
        </div>
      </div>

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

    <div v-if="hasDiscovery" class="explore-search-shell__discovery">
      <div v-if="searchHistory.length" class="explore-search-shell__discovery-group">
        <span class="explore-search-shell__meta-label">最近搜过</span>
        <div class="explore-search-shell__chips explore-search-shell__chips--compact">
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
        <div class="explore-search-shell__chips explore-search-shell__chips--compact">
          <button
            v-for="keywordItem in hotKeywords.slice(0, 6)"
            :key="keywordItem.normalizedKeyword || keywordItem.keyword"
            type="button"
            class="explore-search-shell__chip explore-search-shell__chip--soft"
            @click="emit('apply-hot', keywordItem.keyword)"
          >
            {{ keywordItem.keyword }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="activeCount > 0" class="explore-search-shell__active-bar">
      <span class="explore-search-shell__active-count">{{ activeCount }} 个条件生效</span>
      <button type="button" class="explore-search-shell__clear-btn" @click="emit('clear')">
        清空全部
      </button>
    </div>
  </section>
</template>

<style scoped>
.explore-search-shell {
  display: grid;
  gap: 16px;
  padding: 18px;
  border-radius: 36px;
  background:
    radial-gradient(circle at 16% -8%, rgba(var(--cm-primary-rgb), 0.12), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 249, 241, 0.92));
  box-shadow: 0 18px 48px rgba(88, 62, 43, 0.11);
  transition:
    padding var(--cm-transition-feature),
    border-radius var(--cm-transition-feature),
    gap var(--cm-transition-feature),
    box-shadow var(--cm-transition-feature),
    background var(--cm-transition-feature),
    transform var(--cm-transition-feature);
}

.explore-search-shell__mode-row,
.explore-search-shell__discovery,
.explore-search-shell__active-bar {
  transition:
    opacity var(--cm-transition-feature),
    transform var(--cm-transition-feature),
    max-height var(--cm-transition-feature),
    padding var(--cm-transition-feature),
    margin var(--cm-transition-feature),
    border-color var(--cm-transition-feature);
}

.explore-search-shell__mode-row {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.explore-search-shell__mode-pill {
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  color: var(--cm-text-secondary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.explore-search-shell__mode-pill.is-active {
  background: rgba(var(--cm-primary-rgb), 0.1);
  color: var(--cm-primary-deep);
}

.explore-search-shell__hero-pill {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) auto minmax(240px, 0.86fr) auto minmax(240px, 0.86fr) auto;
  align-items: stretch;
  gap: 0;
  padding: 10px 10px 10px 18px;
  border-radius: 999px;
  border: 1px solid rgba(88, 62, 43, 0.08);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.55), 0 8px 26px rgba(88, 62, 43, 0.08);
  transition:
    padding var(--cm-transition-feature),
    box-shadow var(--cm-transition-feature),
    background var(--cm-transition-feature);
}

.explore-search-shell__segment {
  min-width: 0;
  display: grid;
  align-content: center;
  gap: 10px;
  padding: 10px 22px;
  transition:
    padding var(--cm-transition-feature),
    gap var(--cm-transition-feature),
    opacity var(--cm-transition-feature);
}

.explore-search-shell__segment--search {
  padding-left: 0;
}

.explore-search-shell__segment-copy {
  display: grid;
  gap: 3px;
}

.explore-search-shell__segment-label {
  color: var(--cm-text);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.01em;
}

.explore-search-shell__segment-value {
  color: var(--cm-text-secondary);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.2;
}

.explore-search-shell__divider {
  width: 1px;
  margin-block: 12px;
  background: rgba(88, 62, 43, 0.12);
}

.explore-search-shell__action {
  align-self: center;
  justify-self: end;
  width: 60px;
  height: 60px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff0f5b 0%, #dc0b49 100%);
  color: #fff;
  display: grid;
  place-items: center;
  box-shadow: 0 18px 30px rgba(220, 11, 73, 0.26);
  cursor: pointer;
  transition:
    transform var(--cm-transition),
    box-shadow var(--cm-transition),
    filter var(--cm-transition);
}

.explore-search-shell__action svg {
  width: 24px;
  height: 24px;
}

.explore-search-shell__action:hover {
  transform: scale(1.03);
  box-shadow: 0 22px 34px rgba(220, 11, 73, 0.3);
  filter: saturate(1.02);
}

.explore-search-shell__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.explore-search-shell__chips--compact {
  gap: 6px;
}

.explore-search-shell__chip {
  appearance: none;
  min-height: 34px;
  padding: 6px 12px;
  border: 1px solid rgba(88, 62, 43, 0.12);
  border-radius: 999px;
  background: rgba(248, 242, 234, 0.82);
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
  min-height: 30px;
  padding: 5px 11px;
  background: rgba(255, 255, 255, 0.78);
}

.explore-search-shell__discovery {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 22px;
  padding-top: 14px;
  border-top: 1px solid var(--cm-border);
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

.explore-search-shell__active-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid var(--cm-border);
}

.explore-search-shell__active-count {
  color: var(--cm-primary-deep);
  font-size: 13px;
  font-weight: 700;
}

.explore-search-shell__clear-btn {
  appearance: none;
  padding: 7px 14px;
  border: 1px solid var(--cm-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.85);
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition:
    color var(--cm-transition-micro),
    border-color var(--cm-transition-micro),
    background var(--cm-transition-micro);
}

.explore-search-shell__clear-btn:hover {
  color: var(--cm-text);
  border-color: rgba(var(--cm-primary-rgb), 0.26);
  background: rgba(255, 255, 255, 0.96);
}

.explore-search-shell :deep(.search-suggest) {
  width: 100%;
}

.explore-search-shell :deep(.search-suggest .el-input-group__append) {
  display: none;
}

.explore-search-shell :deep(.search-suggest .el-input__wrapper) {
  min-height: 54px;
  border-radius: 18px;
  background: rgba(246, 240, 232, 0.78) !important;
  box-shadow: none !important;
}

.explore-search-shell :deep(.search-suggest .el-input__inner) {
  font-size: 15px;
}

.explore-search-shell :deep(.search-suggest--active .el-input__wrapper) {
  transform: none;
  background: rgba(255, 255, 255, 0.96) !important;
}

.explore-search-shell :deep(.search-suggest__panel) {
  top: calc(100% + 8px);
  padding-top: 12px;
  border-radius: 22px;
  background: rgba(255, 252, 247, 0.98);
}

.explore-search-shell.is-condensed {
  gap: 8px;
  padding: 10px 12px;
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 249, 243, 0.9)),
    rgba(255, 255, 255, 0.72);
  box-shadow: 0 14px 36px rgba(88, 62, 43, 0.1);
}

.explore-search-shell.is-condensed .explore-search-shell__mode-row,
.explore-search-shell.is-condensed .explore-search-shell__discovery,
.explore-search-shell.is-condensed .explore-search-shell__active-bar {
  max-height: 0;
  opacity: 0;
  overflow: hidden;
  transform: translateY(-8px);
  padding-top: 0;
  margin: 0;
  border-color: transparent;
  pointer-events: none;
}

.explore-search-shell.is-condensed .explore-search-shell__hero-pill {
  grid-template-columns: minmax(0, 1.2fr) auto minmax(145px, 0.58fr) auto minmax(145px, 0.58fr) auto;
  align-items: center;
  padding: 4px 6px 4px 12px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.6), 0 6px 18px rgba(88, 62, 43, 0.08);
}

.explore-search-shell.is-condensed .explore-search-shell__segment {
  min-height: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px;
}

.explore-search-shell.is-condensed .explore-search-shell__segment-copy {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.explore-search-shell.is-condensed .explore-search-shell__segment-label {
  display: none;
}

.explore-search-shell.is-condensed .explore-search-shell__segment-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--cm-text);
}

.explore-search-shell.is-condensed .explore-search-shell__chips {
  max-height: 0;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}

.explore-search-shell.is-condensed .explore-search-shell__divider {
  height: 26px;
  margin-block: 0;
}

.explore-search-shell.is-condensed .explore-search-shell__action {
  width: 48px;
  height: 48px;
  box-shadow: 0 12px 24px rgba(220, 11, 73, 0.2);
}

.explore-search-shell.is-condensed .explore-search-shell__action svg {
  width: 20px;
  height: 20px;
}

.explore-search-shell.is-condensed :deep(.search-suggest .el-input__wrapper) {
  min-height: 38px;
  border-radius: 999px;
  background: transparent !important;
  box-shadow: none !important;
}

.explore-search-shell.is-condensed :deep(.search-suggest .el-input__inner) {
  font-size: 14px;
  font-weight: 600;
}

.explore-search-shell.is-condensed :deep(.search-suggest .el-input__wrapper:hover),
.explore-search-shell.is-condensed :deep(.search-suggest .el-input__wrapper.is-focus) {
  box-shadow: none !important;
}

.explore-search-shell.is-condensed .explore-search-shell__segment--search {
  padding-left: 0;
}

.explore-search-shell.is-condensed .explore-search-shell__segment--search .explore-search-shell__segment-copy {
  display: none;
}

@media (max-width: 1180px) {
  .explore-search-shell__hero-pill {
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
    padding: 14px;
    border-radius: 28px;
  }

  .explore-search-shell__divider {
    display: none;
  }

  .explore-search-shell__segment,
  .explore-search-shell__segment--search {
    padding: 0;
  }

  .explore-search-shell__action {
    justify-self: stretch;
    width: 100%;
    height: 52px;
  }

  .explore-search-shell.is-condensed .explore-search-shell__hero-pill {
    grid-template-columns: minmax(0, 1fr);
    padding: 10px 12px;
    gap: 8px;
  }

  .explore-search-shell.is-condensed .explore-search-shell__segment {
    padding: 0;
  }

  .explore-search-shell.is-condensed .explore-search-shell__chips {
    max-height: none;
    opacity: 1;
    overflow: visible;
    pointer-events: auto;
  }
}

@media (max-width: 768px) {
  .explore-search-shell {
    padding: 14px;
    border-radius: 28px;
  }

  .explore-search-shell__mode-row {
    justify-content: flex-start;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .explore-search-shell__mode-row::-webkit-scrollbar {
    display: none;
  }

  .explore-search-shell__discovery {
    gap: 10px 14px;
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

  .explore-search-shell__active-bar {
    flex-wrap: wrap;
  }

  .explore-search-shell.is-condensed {
    padding: 10px;
  }

  .explore-search-shell.is-condensed .explore-search-shell__hero-pill {
    padding: 10px;
    border-radius: 24px;
  }
}
</style>
