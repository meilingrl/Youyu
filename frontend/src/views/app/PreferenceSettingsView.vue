<script setup>
import { reactive, onMounted } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import PageSection from '@/components/common/PageSection.vue'
import { preferenceOptions } from '@/constants/insightMetrics'
import { useMarketStore } from '@/stores/market'

const marketStore = useMarketStore()

const form = reactive({
  ...marketStore.userPreference,
  notificationPreference: {
    ...marketStore.userPreference.notificationPreference
  }
})

function syncForm(preference) {
  Object.assign(form, {
    ...preference,
    notificationPreference: {
      ...preference.notificationPreference
    }
  })
}

async function loadPreference() {
  try {
    const [preference] = await Promise.all([
      marketStore.loadUserPreference(),
      marketStore.loadUserAddresses().catch(() => [])
    ])
    syncForm(preference)
  } catch {
    ElMessage.warning('偏好设置暂时无法加载，请稍后重试')
  }
}

async function savePreference() {
  try {
    const preference = await marketStore.updateUserPreference({
      defaultSortType: form.defaultSortType,
      defaultAddressId: form.defaultAddressId,
      defaultFulfillmentType: form.defaultFulfillmentType,
      defaultPaymentMethod: form.defaultPaymentMethod,
      notificationPreference: form.notificationPreference
    })
    syncForm(preference)
    ElMessage.success('偏好设置已通过接口保存')
  } catch {
    ElMessage.error('偏好设置保存失败，请稍后重试')
  }
}

onMounted(loadPreference)
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card preference-hero">
      <div>
        <span class="eyebrow">偏好设置</span>
        <h1>偏好设置</h1>
        <p>
          设置商品排序、收货地址、交付方式、支付方式和通知提醒。
        </p>
      </div>
      <div class="preference-hero__actions">
        <el-tag :type="marketStore.preferenceSource === 'api' ? 'success' : 'warning'" effect="plain">
          {{ marketStore.preferenceSource === 'api' ? '已同步' : '待同步' }}
        </el-tag>
        <el-button plain @click="$router.push('/app/settings')">返回设置中心</el-button>
        <el-button type="primary" :loading="marketStore.savingPreference" @click="savePreference">
          保存设置
        </el-button>
      </div>
    </section>

    <el-alert
      v-if="marketStore.preferenceError"
      type="warning"
      show-icon
      :closable="false"
      :title="marketStore.preferenceError"
      description="你的偏好会在下次使用时自动生效。"
    />

    <PageSection title="浏览偏好" description="选择商品列表默认使用的排序方式。">
      <el-form v-loading="marketStore.loadingPreference" label-position="top" class="grid-form">

        <el-form-item label="商品默认排序">
          <el-select v-model="form.defaultSortType">
            <el-option
              v-for="option in preferenceOptions.defaultSortType"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </PageSection>

    <PageSection title="默认交易设置" description="设定你下单时的默认选择。">
      <el-form label-position="top" class="grid-form">
        <el-form-item label="默认收货地址">
          <el-select v-model="form.defaultAddressId">
            <el-option
              v-for="address in marketStore.profile.addresses"
              :key="address.id"
              :label="`${address.type || '收货地址'} · ${address.detail}`"
              :value="address.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="默认交付方式">
          <el-select v-model="form.defaultFulfillmentType">
            <el-option
              v-for="option in preferenceOptions.defaultFulfillmentType"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="默认支付方式">
          <el-select v-model="form.defaultPaymentMethod">
            <el-option
              v-for="option in preferenceOptions.defaultPaymentMethod"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="!marketStore.profile.addresses.length"
        type="info"
        show-icon
        :closable="false"
        title="当前没有可选地址"
        description="你的默认收货地址会在下单时自动填入。"
      />
    </PageSection>

    <PageSection title="通知提醒" description="控制订单和评价的提醒通知。">
      <div class="notification-panel">
        <el-checkbox v-model="form.notificationPreference.orderReminder">接收订单状态提醒</el-checkbox>
        <el-checkbox v-model="form.notificationPreference.reviewReminder">接收评价与审核提醒</el-checkbox>
      </div>
    </PageSection>
  </div>
</template>

<style scoped>
.preference-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.preference-hero h1 {
  margin: 10px 0 12px;
  font-size: clamp(30px, 3vw, 46px);
  line-height: 1.15;
}

.preference-hero__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.notification-panel {
  display: grid;
  gap: 12px;
}

@media (max-width: 768px) {
  .preference-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .preference-hero__actions {
    justify-content: flex-start;
  }
}
</style>
