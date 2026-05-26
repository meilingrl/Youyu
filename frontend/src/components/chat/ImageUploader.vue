<script setup>
import { ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'

const MAX_IMAGE_SIZE = 5 * 1024 * 1024

const emit = defineEmits(['selected'])

const fileInputRef = ref(null)
const reading = ref(false)

function openPicker() {
  if (reading.value) return
  fileInputRef.value?.click()
}

function resetInput(event) {
  event.target.value = ''
}

function handleFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    resetInput(event)
    return
  }

  if (file.size > MAX_IMAGE_SIZE) {
    ElMessage.warning('图片不能超过 5MB')
    resetInput(event)
    return
  }

  reading.value = true
  const reader = new FileReader()

  reader.onload = () => {
    emit('selected', {
      mediaUrl: reader.result,
      fileName: file.name,
      mimeType: file.type
    })
    reading.value = false
    resetInput(event)
  }

  reader.onerror = () => {
    ElMessage.error('读取图片失败')
    reading.value = false
    resetInput(event)
  }

  reader.readAsDataURL(file)
}
</script>

<template>
  <button
    type="button"
    class="image-uploader"
    :class="{ 'is-reading': reading }"
    :disabled="reading"
    :aria-label="reading ? '图片读取中' : '选择图片'"
    :title="reading ? '图片读取中' : '选择图片'"
    @click="openPicker"
  >
    <span class="image-uploader__icon" aria-hidden="true"></span>
    <span class="image-uploader__text">{{ reading ? '读取中' : '图片' }}</span>
  </button>
  <input
    ref="fileInputRef"
    type="file"
    class="image-uploader__input"
    accept="image/*"
    @change="handleFileChange"
  />
</template>

<style scoped>
.image-uploader {
  width: 76px;
  min-height: 42px;
  border: 1px solid #FED7AA;
  border-radius: 18px;
  background: #FFF7ED;
  color: #EA580C;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 160ms ease-out, box-shadow 160ms ease-out, opacity 160ms ease-out;
  flex-shrink: 0;
}

.image-uploader:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px rgba(234, 88, 12, 0.18);
}

.image-uploader:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.image-uploader__icon {
  width: 16px;
  height: 13px;
  border: 2px solid currentColor;
  border-radius: 4px;
  position: relative;
  flex-shrink: 0;
}

.image-uploader__icon::before {
  content: '';
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  top: 2px;
  right: 2px;
}

.image-uploader__icon::after {
  content: '';
  position: absolute;
  left: 2px;
  right: 2px;
  bottom: 2px;
  height: 5px;
  border-radius: 3px 3px 2px 2px;
  background: currentColor;
  clip-path: polygon(0 100%, 38% 35%, 62% 70%, 78% 48%, 100% 100%);
}

.image-uploader__input {
  display: none;
}

@media (max-width: 640px) {
  .image-uploader {
    width: 58px;
    padding: 0 8px;
  }

  .image-uploader__text {
    display: none;
  }
}
</style>
