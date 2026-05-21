<script setup>
defineProps({
  keywords: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select'])
</script>

<template>
  <el-skeleton :loading="loading" animated :rows="2">
    <template #default>
      <div v-if="keywords.length" class="hot-search-list">
        <button
          v-for="(item, index) in keywords"
          :key="item.normalizedKeyword || item.keyword"
          type="button"
          class="hot-search-chip"
          @click="emit('select', item.keyword)"
        >
          <span class="hot-search-chip__rank">#{{ index + 1 }}</span>
          <strong>{{ item.keyword }}</strong>
          <span>{{ item.searchCount }} 次搜索</span>
        </button>
      </div>
      <p v-else class="hot-search-empty">暂时还没有热搜数据，产生更多公开搜索后这里会变得更丰富。</p>
    </template>
  </el-skeleton>
</template>

<style scoped>
.hot-search-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hot-search-chip {
  appearance: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid rgba(249, 115, 22, 0.24);
  border-radius: 999px;
  background: rgba(255, 247, 237, 0.96);
  color: var(--cm-text);
  cursor: pointer;
  text-align: left;
  transition:
    transform var(--cm-transition),
    box-shadow var(--cm-transition),
    border-color var(--cm-transition);
}

.hot-search-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(249, 115, 22, 0.42);
  box-shadow: 0 10px 20px rgba(249, 115, 22, 0.14);
}

.hot-search-chip__rank {
  color: #c2410c;
  font-weight: 700;
}

.hot-search-empty {
  margin: 0;
  color: var(--cm-text-secondary);
}
</style>
