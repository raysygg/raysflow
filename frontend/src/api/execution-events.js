import { subscribeRealtime } from './realtime-websocket'

const EXECUTION_CHANNEL = 'EXECUTION_EVENTS'
const TERMINAL_EVENT_PATTERN = /^EXECUTION_(SUCCEEDED|FAILED|CANCELLED|REJECTED)$/

/** 订阅 Run 实时事件，并在重连时按数据库事件序号补发。 */
export function subscribeExecutionEvents(executionId, options = {}) {
  return subscribeRealtime({
    channel: EXECUTION_CHANNEL,
    resourceId: executionId,
    afterSequence: options.afterSequence,
    detectSequenceGap: true,
    onEvent: options.onEvent,
    onError: options.onError,
    onState: options.onState,
    isTerminal: event => TERMINAL_EVENT_PATTERN.test(event?.eventType || ''),
  })
}
