import { subscribeRealtime } from './realtime-websocket'

const KNOWLEDGE_PROGRESS_CHANNEL = 'KNOWLEDGE_PROGRESS'
const TERMINAL_PHASES = new Set(['COMPLETED', 'FAILED'])

/** 订阅知识文档索引进度，断线后读取 Redis 中保存的最新进度快照。 */
export function subscribeKnowledgeProgress(documentId, { afterSequence = 0, onProgress, onError, onState } = {}) {
  return subscribeRealtime({
    channel: KNOWLEDGE_PROGRESS_CHANNEL,
    resourceId: documentId,
    afterSequence,
    onEvent: onProgress,
    onError,
    onState,
    isTerminal: event => TERMINAL_PHASES.has(event?.phase),
  })
}
