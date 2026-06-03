<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useMarketStore } from '@/stores/market'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'

const marketStore = useMarketStore()
const loading = ref(false)
const loadError = ref(false)

const filters = reactive({
  keyword: '',
  reviewStatus: ''
})

const ownedShop = computed(() => marketStore.ownedShop.shop)

const rows = computed(() =>
  marketStore.getMyProducts().filter((item) => {
    const keywordMatched =
      !filters.keyword ||
      item.title.toLowerCase().includes(filters.keyword.toLowerCase()) ||
      item.categoryName.toLowerCase().includes(filters.keyword.toLowerCase())

    const reviewMatched = !filters.reviewStatus || item.reviewStatus === filters.reviewStatus

    return keywordMatched && reviewMatched
  })
)

function statusLabel(item) {
  if (item.status === 'draft') {
    return '暂存'
  }

  if (item.reviewStatus === 'pending_review') {
    return '待审核'
  }

  if (item.status === 'pending') {
    return '待上架'
  }

  return '展示中'
}

function reviewStatusLabel(value) {
  return (
    {
      pending_review: '审核中',
      approved: '已通过',
      not_required: '无需审核',
      rejected: '未通过'
    }[value] ||
    value ||
    '状态待确认'
  )
}

async function loadProducts() {
  loading.value = true
  loadError.value = false
  try {
    const results = await Promise.allSettled([marketStore.loadMyProducts(), marketStore.loadMyShop()])
    if (results.some((item) => item.status === 'rejected')) {
      throw new Error('店铺商品管理加载失败')
    }
  } catch {
    loadError.value = true
    ElMessage.error('店铺商品管理加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadProducts)
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card shell-hero shell-hero--compact">
      <div>
        <span class="eyebrow">Shop Manage</span>
        <h1>店铺商品管理</h1>
        <p>
          店主管理能力继续留在前台个人域。
          {{
            ownedShop?.id
              ? `当前关联店铺为「${ownedShop.name}」，商品公开展示仍以店铺为主体。`
              : '当前账号暂未绑定店铺，仍可先整理商品信息并提交发布。'
          }}
        </p>
      </div>
      <div class="shell-hero__meta shell-hero__meta--column">
        <el-button type="primary" @click="$router.push('/app/shop/manage/publish')">发布商品</el-button>
        <el-button plain @click="$router.push('/app/me')">返回个人主页</el-button>
        <el-button v-if="ownedShop?.id" plain @click="$router.push(`/app/shops/${ownedShop.id}`)">
          查看店铺主页
        </el-button>
      </div>
    </section>

    <el-alert
      v-if="!ownedShop?.id"
      type="info"
      show-icon
      :closable="false"
      title="未检测到明确店主状态"
      description="管理你已发布的所有商品，调整状态或继续发布新商品。"
    />

    <section class="shell-card panel-block">
      <div class="filter-row">
        <el-input v-model="filters.keyword" clearable placeholder="搜索我的商品" />
        <el-select v-model="filters.reviewStatus" clearable placeholder="审核状态">
          <el-option label="待审核" value="pending_review" />
          <el-option label="已通过" value="approved" />
          <el-option label="无需审核" value="not_required" />
        </el-select>
      </div>
    </section>

    <ErrorBlock v-if="loadError" @retry="loadProducts" />

    <EmptyState
      v-else-if="!loading && !rows.length"
      title="暂无商品记录"
      description="你还没有发布或暂存商品，可以先创建一件商品。"
    >
      <el-button type="primary" @click="$router.push('/app/shop/manage/publish')">继续发布</el-button>
    </EmptyState>

    <section v-else class="my-publish-list" v-loading="loading">
      <article v-for="item in rows" :key="item.id" class="shell-card publish-card">
        <img :src="item.cover" :alt="item.title" class="publish-card__cover" />
        <div class="publish-card__content">
          <div class="product-card__meta">
            <el-tag size="small">{{ item.categoryName }}</el-tag>
            <el-tag size="small" type="warning">{{ statusLabel(item) }}</el-tag>
            <el-tag size="small" type="success">{{ reviewStatusLabel(item.reviewStatus) }}</el-tag>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.subtitle }}</p>
          <div class="detail-inline-metrics">
            <span>¥{{ item.price }}</span>
            <span>{{ item.deliveryMethods.join(' / ') }}</span>
            <span>{{ item.publishedAt }}</span>
          </div>
        </div>
        <div class="publish-card__actions">
          <el-button plain @click="$router.push(`/app/products/${item.id}`)">查看详情</el-button>
          <el-button plain @click="$router.push('/app/shop/manage/publish')">继续编辑</el-button>
        </div>
      </article>
    </section>
  </div>
</template>
