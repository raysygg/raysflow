import { API_ERROR_CATEGORY, normalizeApiError } from './errors.js'

/** 将统一 API 错误转换为页面状态展示参数。 */
export function pageStateFromError(error, fallbackTitle = '页面暂时不可用') {
  const normalized = normalizeApiError(error)
  if (normalized.category === API_ERROR_CATEGORY.PERMISSION) {
    return {
      type: 'forbidden',
      title: '当前账号权限不足',
      message: normalized.message,
      actionLabel: '',
      details: normalized.details,
    }
  }
  if (normalized.category === API_ERROR_CATEGORY.TENANT) {
    return {
      type: 'error',
      title: '租户上下文已失效',
      message: '请重新登录并选择有效的企业空间。',
      actionLabel: '',
      details: [],
    }
  }
  return {
    type: 'error',
    title: fallbackTitle,
    message: normalized.message,
    actionLabel: normalized.retryable ? '重新加载' : '',
    details: normalized.details,
  }
}
