<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import { getAdminShopDetail, getAdminShops, updateAdminShopStatus } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

const loading = ref(false)
const error = ref('')
const detailLoading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detail = ref({ shop: null, products: [] })
const filters = reactive({
  keyword: '',
  status: '',
  reviewStatus: ''
})

async function loadShops() {
  loading.value = true
  error.value = ''

  try {
    const response = await getAdminShops({ ...filters, page: page.value, pageSize: pageSize.value })
    rows.value = response.data.items || []
    total.value = response.data.total || 0
  } catch (err) {
    error.value = resolveErrorMessage(err)
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  loadShops()
}

function onPageSizeChange(ps) {
  pageSize.value = ps
  page.value = 1
  loadShops()
}

function onSearch() {
  page.value = 1
  loadShops()
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true

  try {
    const response = await getAdminShopDetail(row.id)
    detail.value = response.data
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    detailLoading.value = false
  }
}

async function updateStatus(row, payload, confirmText) {
  try {
    await ElMessageBox.confirm(confirmText, '店铺状态变更', { type: 'warning' })
    await updateAdminShopStatus(row.id, payload)
    ElMessage.success('店铺状态已更新')
    await loadShops()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

async function rejectShop(row) {
  try {
    const result = await ElMessageBox.prompt('请填写店铺驳回原因', '店铺审核驳回', {
      confirmButtonText: '提交驳回',
      cancelButtonText: '取消'
    })
    await updateAdminShopStatus(row.id, {
      status: 'inactive',
      reviewStatus: 'rejected',
      rejectReason: result.value
    })
    ElMessage.success('店铺已驳回')
    await loadShops()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

onMounted(loadShops)
</script>

<template>
  <ListPageShell
    title="店铺管理"
    description="管理店铺列表与详情，支持审核与启停等操作。"
    :rows="rows"
    :loading="loading"
    :error="error"
    empty-title="暂无店铺记录"
    empty-description="当前没有符合条件的店铺。"
    @retry="loadShops"
  >
    <template #filters>
      <div class="filter-row">
        <el-input v-model="filters.keyword" placeholder="搜索店铺名 / 店主 / 简介" clearable @keyup.enter="onSearch" />
        <el-select v-model="filters.status" placeholder="店铺状态" clearable>
          <el-option label="营业中" value="active" />
          <el-option label="待开通" value="inactive" />
          <el-option label="已停用" value="disabled" />
        </el-select>
        <el-select v-model="filters.reviewStatus" placeholder="审核状态" clearable>
          <el-option label="待审核" value="pending_review" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </div>
    </template>

    <template #table>
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="name" label="店铺名称" min-width="180" />
        <el-table-column prop="ownerName" label="店主" min-width="100" />
        <el-table-column prop="status" label="店铺状态" min-width="100" />
        <el-table-column prop="reviewStatus" label="审核状态" min-width="120" />
        <el-table-column prop="capabilityLevel" label="能力等级" min-width="120" />
        <el-table-column prop="rejectReason" label="驳回原因" min-width="220" />
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.reviewStatus === 'pending_review'"
              link
              type="success"
              @click="updateStatus(row, { reviewStatus: 'approved', status: 'active' }, `确认开通店铺 ${row.name} 吗？`)"
            >
              通过开通
            </el-button>
            <el-button
              v-if="row.reviewStatus === 'pending_review'"
              link
              type="danger"
              @click="rejectShop(row)"
            >
              驳回
            </el-button>
            <el-button
              v-if="row.status === 'active'"
              link
              type="warning"
              @click="updateStatus(row, { status: 'disabled' }, `确认停用店铺 ${row.name} 吗？`)"
            >
              停用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="onPageChange"
          @size-change="onPageSizeChange"
        />
      </div>
    </template>
  </ListPageShell>

  <el-drawer v-model="detailVisible" size="42%" title="店铺详情">
    <div v-loading="detailLoading" class="page-stack">
      <div v-if="detail.shop" class="shell-card detail-grid">
        <span>店铺名称：{{ detail.shop.name }}</span>
        <span>店主：{{ detail.shop.ownerName }}</span>
        <span>店铺状态：{{ detail.shop.status }}</span>
        <span>审核状态：{{ detail.shop.reviewStatus }}</span>
        <span>能力等级：{{ detail.shop.capabilityLevel }}</span>
        <span>店铺评分：{{ detail.shop.ratingScore }}</span>
        <span>驳回原因：{{ detail.shop.rejectReason || '无' }}</span>
        <span>简介：{{ detail.shop.description }}</span>
      </div>

      <div class="shell-card">
        <div class="section-heading"><h3>店铺商品</h3></div>
        <el-table :data="detail.products || []">
          <el-table-column prop="title" label="商品标题" min-width="200" />
          <el-table-column prop="status" label="状态" min-width="100" />
          <el-table-column prop="reviewStatus" label="审核状态" min-width="120" />
        </el-table>
      </div>
    </div>
  </el-drawer>
</template>
