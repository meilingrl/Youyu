<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const submitting = ref(false)
const codeSending = ref(false)
const codeCooldown = ref(0)
let codeTimer = null

const form = reactive({
  email: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: ''
})

function resolveErrorMessage(error) {
  return error?.response?.data?.message || error?.message || '请求失败'
}

onBeforeUnmount(() => {
  window.clearInterval(codeTimer)
})

function startCodeCooldown(seconds) {
  window.clearInterval(codeTimer)
  codeCooldown.value = Math.max(1, Number(seconds) || 60)
  codeTimer = window.setInterval(() => {
    codeCooldown.value -= 1

    if (codeCooldown.value <= 0) {
      window.clearInterval(codeTimer)
    }
  }, 1000)
}

async function handleSendCode() {
  if (!form.email) {
    ElMessage.warning('请先输入注册邮箱')
    return
  }

  codeSending.value = true

  try {
    const result = await authStore.sendEmailCode(form.email, 'reset_password')
    startCodeCooldown(result.cooldownSeconds)
    ElMessage.success('验证码已发送，请检查邮箱')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    codeSending.value = false
  }
}

async function handleResetPassword() {
  if (!form.email || !form.emailCode || !form.newPassword) {
    ElMessage.warning('请先补全重置密码信息')
    return
  }

  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  submitting.value = true

  try {
    await authStore.resetPassword(form)
    ElMessage.success('密码已重置，请使用新密码登录')
    router.replace({ name: 'login' })
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-page__panel shell-card auth-panel">
      <div class="login-page__intro">
        <span class="eyebrow">Youyu</span>
        <h1>找回密码</h1>
        <p>使用注册邮箱接收验证码并设置新密码。重置成功后，请返回登录页重新登录。</p>
      </div>

      <el-form label-position="top" @submit.prevent="handleResetPassword">
        <el-form-item label="注册邮箱">
          <el-input v-model="form.email" placeholder="输入注册时使用的邮箱" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="auth-code-row">
            <el-input v-model="form.emailCode" placeholder="输入邮箱中的验证码" />
            <el-button
              plain
              :disabled="codeCooldown > 0"
              :loading="codeSending"
              @click="handleSendCode"
            >
              {{ codeCooldown > 0 ? `${codeCooldown} 秒后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="输入新密码"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入新密码"
          />
        </el-form-item>
        <div class="login-page__actions">
          <el-button plain @click="$router.push('/login')">返回登录</el-button>
          <el-button type="primary" :loading="submitting" @click="handleResetPassword">
            重置密码
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.auth-code-row {
  width: 100%;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.auth-code-row .el-input {
  flex: 1 1 220px;
}
</style>
