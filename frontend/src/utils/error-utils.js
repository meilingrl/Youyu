/**
 * 从 API 错误对象或异常中提取中文错误消息，提供多层回退策略。
 *
 * @param {*} error - API 错误对象或 Error 异常
 * @returns {string} 中文错误消息
 */
export function resolveErrorMessage(error) {
  return error?.response?.data?.message || error?.message || '请求失败'
}
