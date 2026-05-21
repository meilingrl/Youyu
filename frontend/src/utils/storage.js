/**
 * 从 localStorage 读取并解析 JSON 值，失败或不存在时返回 fallback。
 *
 * @param {string} key - localStorage 键名
 * @param {*} [fallback=null] - 读取失败或键不存在时的回退值
 * @returns {*} 解析后的 JSON 值或 fallback
 */
export function getStorage(key, fallback = null) {
  const raw = window.localStorage.getItem(key)

  if (!raw) {
    return fallback
  }

  try {
    return JSON.parse(raw)
  } catch {
    return fallback
  }
}

/**
 * 将值 JSON 序列化后写入 localStorage。
 *
 * @param {string} key - localStorage 键名
 * @param {*} value - 要存储的值（将自动 JSON 序列化）
 */
export function setStorage(key, value) {
  window.localStorage.setItem(key, JSON.stringify(value))
}

/**
 * 从 localStorage 移除指定 key。
 *
 * @param {string} key - 要移除的 localStorage 键名
 */
export function removeStorage(key) {
  window.localStorage.removeItem(key)
}
