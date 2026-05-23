import { getStorage, removeStorage, setStorage } from './storage'

const AUTH_STORAGE_KEY = 'youyu-auth'

/**
 * 从 localStorage 读取认证会话数据。
 *
 * @returns {object|null} 解析后的认证数据对象，无数据时返回 null
 */
export function getAuthStorage() {
  return getStorage(AUTH_STORAGE_KEY, null)
}

/**
 * 将认证会话数据写入 localStorage。
 *
 * @param {object|null} payload - 认证数据对象，传 null 时等价于清除
 */
export function setAuthStorage(payload) {
  setStorage(AUTH_STORAGE_KEY, payload)
}

/**
 * 从 localStorage 移除认证会话数据。
 */
export function clearAuthStorage() {
  removeStorage(AUTH_STORAGE_KEY)
}

/**
 * 从当前认证会话中提取 JWT token 字符串。
 *
 * @returns {string} JWT token，无会话时返回空字符串
 */
export function getAuthToken() {
  return getAuthStorage()?.token || ''
}
