<script setup>
import { ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useReviewStore } from '@/stores/review'

const MAX_IMAGE_COUNT = 3
const MAX_IMAGE_SIZE = 5 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

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
const readingImages = ref(false)
const imageError = ref('')
const images = ref([])
const imageInputRef = ref(null)

function openImagePicker() {
  if (submitting.value || readingImages.value || images.value.length >= MAX_IMAGE_COUNT) return
  imageInputRef.value?.click()
}

function resetImageInput(event) {
  event.target.value = ''
}

function readImage(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      resolve({
        mediaUrl: reader.result,
        fileName: file.name,
        mimeType: file.type
      })
    }
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

async function handleImageChange(event) {
  const files = Array.from(event.target.files || [])
  imageError.value = ''
  if (!files.length) return

  const remainingSlots = MAX_IMAGE_COUNT - images.value.length
  if (files.length > remainingSlots) {
    imageError.value = `最多可上传 ${MAX_IMAGE_COUNT} 张评价图片。`
    resetImageInput(event)
    return
  }

  const invalidType = files.find((file) => !ALLOWED_IMAGE_TYPES.includes(file.type))
  if (invalidType) {
    imageError.value = '仅支持 JPG、PNG、GIF 和 WebP 格式的图片。'
    resetImageInput(event)
    return
  }

  const oversized = files.find((file) => file.size > MAX_IMAGE_SIZE)
  if (oversized) {
    imageError.value = '每张评价图片不能超过 5MB。'
    resetImageInput(event)
    return
  }

  readingImages.value = true
  try {
    const nextImages = await Promise.all(files.map(readImage))
    images.value = [...images.value, ...nextImages]
  } catch (error) {
    imageError.value = error?.message || '图片读取失败。'
  } finally {
    readingImages.value = false
    resetImageInput(event)
  }
}

function removeImage(index) {
  images.value = images.value.filter((_, imageIndex) => imageIndex !== index)
  imageError.value = ''
}

async function handleSubmit() {
  if (score.value < 1 || score.value > 5) {
    ElMessage.warning('请选择 1 到 5 星评分。')
    return
  }
  submitting.value = true
  try {
    if (props.type === 'product') {
      await store.doSubmitProductReview({
        orderItemId: props.orderItem.id,
        score: score.value,
        content: content.value,
        images: images.value
      })
      ElMessage.success('评价已提交')
    } else {
      await store.doSubmitShopReview({
        shopId: props.shopId,
        score: score.value,
        content: content.value,
        images: images.value
      })
      ElMessage.success('店铺评价已提交')
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
        placeholder="填写评价内容，可补充商品成色、使用体验或交易沟通情况"
        :disabled="submitting"
      />
    </div>
    <div class="review-form__images">
      <div class="review-form__image-head">
        <span class="review-form__label">评价图片</span>
        <el-button
          size="small"
          plain
          :disabled="submitting || readingImages || images.length >= MAX_IMAGE_COUNT"
          :loading="readingImages"
          @click="openImagePicker"
        >
          添加图片
        </el-button>
        <input
          ref="imageInputRef"
          type="file"
          class="review-form__image-input"
          accept="image/jpeg,image/png,image/gif,image/webp"
          multiple
          @change="handleImageChange"
        />
      </div>
      <p v-if="imageError" class="review-form__image-error">{{ imageError }}</p>
      <div v-if="images.length" class="review-form__image-grid">
        <figure v-for="(image, index) in images" :key="`${image.fileName}-${index}`" class="review-form__image">
          <img :src="image.mediaUrl" :alt="image.fileName || '评价图片'" />
          <button type="button" :disabled="submitting" @click="removeImage(index)">移除</button>
        </figure>
      </div>
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
.review-form__images,
.review-form__image-head {
  display: grid;
  gap: 8px;
}
.review-form__image-head {
  grid-template-columns: 1fr auto;
  align-items: center;
}
.review-form__image-input {
  display: none;
}
.review-form__image-error {
  margin: 0;
  color: var(--el-color-danger);
  font-size: 13px;
}
.review-form__image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  gap: 10px;
}
.review-form__image {
  position: relative;
  margin: 0;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}
.review-form__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.review-form__image button {
  position: absolute;
  right: 4px;
  bottom: 4px;
  border: 0;
  border-radius: 6px;
  padding: 4px 6px;
  background: rgba(17, 24, 39, 0.72);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}
</style>
