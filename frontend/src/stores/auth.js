import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi } from '@/api/modules/auth'
import { clearAuthStorage, getAuthStorage, setAuthStorage } from '@/utils/auth'

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

  const isLoggedIn = computed(() => Boolean(session.value?.token))
  const currentRole = computed(() => session.value?.role || 'guest')
  const currentUser = computed(() => session.value?.user || null)

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

  /**
   * 统一登录入口：发送凭证到服务端，根据返回的角色信息写入会话。
   * 服务端根据账号类型（普通用户/管理员）返回对应 role，客户端统一写入会话。
   *
   * @param {object} credentials - 登录凭证，包含 loginId 和 password
   * @returns {Promise<object>} 登录成功后的会话数据
   * @sideEffects 写入 localStorage session，更新 session 响应式 ref
   */
  async function login(credentials) {
    const response = await loginApi(credentials)
    setSession(response.data)
    return response.data
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

  /**
   * 退出登录：清空会话状态并移除 localStorage 中的认证数据。
   *
   * @sideEffects 将 session ref 设为 null，调用 clearAuthStorage 清除 localStorage
   */
  function logout() {
    session.value = null
    clearAuthStorage()
  }

  return {
    session,
    isLoggedIn,
    currentRole,
    currentUser,
    setSession,
    login,
    registerAsUser,
    logout
  }
})
