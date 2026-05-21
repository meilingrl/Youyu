<script setup>
defineProps({
  title: {
    type: String,
    default: '加载失败'
  },
  message: {
    type: String,
    default: '网络异常或服务暂时不可用，请稍后重试。'
  }
})

const emit = defineEmits(['retry'])
</script>

<template>
  <div class="cm-error-block shell-card" role="alert">
    <div class="cm-error-block__icon" aria-hidden="true">⚠️</div>
    <div class="cm-error-block__content">
      <h2 class="cm-error-block__title">{{ title }}</h2>
      <p class="cm-error-block__message">{{ message }}</p>
    </div>
    <el-button type="primary" @click="emit('retry')">重试</el-button>
  </div>
</template>

<style scoped>
.cm-error-block {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
  padding: 22px 24px;
}

.cm-error-block__icon {
  font-size: 32px;
  line-height: 1;
}

.cm-error-block__content {
  flex: 1;
  min-width: 200px;
}

.cm-error-block__title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
}

.cm-error-block__message {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
  font-size: 14px;
}
</style>
