<script setup>
import { computed } from 'vue'
import SearchSuggestInput from '@/components/search/SearchSuggestInput.vue'
import HotSearchList from '@/components/search/HotSearchList.vue'

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
</script>

<template>
  <section class="explore-search-shell shell-card">
    <div class="explore-search-shell__hero">
      <div>
        <span class="eyebrow">Explore</span>
        <h1>从校园热搜到合适商品，一次顺手逛完</h1>
        <p>把搜索、筛选、建议和热搜放进同一块柔和表面里，让探索更像逛集市，而不是填写后台表单。</p>
      </div>
      <div class="explore-search-shell__status">
        <span class="explore-search-shell__status-pill">{{ activeCount }} 个筛选条件生效</span>
        <button type="button" class="explore-search-shell__clear" @click="emit('clear')">清空筛选</button>
      </div>
    </div>

    <div class="explore-search-shell__surface">
      <div class="explore-search-shell__surface-head">
        <div class="explore-search-shell__segment">
          <span class="explore-search-shell__label">想找什么</span>
          <SearchSuggestInput
            :model-value="modelValue"
            placeholder="搜索笔记、耳机、宿舍好物或服务"
            button-label="进入探索"
            :suggestions="suggestions"
            :loading="loadingSuggestions"
            :error="suggestionError"
            @update:model-value="emit('update:modelValue', $event)"
            @change="emit('change', $event)"
            @submit="emit('submit', $event)"
            @select-suggestion="emit('select-suggestion', $event)"
          />
        </div>
      </div>

      <div class="explore-search-shell__body">
        <div class="explore-search-shell__filters">
          <div class="explore-search-shell__filter-group">
            <span class="explore-search-shell__label">分类</span>
            <div class="explore-search-shell__chips">
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

          <div class="explore-search-shell__filter-group">
            <span class="explore-search-shell__label">商品类型</span>
            <div class="explore-search-shell__chips">
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
        </div>

        <div class="explore-search-shell__support">
          <div v-if="searchHistory.length" class="explore-search-shell__support-block">
            <span class="explore-search-shell__label">最近搜过</span>
            <div class="explore-search-shell__chips explore-search-shell__chips--compact">
              <button
                v-for="item in searchHistory"
                :key="item"
                type="button"
                class="explore-search-shell__chip explore-search-shell__chip--ghost"
                @click="emit('apply-history', item)"
              >
                {{ item }}
              </button>
            </div>
          </div>

          <div class="explore-search-shell__support-block">
            <span class="explore-search-shell__label">此刻热搜</span>
            <HotSearchList
              :keywords="hotKeywords.slice(0, 6)"
              :loading="loadingHotKeywords"
              @select="emit('apply-hot', $event)"
            />
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.explore-search-shell {
  display: grid;
  gap: 24px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(255, 219, 179, 0.52), transparent 34%),
    linear-gradient(180deg, rgba(255, 251, 246, 0.98), rgba(248, 241, 233, 0.96));
}

.explore-search-shell__hero,
.explore-search-shell__surface-head,
.explore-search-shell__body {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.explore-search-shell__hero h1 {
  margin: 10px 0 0;
  max-width: 12ch;
}

.explore-search-shell__hero p {
  max-width: 62ch;
  margin: 12px 0 0;
  color: var(--cm-text-secondary);
  line-height: 1.7;
}

.explore-search-shell__status {
  display: grid;
  justify-items: end;
  gap: 12px;
  min-width: 180px;
}

.explore-search-shell__status-pill,
.explore-search-shell__clear {
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.explore-search-shell__status-pill {
  padding: 9px 14px;
  background: rgba(255, 247, 237, 0.92);
  color: #b45309;
}

.explore-search-shell__clear {
  appearance: none;
  padding: 9px 14px;
  border: 1px solid rgba(179, 128, 83, 0.2);
  background: rgba(255, 255, 255, 0.8);
  color: var(--cm-text);
  cursor: pointer;
}

.explore-search-shell__surface {
  display: grid;
  gap: 18px;
  padding: 22px;
  border-radius: 32px;
  background: rgba(255, 253, 249, 0.78);
  box-shadow: inset 0 0 0 1px rgba(155, 123, 95, 0.08);
}

.explore-search-shell__surface-head {
  align-items: stretch;
}

.explore-search-shell__segment,
.explore-search-shell__filter-group,
.explore-search-shell__support-block {
  display: grid;
  gap: 12px;
}

.explore-search-shell__segment {
  flex: 1;
}

.explore-search-shell__body {
  align-items: stretch;
}

.explore-search-shell__filters,
.explore-search-shell__support {
  display: grid;
  gap: 18px;
}

.explore-search-shell__filters {
  flex: 1.2;
}

.explore-search-shell__support {
  flex: 0.8;
}

.explore-search-shell__label {
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.explore-search-shell__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.explore-search-shell__chips--compact {
  gap: 8px;
}

.explore-search-shell__chip {
  appearance: none;
  min-height: 42px;
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid rgba(144, 113, 89, 0.15);
  background: rgba(255, 255, 255, 0.85);
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition:
    transform var(--cm-transition),
    box-shadow var(--cm-transition),
    border-color var(--cm-transition),
    color var(--cm-transition);
}

.explore-search-shell__chip:hover {
  transform: translateY(-1px);
  color: var(--cm-text);
  border-color: rgba(201, 93, 49, 0.28);
  box-shadow: 0 10px 18px rgba(95, 58, 30, 0.08);
}

.explore-search-shell__chip.is-active {
  color: var(--cm-text);
  border-color: rgba(201, 93, 49, 0.35);
  background: rgba(255, 246, 236, 0.96);
}

.explore-search-shell__chip--ghost {
  min-height: 38px;
  padding: 8px 14px;
}

@media (max-width: 960px) {
  .explore-search-shell,
  .explore-search-shell__surface {
    padding: 22px;
  }

  .explore-search-shell__hero,
  .explore-search-shell__body {
    flex-direction: column;
  }

  .explore-search-shell__status {
    justify-items: start;
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .explore-search-shell {
    padding: 18px;
    border-radius: 28px;
  }

  .explore-search-shell__surface {
    padding: 16px;
    border-radius: 24px;
  }

  .explore-search-shell__hero h1 {
    max-width: none;
  }

  .explore-search-shell__chip {
    min-height: 40px;
    padding: 9px 14px;
  }
}
</style>
