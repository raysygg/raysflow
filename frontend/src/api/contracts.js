import { apiErrorFromResponse, isPermissionApiError, normalizeApiError } from './errors.js'

const DEFAULT_ERROR_MESSAGE = '请求处理失败，请稍后重试。'

export const RUNTIME_TERMINAL_STATUSES = Object.freeze([
  'SUCCEEDED', 'SUCCESS', 'COMPLETED', 'FAILED', 'CANCELLED', 'REJECTED', 'EXPIRED',
])
export const RUNTIME_WAITING_STATUSES = Object.freeze(['WAITING_HUMAN', 'WAITING_APPROVAL'])

/** 读取统一 API 响应的数据部分。 */
export function apiData(response, fallback = null) {
  return response?.success === true && response.data !== undefined ? response.data : fallback
}

/** 读取后端返回的中文业务消息。 */
export function apiMessage(response, fallback = DEFAULT_ERROR_MESSAGE) {
  return response?.message || fallback
}

/** 读取统一 API 异常的安全中文提示。 */
export function apiErrorMessage(error, fallback = DEFAULT_ERROR_MESSAGE) {
  return normalizeApiError(error, fallback).message
}

export function isApiSuccess(response) {
  return response?.success === true
}

/** 业务失败统一抛出 ApiClientError，保留字段路径和可重试信息。 */
export function requireApiSuccess(response, fallback = DEFAULT_ERROR_MESSAGE) {
  if (!isApiSuccess(response)) {
    throw apiErrorFromResponse(response, { fallback })
  }
  return response.data
}

export function isPermissionError(error) {
  return isPermissionApiError(error)
}

const RUNTIME_STATUS_LABELS = Object.freeze({
  QUEUED: '排队中',
  RUNNING: '运行中',
  WAITING_TOOL: '等待工具',
  WAITING_HUMAN: '等待人工处理',
  WAITING_APPROVAL: '等待审批',
  PAUSED: '已暂停',
  PAUSING: '正在暂停',
  SUCCEEDED: '已完成',
  SUCCESS: '已完成',
  COMPLETED: '已完成',
  FAILED: '失败',
  CANCELLED: '已取消',
  REJECTED: '已驳回',
  EXPIRED: '已过期',
})

export function runtimeStatusLabel(status) {
  return RUNTIME_STATUS_LABELS[status] || (status ? '未知状态' : '未开始')
}

export function isTerminalRuntimeStatus(status) {
  return RUNTIME_TERMINAL_STATUSES.includes(status)
}

export function isRuntimeWaitingStatus(status) {
  return RUNTIME_WAITING_STATUSES.includes(status)
}

export function isRuntimePausableStatus(status) {
  return ['RUNNING', 'PAUSING'].includes(status)
}

export function isRuntimeResumableStatus(status) {
  return ['PAUSED', 'PAUSING', 'QUEUED'].includes(status)
}

export function isRuntimeCancelableStatus(status) {
  return !isTerminalRuntimeStatus(status) && !isRuntimeWaitingStatus(status)
}

export function isRuntimeRetryableStatus(status) {
  return ['FAILED', 'CANCELLED'].includes(status)
}
