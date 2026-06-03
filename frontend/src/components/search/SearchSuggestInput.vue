<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '搜索'
  },
  buttonLabel: {
    type: String,
    default: '搜索'
  },
  suggestions: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  elevated: {
    type: Boolean,
    default: true
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

const emit = defineEmits(['update:modelValue', 'change', 'submit', 'selectSuggestion', 'applyHistory', 'applyHot'])

const rootRef = ref(null)
const panelVisible = ref(false)

const hasQuery = computed(() => String(props.modelValue || '').trim().length > 0)
const hasDiscovery = computed(() => props.searchHistory.length || props.hotKeywords.length || props.loadingHotKeywords)
const showEmpty = computed(() => hasQuery.value && !props.loading && !props.error && !props.suggestions.length)
const shouldShowPanel = computed(() =>
  panelVisible.value &&
  ((hasQuery.value && (props.loading || props.error || props.suggestions.length || showEmpty.value)) ||
    (!hasQuery.value && hasDiscovery.value))
)

function updateValue(value) {
  emit('update:modelValue', value)
  emit('change', value)
  panelVisible.value = true
}

function submitCurrent() {
  panelVisible.value = false
  emit('submit', props.modelValue)
}

function selectSuggestion(keyword) {
  panelVisible.value = false
  emit('update:modelValue', keyword)
  emit('selectSuggestion', keyword)
}

function applyHistory(keyword) {
  panelVisible.value = false
  emit('update:modelValue', keyword)
  emit('applyHistory', keyword)
}

function applyHot(keyword) {
  panelVisible.value = false
  emit('update:modelValue', keyword)
  emit('applyHot', keyword)
}

function handleFocus() {
  panelVisible.value = true
}

function handleClear() {
  panelVisible.value = false
  emit('update:modelValue', '')
  emit('change', '')
}

function handleOutsideClick(event) {
  if (!rootRef.value?.contains(event.target)) {
    panelVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleOutsideClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleOutsideClick)
})
</script>

<template>
  <div
    ref="rootRef"
    class="search-suggest"
    :class="{
      'search-suggest--active': elevated && (panelVisible || hasQuery),
      'search-suggest--panel': shouldShowPanel
    }"
  >
    <el-input
      :model-value="modelValue"
      clearable
      :placeholder="placeholder"
      @update:model-value="updateValue"
      @focus="handleFocus"
      @clear="handleClear"
      @keyup.enter="submitCurrent"
      @keyup.esc="panelVisible = false"
    >
      <template #append>
        <el-button @click="submitCurrent">{{ buttonLabel }}</el-button>
      </template>
    </el-input>

    <div v-if="shouldShowPanel" class="search-suggest__panel">
      <template v-if="hasQuery">
        <div v-if="loading" class="search-suggest__state">正在加载建议...</div>
        <div v-else-if="error" class="search-suggest__state search-suggest__state--error">
          {{ error }}，按回车仍可直接搜索。
        </div>
        <div v-else-if="showEmpty" class="search-suggest__state">暂时没有相关建议</div>
        <button
          v-for="item in suggestions"
          v-else
          :key="item.normalizedKeyword || item.keyword"
          type="button"
          class="search-suggest__item"
          @click="selectSuggestion(item.keyword)"
        >
          <div class="search-suggest__text">
            <strong>{{ item.keyword }}</strong>
            <span v-if="item.pinned" class="search-suggest__badge">置顶</span>
          </div>
          <span class="search-suggest__meta">{{ item.searchCount }} 次搜索</span>
        </button>
      </template>

      <template v-else>
        <div v-if="searchHistory.length" class="search-suggest__discovery-group">
          <span class="search-suggest__section-label">最近搜索</span>
          <div class="search-suggest__chips">
            <button
              v-for="item in searchHistory"
              :key="item"
              type="button"
              class="search-suggest__chip"
              @click="applyHistory(item)"
            >
              {{ item }}
            </button>
          </div>
        </div>

        <div v-if="loadingHotKeywords" class="search-suggest__state">正在加载热门搜索...</div>

        <div v-if="hotKeywords.length" class="search-suggest__discovery-group">
          <span class="search-suggest__section-label">热门搜索</span>
          <div class="search-suggest__chips">
            <button
              v-for="kw in hotKeywords.slice(0, 6)"
              :key="kw.normalizedKeyword || kw.keyword"
              type="button"
              class="search-suggest__chip"
              @click="applyHot(kw.keyword)"
            >
              {{ kw.keyword }}
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.search-suggest {
  position: relative;
  width: 100%;
  min-width: 0;
  min-height: 46px;
}

.search-suggest :deep(.el-input__wrapper) {
  min-height: 58px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 24px rgba(95, 58, 30, 0.08);
  transition:
    box-shadow 260ms ease,
    background-color 260ms ease;
}

.search-suggest :deep(.el-input__inner) {
  font-size: 15px;
}

.search-suggest :deep(.el-input-group__append) {
  border-radius: 999px;
  padding-right: 8px;
  background: transparent;
  box-shadow: none;
}

.search-suggest :deep(.el-input-group__append .el-button) {
  min-height: 42px;
  padding-inline: 18px;
  border-radius: 999px;
  border: 0;
}

.search-suggest--active :deep(.el-input__wrapper) {
  background: rgba(255, 253, 249, 0.98);
  box-shadow: 0 18px 40px rgba(95, 58, 30, 0.14);
}

.search-suggest__panel {
  position: absolute;
  z-index: 20;
  top: calc(100% - 10px);
  left: 0;
  width: 100%;
  padding: 20px 12px 12px;
  display: grid;
  gap: 6px;
  border-radius: 0 0 24px 24px;
  background: rgba(255, 253, 249, 0.98);
  box-shadow: 0 24px 40px rgba(95, 58, 30, 0.16);
  backdrop-filter: blur(18px);
}

.search-suggest__item {
  appearance: none;
  width: 100%;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 0;
  border-radius: 16px;
  background: rgba(247, 248, 250, 0.88);
  color: var(--cm-text);
  cursor: pointer;
  transition: background var(--cm-transition);
}

.search-suggest__item:hover {
  background: rgba(255, 247, 237, 0.96);
}

.search-suggest__text {
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.search-suggest__badge {
  font-size: 12px;
  color: #c2410c;
}

.search-suggest__meta,
.search-suggest__state {
  font-size: 13px;
  color: var(--cm-text-secondary);
}

.search-suggest__state {
  padding: 10px 12px;
}

.search-suggest__state--error {
  color: #b42318;
}

.search-suggest__discovery-group {
  display: grid;
  gap: 10px;
  padding: 8px 4px;
}

.search-suggest__section-label {
  color: var(--cm-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.search-suggest__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-suggest__chip {
  appearance: none;
  min-height: 32px;
  padding: 6px 12px;
  border: 1px solid rgba(88, 62, 43, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background var(--cm-transition-micro),
    border-color var(--cm-transition-micro),
    color var(--cm-transition-micro);
}

.search-suggest__chip:hover {
  background: rgba(255, 247, 237, 0.96);
  border-color: rgba(var(--cm-primary-rgb), 0.24);
  color: var(--cm-text);
}

@media (max-width: 768px) {
  .search-suggest :deep(.el-input__wrapper) {
    min-height: 54px;
  }

  .search-suggest__panel {
    max-height: 280px;
    overflow-y: auto;
  }
}
</style>
