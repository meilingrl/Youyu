<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import FormPageShell from '@/components/shell/FormPageShell.vue'
import { submitVerificationApplication } from '@/api/modules/user'
import { useAuthStore } from '@/stores/auth'
import { useMarketStore } from '@/stores/market'

const authStore = useAuthStore()
const marketStore = useMarketStore()
const form = reactive(marketStore.getVerificationDraft())
const submitting = ref(false)

function syncFormFromProfile() {
  Object.assign(form, marketStore.getVerificationDraft())
}

async function handleSubmitToApi() {
  if (!form.realName || !form.studentNo || !form.college || !form.major || !form.grade) {
    ElMessage.warning('请补全认证申请信息')
    return
  }

  submitting.value = true
  try {
    await submitVerificationApplication(form)
    await marketStore.loadProfile(authStore.currentUser)
    syncFormFromProfile()
    ElMessage.success('认证申请已提交')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '认证申请接口暂不可用')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  marketStore
    .loadProfile(authStore.currentUser)
    .then(syncFormFromProfile)
    .catch(() => {})
})
</script>

<template>
  <div class="shell-container">
    <FormPageShell title="学生认证申请" description="聚焦前台认证申请与状态展示，不扩展后台审核流程。">
      <template #summary>
        <el-tag type="success">当前状态：{{ marketStore.profile.verification.label }}</el-tag>
      </template>

      <div class="info-banner">
        <strong>认证影响</strong>
        <span>认证通过后，个人主页会显示更完整的校园身份信息，也能稳定开启发布和开店相关入口。</span>
      </div>

      <el-form :model="form" label-position="top" class="grid-form">
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="form.studentNo" placeholder="一学号一账号" />
        </el-form-item>

        <el-form-item label="学院">
          <el-input v-model="form.college" placeholder="例如：计算机学院" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.major" placeholder="例如：软件工程" />
        </el-form-item>

        <el-form-item label="年级">
          <el-input v-model="form.grade" placeholder="例如：2023 级" />
        </el-form-item>
        <el-form-item label="校园邮箱">
          <el-input v-model="form.campusEmail" placeholder="例如：xxx@mail.neu.edu.cn" />
        </el-form-item>

        <el-form-item label="认证方式">
          <el-radio-group v-model="form.verifyMethod">
            <el-radio value="campus_email">校园邮箱验证</el-radio>
            <el-radio value="manual_review">人工审核材料</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="补充说明" class="grid-form__full">
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="4"
            placeholder="可说明校区、学院缩写或特殊情况"
          />
        </el-form-item>

        <el-form-item class="grid-form__full">
          <div class="shell-inline-actions">
            <el-button plain :disabled="submitting" @click="$router.push('/app/me')">返回个人主页</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmitToApi">提交申请</el-button>
          </div>
        </el-form-item>
      </el-form>
    </FormPageShell>
  </div>
</template>
