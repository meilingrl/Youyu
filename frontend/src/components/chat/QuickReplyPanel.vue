<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useChatStore } from '@/stores/chat'

const props = defineProps({
  scenario: {
    type: String,
    default: 'buyer'
  }
})

const emit = defineEmits(['select'])
const chatStore = useChatStore()
const activeCategory = ref(props.scenario)

const defaults = {
  buyer: [
    '你好，这个还在吗？',
    '方便看一下实物照片吗？',
    '可以校内自提吗？',
    '价格还能商量吗？',
    '今天方便交易吗？'
  ],
  seller: [
    '您好，这件目前还有现货。',
    '可以校内自提，也能约时间当面验货。',
    '确认的话我这边尽快安排发出。',
    '商品细节我可以再补几张照片。',
    '有任何问题都可以继续留言，我会及时回复。'
  ],
  support: [
    '请先提供订单号，我帮您核对一下。',
    '这个问题我需要确认后再回复您。',
    '我已经收到反馈，会继续跟进。',
    '请补充截图或商品链接，方便定位问题。',
    '处理结果会在当前会话同步给您。'
  ]
}

const categories = computed(() => {
  const customReplies = chatStore.quickReplies
    .filter((item) => item.content?.trim())
    .slice(0, 10)
    .map((item) => ({ id: `custom-${item.id}`, content: item.content }))

  return [
    {
      id: 'custom',
      label: '我的',
      replies: customReplies
    },
    {
      id: 'buyer',
      label: '买家',
      replies: defaults.buyer.map((content, index) => ({ id: `buyer-${index}`, content }))
    },
    {
      id: 'seller',
      label: '卖家',
      replies: defaults.seller.map((content, index) => ({ id: `seller-${index}`, content }))
    },
    {
      id: 'support',
      label: '客服',
      replies: defaults.support.map((content, index) => ({ id: `support-${index}`, content }))
    }
  ]
})

const activeReplies = computed(() => {
  return categories.value.find((item) => item.id === activeCategory.value)?.replies
    || categories.value[0]?.replies
    || []
})

onMounted(() => {
  if (chatStore.quickReplies.length === 0) {
    chatStore.fetchQuickReplies().catch(() => {})
  }
})

watch(
  () => props.scenario,
  (value) => {
    activeCategory.value = value || 'buyer'
  },
  { immediate: true }
)

function choose(reply) {
  if (reply?.content?.trim()) {
    emit('select', reply.content.trim())
  }
}
</script>

<template>
  <div class="quick-reply-panel">
    <div class="quick-reply-panel__tabs">
      <button
        v-for="item in categories"
        :key="item.id"
        type="button"
        class="quick-reply-panel__tab"
        :class="{ 'is-active': activeCategory === item.id }"
        @click="activeCategory = item.id"
      >
        {{ item.label }}
      </button>
    </div>

    <div v-if="chatStore.quickRepliesLoading" class="quick-reply-panel__loading">
      加载中...
    </div>

    <button
      v-for="reply in activeReplies"
      :key="reply.id"
      type="button"
      class="quick-reply-panel__item"
      @mousedown.prevent
      @click="choose(reply)"
    >
      {{ reply.content }}
    </button>

    <EmptyState
      v-if="!chatStore.quickRepliesLoading && activeReplies.length === 0"
      emoji="💬"
      title="暂无快捷回复"
      description="可以先创建一些常用话术。"
    />
  </div>
</template>

<style scoped>
.quick-reply-panel {
  width: min(520px, 100%);
  max-height: 280px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid #fed7aa;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.12);
}

.quick-reply-panel__tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.quick-reply-panel__tab {
  height: 28px;
  padding: 0 10px;
  border: none;
  border-radius: 8px;
  background: #f5f5f4;
  color: #78716c;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.quick-reply-panel__tab.is-active {
  background: #fed7aa;
  color: #ea580c;
  font-weight: 700;
}

.quick-reply-panel__item {
  width: 100%;
  display: block;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #1f2937;
  font: inherit;
  font-size: 14px;
  line-height: 1.5;
  text-align: left;
  cursor: pointer;
}

.quick-reply-panel__item:hover,
.quick-reply-panel__item:focus-visible {
  background: #fff7ed;
  color: #ea580c;
  outline: none;
}

.quick-reply-panel__loading {
  padding: 12px;
  color: #6b7280;
  font-size: 14px;
}
</style>
