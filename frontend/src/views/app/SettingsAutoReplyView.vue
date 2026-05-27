<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { getAutoReplySettings, updateAutoReplySettings } from '@/api/modules/chat'

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  isEnabled: false,
  replyContent: ''
})

async function load() {
  loading.value = true
  try {
    const response = await getAutoReplySettings()
    const data = response?.data?.data ?? response?.data ?? {}
    form.isEnabled = Boolean(data.isEnabled)
    form.replyContent = data.replyContent || '您好，我现在不方便及时回复，稍后看到消息会尽快联系您。'
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载自动回复失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await updateAutoReplySettings({
      isEnabled: form.isEnabled,
      replyContent: form.replyContent
    })
    ElMessage.success('自动回复设置已保存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="auto-reply-page">
    <section class="auto-reply-card">
      <h1>自动回复设置</h1>
      <p>离线时，系统会向买家自动发送一条回复。每个会话 24 小时内只会触发一次。</p>

      <label class="auto-reply-switch">
        <input v-model="form.isEnabled" type="checkbox" />
        <span>启用自动回复</span>
      </label>

      <textarea
        v-model="form.replyContent"
        class="auto-reply-textarea"
        :disabled="!form.isEnabled"
        rows="5"
        maxlength="500"
      />

      <div class="auto-reply-actions">
        <button type="button" class="auto-reply-save" :disabled="loading || saving" @click="save">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auto-reply-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 32px 24px;
}

.auto-reply-card {
  display: grid;
  gap: 16px;
  padding: 24px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.08);
}

.auto-reply-switch {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.auto-reply-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e7e5e4;
  border-radius: 12px;
  font: inherit;
  resize: vertical;
}

.auto-reply-actions {
  display: flex;
  justify-content: flex-end;
}

.auto-reply-save {
  height: 40px;
  padding: 0 18px;
  border: none;
  border-radius: 10px;
  background: #ea580c;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.auto-reply-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
