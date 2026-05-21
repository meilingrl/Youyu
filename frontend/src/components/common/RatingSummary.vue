<script setup>
defineProps({
  summary: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})
</script>

<template>
  <div class="rating-summary">
    <div v-if="loading" class="rating-summary__loading">
      <el-skeleton :rows="2" animated />
    </div>
    <template v-else-if="summary && summary.reviewCount > 0">
      <div class="rating-summary__score">
        <span class="rating-summary__avg">{{ summary.avgScore }}</span>
        <el-rate :model-value="summary.avgScore" disabled show-score size="small" />
        <span class="rating-summary__count">{{ summary.reviewCount }} 条评价</span>
      </div>
      <div v-if="summary.distribution && summary.distribution.length > 0" class="rating-summary__bars">
        <div v-for="item in summary.distribution" :key="item.score" class="rating-summary__bar-row">
          <span class="rating-summary__bar-label">{{ item.score }}星</span>
          <div class="rating-summary__bar-track">
            <div
              class="rating-summary__bar-fill"
              :style="{ width: summary.reviewCount > 0 ? (item.count / summary.reviewCount * 100) + '%' : '0%' }"
            />
          </div>
          <span class="rating-summary__bar-count">{{ item.count }}</span>
        </div>
      </div>
    </template>
    <div v-else class="rating-summary__empty">
      暂无评分
    </div>
  </div>
</template>

<style scoped>
.rating-summary {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}
.rating-summary__score {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.rating-summary__avg {
  font-size: 32px;
  font-weight: 700;
  color: var(--el-color-warning);
}
.rating-summary__count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.rating-summary__bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.rating-summary__bar-label {
  width: 32px;
  font-size: 12px;
  color: var(--el-text-color-regular);
  text-align: right;
}
.rating-summary__bar-track {
  flex: 1;
  height: 8px;
  background: var(--el-fill-color);
  border-radius: 4px;
  overflow: hidden;
}
.rating-summary__bar-fill {
  height: 100%;
  background: var(--el-color-warning);
  border-radius: 4px;
  transition: width 0.3s;
}
.rating-summary__bar-count {
  width: 24px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.rating-summary__empty {
  text-align: center;
  padding: 12px;
  font-size: 14px;
  color: var(--el-text-color-placeholder);
}
</style>
