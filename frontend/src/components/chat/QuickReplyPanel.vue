<script setup>
import { computed, onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'

const DEFAULT_QUICK_REPLIES = [
  '亲，在的！有什么可以帮您的吗？',
  '包邮哦，全国包邮！',
  '今天下单，明天发货！',
  '有现货，可以立即发货！',
  '支持七天无理由退换货！',
  '有任何问题随时联系我！'
]

const emit = defineEmits(['select'])

const chatStore = useChatStore()

const replies = computed(() => {
  const customReplies = chatStore.quickReplies
    .filter((item) => item.content?.trim())
    .slice(0, 10)

  if (customReplies.length > 0) {
    return customReplies
  }

  return DEFAULT_QUICK_REPLIES.map((content, index) => ({
    id: `default-${index}`,
    content,
    sortOrder: index
  }))
})

onMounted(() => {
  if (chatStore.quickReplies.length === 0) {
    chatStore.fetchQuickReplies().catch((error) => {
      console.error('Failed to load quick replies:', error)
    })
  }
})

function selectReply(reply) {
  const content = reply.content?.trim()
  if (content) {
    emit('select', content)
  }
}
</script>

<template>
  <div class="quick-reply-panel" role="menu" aria-label="快捷回复">
    <div v-if="chatStore.quickRepliesLoading" class="quick-reply-panel__loading">
      加载中...
    </div>
    <template v-else>
      <button
        v-for="reply in replies"
        :key="reply.id"
        type="button"
        class="quick-reply-panel__item"
        role="menuitem"
        @mousedown.prevent
        @click="selectReply(reply)"
      >
        {{ reply.content }}
      </button>
    </template>
  </div>
</template>

<style scoped>
.quick-reply-panel {
  width: min(520px, 100%);
  max-height: 260px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid #FED7AA;
  border-radius: 12px;
  background: #FFFFFF;
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.12);
}

.quick-reply-panel__loading {
  padding: 12px;
  color: #6B7280;
  font-size: 14px;
}

.quick-reply-panel__item {
  width: 100%;
  display: block;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #1F2937;
  font: inherit;
  font-size: 14px;
  line-height: 1.5;
  text-align: left;
  cursor: pointer;
}

.quick-reply-panel__item:hover,
.quick-reply-panel__item:focus-visible {
  background: #FFF7ED;
  color: #EA580C;
  outline: none;
}

@media (max-width: 640px) {
  .quick-reply-panel {
    max-height: 220px;
  }
}
</style>
