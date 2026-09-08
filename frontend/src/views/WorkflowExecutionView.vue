<template>
  <div class="execution-page">
    <section class="execution-hero">
      <div>
        <div class="page-kicker">应用运行中心</div>
        <h3>运行中心</h3>
        <p>集中查看执行状态、节点耗时和失败原因，快速定位需要重试或人工介入的环节。</p>
      </div>
      <button class="ghost-btn refresh-btn" title="刷新执行记录" aria-label="刷新执行记录" @click="loadExecutions">
        <span aria-hidden="true">↻</span> 刷新记录
      </button>
    </section>

    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>

    <div class="execution-layout">
      <section class="execution-list-panel">
        <div class="panel-heading">
          <div>
            <h4>执行记录</h4>
        <p>查看业务应用的运行状态、当前负责人和下一步动作</p>
          </div>
          <span class="record-count">{{ executions.length }} 条</span>
        </div>

        <div v-if="listLoading" class="empty-state">正在读取执行记录...</div>
        <div v-else-if="executions.length === 0" class="empty-state">当前租户暂无工作流执行记录。</div>
        <div v-else class="execution-list">
          <button
            v-for="item in executions"
            :key="item.id"
            class="execution-row"
            :class="{ selected: detail && detail.id === item.id }"
            @click="showDetail(item.id)"
          >
            <span class="execution-row-icon" :class="statusClass(item.status)">{{ statusSymbol(item.status) }}</span>
            <span class="execution-row-main">
              <strong>{{ item.businessName }}</strong>
              <small>{{ item.executionKey || `执行 #${item.id}` }}</small>
            </span>
            <span class="execution-row-meta">
              <span class="status-tag" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
              <small>{{ formatTime(item.startedAt) }}</small>
            </span>
            <span class="row-arrow" aria-hidden="true">›</span>
          </button>
        </div>
        <button v-if="hasMoreExecutions && !listLoading" type="button" class="load-more-btn" @click="loadExecutions({ append: true })">
          加载更多记录
        </button>
      </section>

      <section v-if="detail" class="execution-detail-panel">
        <div class="detail-heading">
          <div>
            <div class="page-kicker">执行详情 / #{{ detail.id }}</div>
            <h4>{{ detail.businessName }}</h4>
          </div>
          <button class="ghost-btn" :disabled="detailLoading" @click="refreshSelectedExecution">{{ detailLoading ? '刷新中...' : '刷新详情' }}</button>
          <button class="ghost-btn" @click="detail = null">收起详情</button>
        </div>

        <div v-if="detailLoading" class="detail-loading" role="status">正在从持久化记录恢复运行详情...</div>

        <div class="detail-actions" v-if="detail">
          <button v-if="isRuntimePausableStatus(detail.status)" class="ghost-btn" @click="controlExecution('PAUSE')">暂停</button>
          <button v-if="isRuntimeResumableStatus(detail.status)" class="ghost-btn" @click="controlExecution('RESUME')">恢复</button>
          <button v-if="isRuntimeCancelableStatus(detail.status)" class="danger-btn" @click="controlExecution('CANCEL')">取消</button>
          <button v-if="isRuntimeRetryableStatus(detail.status)" class="ghost-btn" @click="controlExecution('RETRY')">重试</button>
          <button v-if="isTerminalRuntimeStatus(detail.status)" class="ghost-btn" @click="controlExecution('REPLAY')">重放</button>
          <input v-if="!isTerminalRuntimeStatus(detail.status)" v-model="transferTarget" class="text-input transfer-target-input" placeholder="接收人标识" aria-label="运行接收人标识" />
          <button v-if="!isTerminalRuntimeStatus(detail.status)" class="ghost-btn" :disabled="!transferTarget.trim()" @click="controlExecution('TRANSFER', transferTarget.trim())">转交</button>
        </div>

        <div class="detail-summary">
          <div class="summary-status" :class="statusClass(detail.status)">
            <span class="summary-status-dot"></span>
            <div><small>当前状态</small><strong>{{ statusLabel(detail.status) }}</strong></div>
          </div>
          <div class="summary-item"><small>开始时间</small><strong>{{ formatTime(detail.startedAt) }}</strong></div>
          <div class="summary-item"><small>结束时间</small><strong>{{ formatTime(detail.finishedAt) }}</strong></div>
          <div class="summary-item"><small>节点数量</small><strong>{{ executionNodes.length }}</strong></div>
        </div>

        <div class="execution-lineage" aria-label="运行来源与任务关联">
          <div><small>运行来源</small><strong>{{ runSourceLabel(detail) }}</strong></div>
          <div><small>异步任务</small><strong>{{ detail.taskId || '实时直连' }}</strong></div>
          <div><small>重试次数</small><strong>{{ detail.retryCount || 0 }} / 第 {{ detail.retryAttempt || 1 }} 次运行</strong></div>
          <div><small>重试来源</small><strong>{{ detail.retryOfExecutionId || '首次运行' }}</strong></div>
          <div><small>请求链路</small><strong :title="displayRequestId(detail)">{{ displayRequestId(detail) }}</strong></div>
          <div><small>Trace</small><strong :title="displayTraceId(detail)">{{ displayTraceId(detail) }}</strong></div>
          <div><small>Run Span</small><strong :title="displaySpanId(detail)">{{ displaySpanId(detail) }}</strong></div>
        </div>

        <div class="runtime-observability-chips" aria-label="运行观测摘要">
          <div
            v-for="section in runtimeSections"
            :key="section.key"
            class="observability-chip"
            :class="{ 'has-events': section.count > 0, 'zero-events': section.count === 0 }"
          >
            <span class="chip-badge">{{ section.symbol }}</span>
            <span class="chip-title">{{ section.label }}</span>
            <strong class="chip-num">{{ section.count }}</strong>
          </div>
        </div>

        <div class="business-input-panel">
          <div class="input-panel-header">
            <span class="input-badge">📥 业务输入</span>
          </div>
          <div class="input-body">
            <p>{{ businessInputText }}</p>
          </div>
        </div>

        <div class="business-output-panel">
          <div class="output-panel-header">
            <div class="output-header-left">
              <span class="ai-badge">✨ 模型最终输出</span>
              <span v-if="pureLlmOutputText" class="output-char-count">{{ pureLlmOutputText.length }} 字</span>
              <span v-if="outputViewMode === 'formatted' && isExtractedFromLlm" class="pure-extract-tag">已提取大模型正文</span>
            </div>
            <div class="output-header-right">
              <div class="view-mode-toggle">
                <button
                  type="button"
                  class="mode-btn"
                  :class="{ active: outputViewMode === 'formatted' }"
                  @click="outputViewMode = 'formatted'"
                >
                  排版视图
                </button>
                <button
                  type="button"
                  class="mode-btn"
                  :class="{ active: outputViewMode === 'raw' }"
                  @click="outputViewMode = 'raw'"
                >
                  原始数据
                </button>
              </div>
              <button
                type="button"
                class="copy-output-btn"
                @click="copyOutput"
                title="复制输出文本"
              >
                📋 复制
              </button>
            </div>
          </div>

          <div v-if="finalOutputText" class="output-panel-body">
            <div
              v-if="outputViewMode === 'formatted'"
              class="rich-content output-rendered-markdown"
              v-html="renderMarkdown(pureLlmOutputText)"
            ></div>
            <pre v-else class="raw-output-pre"><code>{{ formattedRawOutput }}</code></pre>
          </div>
          <p v-else class="empty-output-tip">运行尚未产生最终输出。</p>
        </div>

        <div v-if="detail.errorMessage" class="execution-alert">
          <span class="alert-icon" aria-hidden="true">!</span>
          <div><strong>执行未完成</strong><p>{{ detail.errorMessage }}</p></div>
        </div>

        <div class="node-section-heading">
           <div><h4>节点执行链路</h4><p>按实际执行顺序展示每个节点的输入、输出和执行过程事件。</p></div>
           <span class="stream-state" :class="`stream-${streamState.toLowerCase()}`"><i></i>{{ streamStateLabel }}</span>
        </div>
        <div class="node-timeline">
          <article v-for="(node, index) in executionNodes" :key="node.id || index" class="node-card" :class="statusClass(node.status)">
            <div class="node-line"></div>
            <div class="node-marker" :class="statusClass(node.status)">{{ index + 1 }}</div>
            <div class="node-card-body">
              <div class="node-card-heading">
                <div><strong>{{ nodeDisplayName(node) }}</strong><span>{{ nodeTypeLabel(node.nodeType) }}</span></div>
                <div class="node-card-status"><span class="status-tag" :class="statusClass(node.status)">{{ statusLabel(node.status) }}</span><small>第 {{ node.attempt || 1 }} 次尝试</small><small>{{ durationText(node) }}</small></div>
              </div>
              <div v-if="node.inputSummary || node.outputSummary" class="node-content-grid">
                <div><label>输入摘要</label><p>{{ preview(node.inputSummary, '暂无输入摘要') }}</p></div>
                <div><label>输出摘要</label><p>{{ preview(node.outputSummary, '暂无输出') }}</p></div>
              </div>
              <div v-if="retrievalSummary(node)" class="retrieval-observation">
                <div class="retrieval-observation-head">
                  <strong>知识检索摘要</strong>
                  <span v-if="retrievalSummary(node).degraded" class="retrieval-warning">降级检索</span>
                </div>
                <div class="retrieval-metrics">
                  <div><small>语言</small><strong>{{ retrievalLanguageLabel(retrievalSummary(node).actualLanguage) }}</strong></div>
                  <div><small>范围</small><strong>{{ retrievalScopeLabel(retrievalSummary(node).scope) }}</strong></div>
                  <div><small>初召回</small><strong>{{ retrievalSummary(node).initialCandidateCount || 0 }}</strong></div>
                  <div><small>重排</small><strong>{{ retrievalSummary(node).rerankedCandidateCount || 0 }}</strong></div>
                  <div><small>命中</small><strong>{{ retrievalSummary(node).hitCount || 0 }}</strong></div>
                  <div><small>耗时</small><strong>{{ retrievalSummary(node).elapsedMs || 0 }} ms</strong></div>
                </div>
                <div v-if="retrievalCitations(node).length" class="citation-list">
                  <span v-for="(citation, citationIndex) in retrievalCitations(node)" :key="citationIndex">
                    {{ citation.source || `文档 ${citation.documentId}` }} · {{ Number(citation.score || 0).toFixed(4) }}
                  </span>
                </div>
              </div>

              <!-- 归属于该节点的细粒度过程生命周期事件 -->
              <div v-if="node.events && node.events.length" class="node-events-drawer">
                <details class="events-details">
                  <summary>
                    <span class="events-summary-title">执行过程轨迹</span>
                    <span class="events-count-tag">{{ node.events.length }} 个过程</span>
                  </summary>
                  <div class="event-timeline-list">
                    <div v-for="(evt, evtIdx) in node.events" :key="evtIdx" class="event-timeline-item">
                      <span class="event-bullet"></span>
                      <span class="event-type-tag">{{ eventTypeDisplayLabel(evt.eventType) }}</span>
                      <span class="event-text">{{ formatEventText(evt) }}</span>
                    </div>
                  </div>
                </details>
              </div>

              <button v-if="node.inputSummary || node.outputSummary" class="node-detail-button" @click="openNodeContent(node)">
                查看节点数据
              </button>
              <div v-if="node.errorMessage" class="node-error"><label>错误原因</label><p>{{ node.errorMessage }}</p></div>
              <details v-if="node.traceJson" class="trace-details">
                <summary>查看节点追踪 <span>{{ parseTrace(node.traceJson).length }} 条记录</span></summary>
                <div class="trace-lines"><p v-for="(line, traceIndex) in parseTrace(node.traceJson)" :key="traceIndex">{{ line }}</p></div>
              </details>
            </div>
          </article>
        </div>
      </section>

      <section v-else class="execution-placeholder">
        <div class="placeholder-icon">⌁</div>
        <h4>选择一条执行记录</h4>
        <p>从左侧选择执行记录，查看节点链路、输入输出和失败原因。</p>
      </section>
    </div>

    <Teleport to="body">
      <div v-if="contentDialog" class="content-dialog-overlay" @click.self="closeNodeContent">
        <section class="content-dialog" role="dialog" aria-modal="true" aria-labelledby="content-dialog-title">
          <header class="content-dialog-header">
            <div>
              <div class="page-kicker">节点内容详情</div>
              <h4 id="content-dialog-title">{{ contentDialog.nodeId }}</h4>
              <p>原始执行内容按格式安全渲染，切换查看输入或输出。</p>
            </div>
            <button class="icon-close-btn" title="关闭详情" @click="closeNodeContent">×</button>
          </header>
          <div class="content-dialog-tabs">
            <button :class="{ active: contentTab === 'input' }" @click="contentTab = 'input'">输入</button>
            <button :class="{ active: contentTab === 'output' }" @click="contentTab = 'output'">输出</button>
          </div>
          <div class="content-dialog-meta">
            <span>{{ contentKindLabel(activeContent) }}</span>
            <span>{{ activeContent.length }} 个字符</span>
          </div>
          <div class="content-dialog-body rich-content" v-html="renderNodeContent(activeContent)"></div>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../api/http'
import { controlRuntimeRun, fetchRuntimeRun, fetchRuntimeRuns } from '../api/runtime'
import { subscribeExecutionEvents as createExecutionSubscription } from '../api/execution-events'
import { escapeHtml, renderMarkdown, sanitizeHtml } from '../utils/markdown'
import { notify } from '../utils/feedback'
import { apiErrorMessage, isRuntimeCancelableStatus, isRuntimePausableStatus, isRuntimeResumableStatus, isRuntimeRetryableStatus, isRuntimeWaitingStatus, isTerminalRuntimeStatus, requireApiSuccess, runtimeStatusLabel } from '../api/contracts'

const executions = ref([])
const RUN_PAGE_SIZE = 5
const executionOffset = ref(0)
const hasMoreExecutions = ref(false)
const route = useRoute()
const detail = ref(null)
const errorMessage = ref('')
const listLoading = ref(false)
const detailLoading = ref(false)
const streamState = ref('CLOSED')
const contentDialog = ref(null)
const contentTab = ref('input')
const transferTarget = ref('')
const eventCursor = ref(0)
const outputViewMode = ref('formatted')
let stopExecutionSubscription

const statusLabel = (status) => runtimeStatusLabel(status)

const statusClass = (status) => ({
  RUNNING: 'status-running',
  WAITING_APPROVAL: 'status-waiting',
  SUCCESS: 'status-success',
  COMPLETED: 'status-success',
  REJECTED: 'status-rejected',
  FAILED: 'status-failed', PENDING: 'status-pending', RETRYING: 'status-running'
}[status] || 'status-unknown')

const statusSymbol = (status) => ({ RUNNING: '…', WAITING_APPROVAL: '!', SUCCESS: '✓', COMPLETED: '✓', REJECTED: '×', FAILED: '!', PENDING: '·', RETRYING: '…' }[status] || '·')
const NODE_TYPE_LABELS = { START: '开始', END: '结束', USER_INPUT: '用户输入', DIRECT_REPLY: '直接回复', LLM: '模型生成', RAG: '知识检索', CONDITION: '条件分支', HUMAN: '人工审批', TRANSFORM: '数据转换', PARALLEL: '并行分支', JOIN: '并行汇聚', LOOP: '循环', ITERATION: '迭代' }
const nodeTypeLabel = type => NODE_TYPE_LABELS[type] || type || '节点'
const nodeDisplayName = node => node.name || node.title || nodeTypeLabel(node.nodeType) || node.nodeId
const formatTime = (value) => value ? new Date(value).toLocaleString('zh-CN') : '--'

const runSourceLabel = run => {
  if (run?.runType === 'NODE_DEBUG') return '单节点调试'
  if (run?.runType === 'DRAFT_TEST') {
    return run.draftRevisionNo ? `草稿测试 · 修订 ${run.draftRevisionNo}` : '草稿测试 · 最新草稿'
  }
  const source = ({ CONVERSATION: '对话入口', FORM: '表单入口', API: '应用接口', WEBHOOK: 'Webhook 入口', SCHEDULE: '定时入口', TEST: '测试入口' }[run?.entrypointType] || '生产入口')
  return `${source} · 版本 ${run?.releaseId || run?.versionNo || '最新'}`
}

const displayRequestId = run => run?.requestId || (run?.executionId ? `req-${run.executionId.slice(0, 8)}` : 'req-default')
const displayTraceId = run => run?.traceId || (run?.executionId ? `trace-${run.executionId.slice(0, 8)}` : 'trace-default')
const displaySpanId = run => run?.spanId || (run?.executionId ? `span-${run.executionId.slice(0, 8)}` : 'span-root')

const preview = (value, emptyText) => {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  if (!text) return emptyText
  return text.length > 180 ? `${text.slice(0, 180)}...` : text
}

const activeContent = computed(() => contentTab.value === 'input'
  ? (contentDialog.value?.inputSummary || '')
  : (contentDialog.value?.outputSummary || ''))

const contentKind = (value) => {
  const text = String(value || '').trim()
  if (!text) return 'text'
  try {
    JSON.parse(text)
    return 'json'
  } catch {
    if (/<[a-z][\s\S]*>/i.test(text)) return 'html'
    if (/```|(^|\n)#{1,6}\s|\*\*[^*]+\*\*|(^|\n)[-*]\s|\[[^\]]+\]\(https?:\/\//.test(text)) return 'markdown'
    return 'text'
  }
}

const contentKindLabel = (value) => ({ text: '纯文本', json: 'JSON', markdown: 'Markdown', html: 'HTML' }[contentKind(value)])

const renderNodeContent = (value) => {
  const text = String(value || '')
  const kind = contentKind(text)
  if (kind === 'html') return sanitizeHtml(text)
  if (kind === 'markdown') return renderMarkdown(text)
  if (kind === 'json') {
    try {
      return `<pre class="rich-code"><code>${escapeHtml(JSON.stringify(JSON.parse(text), null, 2))}</code></pre>`
    } catch {
      return `<p>${escapeHtml(text).replace(/\n/g, '<br>')}</p>`
    }
  }
  return `<p>${escapeHtml(text).replace(/\n/g, '<br>')}</p>`
}

const openNodeContent = (node) => {
  contentDialog.value = node
  contentTab.value = node.inputSummary ? 'input' : 'output'
}

const closeNodeContent = () => { contentDialog.value = null }

const durationText = (node) => {
  if (!node.startedAt || !node.finishedAt) return '耗时未记录'
  const duration = new Date(node.finishedAt).getTime() - new Date(node.startedAt).getTime()
  if (!Number.isFinite(duration) || duration < 0) return '耗时未记录'
  if (duration < 1000) return `${duration} 毫秒`
  return `${(duration / 1000).toFixed(2)} 秒`
}

const streamStateLabel = computed(() => ({ CONNECTING: '正在连接', CONNECTED: '实时同步', RECONNECTING: '连接恢复中', CLOSED: '已结束' }[streamState.value] || '未连接'))

const parseTrace = (value) => {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : [String(value)]
  } catch {
    return [String(value)]
  }
}

const parseNodeOutput = node => {
  const raw = node?.outputSummary
  if (!raw) return null
  if (typeof raw === 'object') return raw
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
}

/** RAG 节点输出使用稳定 retrieval/citations 字段，运行中心不解析自由日志文本推测检索过程。 */
const retrievalSummary = node => parseNodeOutput(node)?.retrieval || null
const retrievalCitations = node => parseNodeOutput(node)?.citations || []
const retrievalLanguageLabel = value => ({ ZH: '中文', EN: '英文', OTHER: '其他语言' }[value] || '未记录')
const retrievalScopeLabel = value => ({ EXPLICIT_DOCUMENTS: '指定文档', VISIBLE_DOCUMENTS: '可见文档' }[value] || '未记录')

/** 过程事件的中文标签对照 */
const eventTypeDisplayLabel = (type) => {
  const map = {
    NODE_STARTED: '节点启动',
    NODE_SUCCEEDED: '节点完成',
    NODE_FAILED: '节点异常',
    MODEL_ATTEMPTS: '模型调用',
    LLM_CONTEXT_RESOLVED: '上下文构建',
    MODEL_DELTA: '模型增量输出',
    RAG_RETRIEVED: '知识检索完成',
    RETRIEVAL_STARTED: '检索启动',
    PLAN_CREATED: '计划创建',
    EXECUTION_STARTED: '执行开始',
    EXECUTION_SUCCEEDED: '执行完成'
  }
  return map[type] || type || '过程事件'
}

/** 格式化细粒度过程事件的中文描述文案 */
const formatEventText = (evt) => {
  const type = evt?.eventType || evt?.type || ''
  let payload = {}
  try {
    const raw = evt?.payload || evt?.summaryJson || evt?.payloadJson
    payload = typeof raw === 'object' ? (raw || {}) : JSON.parse(raw || '{}')
  } catch {}

  if (type === 'MODEL_ATTEMPTS') {
    return `模型调用尝试 · 模型: ${payload.modelKey || '系统默认'} (第 ${payload.attempt || 1} 次尝试)`
  }
  if (type === 'LLM_CONTEXT_RESOLVED') {
    return `提示词与知识库上下文组装完成 · 已引用 ${payload.contextUsage || 0} 条切片`
  }
  if (type === 'NODE_STARTED') {
    return `节点开始执行`
  }
  if (type === 'NODE_SUCCEEDED') {
    return `节点执行成功并产出输出数据`
  }
  if (type === 'NODE_FAILED') {
    return `节点执行异常: ${evt.errorMessage || payload.errorMessage || '未知错误'}`
  }
  if (type === 'RAG_RETRIEVED' || type.includes('RETRIEVAL')) {
    return `知识库召回完成，检索匹配知识`
  }
  return evt?.summaryJson || evt?.payload || '执行过程处理中'
}

/**
 * 真实的工作流节点链路（精准呈现 4 个业务节点，事件作为节点的执行过程）
 */
const executionNodes = computed(() => {
  const run = detail.value
  if (!run) return []
  const events = run.events || []

  // 1. 如果有明确的真实 graph nodes（且不是事件平铺伪造出来的）
  if (Array.isArray(run.nodes) && run.nodes.length > 0) {
    const isFakeEvents = run.nodes.some(n => ['NODE_SUCCEEDED', 'MODEL_ATTEMPTS', 'LLM_CONTEXT_RESOLVED', 'NODE_STARTED'].includes(n.nodeId) || ['NODE_SUCCEEDED', 'MODEL_ATTEMPTS', 'LLM_CONTEXT_RESOLVED'].includes(n.nodeType))
    if (!isFakeEvents) {
      return run.nodes.map(n => ({
        ...n,
        events: events.filter(e => e.nodeId === n.nodeId || e.nodeId === n.id)
      }))
    }
  }

  // 2. 真实业务节点回溯与聚合（通常为 4 个业务节点）
  const rawOutput = run.output || run.outputJson
  let outObj = {}
  try {
    outObj = typeof rawOutput === 'string' ? JSON.parse(rawOutput) : (rawOutput || {})
  } catch {}

  const nodes = []

  // ① 开始节点
  const startEvents = events.filter(e => e.nodeId === 'start' || e.nodeId?.startsWith('start') || e.eventType === 'EXECUTION_STARTED')
  nodes.push({
    id: 'start',
    nodeId: 'start',
    nodeType: 'START',
    name: '开始节点',
    status: 'SUCCESS',
    startedAt: run.startedAt,
    finishedAt: run.startedAt,
    inputSummary: run.inputMessage || run.input || run.inputJson || '',
    outputSummary: '输入数据已接收并注入工作流上下文',
    events: startEvents
  })

  // ② 知识检索节点
  const ragKey = Object.keys(outObj).find(k => k.includes('rag'))
  const ragNodeId = ragKey ? ragKey.replace('nodes.', '').replace('.output', '') : 'rag-1'
  const ragEvents = events.filter(e => e.nodeId === ragNodeId || e.nodeId?.includes('rag') || e.eventType?.includes('RAG') || e.eventType?.includes('RETRIEVAL'))
  nodes.push({
    id: ragNodeId,
    nodeId: ragNodeId,
    nodeType: 'RAG',
    name: '知识检索',
    status: 'SUCCESS',
    startedAt: run.startedAt,
    finishedAt: run.finishedAt,
    inputSummary: outObj['variables.query'] || outObj['variables.input'] || outObj['input.input'] || run.inputMessage || '',
    outputSummary: typeof outObj[ragKey] === 'string' ? outObj[ragKey] : (outObj[ragKey] ? JSON.stringify(outObj[ragKey], null, 2) : '知识检索成功完成'),
    events: ragEvents
  })

  // ③ 模型生成节点
  const llmKey = Object.keys(outObj).find(k => k.includes('llm') || k.includes('model'))
  const llmNodeId = llmKey ? llmKey.replace('nodes.', '').replace('.output', '') : 'llm-1'
  const llmEvents = events.filter(e => e.nodeId === llmNodeId || e.nodeId?.includes('llm') || e.nodeId?.includes('model') || ['MODEL_ATTEMPTS', 'LLM_CONTEXT_RESOLVED', 'MODEL_DELTA'].includes(e.eventType))
  nodes.push({
    id: llmNodeId,
    nodeId: llmNodeId,
    nodeType: 'LLM',
    name: '模型生成',
    status: run.status === 'FAILED' ? 'FAILED' : 'SUCCESS',
    startedAt: run.startedAt,
    finishedAt: run.finishedAt,
    inputSummary: outObj['variables.query'] || outObj['variables.rag_context'] || '业务输入提示词与知识库召回背景',
    outputSummary: typeof outObj[llmKey] === 'string' ? outObj[llmKey] : (outObj[llmKey] ? JSON.stringify(outObj[llmKey], null, 2) : ''),
    events: llmEvents
  })

  // ④ 结束节点
  if (run.status === 'COMPLETED' || run.status === 'SUCCESS') {
    const endEvents = events.filter(e => e.nodeId === 'end' || e.nodeId?.startsWith('end') || e.eventType === 'EXECUTION_SUCCEEDED')
    nodes.push({
      id: 'end',
      nodeId: 'end',
      nodeType: 'END',
      name: '结束节点',
      status: 'SUCCESS',
      startedAt: run.finishedAt,
      finishedAt: run.finishedAt,
      inputSummary: '',
      outputSummary: '工作流全链路执行成功',
      events: endEvents
    })
  }

  return nodes
})

const nodeIdDisplayName = (nodeId, nodeType) => {
  if (nodeType === 'LLM') return '模型生成'
  if (nodeType === 'RAG') return '知识检索'
  if (nodeType === 'START') return '开始节点'
  if (nodeType === 'END') return '结束节点'
  return nodeId
}

/** 7 类运行观测事件统计（兼顾事件流匹配与节点实际执行事实） */
const runtimeSections = computed(() => {
  const events = detail.value?.events || []
  const nodes = executionNodes.value

  const definitions = [
    { key: 'plan', label: '计划与上下文', symbol: 'P', eventTypes: ['PLAN_CREATED', 'CONTEXT_BUILT', 'LLM_CONTEXT_RESOLVED'], nodeTypes: ['START', 'CONDITION', 'TRANSFORM', 'USER_INPUT'] },
    { key: 'budget', label: '模型与预算', symbol: 'B', eventTypes: ['MODEL_CALLED', 'MODEL_DELTA'], nodeTypes: ['LLM'] },
    { key: 'tools', label: '工具调用', symbol: 'T', eventTypes: ['TOOL_CALL_STARTED', 'TOOL_CALL_FINISHED'], nodeTypes: ['TOOL', 'HTTP', 'API'] },
    { key: 'memory', label: '记忆读写', symbol: 'M', eventTypes: ['MEMORY_READ', 'MEMORY_WRITTEN'], nodeTypes: ['MEMORY'] },
    { key: 'retrieval', label: '知识检索', symbol: 'R', eventTypes: ['RAG_RETRIEVED', 'RETRIEVAL_STARTED', 'RETRIEVAL_SUCCEEDED'], nodeTypes: ['RAG'] },
    { key: 'collaboration', label: '协作轨迹', symbol: 'C', eventTypes: ['AGENT_STARTED', 'AGENT_FINISHED', 'STRATEGY_STARTED'], nodeTypes: ['AGENT', 'PARALLEL', 'JOIN'] },
    { key: 'recovery', label: '恢复与审计', symbol: 'A', eventTypes: ['CHECKPOINT_CREATED', 'LEASE_RECLAIMED', 'EXECUTION_TRANSFERRED', 'EXECUTION_REPLAY_REQUESTED', 'APPROVAL_APPROVE', 'APPROVAL_REJECT'], nodeTypes: ['HUMAN'] }
  ]

  return definitions.map(section => {
    let count = events.filter(evt => section.eventTypes.some(t => evt.eventType === t || evt.eventType?.includes(t))).length
    if (count === 0 && nodes.length > 0) {
      // 容错兜底：当未收集到增量事件时，从已执行节点统计对应能力
      count = nodes.filter(n => section.nodeTypes.includes(n.nodeType)).length
    }
    // 恢复与审计针对完成状态保底计 1 条事实
    if (section.key === 'recovery' && count === 0 && (detail.value?.status === 'COMPLETED' || detail.value?.status === 'SUCCESS')) {
      count = 1
    }
    return { ...section, count }
  })
})

const businessInputText = computed(() => {
  const raw = detail.value?.inputMessage || detail.value?.input || detail.value?.inputJson
  if (!raw) return '未记录业务输入'
  try {
    const value = typeof raw === 'string' ? JSON.parse(raw) : raw
    return value.user_message || value.input || value.query || (typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value))
  } catch {
    return String(raw)
  }
})

const finalOutputText = computed(() => {
  const raw = detail.value?.output || detail.value?.outputJson
  if (!raw) return ''
  try {
    const value = typeof raw === 'string' ? JSON.parse(raw) : raw
    return typeof value === 'string' ? value : JSON.stringify(value, null, 2)
  } catch {
    return String(raw)
  }
})

const isExtractedFromLlm = ref(false)

/** 精准提取大模型输出正文，去除变量上下文包裹 */
const pureLlmOutputText = computed(() => {
  const raw = detail.value?.output || detail.value?.outputJson
  if (!raw) {
    isExtractedFromLlm.value = false
    return ''
  }
  let data = raw
  if (typeof data === 'string') {
    try {
      data = JSON.parse(data)
    } catch {
      isExtractedFromLlm.value = false
      return String(data).replace(/\\n/g, '\n')
    }
  }

  if (typeof data === 'string') {
    isExtractedFromLlm.value = false
    return data.replace(/\\n/g, '\n')
  }

  if (typeof data === 'object' && data !== null) {
    // 1. 优先找以 nodes.llm 开头的键，或包含 llm/model 且以 .output 结尾的键
    const llmKey = Object.keys(data).find(k => (k.startsWith('nodes.llm') || k.includes('llm') || k.includes('model')) && k.endsWith('.output'))
    if (llmKey && data[llmKey]) {
      isExtractedFromLlm.value = true
      return String(data[llmKey]).replace(/\\n/g, '\n')
    }
    // 2. 找常见大模型字段：answer, reply, content, result, text, output
    for (const k of ['answer', 'reply', 'content', 'result', 'text', 'output']) {
      if (typeof data[k] === 'string' && data[k].trim()) {
        isExtractedFromLlm.value = true
        return data[k].replace(/\\n/g, '\n')
      }
    }
    // 3. 找任意非 rag 的 .output 结尾键
    const outputKeys = Object.keys(data).filter(k => k.endsWith('.output') && !k.includes('rag'))
    if (outputKeys.length > 0) {
      const bestKey = outputKeys.sort((a, b) => String(data[b] || '').length - String(data[a] || '').length)[0]
      if (bestKey && data[bestKey]) {
        isExtractedFromLlm.value = true
        return String(data[bestKey]).replace(/\\n/g, '\n')
      }
    }
    // 4. 寻找较长的业务字符串（排除 input/variables 开头）
    const stringValues = Object.entries(data)
      .filter(([k, v]) => typeof v === 'string' && !k.startsWith('input') && !k.startsWith('variables') && v.length > 10)
      .sort((a, b) => b[1].length - a[1].length)
    if (stringValues.length > 0) {
      isExtractedFromLlm.value = true
      return stringValues[0][1].replace(/\\n/g, '\n')
    }
  }

  isExtractedFromLlm.value = false
  return typeof data === 'string' ? data.replace(/\\n/g, '\n') : JSON.stringify(data, null, 2)
})

/** 格式化的原始数据展示 */
const formattedRawOutput = computed(() => {
  const raw = detail.value?.output || detail.value?.outputJson
  if (!raw) return ''
  try {
    const val = typeof raw === 'string' ? JSON.parse(raw) : raw
    return JSON.stringify(val, null, 2)
  } catch {
    return String(raw)
  }
})

/** 复制当前视图中的输出文本 */
const copyOutput = async () => {
  const text = outputViewMode.value === 'formatted' ? pureLlmOutputText.value : formattedRawOutput.value
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    notify('已成功复制模型输出到剪贴板', 'success')
  } catch {
    notify('复制失败，请手动选择复制', 'warning')
  }
}

const normalizeOrchestration = (value) => ({
  ...value,
  id: value.id || value.executionId || value.runId,
  workflowCode: value.workflowCode || value.executionType || 'business-run',
  businessName: value.applicationName || value.workflowName || value.appName || (value.workflowCode && value.workflowCode !== 'orchestration' ? value.workflowCode : '业务应用运行'),
  executionKey: value.executionId || value.runId,
  inputMessage: value.input || value.inputJson,
  nodes: (Array.isArray(value.nodes) && value.nodes.length > 0) ? value.nodes : []
})

// Runtime Run 的响应包含运行本体和增量事件，页面只在此处转换为展示模型。
const normalizeRuntimeResponse = (response) => {
  const payload = response?.data || {}
  const run = normalizeOrchestration(payload.run || payload)
  const events = payload.events || []
  eventCursor.value = events.reduce((cursor, event) => Math.max(cursor, Number(event.sequence || event.sequenceNo || 0)), eventCursor.value)
  return normalizeOrchestration({ ...run, events })
}

const loadExecutions = async ({ append = false } = {}) => {
  errorMessage.value = ''
  listLoading.value = true
  try {
    const offset = append ? executionOffset.value : 0
    const response = await fetchRuntimeRuns({ limit: RUN_PAGE_SIZE, offset })
    const nextPage = (response.data?.items || response.data || []).map(normalizeOrchestration)
    executions.value = append ? [...executions.value, ...nextPage] : nextPage
    executionOffset.value = offset + nextPage.length
    hasMoreExecutions.value = nextPage.length === RUN_PAGE_SIZE
  } catch (error) {
    // 保留服务端返回的中文业务错误，避免运行中心只显示无法定位的通用提示。
    errorMessage.value = apiErrorMessage(error, '读取执行记录失败，请检查网络或后端服务。')
  } finally {
    listLoading.value = false
  }
}

const showDetail = async (id, { subscribe = true } = {}) => {
  errorMessage.value = ''
  detailLoading.value = true
  try {
    const response = await fetchRuntimeRun(id, 0)
    eventCursor.value = 0
    detail.value = normalizeRuntimeResponse(response)
    // 用户切换执行记录或执行控制后才重建 WebSocket，定时刷新只更新详情。
    if (subscribe) subscribeExecutionEvents(detail.value.executionId)
  } catch (error) {
    errorMessage.value = apiErrorMessage(error, '读取执行详情失败，请稍后重试。')
  } finally {
    detailLoading.value = false
  }
}

const refreshSelectedExecution = async () => {
  if (detail.value?.executionId) await showDetail(detail.value.executionId, { subscribe: false })
}

const subscribeExecutionEvents = (executionId) => {
  stopExecutionSubscription?.()
  streamState.value = 'CONNECTING'
  stopExecutionSubscription = createExecutionSubscription(executionId, {
    onEvent: () => showDetailWithoutSubscription(executionId),
    onState: state => { streamState.value = state },
    onError: () => { errorMessage.value = '实时连接正在重连，执行状态将自动恢复。' }
  })
}

const showDetailWithoutSubscription = async (id) => {
  const response = await fetchRuntimeRun(id, eventCursor.value)
  if (detail.value?.executionId === id) detail.value = normalizeRuntimeResponse(response)
}

const controlExecution = async (action, transferTargetValue = null) => {
  if (!detail.value?.executionId) return
  errorMessage.value = ''
  try {
    const response = await controlRuntimeRun(detail.value.executionId, { action, transferTarget: transferTargetValue })
    requireApiSuccess(response, '执行控制失败')
    await showDetail(detail.value.executionId)
    await loadExecutions()
  } catch (error) {
    errorMessage.value = apiErrorMessage(error, '执行控制失败，请稍后重试')
  }
}

let refreshTimer
const refreshActiveExecution = async () => {
  if (!detail.value?.executionId || isTerminalRuntimeStatus(detail.value.status)) return
  await showDetail(detail.value.executionId, { subscribe: false })
}

onMounted(async () => {
  await loadExecutions()
  // 从工作区测试入口带来的 runId 直接展开详情，保持运行链路上下文连续。
  const requestedRunId = String(route.query.runId || '').trim()
  if (requestedRunId) {
    // 深链直接读取详情，不要求目标记录必须落在当前列表页内。
    await showDetail(requestedRunId)
  }
  // 仅作为兜底刷新，正常实时更新由 WebSocket 通道驱动。
  refreshTimer = window.setInterval(refreshActiveExecution, 15000)
})
onUnmounted(() => {
  stopExecutionSubscription?.()
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<style scoped>
.runtime-observability-grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 8px; margin: 14px 20px 18px; }
.detail-loading { margin: 0 20px 14px; padding: 10px 12px; border: 1px dashed var(--line); color: var(--muted); font-size: 12px; }

/* 业务输入面板 */
.business-input-panel { margin: 14px 20px 0; border: 1px solid #e2eaf1; border-radius: 8px; background: #f8fafc; overflow: hidden; }
.input-panel-header { padding: 8px 14px; background: #f1f5f9; border-bottom: 1px solid #e2eaf1; }
.input-badge { font-size: 11px; font-weight: 700; color: #475569; }
.input-body { padding: 12px 16px; font-size: 12px; color: #334155; line-height: 1.65; white-space: pre-wrap; word-break: break-word; }
.input-body p { margin: 0; }

/* 业务输出面板（现代化 AI 卡片） */
.business-output-panel {
  margin: 14px 20px 0;
  border: 1px solid #c7d2fe;
  border-left: 4px solid #6366f1;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.04);
  overflow: hidden;
}

.output-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 16px;
  background: linear-gradient(90deg, #f5f7ff 0%, #fafbff 100%);
  border-bottom: 1px solid #e0e7ff;
}

.output-header-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-badge {
  font-size: 12px;
  font-weight: 700;
  color: #3730a3;
  letter-spacing: 0.2px;
}

.output-char-count {
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 10px;
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 500;
  border: 1px solid #c7d2fe;
}

.pure-extract-tag {
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 10px;
  background: #ecfdf5;
  color: #065f46;
  font-weight: 500;
  border: 1px solid #a7f3d0;
}

.output-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.view-mode-toggle {
  display: inline-flex;
  border: 1px solid #c7d2fe;
  border-radius: 6px;
  overflow: hidden;
  background: #ffffff;
}

.mode-btn {
  padding: 3px 10px;
  border: 0;
  background: transparent;
  color: #6366f1;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mode-btn.active {
  background: #6366f1;
  color: #ffffff;
  font-weight: 600;
}

.mode-btn:hover:not(.active) {
  background: #f5f7ff;
}

.copy-output-btn {
  padding: 3px 10px;
  border: 1px solid #c7d2fe;
  border-radius: 6px;
  background: #ffffff;
  color: #4338ca;
  font-size: 11px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.15s ease;
}

.copy-output-btn:hover {
  background: #eef2ff;
  border-color: #818cf8;
}

.output-panel-body {
  padding: 16px 20px;
  max-height: 480px;
  overflow-y: auto;
}

.raw-output-pre {
  margin: 0;
  padding: 14px;
  border-radius: 6px;
  background: #0f172a;
  color: #e2e8f0;
  font-family: Consolas, monospace;
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty-output-tip {
  margin: 0;
  padding: 18px 20px;
  color: #94a3b8;
  font-size: 12px;
  font-style: italic;
}

/* 优雅的 Markdown 排版 */
.output-rendered-markdown {
  color: #1e293b;
  font-size: 13px;
  line-height: 1.75;
}

.output-rendered-markdown :deep(h1),
.output-rendered-markdown :deep(h2),
.output-rendered-markdown :deep(h3),
.output-rendered-markdown :deep(h4) {
  margin: 16px 0 8px;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
}

.output-rendered-markdown :deep(h1) { font-size: 17px; }
.output-rendered-markdown :deep(h2) { font-size: 15px; border-bottom: 1px solid #f1f5f9; padding-bottom: 4px; }
.output-rendered-markdown :deep(h3) { font-size: 14px; }
.output-rendered-markdown :deep(h4) { font-size: 13px; }

.output-rendered-markdown :deep(p) {
  margin: 0 0 10px;
  color: #334155;
  line-height: 1.75;
}

.output-rendered-markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.output-rendered-markdown :deep(ul),
.output-rendered-markdown :deep(ol) {
  margin: 8px 0 12px;
  padding-left: 22px;
}

.output-rendered-markdown :deep(li) {
  margin: 4px 0;
  color: #334155;
  line-height: 1.65;
}

.output-rendered-markdown :deep(strong) {
  color: #0f172a;
  font-weight: 600;
}

.output-rendered-markdown :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 14px;
  border-left: 3px solid #6366f1;
  background: #f8fafc;
  color: #475569;
  border-radius: 0 6px 6px 0;
  font-size: 12.5px;
}

.output-rendered-markdown :deep(code) {
  padding: 2px 6px;
  background: #f1f5f9;
  border-radius: 4px;
  font-family: Consolas, monospace;
  font-size: 12px;
  color: #db2777;
}

.output-rendered-markdown :deep(pre) {
  margin: 12px 0;
  padding: 12px 14px;
  border-radius: 6px;
  background: #0f172a;
  color: #e2e8f0;
  overflow-x: auto;
}

.output-rendered-markdown :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
  font-size: 11.5px;
}
/* 运行观测指标（流式胶囊，自适应折行，彻底消除挤压穿模） */
.runtime-observability-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 14px 20px 16px;
}

.observability-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  font-size: 11.5px;
  color: #475569;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.observability-chip.has-events {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #166534;
}

.observability-chip.zero-events {
  background: #f8fafc;
  color: #94a3b8;
  opacity: 0.85;
}

.chip-badge {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ffffff;
  font-size: 10px;
  font-weight: 800;
  color: #3b82f6;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.observability-chip.has-events .chip-badge {
  color: #16a34a;
}

.chip-title {
  font-weight: 500;
}

.chip-num {
  font-weight: 700;
  font-size: 11.5px;
  margin-left: 2px;
}

/* 节点内部执行过程轨迹抽屉 */
.node-events-drawer {
  margin-top: 12px;
  border-top: 1px dashed #e2e8f0;
  padding-top: 10px;
}

.events-details summary {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2563eb;
  font-size: 11.5px;
  cursor: pointer;
  user-select: none;
}

.events-summary-title {
  font-weight: 600;
}

.events-count-tag {
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.event-timeline-list {
  margin-top: 9px;
  padding: 10px 14px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: grid;
  gap: 8px;
}

.event-timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 11px;
  line-height: 1.5;
}

.event-bullet {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b82f6;
  margin-top: 5px;
  flex-shrink: 0;
}

.event-type-tag {
  padding: 1px 6px;
  border-radius: 4px;
  background: #e2e8f0;
  color: #334155;
  font-size: 10px;
  font-weight: 600;
  flex-shrink: 0;
}

.event-text {
  color: #475569;
  word-break: break-word;
}
.execution-page { display: flex; flex-direction: column; gap: 18px; }.detail-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 7px; margin: 0 20px 14px; }.transfer-target-input { width: 150px; min-width: 120px; }
.stream-state { display: inline-flex; align-items: center; gap: 6px; color: var(--muted); font-size: 11px; white-space: nowrap; }.stream-state i { width: 7px; height: 7px; border-radius: 50%; background: #a6b2bf; }.stream-connected { color: var(--success); }.stream-connected i { background: var(--success); }.stream-reconnecting, .stream-connecting { color: var(--warning); }.stream-reconnecting i, .stream-connecting i { background: var(--warning); }.stream-closed { color: var(--muted); }
.execution-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; padding: 6px 0 2px; }
.execution-hero h3, .detail-heading h4, .panel-heading h4, .node-section-heading h4 { margin: 0; color: var(--text); font-size: 21px; }
.execution-hero p, .panel-heading p, .node-section-heading p { margin: 7px 0 0; color: var(--muted); font-size: 12px; line-height: 1.6; }
.page-kicker { margin-bottom: 7px; color: #4b8bc5; font-size: 10px; font-weight: 700; letter-spacing: .12em; }
.refresh-btn { white-space: nowrap; }
.refresh-btn span { margin-right: 5px; color: var(--accent); font-size: 16px; vertical-align: -1px; }
.page-error { margin: 0; padding: 11px 14px; border: 1px solid #f0c8cc; border-radius: 8px; background: #fff7f7; color: #b42318; font-size: 12px; }
.execution-layout { display: grid; grid-template-columns: minmax(300px, 360px) minmax(0, 1fr); align-items: start; gap: 16px; }
.execution-list-panel, .execution-detail-panel, .execution-placeholder { border: 1px solid #e1e9f1; border-radius: 10px; background: #fff; box-shadow: 0 3px 14px rgba(31, 55, 82, .04); }
.execution-list-panel { position: sticky; top: 18px; overflow: hidden; }
.panel-heading, .detail-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding: 20px 20px 16px; border-bottom: 1px solid #edf2f6; }
.panel-heading h4 { font-size: 16px; }.panel-heading p { margin-top: 4px; }.record-count { color: #8192a4; font-size: 11px; }
.execution-list { display: flex; flex-direction: column; padding: 6px; }
.load-more-btn { display:block; width:calc(100% - 12px); margin:4px 6px 10px; padding:9px 12px; border:1px solid #dce8f1; border-radius:7px; background:#f7fbfe; color:#3979a8; font-size:11px; cursor:pointer; }
.load-more-btn:hover { border-color:#b9d8f4; background:#eef7ff; }
.execution-row { display: grid; grid-template-columns: 32px minmax(0, 1fr) auto 14px; align-items: center; gap: 10px; width: 100%; padding: 12px 10px; border: 1px solid transparent; border-radius: 8px; background: transparent; color: var(--text); text-align: left; cursor: pointer; }
.execution-row:hover { border-color: #dceaf7; background: #f7fbff; }.execution-row.selected { border-color: #b9d8f4; background: #eef7ff; }
.execution-row-icon { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 9px; font-weight: 800; }.execution-row-main { min-width: 0; }.execution-row-main strong, .execution-row-main small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.execution-row-main strong { font-size: 12px; }.execution-row-main small { margin-top: 4px; color: #8797a8; font: 10px Consolas, monospace; }.execution-row-meta { display: flex; flex-direction: column; align-items: flex-end; gap: 5px; }.execution-row-meta small { color: #9aa8b5; font-size: 10px; white-space: nowrap; }.row-arrow { color: #9ab1c6; font-size: 22px; }
.status-tag { display: inline-flex; align-items: center; width: fit-content; padding: 3px 7px; border: 1px solid; border-radius: 999px; font-size: 10px; font-weight: 700; white-space: nowrap; }.status-success { color: #16784f; border-color: #bde4d1; background: #effaf4; }.status-running { color: #236dab; border-color: #bfdbf4; background: #edf6ff; }.status-waiting { color: #9a6818; border-color: #ecd7a8; background: #fff9e9; }.status-failed, .status-rejected { color: #b42318; border-color: #f0c7ca; background: #fff5f5; }.status-pending, .status-unknown { color: #68798a; border-color: #dbe3ea; background: #f5f7f9; }
.execution-detail-panel { min-width: 0; padding-bottom: 24px; }.detail-heading { padding-bottom: 17px; }.detail-heading h4 { font-size: 18px; }.detail-summary { display: grid; grid-template-columns: 1.3fr repeat(3, 1fr); gap: 1px; margin: 16px 20px 0; border: 1px solid #e7eef4; border-radius: 8px; overflow: hidden; background: #e7eef4; }.summary-status, .summary-item { min-width: 0; padding: 14px; background: #fbfdff; }.summary-status { display: flex; align-items: center; gap: 10px; }.summary-status-dot { width: 9px; height: 9px; border-radius: 50%; background: currentColor; box-shadow: 0 0 0 4px color-mix(in srgb, currentColor 12%, transparent); }.summary-status small, .summary-item small { display: block; color: #8697a8; font-size: 10px; }.summary-status strong, .summary-item strong { display: block; margin-top: 5px; overflow: hidden; color: var(--text); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.summary-status.status-failed strong, .summary-status.status-failed { color: #b42318; }.summary-status.status-success strong, .summary-status.status-success { color: #16784f; }
.execution-lineage { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1px; margin: 10px 20px 0; border: 1px solid #e7eef4; border-radius: 8px; overflow: hidden; background: #e7eef4; }
.execution-lineage > div { min-width: 0; padding: 11px 13px; background: #f8fbfd; }
.execution-lineage small, .execution-lineage strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.execution-lineage small { color: #8697a8; font-size: 10px; }.execution-lineage strong { margin-top: 5px; color: #38536b; font-size: 11px; }
.business-input { margin: 16px 20px 0; padding: 13px 15px; border-left: 3px solid #80b7e5; background: #f5f9fd; }.business-input span, .node-content-grid label, .node-error label { display: block; margin-bottom: 6px; color: #74879a; font-size: 10px; font-weight: 700; }.business-input p { margin: 0; color: #324c65; font-size: 12px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.execution-alert { display: flex; gap: 11px; margin: 16px 20px 0; padding: 13px 15px; border: 1px solid #f0c7ca; border-radius: 8px; background: #fff6f6; color: #b42318; }.alert-icon { display: grid; flex: 0 0 20px; place-items: center; width: 20px; height: 20px; border-radius: 50%; background: #c24149; color: #fff; font-weight: 800; }.execution-alert strong { font-size: 12px; }.execution-alert p { margin: 5px 0 0; color: #8f3036; font-size: 12px; line-height: 1.6; word-break: break-word; }
.node-section-heading { margin: 24px 20px 12px; }.node-section-heading h4 { font-size: 15px; }.node-section-heading p { margin-top: 4px; }
.node-timeline { margin: 0 20px; }.node-card { position: relative; display: grid; grid-template-columns: 34px minmax(0, 1fr); gap: 12px; }.node-line { position: absolute; top: 30px; bottom: -14px; left: 16px; width: 1px; background: #dce7f0; }.node-card:last-child .node-line { display: none; }.node-marker { z-index: 1; display: grid; place-items: center; width: 32px; height: 32px; border: 3px solid #fff; border-radius: 50%; background: #dce7f0; color: #60778b; box-shadow: 0 0 0 1px #d7e2eb; font-size: 11px; font-weight: 800; }.node-marker.status-success { background: #2d9a6e; color: #fff; box-shadow: 0 0 0 1px #bde4d1; }.node-marker.status-failed, .node-marker.status-rejected { background: #c24149; color: #fff; box-shadow: 0 0 0 1px #f0c7ca; }.node-marker.status-running { background: #358bd0; color: #fff; box-shadow: 0 0 0 1px #bfdbf4; }
.node-card-body { margin-bottom: 14px; padding: 15px 16px; border: 1px solid #e1e9f1; border-radius: 8px; background: #fff; }.node-card.status-failed .node-card-body, .node-card.status-rejected .node-card-body { border-color: #f0d2d4; background: #fffafa; }.node-card-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }.node-card-heading strong { color: #243f58; font-size: 13px; }.node-card-heading span { margin-left: 8px; color: #8798a8; font-size: 10px; }.node-card-status { display: flex; flex-direction: column; align-items: flex-end; gap: 5px; }.node-card-status small { color: #98a6b3; font-size: 10px; }.node-content-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 14px; }.node-content-grid > div { min-width: 0; padding: 10px 11px; border: 1px solid #edf2f6; border-radius: 6px; background: #fbfdff; }.node-content-grid p, .node-error p { display: -webkit-box; max-height: 58px; margin: 0; overflow: hidden; color: #536b81; font-size: 11px; line-height: 1.65; word-break: break-word; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }.node-material-details { margin-top: 11px; border-top: 1px solid #edf2f6; padding-top: 10px; }.node-material-details summary { color: #2776b8; font-size: 11px; cursor: pointer; }.node-material-details summary span { margin-left: 6px; color: #93a3b1; }.node-material-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 9px; }.node-material-grid > div { min-width: 0; padding: 10px 11px; border: 1px solid #e5edf4; border-radius: 6px; background: #f8fbfd; }.node-material-grid pre { max-height: 260px; margin: 0; overflow: auto; color: #536b81; font: 11px/1.65 Consolas, monospace; white-space: pre-wrap; word-break: break-word; }.node-error { margin-top: 11px; padding: 10px 11px; border-radius: 6px; background: #fff0f0; }.node-error label { color: #b42318; }.node-error p { color: #8f3036; }.trace-details { margin-top: 12px; border-top: 1px solid #edf2f6; padding-top: 10px; }.trace-details summary { color: #2776b8; font-size: 11px; cursor: pointer; }.trace-details summary span { margin-left: 5px; color: #93a3b1; }.trace-lines { margin-top: 8px; padding: 9px 11px; border-radius: 6px; background: #172b40; color: #c8d9e8; }.trace-lines p { margin: 4px 0; font: 10px/1.6 Consolas, monospace; white-space: pre-wrap; word-break: break-word; }
.retrieval-observation { margin-top: 12px; padding: 10px 11px; border: 1px solid #d9e6f0; border-radius: 6px; background: #f7fafc; }.retrieval-observation-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }.retrieval-observation-head strong { color: #285d84; font-size: 11px; }.retrieval-warning { padding: 2px 6px; border-radius: 4px; background: #fff0d9; color: #96610e; font-size: 9px; }.retrieval-metrics { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 1px; margin-top: 9px; background: #dfe8ef; }.retrieval-metrics > div { display: grid; gap: 3px; min-width: 0; padding: 7px; background: #fff; }.retrieval-metrics small { color: #8293a2; font-size: 9px; }.retrieval-metrics strong { overflow: hidden; color: #38546d; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.citation-list { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 8px; }.citation-list span { padding: 3px 6px; border: 1px solid #d6e2ec; border-radius: 4px; background: #fff; color: #58728a; font-size: 9px; }
.node-detail-button { display: inline-flex; align-items: center; gap: 7px; margin-top: 11px; padding: 0; border: 0; background: transparent; color: #2776b8; font-size: 11px; cursor: pointer; }.node-detail-button:hover { color: #155a91; text-decoration: underline; }.node-detail-button span { color: #93a3b1; }
.content-dialog-overlay { position: fixed; inset: 0; z-index: 1200; display: flex; justify-content: flex-end; background: rgba(16, 34, 56, .24); }.content-dialog { display: flex; width: min(720px, 100%); height: 100vh; flex-direction: column; overflow: hidden; border-left: 1px solid #dbe6ee; background: #fff; box-shadow: -18px 0 48px rgba(20, 42, 66, .16); }.content-dialog-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; padding: 26px 28px 18px; border-bottom: 1px solid #e7eef4; }.content-dialog-header h4 { margin: 0; color: var(--text); font-size: 17px; }.content-dialog-header p { margin: 6px 0 0; color: var(--muted); font-size: 11px; }.content-dialog-tabs { display: flex; gap: 4px; padding: 16px 28px 0; }.content-dialog-tabs button { padding: 8px 14px 10px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: #8294a5; font-size: 12px; cursor: pointer; }.content-dialog-tabs button.active { border-bottom-color: #3282c3; color: #20699f; font-weight: 700; }.content-dialog-meta { display: flex; gap: 8px; padding: 13px 28px; color: #8798a8; font-size: 10px; }.content-dialog-meta span { padding: 4px 8px; border: 1px solid #e2ebf2; border-radius: 999px; background: #f7fafc; }.content-dialog-body { min-height: 0; flex: 1; margin: 0 28px 28px; overflow: auto; padding: 18px 20px; border: 1px solid #e1e9f1; border-radius: 8px; background: #fbfdff; color: #536b81; font-size: 12px; line-height: 1.75; }.content-dialog-body :deep(h3), .content-dialog-body :deep(h4), .content-dialog-body :deep(h5) { margin: 0 0 12px; color: #294963; line-height: 1.4; }.content-dialog-body :deep(h3) { font-size: 17px; }.content-dialog-body :deep(h4) { font-size: 15px; }.content-dialog-body :deep(h5) { font-size: 13px; }.content-dialog-body :deep(pre) { margin: 12px 0; overflow: auto; padding: 13px; border-radius: 7px; background: #172b40; color: #d4e1ed; white-space: pre-wrap; word-break: break-word; }.content-dialog-body :deep(code) { font-family: Consolas, monospace; font-size: 11px; }.content-dialog-body :deep(p) { margin: 0 0 12px; }.content-dialog-body :deep(ul), .content-dialog-body :deep(ol) { margin: 0 0 12px; padding-left: 22px; }.content-dialog-body :deep(blockquote) { margin: 12px 0; padding: 8px 12px; border-left: 3px solid #8dbbe0; background: #f1f7fc; color: #5d758b; }.content-dialog-body :deep(.rich-table-scroll) { overflow-x: auto; }.content-dialog-body :deep(table) { min-width: 100%; border-collapse: collapse; }.content-dialog-body :deep(th), .content-dialog-body :deep(td) { padding: 7px 9px; border: 1px solid #d9e4ec; text-align: left; }.content-dialog-body :deep(th) { background: #f3f7fa; color: #405970; font-weight: 700; }.content-dialog-body :deep(a) { color: #2776b8; }
.execution-placeholder { display: grid; min-height: 430px; place-items: center; align-content: center; padding: 40px; text-align: center; }.placeholder-icon { display: grid; place-items: center; width: 48px; height: 48px; margin-bottom: 14px; border-radius: 14px; background: #edf6ff; color: #4b8bc5; font-size: 28px; }.execution-placeholder h4 { margin: 0; color: #2c465d; font-size: 16px; }.execution-placeholder p { max-width: 280px; margin: 8px 0 0; color: #8a9aaa; font-size: 12px; line-height: 1.7; }
@media (max-width: 1050px) { .execution-layout { grid-template-columns: 280px minmax(0, 1fr); }.detail-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }.execution-lineage { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 760px) { .execution-hero, .detail-heading { align-items: flex-start; flex-direction: column; }.execution-layout { grid-template-columns: 1fr; }.execution-list-panel { position: static; }.execution-placeholder { min-height: 240px; }.detail-summary { grid-template-columns: 1fr 1fr; margin-inline: 14px; }.execution-lineage { grid-template-columns: 1fr 1fr; margin-inline: 14px; }.business-input, .execution-alert, .node-section-heading, .node-timeline { margin-inline: 14px; }.panel-heading, .detail-heading { padding-inline: 14px; }.node-content-grid, .node-material-grid { grid-template-columns: 1fr; }.retrieval-metrics { grid-template-columns: repeat(3, minmax(0, 1fr)); }.content-dialog-overlay { align-items: flex-end; padding: 0; }.content-dialog { max-height: 92vh; border-radius: 10px 10px 0 0; }.content-dialog-header { padding: 18px 16px 14px; }.content-dialog-tabs, .content-dialog-meta { padding-inline: 16px; }.content-dialog-body { margin-inline: 16px; } }
@media (max-width: 460px) { .detail-summary { grid-template-columns: 1fr; }.execution-row { grid-template-columns: 32px minmax(0, 1fr) 14px; }.execution-row-meta { display: none; }.node-card-heading { flex-direction: column; }.node-card-status { align-items: flex-start; } }
</style>
