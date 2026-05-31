<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import PageSection from '@/components/common/PageSection.vue'
import { useAuthStore } from '@/stores/auth'
import { useMarketStore } from '@/stores/market'

const authStore = useAuthStore()
const marketStore = useMarketStore()
const result = ref(null)
const form = reactive({
  email: ''
})

const profile = computed(() => marketStore.profile)

async function load() {
  await marketStore.loadProfile(authStore.currentUser).catch(() => null)
  form.email = profile.value.email || ''
}

async function submitEmail() {
  try {
    result.value = await marketStore.bindEmail({ email: form.email })
    ElMessage.success('邮箱已提交')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '邮箱提交失败')
  }
}

load()
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card security-hero">
      <div>
        <span class="eyebrow">账号安全</span>
        <h1>邮箱绑定</h1>
        <p>填写常用邮箱，后续可用于账号验证、消息通知和邮箱登录。</p>
      </div>
      <el-button plain @click="$router.push('/app/settings')">返回设置中心</el-button>
    </section>

    <PageSection title="绑定邮箱" description="请填写本人可正常接收邮件的邮箱地址。">
      <el-form label-position="top" class="security-form">
        <el-form-item label="登录账号">
          <el-input :model-value="profile.loginId" disabled />
        </el-form-item>
        <el-form-item label="邮箱地址">
          <el-input v-model="form.email" placeholder="name@example.edu" />
        </el-form-item>
      </el-form>
      <template #actions>
        <el-button type="primary" :loading="marketStore.bindingEmail" @click="submitEmail">
          保存邮箱
        </el-button>
      </template>
    </PageSection>

    <el-alert
      v-if="result"
      type="info"
      show-icon
      :closable="false"
      title="邮箱待验证"
      :description="result.message"
    />
  </div>
</template>

<style scoped>
.security-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
}

.security-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 768px) {
  .security-hero,
  .security-form {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
