<script setup>
import { computed, ref } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useChatStore } from '@/stores/chat'

const emit = defineEmits(['open-result'])
const chatStore = useChatStore()

const keyword = ref('')
const rangeAnchor = ref('')
const selectedDates = ref([])
const calendarCursor = ref(new Date())
const dateFilterOpen = ref(false)
const hasSearched = ref(false)

const results = computed(() => chatStore.searchResults)
const conversationMap = computed(() => new Map(chatStore.conversations.map((item) => [String(item.id), item])))
const monthNames = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const monthLabel = computed(() => `${calendarCursor.value.getFullYear()} 年 ${monthNames[calendarCursor.value.getMonth()]}`)
const sortedSelectedDates = computed(() => [...selectedDates.value].sort())
const selectedDateLabel = computed(() => {
  const dates = sortedSelectedDates.value
  if (dates.length === 0) return '不限时间'
  if (dates.length === 1) return dates[0]
  return `${dates[0]} 至 ${dates.at(-1)}`
})

const calendarCells = computed(() => {
  const year = calendarCursor.value.getFullYear()
  const month = calendarCursor.value.getMonth()
  const first = new Date(year, month, 1)
  const last = new Date(year, month + 1, 0)
  const cells = []
  for (let i = 0; i < first.getDay(); i += 1) {
    cells.push({ key: `empty-${i}`, day: '', date: '', empty: true })
  }
  for (let day = 1; day <= last.getDate(); day += 1) {
    const date = toDateString(new Date(year, month, day))
    cells.push({ key: date, day, date, empty: false })
  }
  return cells
})

function toIso(dateValue, endOfDay = false) {
  if (!dateValue) return undefined
  const time = endOfDay ? '23:59:59' : '00:00:00'
  const date = new Date(`${dateValue}T${time}`)
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString()
}

function toDateString(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function changeMonth(delta) {
  const next = new Date(calendarCursor.value)
  next.setMonth(next.getMonth() + delta)
  calendarCursor.value = next
}

function selectDate(dateValue) {
  if (!dateValue) return
  if (isDateSelected(dateValue)) {
    selectedDates.value = selectedDates.value.filter((item) => item !== dateValue)
    if (rangeAnchor.value === dateValue) rangeAnchor.value = ''
    return
  }

  if (!rangeAnchor.value) {
    rangeAnchor.value = dateValue
    selectedDates.value = [...new Set([...selectedDates.value, dateValue])].sort()
    return
  }

  const range = enumerateInclusiveRange(rangeAnchor.value, dateValue)
  selectedDates.value = [...new Set([...selectedDates.value, ...range])].sort()
  rangeAnchor.value = ''
}

function clearDateRange() {
  selectedDates.value = []
  rangeAnchor.value = ''
}

function enumerateInclusiveRange(aIso, bIso) {
  const d1 = new Date(`${aIso}T12:00:00`)
  const d2 = new Date(`${bIso}T12:00:00`)
  const start = d1 <= d2 ? d1 : d2
  const end = d1 <= d2 ? d2 : d1
  const out = []
  const cursor = new Date(start)
  while (cursor <= end) {
    out.push(toDateString(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }
  return out
}

function isDateSelected(dateValue) {
  return Boolean(dateValue && selectedDates.value.includes(dateValue))
}

function isToday(dateValue) {
  return dateValue === toDateString(new Date())
}

async function submit(page = 0) {
  const dates = sortedSelectedDates.value
  hasSearched.value = true
  dateFilterOpen.value = false
  await chatStore.searchMessages({
    keyword: keyword.value.trim() || undefined,
    startTime: toIso(dates[0]),
    endTime: toIso(dates.at(-1), true),
    page,
    size: 20
  })
}

function reset() {
  keyword.value = ''
  clearDateRange()
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
      <input v-model="keyword" type="search" class="message-search__keyword" placeholder="搜索消息、商品名或订单号" />
      <div class="message-search__date-filter">
        <button type="button" class="message-search__date-trigger" :class="{ 'is-open': dateFilterOpen }" @click="dateFilterOpen = !dateFilterOpen">
          <span>日期</span>
          <strong>{{ selectedDateLabel }}</strong>
        </button>
        <section v-if="dateFilterOpen" class="message-search__calendar" aria-label="消息日期范围">
          <header class="message-search__calendar-head">
            <div>
              <strong>按日期筛选</strong>
              <span>{{ selectedDates.length ? `已选 ${selectedDates.length} 天` : '点击两个日期可快速选中一段范围' }}</span>
            </div>
            <button type="button" @click="clearDateRange">清空</button>
          </header>
          <div class="message-search__calendar-nav">
            <button type="button" aria-label="上个月" @click="changeMonth(-1)">‹</button>
            <strong>{{ monthLabel }}</strong>
            <button type="button" aria-label="下个月" @click="changeMonth(1)">›</button>
          </div>
          <div class="message-search__weekdays">
            <span v-for="day in weekdays" :key="day">{{ day }}</span>
          </div>
          <div class="message-search__days">
            <button
              v-for="cell in calendarCells"
              :key="cell.key"
              type="button"
              :disabled="cell.empty"
              :class="{ 'is-selected': isDateSelected(cell.date), 'is-anchor': rangeAnchor === cell.date, 'is-today': isToday(cell.date) }"
              @click="selectDate(cell.date)"
            >
              {{ cell.day }}
            </button>
          </div>
        </section>
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
        emoji="馃攷"
        title="没有找到匹配消息"
        description="可以换一个关键词，或者放宽日期范围。"
      />

      <EmptyState
        v-else
        emoji="馃晳"
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
  grid-template-columns: minmax(220px, 1fr) minmax(190px, auto) auto;
  align-items: center;
  gap: 12px;
  padding: 20px 24px;
  border-bottom: 1px solid #f5f5f4;
  background: #fffbf5;
  position: relative;
  overflow: visible;
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

.message-search__actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.message-search__date-filter {
  position: relative;
}

.message-search__date-trigger {
  min-width: 190px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #e7e5e4;
  border-radius: 10px;
  padding: 0 12px;
  background: #fff;
  color: #57534e;
  font: inherit;
  cursor: pointer;
}

.message-search__date-trigger span {
  font-size: 13px;
  font-weight: 800;
}

.message-search__date-trigger strong {
  max-width: 148px;
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-search__date-trigger.is-open,
.message-search__date-trigger:hover,
.message-search__date-trigger:focus-visible {
  border-color: #fed7aa;
  background: #fff7ed;
  outline: none;
}

.message-search__calendar {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 40;
  width: min(320px, calc(100vw - 48px));
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e7e5e4;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(31, 41, 55, 0.18);
}

.message-search__calendar-head,
.message-search__calendar-nav,
.message-search__weekdays,
.message-search__days {
  display: grid;
}

.message-search__calendar-head {
  grid-template-columns: 1fr auto;
  align-items: start;
  gap: 10px;
}

.message-search__calendar-head div {
  display: grid;
  gap: 2px;
}

.message-search__calendar-head strong {
  color: #1f2937;
  font-size: 14px;
}

.message-search__calendar-head span {
  color: #78716c;
  font-size: 12px;
}

.message-search__calendar-head button {
  height: 28px;
  border: none;
  border-radius: 8px;
  padding: 0 9px;
  background: #f5f5f4;
  color: #57534e;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.message-search__calendar-nav {
  grid-template-columns: 32px 1fr 32px;
  align-items: center;
  text-align: center;
}

.message-search__calendar-nav button {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #57534e;
  font-size: 22px;
  cursor: pointer;
}

.message-search__calendar-nav button:hover,
.message-search__calendar-nav button:focus-visible,
.message-search__calendar-head button:hover,
.message-search__calendar-head button:focus-visible {
  background: #fff7ed;
  color: #ea580c;
  outline: none;
}

.message-search__weekdays,
.message-search__days {
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 4px;
}

.message-search__weekdays span {
  color: #a8a29e;
  font-size: 11px;
  font-weight: 800;
  text-align: center;
}

.message-search__days button {
  aspect-ratio: 1;
  min-width: 0;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #57534e;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.message-search__days button:hover,
.message-search__days button:focus-visible {
  background: #f5f5f4;
  outline: none;
}

.message-search__days button.is-selected,
.message-search__days button.is-anchor {
  background: #ea580c;
  color: #fff;
  box-shadow: 0 4px 10px rgba(234, 88, 12, 0.22);
}

.message-search__days button.is-today:not(.is-selected):not(.is-anchor) {
  color: #c2410c;
  outline: 1px solid #fed7aa;
}

.message-search__days button:disabled {
  cursor: default;
  opacity: 0;
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

  .message-search__actions {
    flex-wrap: wrap;
  }
}
</style>
