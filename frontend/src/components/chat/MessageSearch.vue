<script setup>
import { computed, ref } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useChatStore } from '@/stores/chat'

const emit = defineEmits(['open-result'])
const chatStore = useChatStore()

const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const hasSearched = ref(false)

const results = computed(() => chatStore.searchResults)
const conversationMap = computed(() => new Map(chatStore.conversations.map((item) => [String(item.id), item])))

function toIso(dateValue, endOfDay = false) {
  if (!dateValue) return undefined
  const time = endOfDay ? '23:59:59' : '00:00:00'
  const date = new Date(`${dateValue}T${time}`)
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString()
}

async function submit(page = 0) {
  hasSearched.value = true
  await chatStore.searchMessages({
    keyword: keyword.value.trim() || undefined,
    startTime: toIso(startDate.value),
    endTime: toIso(endDate.value, true),
    page,
    size: 20
  })
}

function reset() {
  keyword.value = ''
  startDate.value = ''
  endDate.value = ''
  hasSearched.value = false
  chatStore.clearSearchResults()
}

function titleFor(result) {
  const conversation = conversationMap.value.get(String(result.conversationId))
  const peerUser = conversation?.peerUser || {}
  return peerUser.nickname || peerUser.username || `会话 #${result.conversationId}`
}

function snippet(result) {
  if (result.isRecalled) return '消息已撤回'
  return result.body || ''
}

function highlight(text) {
  const source = String(text || '')
  const token = keyword.value.trim()
  if (!token) return [{ text: source, match: false }]

  const lowerSource = source.toLowerCase()
  const lowerToken = token.toLowerCase()
  const parts = []
  let cursor = 0
  let index = lowerSource.indexOf(lowerToken)

  while (index !== -1) {
    if (index > cursor) parts.push({ text: source.slice(cursor, index), match: false })
    parts.push({ text: source.slice(index, index + token.length), match: true })
    cursor = index + token.length
    index = lowerSource.indexOf(lowerToken, cursor)
  }

  if (cursor < source.length) parts.push({ text: source.slice(cursor), match: false })
  return parts.length ? parts : [{ text: source, match: false }]
}

function openResult(result) {
  emit('open-result', result)
}
</script>

<template>
  <section class="message-search">
    <form class="message-search__form" @submit.prevent="submit(0)">
      <input v-model="keyword" type="search" class="message-search__keyword" placeholder="搜索消息、商品名、订单号" />
      <div class="message-search__dates">
        <label><span>开始</span><input v-model="startDate" type="date" /></label>
        <label><span>结束</span><input v-model="endDate" type="date" /></label>
      </div>
      <div class="message-search__actions">
        <button type="button" class="message-search__ghost" @click="reset">清空</button>
        <button type="submit" class="message-search__primary" :disabled="chatStore.searchLoading">
          {{ chatStore.searchLoading ? '搜索中...' : '搜索' }}
        </button>
      </div>
    </form>

    <div class="message-search__body">
      <div v-if="chatStore.searchLoading" class="message-search__loading">
        <el-skeleton animated :rows="4" />
      </div>

      <template v-else-if="results.length">
        <button
          v-for="result in results"
          :key="result.id"
          type="button"
          class="message-search__result"
          @click="openResult(result)"
        >
          <span class="message-search__result-top">
            <strong>{{ titleFor(result) }}</strong>
            <time>{{ new Date(result.createdAt).toLocaleString('zh-CN') }}</time>
          </span>
          <span class="message-search__snippet">
            <template v-for="(part, index) in highlight(snippet(result))" :key="`${result.id}-${index}`">
              <mark v-if="part.match">{{ part.text }}</mark>
              <span v-else>{{ part.text }}</span>
            </template>
          </span>
        </button>

        <button
          v-if="chatStore.searchPagination.page + 1 < chatStore.searchPagination.totalPages"
          type="button"
          class="message-search__more"
          @click="submit(chatStore.searchPagination.page + 1)"
        >
          加载更多
        </button>
      </template>

      <EmptyState
        v-else-if="hasSearched"
        emoji="🔎"
        title="未找到匹配的消息"
        description="可以换一个关键词或放宽日期范围。"
      />

      <EmptyState
        v-else
        emoji="🕑"
        title="搜索历史消息"
        description="输入关键词或日期范围，快速定位聊天记录。"
      />
    </div>
  </section>
</template>

<style scoped>
.message-search {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
  background: #fff;
}

.message-search__form {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto auto;
  gap: 12px;
  padding: 20px 24px;
  border-bottom: 1px solid #f5f5f4;
  background: #fffbf5;
}

.message-search input {
  height: 40px;
  border: 1px solid #e7e5e4;
  border-radius: 10px;
  padding: 0 12px;
  font: inherit;
  color: #1f2937;
  background: #fff;
}

.message-search__dates,
.message-search__actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.message-search__ghost,
.message-search__primary,
.message-search__more {
  height: 40px;
  border: none;
  border-radius: 10px;
  padding: 0 16px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.message-search__ghost {
  background: #f5f5f4;
  color: #57534e;
}

.message-search__primary,
.message-search__more {
  background: #ea580c;
  color: #fff;
}

.message-search__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px 24px;
}

.message-search__result {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border: none;
  border-radius: 12px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.message-search__result:hover,
.message-search__result:focus-visible {
  background: #fff7ed;
  outline: none;
}

.message-search__result-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.message-search__snippet mark {
  padding: 0 2px;
  border-radius: 4px;
  background: #fef08a;
}

.message-search__more {
  display: block;
  margin: 16px auto 0;
}

@media (max-width: 900px) {
  .message-search__form {
    grid-template-columns: 1fr;
  }

  .message-search__dates,
  .message-search__actions {
    flex-wrap: wrap;
  }
}
</style>
