<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const submitting = ref(false)
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
  password: ''
})

const registerForm = reactive({
  nickname: '',
  account: '',
  password: '',
  confirmPassword: ''
})

function resolveErrorMessage(error) {
  return error?.response?.data?.message || error?.message || '请求失败'
}

function defaultPathAfterLogin() {
  return authStore.currentRole === 'admin' ? '/admin/dashboard' : '/app/home'
}

function resolvedPathAfterLogin() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''

  if (authStore.currentRole === 'admin') {
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

async function handleLogin() {
  if (!loginForm.account || !loginForm.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  submitting.value = true

  try {
    await authStore.login({
      loginId: loginForm.account,
      password: loginForm.password
    })
    const isAdmin = authStore.currentRole === 'admin'
    ElMessage.success(isAdmin ? '登录成功，已进入管理后台' : '登录成功')
    router.replace(resolvedPathAfterLogin())
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  if (!registerForm.nickname || !registerForm.account || !registerForm.password) {
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
          使用同一入口登录：系统根据账号类型自动进入前台或管理后台。管理员与普通用户使用各自账号与密码；演示用户可使用学号/用户名/邮箱登录。
        </p>
      </div>

      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form label-position="top" @submit.prevent="handleLogin">
            <el-form-item label="账号">
              <el-input
                v-model="loginForm.account"
                placeholder="学号 / 邮箱 / 用户名，或管理员登录 ID"
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
            <div class="login-page__actions">
              <el-button plain @click="$router.push('/app/home')">先去逛逛</el-button>
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
              <el-button type="primary" @click="handleRegister">注册并进入</el-button>
            </div>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>
