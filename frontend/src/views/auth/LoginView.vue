<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const submitting = ref(false)
const registerCodeSending = ref(false)
const registerCodeCooldown = ref(0)
let registerCodeTimer = null
const tab = computed({
  get: () => {
    if (route.name === 'register' || route.query.mode === 'register') {
      return 'register'
    }
    return 'login'
  },
  set: (value) => {
    router.replace({
      name: value === 'register' ? 'register' : 'login',
      query: {
        ...route.query,
        mode: value
      }
    })
  }
})

const loginForm = reactive({
  account: '',
  password: '',
  captchaCode: ''
})

const registerForm = reactive({
  nickname: '',
  account: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: ''
})

function resolveErrorMessage(error) {
  return error?.response?.data?.message || error?.message || '请求失败'
}

function defaultPathAfterLogin() {
  return authStore.isAdmin ? '/admin/dashboard' : '/app/home'
}

function resolvedPathAfterLogin() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''

  if (authStore.isAdmin) {
    return redirect.startsWith('/admin/') ? redirect : '/admin/dashboard'
  }

  return redirect && !redirect.startsWith('/admin') ? redirect : defaultPathAfterLogin()
}

watch(
  () => authStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      router.replace(resolvedPathAfterLogin())
    }
  },
  { immediate: true }
)

watch(
  () => loginForm.account,
  (account, previousAccount) => {
    if (previousAccount && account !== previousAccount && authStore.captchaRequired) {
      loginForm.captchaCode = ''
      authStore.clearCaptcha()
    }
  }
)

onBeforeUnmount(() => {
  window.clearInterval(registerCodeTimer)
})

function startRegisterCodeCooldown(seconds) {
  window.clearInterval(registerCodeTimer)
  registerCodeCooldown.value = Math.max(1, Number(seconds) || 60)
  registerCodeTimer = window.setInterval(() => {
    registerCodeCooldown.value -= 1

    if (registerCodeCooldown.value <= 0) {
      window.clearInterval(registerCodeTimer)
    }
  }, 1000)
}

async function handleSendRegisterCode() {
  if (!registerForm.email) {
    ElMessage.warning('请先输入接收验证码的邮箱')
    return
  }

  registerCodeSending.value = true

  try {
    const result = await authStore.sendEmailCode(registerForm.email, 'register')
    startRegisterCodeCooldown(result.cooldownSeconds)
    ElMessage.success('验证码已发送，请检查邮箱')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    registerCodeSending.value = false
  }
}

async function handleRefreshCaptcha() {
  try {
    await authStore.refreshCaptcha()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  }
}

async function handleLogin() {
  if (!loginForm.account || !loginForm.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  if (authStore.captchaRequired && !loginForm.captchaCode) {
    ElMessage.warning('请输入图形验证码')
    return
  }

  submitting.value = true

  try {
    await authStore.login({
      loginId: loginForm.account,
      password: loginForm.password,
      captchaCode: loginForm.captchaCode
    })
    ElMessage.success(authStore.isAdmin ? '登录成功，已进入管理后台' : '登录成功')
    router.replace(resolvedPathAfterLogin())
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  if (
    !registerForm.nickname
    || !registerForm.account
    || !registerForm.email
    || !registerForm.emailCode
    || !registerForm.password
  ) {
    ElMessage.warning('请先补全注册信息')
    return
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  submitting.value = true
  try {
    await authStore.registerAsUser(registerForm)
    ElMessage.success('注册申请已提交，请使用账号登录')
    tab.value = 'login'
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
        <h1>统一登录</h1>
        <p>
          使用同一入口登录：系统根据账号类型自动进入前台或管理后台。管理员与普通用户使用各自账号与密码；校园用户可使用学号、用户名或邮箱登录。
        </p>
      </div>

      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form label-position="top" @submit.prevent="handleLogin">
            <el-form-item label="账号">
              <el-input
                v-model="loginForm.account"
                placeholder="学号 / 邮箱 / 用户名，或管理员账号"
              />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                placeholder="输入密码"
              />
            </el-form-item>
            <el-form-item v-if="authStore.captchaRequired" label="图形验证码">
              <div class="auth-code-row">
                <el-input
                  v-model="loginForm.captchaCode"
                  placeholder="输入图片中的字符"
                />
                <img
                  v-if="authStore.captcha?.imageDataUrl"
                  class="auth-captcha-image"
                  :src="authStore.captcha.imageDataUrl"
                  alt="图形验证码"
                  @click="handleRefreshCaptcha"
                />
                <el-button
                  plain
                  :loading="authStore.captchaLoading"
                  @click="handleRefreshCaptcha"
                >
                  刷新验证码
                </el-button>
              </div>
              <p v-if="authStore.captchaError" class="auth-field-feedback">
                {{ authStore.captchaError }}
              </p>
            </el-form-item>
            <div class="login-page__actions">
              <el-button plain @click="$router.push('/app/home')">先去逛逛</el-button>
              <el-button plain @click="$router.push('/forgot-password')">忘记密码</el-button>
              <el-button type="primary" :loading="submitting" @click="handleLogin">登录</el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form label-position="top" @submit.prevent="handleRegister">
            <el-form-item label="昵称">
              <el-input v-model="registerForm.nickname" placeholder="用于前台展示" />
            </el-form-item>
            <el-form-item label="账号">
              <el-input v-model="registerForm.account" placeholder="建议使用学号或校园邮箱" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="registerForm.email" placeholder="用于接收注册验证码" />
            </el-form-item>
            <el-form-item label="邮箱验证码">
              <div class="auth-code-row">
                <el-input v-model="registerForm.emailCode" placeholder="输入邮箱中的验证码" />
                <el-button
                  plain
                  :disabled="registerCodeCooldown > 0"
                  :loading="registerCodeSending"
                  @click="handleSendRegisterCode"
                >
                  {{ registerCodeCooldown > 0 ? `${registerCodeCooldown} 秒后重发` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="registerForm.password"
                type="password"
                show-password
                placeholder="至少 6 位"
              />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入密码"
              />
            </el-form-item>
            <div class="login-page__actions">
              <el-button plain @click="tab = 'login'">已有账号，去登录</el-button>
              <el-button type="primary" :loading="submitting" @click="handleRegister">注册</el-button>
            </div>
          </el-form>
        </el-tab-pane>
      </el-tabs>
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

.auth-captcha-image {
  width: 132px;
  height: 44px;
  border: 1px solid var(--cm-border);
  border-radius: 12px;
  object-fit: cover;
  cursor: pointer;
}

.auth-field-feedback {
  width: 100%;
  color: #b93d2f;
  font-size: 13px;
}
</style>
