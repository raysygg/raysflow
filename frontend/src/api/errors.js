export const API_ERROR_CATEGORY = Object.freeze({
  BUSINESS: 'BUSINESS',
  FIELD: 'FIELD',
  AUTHENTICATION: 'AUTHENTICATION',
  PERMISSION: 'PERMISSION',
  TENANT: 'TENANT',
  DEPENDENCY: 'DEPENDENCY',
  NETWORK: 'NETWORK',
})

const AUTHENTICATION_CODES = new Set(['UNAUTHORIZED', 'AUTHENTICATION_EXPIRED'])
const PERMISSION_CODES = new Set(['FORBIDDEN', 'PERMISSION_DENIED'])
const TENANT_CODES = new Set(['TENANT_CONTEXT_INVALID', 'TENANT_NOT_FOUND', 'TENANT_DISABLED'])
const FIELD_CODES = new Set(['VALIDATION_FAILED', 'FIELD_VALIDATION_FAILED'])
const DEPENDENCY_CODES = [
  'DEPENDENCY_UNAVAILABLE', 'VERSION_UNAVAILABLE', 'REALTIME_UNAVAILABLE',
  'RAG_EMBEDDING_UNAVAILABLE', 'MODEL_UNAVAILABLE', 'CONNECTOR_UNAVAILABLE',
]

const DEFAULT_ERROR_MESSAGE = '请求处理失败，请稍后重试。'

/**
 * 前端统一 API 错误，页面只依赖稳定类别和恢复信息。
 */
export class ApiClientError extends Error {
  constructor(message, options = {}) {
    super(message || DEFAULT_ERROR_MESSAGE, { cause: options.cause })
    this.name = 'ApiClientError'
    this.code = options.code || 'UNKNOWN_ERROR'
    this.category = options.category || API_ERROR_CATEGORY.BUSINESS
    this.fieldPath = options.fieldPath || null
    this.retryable = options.retryable === true
    this.details = Array.isArray(options.details) ? options.details.filter(Boolean) : []
    this.httpStatus = options.httpStatus || null
  }
}

/** 根据服务端错误码和 HTTP 状态确定稳定错误类别。 */
export function apiErrorCategory(code, status, fieldPath) {
  if (fieldPath || FIELD_CODES.has(code)) return API_ERROR_CATEGORY.FIELD
  if (TENANT_CODES.has(code)) return API_ERROR_CATEGORY.TENANT
  if (AUTHENTICATION_CODES.has(code) || status === 401) return API_ERROR_CATEGORY.AUTHENTICATION
  if (PERMISSION_CODES.has(code) || status === 403) return API_ERROR_CATEGORY.PERMISSION
  if (DEPENDENCY_CODES.some(prefix => String(code || '').startsWith(prefix)) || status >= 500) {
    return API_ERROR_CATEGORY.DEPENDENCY
  }
  return API_ERROR_CATEGORY.BUSINESS
}

/** 将 HTTP 200 的业务失败响应转换为统一异常。 */
export function apiErrorFromResponse(body, options = {}) {
  const status = options.httpStatus || null
  const code = body?.code || (status ? `HTTP_${status}` : 'BUSINESS_ERROR')
  const fieldPath = body?.fieldPath || body?.data?.fieldPath || null
  return new ApiClientError(body?.message || options.fallback || DEFAULT_ERROR_MESSAGE, {
    code,
    category: apiErrorCategory(code, status, fieldPath),
    fieldPath,
    retryable: body?.retryable === true || status >= 500,
    details: body?.details || body?.data?.details || [],
    httpStatus: status,
    cause: options.cause,
  })
}

/** 将 Axios、超时和未知错误转换为统一异常。 */
export function normalizeApiError(error, fallback = DEFAULT_ERROR_MESSAGE) {
  if (error instanceof ApiClientError) return error
  const response = error?.response
  if (response?.data) {
    return apiErrorFromResponse(response.data, {
      httpStatus: response.status,
      fallback,
      cause: error,
    })
  }
  const timeout = error?.code === 'ECONNABORTED' || String(error?.message || '').toLowerCase().includes('timeout')
  return new ApiClientError(
    timeout ? '网络请求超时，请检查连接后重试。' : (error?.message || fallback),
    {
      code: timeout ? 'NETWORK_TIMEOUT' : 'NETWORK_UNAVAILABLE',
      category: API_ERROR_CATEGORY.NETWORK,
      retryable: true,
      cause: error,
    },
  )
}

export const isAuthenticationError = error =>
  normalizeApiError(error).category === API_ERROR_CATEGORY.AUTHENTICATION

export const isPermissionApiError = error =>
  normalizeApiError(error).category === API_ERROR_CATEGORY.PERMISSION

export const isTenantContextError = error =>
  normalizeApiError(error).category === API_ERROR_CATEGORY.TENANT
