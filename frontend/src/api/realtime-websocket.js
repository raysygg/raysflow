import http from './http'
import { wsBaseUrl } from '../config/runtime'

const BASE_RECONNECT_DELAY_MS = 1500
const MAX_RECONNECT_DELAY_MS = 15000
const RECONNECT_JITTER_RATIO = 0.2
const DEFAULT_HEARTBEAT_SECONDS = 25
const NORMAL_CLOSE_CODE = 1000
const POLICY_VIOLATION_CODE = 1008
const UNSUPPORTED_DATA_CODE = 1003
const TERMINAL_CLOSE_REASON = '实时任务已结束'

/**
 * 统一实时订阅客户端。每次连接都先换取一次性票据，JWT 不会进入 WebSocket 地址。
 */
export function subscribeRealtime({
  channel,
  resourceId,
  afterSequence = 0,
  detectSequenceGap = false,
  isTerminal,
  onEvent,
  onError,
  onState,
} = {}) {
  let disposed = false
  let socket = null
  let reconnectTimer = null
  let heartbeatTimer = null
  let reconnectAttempt = 0
  let sequence = Number(afterSequence) || 0

  const clearTimers = () => {
    if (reconnectTimer) window.clearTimeout(reconnectTimer)
    if (heartbeatTimer) window.clearInterval(heartbeatTimer)
    reconnectTimer = null
    heartbeatTimer = null
  }

  const scheduleReconnect = () => {
    if (disposed) return
    onState?.('RECONNECTING')
    const exponentialDelay = Math.min(MAX_RECONNECT_DELAY_MS,
      BASE_RECONNECT_DELAY_MS * (2 ** reconnectAttempt++))
    const jitter = exponentialDelay * RECONNECT_JITTER_RATIO * (Math.random() * 2 - 1)
    reconnectTimer = window.setTimeout(connect, Math.max(250, exponentialDelay + jitter))
  }

  const startHeartbeat = (seconds) => {
    const interval = Math.max(5, Number(seconds) || DEFAULT_HEARTBEAT_SECONDS) * 1000
    heartbeatTimer = window.setInterval(() => {
      if (socket?.readyState === WebSocket.OPEN) socket.send(JSON.stringify({ type: 'PING' }))
    }, interval)
  }

  const handleEnvelope = envelope => {
    if (envelope?.eventType === 'CONNECTED' || envelope?.eventType === 'PONG') return
    const eventSequence = Number(envelope?.sequence || 0)
    if (eventSequence && eventSequence <= sequence) return
    if (detectSequenceGap && eventSequence && sequence > 0 && eventSequence > sequence + 1) {
      socket?.close(1012, '事件序号不连续，重新补发')
      return
    }
    if (eventSequence) sequence = eventSequence
    const terminal = envelope?.terminal || isTerminal?.(envelope?.payload, envelope)
    try {
      onEvent?.(envelope?.payload, sequence, envelope)
    } finally {
      if (terminal) {
        disposed = true
        clearTimers()
        socket?.close(NORMAL_CLOSE_CODE, TERMINAL_CLOSE_REASON)
        onState?.('CLOSED')
      }
    }
  }

  const connect = async () => {
    if (disposed) return
    onState?.(reconnectAttempt === 0 ? 'CONNECTING' : 'RECONNECTING')
    try {
      const response = await http.post('/realtime/tickets', { channel, resourceId: String(resourceId), afterSequence: sequence })
      if (!response.success || !response.data?.websocketPath) {
        throw new Error(response.message || '无法获取实时连接票据')
      }
      if (disposed) return
      socket = new WebSocket(resolveWebSocketUrl(response.data.websocketPath))
      socket.onopen = () => {
        reconnectAttempt = 0
        onState?.('CONNECTED')
        startHeartbeat(response.data.heartbeatIntervalSeconds)
      }
      socket.onmessage = message => {
        try {
          handleEnvelope(JSON.parse(message.data))
        } catch (error) {
          onError?.(new Error(`实时事件解析失败：${error.message}`))
        }
      }
      socket.onerror = () => {
        if (!disposed) onError?.(new Error('实时连接发生网络异常'))
      }
      socket.onclose = event => {
        if (heartbeatTimer) window.clearInterval(heartbeatTimer)
        heartbeatTimer = null
        if (disposed) return
        if (event.code === POLICY_VIOLATION_CODE || event.code === UNSUPPORTED_DATA_CODE) {
          disposed = true
          onState?.('CLOSED')
          onError?.(new Error(event.reason || '实时订阅被拒绝，请刷新页面后重试'))
          return
        }
        scheduleReconnect()
      }
    } catch (error) {
      if (!disposed) {
        onError?.(error)
        scheduleReconnect()
      }
    }
  }

  connect()
  return () => {
    disposed = true
    clearTimers()
    if (socket && socket.readyState < WebSocket.CLOSING) socket.close(NORMAL_CLOSE_CODE, '用户关闭实时订阅')
    onState?.('CLOSED')
  }
}

function resolveWebSocketUrl(websocketPath) {
  const base = new URL(wsBaseUrl, window.location.href)
  const origin = `${base.protocol}//${base.host}`
  return new URL(websocketPath, origin).toString()
}
