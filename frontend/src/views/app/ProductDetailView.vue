<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import { addCartItem } from '@/api/modules/order'
import PageSection from '@/components/common/PageSection.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import RatingSummary from '@/components/common/RatingSummary.vue'
import ReviewList from '@/components/common/ReviewList.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useMarketStore } from '@/stores/market'
import { useRecommendStore } from '@/stores/recommend'
import { useReviewStore } from '@/stores/review'
import { handleImageFallback } from '@/utils/image-fallback'
import { isValidEntityId } from '@/utils/id-utils'

const props = defineProps({
  id: {
    type: String,
    default: ''
  }
})

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const marketStore = useMarketStore()
const recommendStore = useRecommendStore()
const reviewStore = useReviewStore()

const loading = ref(false)
const loadError = ref(false)
const quantity = ref(1)
const activeMediaIndex = ref(0)
const addToCartLoading = ref(false)
const buyNowLoading = ref(false)
const favoriteLoading = ref(false)
const shareDialogVisible = ref(false)
const shareLoading = ref(false)
const selectedShareConversationId = ref('')
const reviewPage = ref(1)
const product = ref(null)

const fulfillmentLabels = {
  logistics: '物流配送',
  offline: '线下交易',
  digital: '数字交付'
}

const productTypeLabels = {
  digital: '数字商品',
  physical: '实物商品',
  service: '服务'
}

const statusLabels = {
  on_sale: '在售',
  off_sale: '暂不售卖',
  draft: '草稿',
  closed: '已下架'
}

const reviewStatusLabels = {
  approved: '审核通过',
  pending_review: '审核中',
  not_required: '无需审核',
  rejected: '审核未通过'
}

const mediaList = computed(() => product.value?.media?.filter(Boolean) || [])
const selectedMedia = computed(() => mediaList.value[activeMediaIndex.value] || mediaList.value[0] || '')
const isFavorite = computed(() => marketStore.isFavorite(product.value?.id))

const detailModel = computed(() => {
  if (!product.value) {
    return null
  }

  const item = product.value
  const fulfillmentTypes = item.allowedFulfillmentTypes || []
  const conditionLabel =
    item.conditionLabel ||
    item.itemCondition ||
    item.condition ||
    (item.productType === 'digital' ? '数字内容交付' : '成色信息待卖家补充')

  const guaranteeItems = [
    item.productType === 'digital'
      ? '完整数字资源会在确认收货后开放，详情页仅展示预览与交付规则。'
      : '支持平台下单后在交易中心跟踪订单状态、评价与售后记录。',
    fulfillmentTypes.length
      ? `当前支持 ${fulfillmentTypes.map((type) => fulfillmentLabels[type]).join(' / ')}。`
      : '交付方式待确认。',
    item.shopId
      ? '可直接进入店铺主页查看卖家主体、店铺评价与更多在售商品。'
      : '当前商品以卖家个人名义展示，店铺主体信息待补充。'
  ]

  return {
    ...item,
    typeLabel: productTypeLabels[item.productType] || item.productType || '商品',
    statusLabel: statusLabels[item.status] || '状态待确认',
    reviewStatusLabel: reviewStatusLabels[item.reviewStatus] || '审核状态待确认',
    conditionLabel,
    fulfillmentText: fulfillmentTypes.length
      ? fulfillmentTypes.map((type) => fulfillmentLabels[type] || type).join(' / ')
      : '待补充',
    guaranteeItems
  }
})

const relatedProducts = computed(() =>
  recommendStore.alsoBoughtList.filter((item) => String(item.id) !== String(product.value?.id))
)

const reviewEntryDescription = computed(() =>
  authStore.isLoggedIn
    ? '只有已完成且归属于你的订单商品会出现在待评价列表。本页不能直接判断你是否购买过当前商品，请以待评价列表为准。'
    : '登录后可查看自己的待评价列表。只有已完成且归属于你的订单商品可以评价，未购买或未完成订单不会开放评价。'
)

function requireLogin(actionLabel) {
  if (authStore.isLoggedIn) {
    return true
  }
  ElMessage.warning(`${actionLabel}前请先登录`)
  router.push('/login')
  return false
}

async function loadProduct() {
  if (!isValidEntityId(props.id)) {
    product.value = null
    loadError.value = true
    return
  }

  loading.value = true
  loadError.value = false
  quantity.value = 1
  reviewPage.value = 1

  try {
    const detail = await marketStore.loadProductDetail(props.id)
    product.value = detail
    activeMediaIndex.value = 0

    await Promise.allSettled([
      recommendStore.loadAlsoBought(detail.id, 6),
      reviewStore.loadProductReviewSummary(detail.id),
      reviewStore.loadProductReviews(detail.id, reviewPage.value, 10),
      authStore.isLoggedIn ? marketStore.loadFavorites() : Promise.resolve([])
    ])
  } catch (error) {
    loadError.value = true
    product.value = null
    ElMessage.error(error?.response?.data?.message || error?.message || '商品详情加载失败')
  } finally {
    loading.value = false
  }
}

async function pushCartFlow(mode) {
  if (!requireLogin(mode === 'buy' ? '购买' : '加购') || !detailModel.value) {
    return
  }

  const loadingRef = mode === 'buy' ? buyNowLoading : addToCartLoading
  loadingRef.value = true

  try {
    await addCartItem({
      productId: Number(detailModel.value.id),
      quantity: quantity.value
    })

    if (mode === 'buy') {
      ElMessage.success('已加入购物车，先带你进入交易入口完成下单')
      router.push('/app/cart')
      return
    }

    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '操作失败，请稍后重试')
  } finally {
    loadingRef.value = false
  }
}

async function handleToggleFavorite() {
  if (!requireLogin('收藏') || !detailModel.value || favoriteLoading.value) {
    return
  }

  favoriteLoading.value = true
  const nextLabel = isFavorite.value ? '已取消收藏' : '已加入收藏'

  try {
    await marketStore.toggleFavoriteRemote(detailModel.value.id)
    ElMessage.success(nextLabel)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '收藏状态更新失败')
  } finally {
    favoriteLoading.value = false
  }
}

async function handleContactSeller() {
  if (!requireLogin('联系卖家') || !detailModel.value) {
    return
  }

  try {
    const conversation = await chatStore.findOrCreateConversation(
      detailModel.value.sellerId,
      detailModel.value.id,
      null
    )
    router.push({
      name: 'app-message-detail',
      params: { conversationId: String(conversation.id) }
    })
  } catch (error) {
    ElMessage.error('无法发起会话，请稍后重试')
  }
}

async function openShareDialog() {
  if (!requireLogin('分享到聊天') || !detailModel.value || shareLoading.value) {
    return
  }

  shareLoading.value = true
  try {
    await chatStore.fetchConversations()
    selectedShareConversationId.value =
      String(chatStore.activeConversationId || chatStore.conversations[0]?.id || '')
    shareDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '会话列表加载失败')
  } finally {
    shareLoading.value = false
  }
}

async function handleShareProductCard() {
  if (!selectedShareConversationId.value || !detailModel.value || shareLoading.value) {
    return
  }

  shareLoading.value = true
  try {
    const conversationId = Number(selectedShareConversationId.value)
    await chatStore.sendProductCardMessage(conversationId, Number(detailModel.value.id))
    shareDialogVisible.value = false
    ElMessage.success('已分享到聊天')
    router.push({
      name: 'app-message-detail',
      params: { conversationId: String(conversationId) }
    })
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '商品卡片发送失败')
  } finally {
    shareLoading.value = false
  }
}

function handleGoShop() {
  if (!detailModel.value?.shopId) {
    ElMessage.warning('当前商品还没有可进入的店铺主页')
    return
  }
  router.push(`/app/shops/${detailModel.value.shopId}`)
}

function handleGoPendingReviews() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('查看待评价前请先登录')
    router.push({
      path: '/login',
      query: {
        redirect: '/app/reviews/pending'
      }
    })
    return
  }

  router.push({
    path: '/app/reviews/pending',
    query: {
      productId: String(detailModel.value?.id || '')
    }
  })
}

function handleContactSupport() {
  if (!requireLogin('联系平台客服')) {
    return
  }
  router.push({
    path: '/app/support',
    query: {
      category: 'product',
      relatedType: 'product',
      relatedId: String(detailModel.value?.id || '')
    }
  })
}

function handleReviewPageChange(page) {
  reviewPage.value = page
  if (!detailModel.value) {
    return
  }
  reviewStore.loadProductReviews(detailModel.value.id, page, 10)
}

watch(() => props.id, loadProduct)
onMounted(loadProduct)
</script>

<template>
  <div class="shell-container page-stack">
    <template v-if="loading">
      <section class="product-detail">
        <div class="product-detail__gallery shell-card product-detail__loading-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" class="product-detail__loading-image" />
              <div class="product-detail__loading-thumbs">
                <el-skeleton-item
                  v-for="index in 4"
                  :key="index"
                  variant="image"
                  class="product-detail__loading-thumb"
                />
              </div>
            </template>
          </el-skeleton>
        </div>

        <aside class="product-detail__decision shell-card product-detail__loading-card">
          <el-skeleton animated :rows="10" />
        </aside>
      </section>
    </template>

    <ErrorBlock v-else-if="loadError" @retry="loadProduct" />

    <EmptyState
      v-else-if="!loading && !detailModel"
      title="商品暂时不可查看"
      description="该商品可能已下架、未通过审核，或详情数据暂时不可用。"
    >
      <el-button type="primary" @click="$router.push('/app/products')">返回商品列表</el-button>
    </EmptyState>

    <template v-else-if="detailModel">
      <section class="product-detail">
        <div class="product-detail__gallery shell-card">
          <div class="product-detail__gallery-main">
            <img
              :src="selectedMedia || detailModel.coverUrl"
              :alt="detailModel.title"
              loading="eager"
              @error="(event) => handleImageFallback(event, detailModel.title)"
            />
          </div>

          <div v-if="mediaList.length > 1" class="product-detail__thumbs">
            <button
              v-for="(media, index) in mediaList"
              :key="`${detailModel.id}-${index}`"
              type="button"
              class="product-detail__thumb"
              :class="{ 'is-active': index === activeMediaIndex }"
              @click="activeMediaIndex = index"
            >
              <img
                :src="media"
                :alt="`${detailModel.title} 预览 ${index + 1}`"
                loading="lazy"
                @error="(event) => handleImageFallback(event, detailModel.title)"
              />
            </button>
          </div>
        </div>

        <aside class="product-detail__decision shell-card">
          <div class="product-detail__eyebrows">
            <span class="eyebrow">{{ detailModel.categoryName || '校园交易' }}</span>
            <el-tag round effect="plain">{{ detailModel.typeLabel }}</el-tag>
            <el-tag round effect="plain" type="success">{{ detailModel.statusLabel }}</el-tag>
          </div>

          <div class="product-detail__headline">
            <h1>{{ detailModel.title }}</h1>
            <p v-if="detailModel.subtitle">{{ detailModel.subtitle }}</p>
          </div>

          <div class="product-detail__price">
            <strong>¥{{ Number(detailModel.salePrice || 0).toFixed(2) }}</strong>
            <span>收藏 {{ detailModel.favoriteCount }} · 浏览 {{ detailModel.viewCount }}</span>
          </div>

          <div class="product-detail__facts">
            <article>
              <span>类别</span>
              <strong>{{ detailModel.categoryName || '待补充' }}</strong>
            </article>
            <article>
              <span>成色 / 状态</span>
              <strong>{{ detailModel.conditionLabel }}</strong>
            </article>
            <article>
              <span>交易方式</span>
              <strong>{{ detailModel.fulfillmentText }}</strong>
            </article>
            <article>
              <span>审核信息</span>
              <strong>{{ detailModel.reviewStatusLabel }}</strong>
            </article>
          </div>

          <div class="product-detail__seller" @click="handleGoShop">
            <div>
              <span class="product-detail__seller-label">卖家 / 店铺</span>
              <strong>{{ detailModel.shopName || detailModel.sellerName || '个人卖家' }}</strong>
              <p>由 {{ detailModel.sellerName || '卖家' }} 发布，进入店铺主页查看主体信息与更多商品。</p>
            </div>
            <el-button text type="primary">进入店铺</el-button>
          </div>

          <div class="product-detail__actions">
            <div class="product-detail__quantity">
              <span>数量</span>
              <el-input-number v-model="quantity" :min="1" :max="99" />
            </div>

            <div class="product-detail__cta-grid">
              <el-button
                type="primary"
                size="large"
                :loading="buyNowLoading"
                @click="pushCartFlow('buy')"
              >
                立即购买
              </el-button>
              <el-button
                plain
                size="large"
                :loading="addToCartLoading"
                @click="pushCartFlow('cart')"
              >
                加入购物车
              </el-button>
              <el-button
                plain
                size="large"
                :loading="favoriteLoading"
                @click="handleToggleFavorite"
              >
                {{ isFavorite ? '已收藏' : '收藏商品' }}
              </el-button>
              <el-button plain size="large" @click="handleContactSeller">联系卖家</el-button>
              <el-button plain size="large" :loading="shareLoading" @click="openShareDialog">分享到聊天</el-button>
              <el-button plain size="large" @click="handleContactSupport">联系平台客服</el-button>
            </div>
          </div>

          <div class="product-detail__trust">
            <article v-for="item in detailModel.guaranteeItems" :key="item">
              <span class="product-detail__trust-dot" />
              <p>{{ item }}</p>
            </article>
          </div>

          <div class="product-detail__review-entry">
            <div>
              <span class="product-detail__review-kicker">评价资格</span>
              <h2>写评价从待评价列表进入</h2>
              <p>{{ reviewEntryDescription }}</p>
            </div>
            <el-button type="primary" plain @click="handleGoPendingReviews">
              {{ authStore.isLoggedIn ? '查看待评价' : '登录后查看待评价' }}
            </el-button>
          </div>
        </aside>
      </section>

      <div class="product-detail__mobile-bar shell-card">
        <div>
          <strong>¥{{ Number(detailModel.salePrice || 0).toFixed(2) }}</strong>
          <span>{{ detailModel.fulfillmentText }}</span>
        </div>
        <div class="product-detail__mobile-actions">
          <el-button plain @click="handleToggleFavorite">收藏</el-button>
          <el-button plain :loading="shareLoading" @click="openShareDialog">分享</el-button>
          <el-button type="primary" @click="pushCartFlow('buy')">购买</el-button>
        </div>
      </div>

      <PageSection title="商品描述" description="卖家对这件商品的详细说明。">
        <div class="product-detail__content-grid">
          <article class="product-detail__copy shell-card">
            <h3>商品说明</h3>
            <p>{{ detailModel.description || '卖家暂未补充更详细的商品说明。' }}</p>
          </article>

          <article class="product-detail__copy shell-card">
            <h3>交易与保障</h3>
            <ul class="product-detail__bullet-list">
              <li>交易方式：{{ detailModel.fulfillmentText }}</li>
              <li>商品类型：{{ detailModel.typeLabel }}</li>
              <li>平台状态：{{ detailModel.statusLabel }} / {{ detailModel.reviewStatusLabel }}</li>
              <li v-if="detailModel.previewRuleText">数字预览规则：{{ detailModel.previewRuleText }}</li>
            </ul>
          </article>
        </div>

        <div v-if="detailModel.previewAssets.length" class="product-detail__preview-assets">
          <article
            v-for="asset in detailModel.previewAssets"
            :key="asset.id || asset.assetName"
            class="product-detail__asset shell-card"
          >
            <strong>{{ asset.assetName || '预览资源' }}</strong>
            <p>{{ asset.assetUrl || '预览链接待补充' }}</p>
          </article>
        </div>
      </PageSection>

      <PageSection title="买家评价" description="看看其他买家的使用体验。">
        <div class="product-detail__reviews">
          <div class="product-detail__summary-card shell-card">
            <h3>评分概览</h3>
            <RatingSummary
              :summary="reviewStore.productReviewSummary"
              :loading="reviewStore.loadingProductSummary"
            />
          </div>

          <div class="product-detail__review-list shell-card">
            <h3>评价详情</h3>
            <ReviewList
              :reviews="reviewStore.productReviews"
              :loading="reviewStore.loadingProductReviews"
              :total="reviewStore.productReviewTotal"
              :page="reviewPage"
              @update:page="handleReviewPageChange"
            />
          </div>
        </div>
      </PageSection>

      <PageSection title="相关推荐" description="你可能还感兴趣的商品。">
        <div v-if="relatedProducts.length" class="product-grid">
          <article
            v-for="item in relatedProducts"
            :key="item.id"
            class="product-card shell-card"
            @click="$router.push(`/app/products/${item.id}`)"
          >
            <img
              :src="item.cover"
              :alt="item.title"
              class="product-card__cover"
              loading="lazy"
              decoding="async"
              @error="(event) => handleImageFallback(event, item.title)"
            />
            <div class="product-card__body">
              <div class="product-card__meta">
                <el-tag size="small">{{ item.categoryName }}</el-tag>
                <el-tag size="small" effect="plain">{{ productTypeLabels[item.productType] || item.productType }}</el-tag>
              </div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.shopName || item.sellerName }}</p>
              <div class="price-row">
                <strong>¥{{ Number(item.salePrice || item.price || 0).toFixed(2) }}</strong>
                <span>{{ item.favoriteCount }} 收藏</span>
              </div>
            </div>
          </article>
        </div>
        <EmptyState
          v-else
          title="暂时没有更多推荐"
          description="暂时没有更多推荐，去探索页发现更多好物吧。"
        >
          <el-button plain @click="handleGoShop">查看店铺</el-button>
          <el-button type="primary" @click="$router.push('/app/products')">继续逛商品</el-button>
        </EmptyState>
      </PageSection>
    </template>

    <el-dialog
      v-model="shareDialogVisible"
      title="分享到聊天"
      width="420px"
      :close-on-click-modal="false"
    >
      <div class="product-share-dialog">
        <p>{{ detailModel?.title }}</p>
        <el-select
          v-model="selectedShareConversationId"
          class="product-share-dialog__select"
          placeholder="选择会话"
          :disabled="shareLoading || !chatStore.conversations.length"
        >
          <el-option
            v-for="conversation in chatStore.conversations"
            :key="conversation.id"
            :label="conversation.peerUser?.nickname || conversation.peerUser?.username || `会话 ${conversation.id}`"
            :value="String(conversation.id)"
          />
        </el-select>
        <EmptyState
          v-if="!chatStore.conversations.length"
          title="暂无可分享会话"
          description="请先联系卖家或从消息中心创建会话。"
        />
      </div>
      <template #footer>
        <el-button @click="shareDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="shareLoading"
          :disabled="!selectedShareConversationId"
          @click="handleShareProductCard"
        >
          发送商品卡片
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.product-detail {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 24px;
  align-items: start;
}

.product-detail__gallery,
.product-detail__decision {
  display: grid;
  gap: 18px;
}

.product-detail__loading-card {
  min-height: 240px;
}

.product-detail__loading-image :deep(.el-skeleton__image) {
  width: 100%;
  height: auto;
  aspect-ratio: 1 / 1;
  border-radius: var(--cm-radius-lg);
}

.product-detail__loading-thumbs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.product-detail__loading-thumb :deep(.el-skeleton__image) {
  width: 100%;
  height: auto;
  aspect-ratio: 1 / 1;
  border-radius: var(--cm-radius-sm);
}

.product-detail__gallery-main {
  overflow: hidden;
  border-radius: var(--cm-radius-lg);
  background:
    radial-gradient(circle at top right, rgba(var(--cm-accent-rgb), 0.12), transparent 30%),
    var(--cm-surface-muted);
}

.product-detail__gallery-main img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.product-detail__thumbs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(82px, 1fr));
  gap: 12px;
}

.product-detail__thumb {
  padding: 0;
  border: 1px solid var(--cm-border);
  border-radius: var(--cm-radius-sm);
  overflow: hidden;
  background: var(--cm-surface-strong);
  cursor: pointer;
  transition:
    transform var(--cm-transition),
    border-color var(--cm-transition),
    box-shadow var(--cm-transition);
}

.product-detail__thumb:hover,
.product-detail__thumb.is-active {
  transform: translateY(-2px);
  border-color: rgba(var(--cm-primary-rgb), 0.35);
  box-shadow: var(--cm-shadow-soft);
}

.product-detail__thumb img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.product-detail__decision {
  position: sticky;
  top: 92px;
}

.product-detail__eyebrows {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.product-detail__headline {
  display: grid;
  gap: 10px;
}

.product-detail__headline h1 {
  margin: 0;
  font-size: clamp(30px, 4vw, 42px);
  line-height: 1.08;
}

.product-detail__headline p,
.product-detail__seller p {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.7;
}

.product-detail__price {
  display: grid;
  gap: 6px;
  padding: 18px 20px;
  border-radius: var(--cm-radius-md);
  background: linear-gradient(135deg, rgba(var(--cm-primary-rgb), 0.08), rgba(var(--cm-accent-rgb), 0.12));
}

.product-detail__price strong {
  font-size: clamp(34px, 4vw, 42px);
  line-height: 1;
  color: var(--cm-price);
}

.product-detail__price span,
.product-detail__quantity span {
  color: var(--cm-text-secondary);
  font-size: 14px;
}

.product-detail__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.product-detail__facts article {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: var(--cm-radius-sm);
  background: var(--cm-surface-muted);
}

.product-detail__facts span {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.product-detail__facts strong {
  color: var(--cm-text);
  line-height: 1.5;
}

.product-detail__seller {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px 18px;
  border: 1px solid var(--cm-border);
  border-radius: var(--cm-radius-md);
  background: rgba(255, 255, 255, 0.68);
  cursor: pointer;
}

.product-detail__seller-label {
  display: inline-block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--cm-text-tertiary);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-detail__actions {
  display: grid;
  gap: 14px;
}

.product-detail__quantity {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.product-detail__cta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.product-detail__trust {
  display: grid;
  gap: 12px;
  padding-top: 6px;
}

.product-detail__trust article {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.product-detail__trust p {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.product-detail__trust-dot {
  width: 10px;
  height: 10px;
  margin-top: 8px;
  border-radius: 50%;
  background: var(--cm-primary);
  flex: none;
}

.product-detail__review-entry {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 16px 18px;
  border: 1px solid rgba(var(--cm-primary-rgb), 0.24);
  border-radius: var(--cm-radius-md);
  background: rgba(var(--cm-primary-rgb), 0.06);
}

.product-detail__review-entry h2 {
  margin: 4px 0 8px;
  font-size: 18px;
  line-height: 1.3;
}

.product-detail__review-entry p {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.product-detail__review-kicker {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-detail__mobile-bar {
  display: none;
}

.product-share-dialog {
  display: grid;
  gap: 14px;
}

.product-share-dialog p {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.product-share-dialog__select {
  width: 100%;
}

.product-detail__content-grid,
.product-detail__reviews {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.product-detail__copy,
.product-detail__summary-card,
.product-detail__review-list,
.product-detail__asset {
  display: grid;
  gap: 14px;
}

.product-detail__copy h3,
.product-detail__summary-card h3,
.product-detail__review-list h3 {
  margin: 0;
}

.product-detail__copy p {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.8;
}

.product-detail__bullet-list {
  margin: 0;
  padding-left: 18px;
  color: var(--cm-text-secondary);
  line-height: 1.8;
}

.product-detail__preview-assets {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.product-detail__asset strong {
  font-size: 16px;
}

.product-detail__asset p {
  margin: 0;
  color: var(--cm-text-secondary);
  word-break: break-all;
}

@media (max-width: 1080px) {
  .product-detail {
    grid-template-columns: 1fr;
  }

  .product-detail__decision {
    position: static;
  }
}

@media (max-width: 768px) {
  .product-detail__facts,
  .product-detail__cta-grid,
  .product-detail__content-grid,
  .product-detail__reviews {
    grid-template-columns: 1fr;
  }

  .product-detail__seller,
  .product-detail__quantity,
  .product-detail__review-entry {
    align-items: flex-start;
    flex-direction: column;
  }

  .product-detail__review-entry {
    grid-template-columns: 1fr;
  }

  .product-detail__mobile-bar {
    position: sticky;
    bottom: 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 14px 16px;
    background: rgba(255, 250, 243, 0.92);
    backdrop-filter: blur(var(--cm-blur-medium));
    z-index: 2;
  }

  .product-detail__mobile-bar strong {
    display: block;
    color: var(--cm-price);
    font-size: 24px;
  }

  .product-detail__mobile-bar span {
    color: var(--cm-text-secondary);
    font-size: 13px;
  }

  .product-detail__mobile-actions {
    display: flex;
    gap: 10px;
  }
}
</style>
