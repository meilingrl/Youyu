<script setup>
defineProps({
  reviews: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 }
})

const emit = defineEmits(['update:page'])
</script>

<template>
  <div class="review-list">
    <div v-if="loading" class="review-list__loading">
      <el-skeleton :rows="3" animated />
    </div>
    <template v-else-if="reviews.length > 0">
      <div v-for="review in reviews" :key="review.id" class="review-list__item">
        <div class="review-list__header">
          <span class="review-list__reviewer">{{ review.reviewerNickname || '匿名用户' }}</span>
          <el-rate :model-value="review.score" disabled show-score size="small" />
        </div>
        <p v-if="review.content" class="review-list__content">{{ review.content }}</p>
        <div v-if="review.images?.length" class="review-list__images">
          <button
            v-for="image in review.images"
            :key="image.id || image.mediaUrl"
            type="button"
            class="review-list__image"
          >
            <img :src="image.mediaUrl" :alt="image.fileName || '评价图片'" loading="lazy" decoding="async" />
          </button>
        </div>
        <span class="review-list__time">{{ review.createdAt }}</span>
      </div>
      <el-pagination
        v-if="total > pageSize"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="(p) => emit('update:page', p)"
      />
    </template>
    <div v-else class="review-list__empty">
      暂无评价
    </div>
  </div>
</template>

<style scoped>
.review-list__item {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.review-list__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.review-list__reviewer {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.review-list__content {
  margin: 6px 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}
.review-list__time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.review-list__images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(72px, 96px));
  gap: 8px;
  margin: 8px 0;
}
.review-list__image {
  overflow: hidden;
  padding: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  aspect-ratio: 1 / 1;
  background: var(--el-fill-color-light);
  cursor: default;
}
.review-list__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.review-list__empty {
  text-align: center;
  padding: 24px;
  color: var(--el-text-color-placeholder);
  font-size: 14px;
}
.review-list__loading {
  padding: 12px 0;
}
</style>
