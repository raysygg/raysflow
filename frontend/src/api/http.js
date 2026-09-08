import axios from 'axios'
import { apiBaseUrl } from '../config/runtime'
import { apiErrorFromResponse, normalizeApiError } from './errors.js'
import { ACCESS_TOKEN_KEY, clearLocalSession } from './session.js'

/** 存储在 localStorage 中的访问令牌 Key */

/** 登录会话中保存在本地存储的所有键列表 */

/** 全局 HTTP 请求超时时间（30 秒） */
const HTTP_TIMEOUT_MS = 30_000

/** 提前刷新 Token 的时间窗口（30 秒） */
const TOKEN_REFRESH_WINDOW_MS = 30_000

/** 未授权 HTTP 状态码 401 */
const HTTP_UNAUTHORIZED = 401

/** 禁止访问 HTTP 状态码 403 */
const HTTP_FORBIDDEN = 403

/** 刷新 Token 端点路径 */
const REFRESH_ENDPOINT = '/auth/refresh'

/** 登录端点路径 */
const LOGIN_ENDPOINT = '/auth/login'

/** 退出登录端点路径 */
const LOGOUT_ENDPOINT = '/auth/logout'

/** 全局 Axios 实例，用于普通业务请求 */
const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: HTTP_TIMEOUT_MS,
  withCredentials: true,
})

/** 专用 Axios 实例，仅用于无感刷新 Token，避免拦截器死循环 */
const refreshClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: HTTP_TIMEOUT_MS,
  withCredentials: true,
})

/** 当前是否正在并发刷新 Token 的标识 */
let isRefreshing = false

/** 在刷新 Token 期间挂起的等待请求队列 */
let pendingRefreshRequests = []

/**
 * 清理本地存储的会话信息，并根据配置决定是否重定向到登录页。
 *
 * @param {boolean} [redirect=true] 是否自动跳转到 /login
 */
function clearSessionAndRedirect(redirect = true) {
  clearLocalSession()
  if (redirect && window.location.pathname !== '/login') {
    window.location.replace('/login')
  }
}

/**
 * 清空并发刷新队列，通知所有等待的 Promise 执行 resolve 或 reject。
 *
 * @param {Error|null} error 刷新失败时的错误对象
 * @param {string|null} token 刷新成功时获取到的最新 Access Token
 */
function flushRefreshQueue(error, token) {
  pendingRefreshRequests.forEach(({ resolve, reject }) => {
    if (error) reject(error)
    else resolve(token)
  })
  pendingRefreshRequests = []
}

/**
 * 向后端发起并发安全的 Access Token 刷新请求。
 *
 * @returns {Promise<string>} 返回最新的 Access Token 字符串
 */
function refreshAccessToken() {
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingRefreshRequests.push({ resolve, reject })
    })
  }

  isRefreshing = true
  return refreshClient.post(REFRESH_ENDPOINT)
    .then(response => {
      if (response.data?.success === false) {
        throw apiErrorFromResponse(response.data, { httpStatus: response.status })
      }
      const newToken = response.data?.data?.token
      if (!newToken) {
        throw new Error('登录会话已过期，请重新登录。')
      }
      localStorage.setItem(ACCESS_TOKEN_KEY, newToken)
      flushRefreshQueue(null, newToken)
      return newToken
    })
    .catch(error => {
      flushRefreshQueue(error, null)
      clearSessionAndRedirect()
      throw error
    })
    .finally(() => {
      isRefreshing = false
    })
}

/**
 * 解析 JWT Token 的过期时间戳（毫秒）。
 *
 * @param {string} token JWT 格式的令牌字符串
 * @returns {number|null} 返回过期时间戳或 null
 */
function readTokenExpiry(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    if (typeof payload.exp !== 'number') return null
    return payload.exp < 1e12 ? payload.exp * 1000 : payload.exp
  } catch {
    return null
  }
}

/**
 * 确保当前本地拥有有效且未临近过期的 Access Token。
 * 若令牌缺失或临近过期，将自动尝试无感刷新。
 *
 * @returns {Promise<string|null>} 返回可用的 Access Token 或 null
 */
export async function ensureAccessToken() {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (!token) {
    try {
      return await refreshAccessToken()
    } catch {
      return null
    }
  }

  const expiryMs = readTokenExpiry(token)
  if (expiryMs !== null && expiryMs - Date.now() < TOKEN_REFRESH_WINDOW_MS) {
    try {
      return await refreshAccessToken()
    } catch {
      return null
    }
  }
  return token
}

/**
 * 恢复本地登录会话。应用初始化时调用，避免页面刷新后丢失认证状态。
 *
 * @returns {Promise<string|null>} 可用的凭证或 null
 */
export async function restoreLoginSession() {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  const expiryMs = token ? readTokenExpiry(token) : null
  if (token && (expiryMs === null || expiryMs - Date.now() >= TOKEN_REFRESH_WINDOW_MS)) {
    return token
  }
  try {
    return await refreshAccessToken()
  } catch {
    clearSessionAndRedirect(false)
    return null
  }
}

// -----------------------------------------------------------------------------
// Axios 拦截器设置
// -----------------------------------------------------------------------------

/** 请求拦截器：自动注入 Bearer Authorization 请求头 */
http.interceptors.request.use(
  config => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error),
)

/** 响应拦截器：统一解包 ApiResponse，并处理 401/403 无感刷新与友好的中文错误包装 */
http.interceptors.response.use(
  response => {
    const body = response.data
    if (body?.success === false) {
      throw apiErrorFromResponse(body, { httpStatus: response.status })
    }
    return body
  },
  error => {
    const response = error?.response
    const config = error?.config

    // 优先使用后端返回的人性化中文 message
    const apiMessage = response?.data?.message || response?.data?.data?.message
    if (apiMessage) {
      error.message = apiMessage
    } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      error.message = '网络请求超时，请检查您的网络连接并重试。'
    } else if (!response) {
      error.message = '网络连接异常或服务未响应，请检查服务状态。'
    }

    const status = response?.status
    const isAuthError = status === HTTP_UNAUTHORIZED
    const isAuthEndpoint = [REFRESH_ENDPOINT, LOGIN_ENDPOINT, LOGOUT_ENDPOINT]
      .some(endpoint => config?.url?.includes(endpoint))

    if (!isAuthError || !config || config._retry || isAuthEndpoint) {
      return Promise.reject(normalizeApiError(error))
    }

    if (!localStorage.getItem(ACCESS_TOKEN_KEY)) {
      clearSessionAndRedirect()
      return Promise.reject(normalizeApiError(error))
    }

    config._retry = true
    return refreshAccessToken().then(newToken => {
      config.headers.Authorization = `Bearer ${newToken}`
      return http(config)
        .catch(retryError => Promise.reject(normalizeApiError(retryError)))
    })
  },
)

export default http
