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
const newReply = ref('')
const saving = ref(false)

const defaults = {
  buyer: [
    '你好，这件还在吗？',
    '方便补几张实拍图吗？',
    '可以校内当面交易吗？',
    '价格还有商量空间吗？',
    '今天方便交易吗？'
  ],
  seller: [
    '你好，这件商品目前还可以正常下单。',
    '支持校内当面交易，也可以继续沟通时间地点。',
    '确认后我会尽快安排发货或交付。',
    '如果需要，我可以再补几张细节图给你。',
    '有其他问题也可以继续留言，我会尽快回复。'
  ],
  support: [
    '请先提供订单号，我帮你核对一下。',
    '这个问题我先为你记录，并继续跟进处理。',
    '我已经收到你的反馈，会尽快同步进展。',
    '请补充截图、链接或订单信息，方便我们定位问题。',
    '处理结果会在当前会话里继续同步给你。'
  ]
}

const customReplies = computed(() => {
  return chatStore.quickReplies
    .filter((item) => item.content?.trim())
    .slice(0, 10)
    .map((item) => ({ id: `custom-${item.id}`, content: item.content }))
})

const presetCategory = computed(() => {
  const scenario = defaults[props.scenario] ? props.scenario : 'buyer'
  const labels = {
    buyer: '买家常用',
    seller: '卖家常用',
    support: '客服常用'
  }
  return {
    id: scenario,
    label: labels[scenario],
    replies: defaults[scenario].map((content, index) => ({ id: `${scenario}-${index}`, content }))
  }
})

const categories = computed(() => {
  return [
    {
      id: 'custom',
      label: '我的',
      replies: customReplies.value
    },
    presetCategory.value
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
    activeCategory.value = defaults[value] ? value : 'buyer'
  },
  { immediate: true }
)

function choose(reply) {
  if (reply?.content?.trim()) {
    emit('select', reply.content.trim())
  }
}

async function createCustomReply() {
  const content = newReply.value.trim()
  if (!content || saving.value) return
  saving.value = true
  try {
    await chatStore.createQuickReply({
      content,
      sortOrder: chatStore.quickReplies.length + 1
    })
    newReply.value = ''
    activeCategory.value = 'custom'
  } finally {
    saving.value = false
  }
}

function deleteCustomReply(reply) {
  const id = String(reply.id || '').replace('custom-', '')
  if (id) chatStore.deleteQuickReply(id).catch(() => {})
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

    <form class="quick-reply-panel__form" @submit.prevent="createCustomReply">
      <input v-model="newReply" type="text" maxlength="500" placeholder="添加一条自定义快捷回复" />
      <button type="submit" :disabled="!newReply.trim() || saving">
        {{ saving ? '保存中' : '添加' }}
      </button>
    </form>

    <div class="quick-reply-panel__list">
      <div
        v-for="reply in activeReplies"
        :key="reply.id"
        class="quick-reply-panel__row"
      >
        <button
          type="button"
          class="quick-reply-panel__item"
          @mousedown.prevent
          @click="choose(reply)"
        >
          {{ reply.content }}
        </button>
        <button
          v-if="activeCategory === 'custom'"
          type="button"
          class="quick-reply-panel__delete"
          aria-label="删除快捷回复"
          @mousedown.prevent
          @click="deleteCustomReply(reply)"
        >
          删除
        </button>
      </div>
    </div>

    <EmptyState
      v-if="!chatStore.quickRepliesLoading && activeReplies.length === 0"
      emoji="馃挰"
      title="暂无快捷回复"
      description="可以先创建几条常用话术。"
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
  flex: 1;
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

.quick-reply-panel__form {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.quick-reply-panel__form input {
  flex: 1;
  min-width: 0;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #e7e5e4;
  border-radius: 8px;
  font: inherit;
  font-size: 14px;
}

.quick-reply-panel__form button,
.quick-reply-panel__delete {
  border: none;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.quick-reply-panel__form button {
  height: 34px;
  padding: 0 12px;
  background: #ea580c;
  color: #fff;
}

.quick-reply-panel__form button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quick-reply-panel__list {
  display: grid;
  gap: 4px;
}

.quick-reply-panel__row {
  display: flex;
  gap: 6px;
  align-items: stretch;
}

.quick-reply-panel__delete {
  flex-shrink: 0;
  padding: 0 10px;
  background: #fee2e2;
  color: #b91c1c;
}
</style>
