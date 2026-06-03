<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useRouter } from 'vue-router'
import FormPageShell from '@/components/shell/FormPageShell.vue'
import { useMarketStore } from '@/stores/market'

const router = useRouter()
const marketStore = useMarketStore()
const submitting = ref(false)

const ownedShop = computed(() => marketStore.ownedShop.shop)

const form = reactive({
  title: '',
  subtitle: '',
  categoryId: '',
  type: 'physical',
  price: '',
  originalPrice: '',
  stock: 1,
  scenarioTags: '',
  cover: '',
  deliveryMethods: [],
  allowPreview: false,
  previewLabel: '',
  previewHint: '',
  description: ''
})

async function handleSave(submitMode) {
  if (!form.title || !form.categoryId || !form.type || !form.price) {
    ElMessage.warning('请先补全标题、分类、类型和价格')
    return
  }

  if (!form.deliveryMethods.length) {
    ElMessage.warning('至少选择一种交付方式')
    return
  }

  submitting.value = true
  try {
    await marketStore.publishProduct({
      ...form,
      productType: form.type,
      salePrice: form.price,
      coverUrl: form.cover,
      previewRuleText: form.previewHint,
      media: form.cover ? [form.cover] : [],
      submitMode
    })
    ElMessage.success(submitMode === 'submit' ? '发布申请已提交' : '商品信息已暂存')
    await marketStore.loadMyProducts()
    await marketStore.loadMyShop().catch(() => {})
    router.push('/app/shop/manage/products')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  marketStore.loadProducts().catch(() => {})
  marketStore.loadMyShop().catch(() => {})
})
</script>

<template>
  <div class="shell-container">
    <FormPageShell title="发布商品" description="发布能力属于前台个人域；店主的对外展示主体仍然是店铺主页。">
      <template #summary>
        <div class="shell-inline-actions">
          <el-tag type="info">
            {{
              ownedShop?.id
                ? `当前店铺：${ownedShop.name}`
                : '当前账号暂未绑定店铺，提交后会按平台发布规则处理'
            }}
          </el-tag>
          <el-button plain @click="$router.push('/app/shop/manage/products')">返回商品管理</el-button>
        </div>
      </template>

      <el-alert
        v-if="!ownedShop?.id"
        type="warning"
        show-icon
        :closable="false"
        title="当前账号暂未绑定店铺"
        description="你仍可填写商品信息并提交发布，平台会按账号权限进行处理。"
      />

      <el-form :model="form" label-position="top" class="grid-form">
        <el-form-item label="商品标题">
          <el-input v-model="form.title" placeholder="例如：高数复习资料包" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="一句话说明亮点" />
        </el-form-item>

        <el-form-item label="商品分类">
          <el-select v-model="form.categoryId" placeholder="选择分类">
            <el-option
              v-for="category in marketStore.categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商品类型">
          <el-select v-model="form.type" placeholder="选择类型">
            <el-option label="实物商品" value="physical" />
            <el-option label="电子资料" value="digital" />
            <el-option label="服务型商品" value="service" />
          </el-select>
        </el-form-item>

        <el-form-item label="价格">
          <el-input v-model="form.price" placeholder="例如 19.9" />
        </el-form-item>
        <el-form-item label="参考原价">
          <el-input v-model="form.originalPrice" placeholder="可选" />
        </el-form-item>

        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="1" />
        </el-form-item>
        <el-form-item label="场景标签">
          <el-input v-model="form.scenarioTags" placeholder="用空格分隔，例如 期末复习 宿舍转卖" />
        </el-form-item>

        <el-form-item label="封面图地址" class="grid-form__full">
          <el-input v-model="form.cover" placeholder="可留空，未填写时使用默认商品封面" />
        </el-form-item>

        <el-form-item label="交付方式" class="grid-form__full">
          <el-checkbox-group v-model="form.deliveryMethods">
            <el-checkbox label="logistics">物流寄送</el-checkbox>
            <el-checkbox label="offline_face_to_face">线下面交</el-checkbox>
            <el-checkbox label="digital_delivery">电子交付</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="资料预览入口" class="grid-form__full">
          <el-switch v-model="form.allowPreview" />
        </el-form-item>
        <el-form-item label="预览按钮文案">
          <el-input v-model="form.previewLabel" placeholder="例如：预览前 10 页" />
        </el-form-item>
        <el-form-item label="预览说明">
          <el-input
            v-model="form.previewHint"
            placeholder="例如：购买后可下载完整资料包；当前页面仅开放部分内容预览"
          />
        </el-form-item>

        <el-form-item label="商品描述" class="grid-form__full">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            placeholder="描述商品内容、适用场景和审核注意事项"
          />
        </el-form-item>

        <el-form-item class="grid-form__full">
          <div class="shell-inline-actions">
            <el-button :loading="submitting" @click="handleSave('draft')">暂存商品信息</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSave('submit')">提交发布</el-button>
          </div>
        </el-form-item>
      </el-form>
    </FormPageShell>
  </div>
</template>
