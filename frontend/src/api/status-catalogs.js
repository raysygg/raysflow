const CATALOGS = Object.freeze({
  runtime: {
    QUEUED: ['排队中', 'info'], RUNNING: ['运行中', 'info'], PAUSED: ['已暂停', 'warning'],
    WAITING_HUMAN: ['等待人工处理', 'warning'], WAITING_APPROVAL: ['等待审批', 'warning'],
    SUCCEEDED: ['已完成', 'success'], COMPLETED: ['已完成', 'success'], SUCCESS: ['已完成', 'success'],
    FAILED: ['失败', 'danger'], CANCELLED: ['已取消', 'muted'], REJECTED: ['已驳回', 'danger'],
  },
  lifecycle: {
    DRAFT: ['草稿', 'muted'], CREATED: ['已创建', 'info'], EVALUATING: ['评测中', 'info'],
    READY: ['可以发布', 'success'], BLOCKED: ['发布受阻', 'danger'], PUBLISHED: ['已发布', 'success'],
    ACTIVE: ['已启用', 'success'], DISABLED: ['已停用', 'muted'], ROLLED_BACK: ['已回滚', 'warning'],
  },
  governance: {
    PENDING_APPROVAL: ['待审批', 'warning'], APPROVED: ['已批准', 'info'], RUNNING: ['处理中', 'info'],
    PARTIAL_FAILED: ['部分失败', 'warning'], FAILED: ['失败', 'danger'], COMPLETED: ['已完成', 'success'],
    SUCCEEDED: ['已完成', 'success'], ACTIVE: ['生效中', 'success'], RELEASED: ['已解除', 'muted'],
  },
  infrastructure: {
    AVAILABLE: ['可用', 'success'], HEALTHY: ['正常', 'success'], DEGRADED: ['能力受限', 'warning'],
    UNAVAILABLE: ['不可用', 'danger'], UNKNOWN: ['待确认', 'muted'], PENDING: ['检查中', 'info'],
    INDEXING: ['索引中', 'info'], INDEXED: ['可检索', 'success'], INDEX_FAILED: ['索引失败', 'danger'],
  },
})

const safeDiagnosticCode = status => /^[A-Z0-9_:-]{1,64}$/.test(String(status || '')) ? String(status) : ''

/** 返回中文主标签、色调和可诊断但不作为主文案展示的状态编码。 */
export function statusPresentation(domain, status) {
  const item = CATALOGS[domain]?.[status]
  if (item) return { label: item[0], tone: item[1], diagnosticCode: '' }
  return { label: '未知状态', tone: 'muted', diagnosticCode: safeDiagnosticCode(status) }
}

export const sharedStatusLabel = (domain, status) => statusPresentation(domain, status).label
export const sharedStatusTone = (domain, status) => statusPresentation(domain, status).tone

export const STATUS_DOMAINS = Object.freeze(Object.keys(CATALOGS))
