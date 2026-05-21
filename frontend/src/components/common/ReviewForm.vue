<script setup>
import { ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useReviewStore } from '@/stores/review'

const props = defineProps({
  orderItem: { type: Object, default: null },
  shopId: { type: Number, default: null },
  type: { type: String, required: true } // 'product' | 'shop'
})

const emit = defineEmits(['submitted', 'cancel'])

const store = useReviewStore()
const score = ref(0)
const content = ref('')
const submitting = ref(false)

async function handleSubmit() {
  if (score.value < 1 || score.value > 5) {
    ElMessage.warning('请选择评分（1-5星）')
    return
  }
  submitting.value = true
  try {
    if (props.type === 'product') {
      await store.doSubmitProductReview({
        orderItemId: props.orderItem.id,
        score: score.value,
        content: content.value
      })
      ElMessage.success('评价提交成功')
    } else {
      await store.doSubmitShopReview({
        shopId: props.shopId,
        score: score.value,
        content: content.value
      })
      ElMessage.success('店铺评价提交成功')
    }
    emit('submitted')
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="review-form">
    <div class="review-form__score">
      <span class="review-form__label">评分</span>
      <el-rate v-model="score" :max="5" :disabled="submitting" />
    </div>
    <div class="review-form__content">
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="1000"
        show-word-limit
        placeholder="写下你的评价（选填）"
        :disabled="submitting"
      />
    </div>
    <div class="review-form__actions">
      <el-button :loading="submitting" type="primary" @click="handleSubmit">提交评价</el-button>
      <el-button :disabled="submitting" @click="emit('cancel')">取消</el-button>
    </div>
  </div>
</template>

<style scoped>
.review-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.review-form__score {
  display: flex;
  align-items: center;
  gap: 12px;
}
.review-form__label {
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.review-form__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
