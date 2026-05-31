<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import PageSection from '@/components/common/PageSection.vue'
import { useMarketStore } from '@/stores/market'

const marketStore = useMarketStore()
const form = reactive({
  receiverName: '',
  receiverPhone: '',
  addressType: 'campus',
  province: '',
  city: '',
  district: '',
  campusArea: '',
  detailAddress: '',
  defaultAddress: true
})

async function load() {
  await marketStore.loadUserAddresses().catch(() => [])
}

async function createAddress() {
  try {
    await marketStore.createAddress(form)
    Object.assign(form, {
      receiverName: '',
      receiverPhone: '',
      addressType: 'campus',
      province: '',
      city: '',
      district: '',
      campusArea: '',
      detailAddress: '',
      defaultAddress: false
    })
    ElMessage.success('地址已保存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '地址保存失败')
  }
}

async function setDefault(addressId) {
  try {
    await marketStore.setDefaultAddress(addressId)
    ElMessage.success('默认地址已更新')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '默认地址更新失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card address-hero">
      <div>
        <span class="eyebrow">收货信息</span>
        <h1>地址管理</h1>
        <p>管理常用收货地址，结算时会优先使用默认地址。</p>
      </div>
      <el-button plain @click="$router.push('/app/settings')">返回设置中心</el-button>
    </section>

    <PageSection title="已保存地址" description="默认地址会排在最前，并在结算时自动选中。">
      <div class="address-list">
        <article v-for="address in marketStore.profile.addresses" :key="address.id" class="address-item">
          <div>
            <strong>{{ address.contactName }} / {{ address.phone }}</strong>
            <p>{{ address.region }} {{ address.detail }}</p>
            <span>{{ address.campusArea || address.type }}</span>
          </div>
          <div class="address-item__actions">
            <el-tag v-if="address.isDefault" type="success" effect="plain">默认地址</el-tag>
            <el-button v-else plain @click="setDefault(address.id)">设为默认</el-button>
          </div>
        </article>
      </div>
    </PageSection>

    <PageSection title="新增地址" description="支持校内自提地址和普通物流地址。">
      <el-form label-position="top" class="address-form">
        <el-form-item label="收货人">
          <el-input v-model="form.receiverName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.receiverPhone" />
        </el-form-item>
        <el-form-item label="地址类型">
          <el-select v-model="form.addressType">
            <el-option label="校内地址" value="campus" />
            <el-option label="物流地址" value="logistics" />
          </el-select>
        </el-form-item>
        <el-form-item label="省份">
          <el-input v-model="form.province" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="form.city" />
        </el-form-item>
        <el-form-item label="区县">
          <el-input v-model="form.district" />
        </el-form-item>
        <el-form-item label="校区/宿舍区">
          <el-input v-model="form.campusArea" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.detailAddress" />
        </el-form-item>
        <el-form-item label="设为默认地址">
          <el-switch v-model="form.defaultAddress" />
        </el-form-item>
      </el-form>
      <template #actions>
        <el-button type="primary" @click="createAddress">保存地址</el-button>
      </template>
    </PageSection>
  </div>
</template>

<style scoped>
.address-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
}

.address-list,
.address-form {
  display: grid;
  gap: 14px;
}

.address-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
}

.address-item p,
.address-item span {
  margin: 6px 0 0;
  color: var(--cm-text-secondary);
}

.address-item__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.address-form {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 768px) {
  .address-hero,
  .address-item {
    flex-direction: column;
    align-items: stretch;
  }

  .address-form {
    grid-template-columns: 1fr;
  }
}
</style>
