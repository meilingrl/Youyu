import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getCaptcha as getCaptchaApi,
  login as loginApi,
  register as registerApi,
  resetPassword as resetPasswordApi,
  sendEmailCode as sendEmailCodeApi
} from '@/api/modules/auth'
import { clearAuthStorage, getAuthStorage, setAuthStorage } from '@/utils/auth'
import { isAdminRole } from '@/utils/admin-permissions'

function captchaIsRequired(error) {
  const payload = error?.response?.data
  const code = String(payload?.code || '').toUpperCase()
  const message = String(payload?.message || '')

  return payload?.captchaRequired === true
    || payload?.data?.captchaRequired === true
    || code === 'CAPTCHA_REQUIRED'
    || /captcha.*required/i.test(message)
    || /需要.*验证码|验证码.*必填/.test(message)
}

function normalizeSession(payload) {
  if (!payload) {
    return null
  }

  return {
    ...payload,
    role: String(payload.role || 'guest').toLowerCase()
  }
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref(normalizeSession(getAuthStorage()))
  const captcha = ref(null)
  const captchaRequired = ref(false)
  const captchaLoading = ref(false)
  const captchaError = ref('')

  const isLoggedIn = computed(() => Boolean(session.value?.token))
  const currentRole = computed(() => session.value?.role || 'guest')
  const currentUser = computed(() => session.value?.user || null)
  const isAdmin = computed(() => isAdminRole(currentRole.value))

  /**
   * 设置当前会话状态，同时持久化到 localStorage。
   * 传入 null 可清除会话（效果等同于 logout），传入有效对象则写入。
   *
   * @param {object|null} payload - 会话数据（包含 token、user、role 等字段），或 null 清除会话
   * @sideEffects 更新 session 响应式 ref，写入 localStorage
   */
  function setSession(payload) {
    const normalized = normalizeSession(payload)
    session.value = normalized
    setAuthStorage(normalized)
  }

  function clearCaptcha() {
    captcha.value = null
    captchaRequired.value = false
    captchaError.value = ''
  }

  async function refreshCaptcha() {
    if (!captchaRequired.value) {
      return null
    }

    captchaLoading.value = true
    captchaError.value = ''

    try {
      const response = await getCaptchaApi()
      captcha.value = response.data
      return response.data
    } catch (error) {
      captcha.value = null
      captchaError.value = error?.response?.data?.message || error?.message || '验证码加载失败'
      throw error
    } finally {
      captchaLoading.value = false
    }
  }

  /**
   * 统一登录入口：发送凭证到服务端，根据返回的角色信息写入会话。
   * 服务端根据账号类型（普通用户/管理员）返回对应 role，客户端统一写入会话。
   *
   * @param {object} credentials - 登录凭证，包含 loginId 和 password
   * @returns {Promise<object>} 登录成功后的会话数据
   * @sideEffects 写入 localStorage session，更新 session 响应式 ref
   */
  async function login(credentials) {
    try {
      const payload = { ...credentials }

      if (captcha.value?.challengeId) {
        payload.captchaChallengeId = captcha.value.challengeId
      }

      const response = await loginApi(payload)
      setSession(response.data)
      clearCaptcha()
      return response.data
    } catch (error) {
      if (captchaRequired.value || captchaIsRequired(error)) {
        captchaRequired.value = true

        try {
          await refreshCaptcha()
        } catch {
          // Keep the original login failure visible while exposing the CAPTCHA load error.
        }
      }

      throw error
    }
  }

  /**
   * 注册新用户账号。注册成功后不自动设置会话——用户需跳转至登录页完成登录。
   *
   * @param {object} [payload={}] - 注册信息（用户名、密码等字段）
   * @returns {Promise<object>} 注册结果数据
   * @sideEffects 无（不设置 session，不写入 localStorage）
   */
  async function registerAsUser(payload = {}) {
    return registerApi(payload)
  }

  async function sendEmailCode(email, purpose) {
    const response = await sendEmailCodeApi({ email, purpose })
    return response.data
  }

  async function resetPassword(payload = {}) {
    return resetPasswordApi(payload)
  }

  /**
   * 退出登录：清空会话状态并移除 localStorage 中的认证数据。
   *
   * @sideEffects 将 session ref 设为 null，调用 clearAuthStorage 清除 localStorage
   */
  function logout() {
    session.value = null
    clearAuthStorage()
    clearCaptcha()
  }

  function updateCurrentUser(payload = {}) {
    if (!session.value?.user) {
      return null
    }
    const nextSession = {
      ...session.value,
      user: {
        ...session.value.user,
        ...payload
      }
    }
    setSession(nextSession)
    return nextSession.user
  }

  return {
    session,
    captcha,
    captchaRequired,
    captchaLoading,
    captchaError,
    isLoggedIn,
    isAdmin,
    currentRole,
    currentUser,
    setSession,
    clearCaptcha,
    refreshCaptcha,
    updateCurrentUser,
    login,
    registerAsUser,
    sendEmailCode,
    resetPassword,
    logout
  }
})
