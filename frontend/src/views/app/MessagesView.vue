<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'

const props = defineProps({
  conversationId: {
    type: String,
    default: ''
  }
})

const route = useRoute()
const router = useRouter()
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)

const categoryOptions = [
  {
    id: 'trade',
    label: '交易',
    eyebrow: 'Trade',
    description: '围绕订单、售后和交付的会话'
  },
  {
    id: 'shop',
    label: '店铺',
    eyebrow: 'Shops',
    description: '咨询商品、联系店主与店铺运营通知'
  },
  {
    id: 'support',
    label: '客服',
    eyebrow: 'Support',
    description: '平台客服、举报协助与异常处理入口'
  },
  {
    id: 'group',
    label: '群聊',
    eyebrow: 'Groups',
    description: '粉丝群、优惠群与活动群'
  }
]

const placeholderConversations = [
  {
    id: 'trade-order-230512',
    category: 'trade',
    title: '订单 #230512 售后协商',
    counterpart: '晨曦二手书铺',
    preview: '您好，请问教材退款进度如何？',
    updatedAt: '今天 14:20',
    unread: 2,
    state: '进行中',
    tags: ['订单关联', '售后'],
    messages: [
      {
        id: 'm1',
        role: 'system',
        label: '系统',
        body: '系统提示：双方可在此沟通订单相关事宜。'
      },
      {
        id: 'm2',
        role: 'other',
        label: '店铺',
        body: '您好，退款已提交审核，预计1-2个工作日内到账。'
      },
      {
        id: 'm3',
        role: 'self',
        label: '我',
        body: '好的，谢谢！'
      }
    ]
  },
  {
    id: 'shop-lab-supplies',
    category: 'shop',
    title: '实验用品店 咨询',
    counterpart: '实验用品店',
    preview: '请问这个实验试剂还有库存吗？',
    updatedAt: '昨天 19:05',
    unread: 0,
    state: '等待回复',
    tags: ['商品咨询', '店铺'],
    messages: [
      {
        id: 'm4',
        role: 'system',
        label: '系统',
        body: '系统提示：您正在咨询实验用品店。'
      },
      {
        id: 'm5',
        role: 'other',
        label: '店铺',
        body: '您好！这款试剂目前有货，明天可以在一教门口交接。'
      }
    ]
  },
  {
    id: 'support-dispute-901',
    category: 'support',
    title: '平台客服协助',
    counterpart: 'CampusMarket Support',
    preview: '我要反馈一个订单问题。',
    updatedAt: '昨天 11:40',
    unread: 1,
    state: '等待处理',
    tags: ['平台客服', '治理'],
    messages: [
      {
        id: 'm6',
        role: 'system',
        label: '系统',
        body: '系统提示：平台客服将在工作时间内回复您。'
      },
      {
        id: 'm7',
        role: 'other',
        label: '客服',
        body: '进行中：客服会话列表、会话状态、指派、处理结论与消息审核规则。'
      }
    ]
  }
]

const groupPlaceholders = [
  {
    title: '粉丝群',
    description: '关注的店铺上新、活动通知第一时间收到。'
  },
  {
    title: '优惠群',
    description: '限时折扣、拼单信息实时推送。'
  },
  {
    title: '校园活动群',
    description: '约自习、拼团购、组队活动都在这里。'
  }
]

const futureEntryConventions = [
  '从商品详情页可直接发起咨询，自动关联到对应店铺。',
  '从店铺主页也可以进入对话，方便询问商品库存或活动信息。',
  '订单详情中可以发起售后沟通或请求平台客服协助。',
  '消息功能正在完善中，更多会话场景即将上线。'
]

const isMobile = computed(() => windowWidth.value < 900)
const selectedCategory = computed(() =>
  categoryOptions.some((item) => item.id === route.query.category)
    ? route.query.category
    : 'trade'
)

const visibleConversations = computed(() =>
  placeholderConversations.filter((item) => item.category === selectedCategory.value)
)

const activeConversation = computed(() => {
  const requestedId = props.conversationId || route.params.conversationId
  if (requestedId) {
    return visibleConversations.value.find((item) => item.id === requestedId) || null
  }
  if (isMobile.value) {
    return null
  }
  return visibleConversations.value[0] || null
})

const activeCategoryMeta = computed(() =>
  categoryOptions.find((item) => item.id === selectedCategory.value) || categoryOptions[0]
)

const entryContext = computed(() => {
  const { entry, entryId, targetType, targetId, intent } = route.query
  if (!entry && !entryId && !targetType && !targetId && !intent) {
    return null
  }

  return {
    entry: entry || 'unknown',
    entryId: entryId || 'pending',
    targetType: targetType || 'pending',
    targetId: targetId || 'pending',
    intent: intent || 'consult'
  }
})

const mobileShowsDetail = computed(() => Boolean(isMobile.value && activeConversation.value))

function buildQuery(nextCategory = selectedCategory.value) {
  return {
    ...route.query,
    category: nextCategory
  }
}

function handleResize() {
  windowWidth.value = window.innerWidth
}

function openCategory(categoryId) {
  const nextConversation = placeholderConversations.find((item) => item.category === categoryId)
  const nextQuery = buildQuery(categoryId)

  if (isMobile.value || !nextConversation) {
    router.push({
      path: '/app/messages',
      query: nextQuery
    })
    return
  }

  router.push({
    path: `/app/messages/${nextConversation.id}`,
    query: nextQuery
  })
}

function openConversation(conversationId) {
  router.push({
    path: `/app/messages/${conversationId}`,
    query: buildQuery()
  })
}

function backToList() {
  router.push({
    path: '/app/messages',
    query: buildQuery()
  })
}

watch(
  () => selectedCategory.value,
  (categoryId) => {
    if (!props.conversationId) {
      return
    }
    const stillVisible = visibleConversations.value.some((item) => item.id === props.conversationId)
    if (!stillVisible) {
      const nextConversation = placeholderConversations.find((item) => item.category === categoryId)
      if (!nextConversation || isMobile.value) {
        backToList()
        return
      }
      openConversation(nextConversation.id)
    }
  }
)

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card shell-hero messages-hero">
      <div class="shell-hero__content">
        <span class="eyebrow">Message Center</span>
        <h1>消息中心</h1>
        <p>
          这里先建立前台消息中心的信息架构和响应式 UI 壳，承接交易、店铺、客服与群聊四类沟通入口。
          当前不会提交真实消息，也不会假装聊天后端、WebSocket 或消息表已经存在。
        </p>
        <div class="shell-inline-actions">
          <el-tag effect="plain" type="info">消息功能建设中</el-tag>
        </div>
      </div>

      <div class="messages-hero__aside">
        <div class="metric-card">
          <strong>4 类入口</strong>
          <span>交易、店铺、客服、群聊统一进入一级消息域。</span>
        </div>
        <div class="metric-card">
          <strong>移动端双态</strong>
          <span>手机端按“列表 / 详情”切换，避免会话区挤压成桌面缩略版。</span>
        </div>
      </div>
    </section>

    <section v-if="entryContext" class="shell-card messages-context">
      <div>
        <span class="eyebrow">Entry Context</span>
        <h2>已收到未来跳转上下文</h2>
        <p>当前页面识别到了来自商品、店铺或订单页的上下文约定，但不会据此创建真实会话。</p>
      </div>

      <div class="messages-context__chips">
        <span>entry={{ entryContext.entry }}</span>
        <span>entryId={{ entryContext.entryId }}</span>
        <span>targetType={{ entryContext.targetType }}</span>
        <span>targetId={{ entryContext.targetId }}</span>
        <span>intent={{ entryContext.intent }}</span>
      </div>
    </section>

    <section class="messages-layout">
      <aside v-if="!mobileShowsDetail" class="shell-card messages-sidebar">
        <div class="messages-sidebar__header">
          <div>
            <span class="eyebrow">Conversations</span>
            <h2>会话列表</h2>
          </div>
          <el-tag effect="plain" type="info">{{ activeCategoryMeta.label }}</el-tag>
        </div>

        <div class="messages-categories" aria-label="消息分类">
          <button
            v-for="item in categoryOptions"
            :key="item.id"
            type="button"
            class="messages-category"
            :class="{ 'is-active': selectedCategory === item.id }"
            @click="openCategory(item.id)"
          >
            <span class="messages-category__eyebrow">{{ item.eyebrow }}</span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </button>
        </div>

        <div v-if="visibleConversations.length" class="messages-conversation-list">
          <button
            v-for="item in visibleConversations"
            :key="item.id"
            type="button"
            class="messages-conversation"
            :class="{ 'is-active': activeConversation?.id === item.id }"
            @click="openConversation(item.id)"
          >
            <div class="messages-conversation__main">
              <div class="messages-avatar">{{ item.counterpart.slice(0, 1) }}</div>
              <div class="messages-conversation__copy">
                <div class="messages-conversation__topline">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.updatedAt }}</span>
                </div>
                <p>{{ item.preview }}</p>
                <div class="messages-conversation__tags">
                  <el-tag
                    v-for="tag in item.tags"
                    :key="tag"
                    effect="plain"
                    size="small"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
            </div>
            <span v-if="item.unread" class="messages-unread">{{ item.unread }}</span>
          </button>
        </div>

        <EmptyState
          v-else
          emoji="◌"
          title="当前分类还没有会话"
          :description="selectedCategory === 'group'
            ? '群聊功能即将上线，敬请期待。'
            : '暂时还没有这个分类的消息。'"
        >
          <div v-if="selectedCategory === 'group'" class="messages-group-placeholders">
            <article
              v-for="item in groupPlaceholders"
              :key="item.title"
              class="messages-group-placeholder"
            >
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </article>
          </div>
        </EmptyState>
      </aside>

      <section class="shell-card messages-detail">
        <div v-if="mobileShowsDetail" class="messages-detail__mobile-back">
          <el-button plain @click="backToList">返回会话列表</el-button>
        </div>

        <template v-if="activeConversation">
          <header class="messages-detail__header">
            <div>
              <span class="eyebrow">{{ activeCategoryMeta.label }}</span>
              <h2>{{ activeConversation.title }}</h2>
              <p>{{ activeConversation.counterpart }} · {{ activeConversation.state }}</p>
            </div>
            <div class="shell-inline-actions">
              <el-tag
                v-for="tag in activeConversation.tags"
                :key="tag"
                effect="plain"
              >
                {{ tag }}
              </el-tag>
            </div>
          </header>

          <div class="messages-thread">
            <article
              v-for="message in activeConversation.messages"
              :key="message.id"
              class="messages-bubble"
              :class="`messages-bubble--${message.role}`"
            >
              <strong>{{ message.label }}</strong>
              <p>{{ message.body }}</p>
            </article>
          </div>

          <section class="messages-detail__disabled-box">
            <div>
              <strong>消息功能建设中</strong>
              <p>
                发送消息功能即将上线，届时你可以在这里与卖家和客服直接沟通。
              </p>
            </div>
            <div class="messages-composer">
              <textarea
                disabled
                rows="4"
                placeholder="消息发送功能即将开放…"
              />
              <div class="shell-inline-actions">
                <el-button plain disabled>发送消息</el-button>
                <el-button plain disabled>上传图片</el-button>
                <el-button plain disabled>发起售后协助</el-button>
              </div>
            </div>
          </section>
        </template>

        <EmptyState
          v-else
          emoji="✉"
          title="选择一个会话查看详情"
          description="点击左侧会话列表中的任意一条消息开始查看。"
        />
      </section>
    </section>

    <section class="shell-card messages-conventions">
      <div class="section-heading">
        <h2>消息使用场景</h2>
      </div>

      <div class="messages-conventions__list">
        <article
          v-for="item in futureEntryConventions"
          :key="item"
          class="messages-conventions__item"
        >
          {{ item }}
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.messages-hero {
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
}

.messages-hero__aside {
  display: grid;
  gap: 16px;
}

.messages-context,
.messages-context__chips,
.messages-group-placeholders,
.messages-conventions__list {
  display: grid;
  gap: 14px;
}

.messages-context__chips {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.messages-context__chips span {
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px dashed var(--cm-border-strong);
  background: rgba(255, 255, 255, 0.72);
  color: var(--cm-text-secondary);
  font-size: 13px;
}

.messages-layout {
  display: grid;
  grid-template-columns: minmax(320px, 360px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.messages-sidebar,
.messages-detail {
  min-height: 720px;
}

.messages-sidebar,
.messages-conversation-list,
.messages-detail,
.messages-thread,
.messages-composer,
.messages-conventions__item {
  display: grid;
  gap: 16px;
}

.messages-sidebar__header,
.messages-conversation,
.messages-detail__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.messages-categories {
  display: grid;
  gap: 10px;
}

.messages-category,
.messages-conversation {
  width: 100%;
  border: 1px solid var(--cm-border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  text-align: left;
  transition:
    transform var(--cm-transition-micro),
    border-color var(--cm-transition-micro),
    box-shadow var(--cm-transition-micro);
}

.messages-category {
  padding: 16px 18px;
  display: grid;
  gap: 6px;
}

.messages-category:hover,
.messages-conversation:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--cm-primary-rgb), 0.24);
  box-shadow: var(--cm-shadow-soft);
}

.messages-category.is-active,
.messages-conversation.is-active {
  border-color: rgba(var(--cm-primary-rgb), 0.32);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(251, 239, 229, 0.94));
  box-shadow: var(--cm-shadow-md);
}

.messages-category__eyebrow {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.messages-category strong,
.messages-group-placeholder strong {
  font-size: 16px;
  line-height: 1.4;
}

.messages-category small,
.messages-group-placeholder p,
.messages-conversation__copy p,
.messages-detail__header p,
.messages-bubble p,
.messages-detail__disabled-box p,
.messages-conventions__item {
  color: var(--cm-text-secondary);
  line-height: 1.7;
}

.messages-conversation {
  align-items: flex-start;
  padding: 16px;
}

.messages-conversation__main {
  display: flex;
  gap: 14px;
  min-width: 0;
}

.messages-avatar {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: rgba(var(--cm-primary-rgb), 0.12);
  color: var(--cm-primary);
  font-weight: 700;
  flex-shrink: 0;
}

.messages-conversation__copy {
  min-width: 0;
  display: grid;
  gap: 8px;
}

.messages-conversation__topline {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.messages-conversation__topline strong {
  font-size: 15px;
  line-height: 1.45;
}

.messages-conversation__topline span {
  flex-shrink: 0;
  color: var(--cm-text-tertiary);
  font-size: 12px;
}

.messages-conversation__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.messages-unread {
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--cm-gradient-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.messages-detail__mobile-back {
  margin-bottom: 6px;
}

.messages-detail__header {
  align-items: flex-start;
}

.messages-thread {
  align-content: start;
}

.messages-bubble {
  width: min(100%, 560px);
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid var(--cm-border);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--cm-shadow-soft);
}

.messages-bubble strong {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
}

.messages-bubble--self {
  justify-self: end;
  background: rgba(255, 247, 239, 0.96);
  border-color: rgba(var(--cm-primary-rgb), 0.22);
}

.messages-bubble--system {
  width: 100%;
  justify-self: stretch;
  border-style: dashed;
  background: rgba(250, 248, 244, 0.94);
}

.messages-detail__disabled-box {
  margin-top: auto;
  padding: 18px;
  border-radius: 20px;
  border: 1px dashed rgba(var(--cm-primary-rgb), 0.24);
  background: rgba(248, 244, 239, 0.92);
}

.messages-composer textarea {
  width: 100%;
  resize: none;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--cm-border);
  background: rgba(255, 255, 255, 0.8);
  color: var(--cm-text-tertiary);
}

.messages-group-placeholder,
.messages-conventions__item {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid var(--cm-border);
  background: rgba(255, 255, 255, 0.72);
}

@media (max-width: 1100px) {
  .messages-layout,
  .messages-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .messages-sidebar,
  .messages-detail {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .messages-sidebar__header,
  .messages-detail__header,
  .messages-conversation,
  .messages-conversation__topline {
    flex-direction: column;
  }

  .messages-conversation__topline span,
  .messages-unread {
    align-self: flex-start;
  }

  .messages-bubble {
    width: 100%;
  }
}
</style>
