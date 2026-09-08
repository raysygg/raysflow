<template>
  <div class="page-grid chat-console-page" :class="{ 'workflow-mode': isWorkflowAgent, 'workflow-status-active': workflowStatusVisible }">
    <ResourceContextRail label="实时对话应用" :name="currentAgentName" state="运行中" hint="选择应用后开始对话或恢复历史会话" mark="C" />
    <div class="chat-shell">
      <!-- 顶部工具条 -->
      <div class="chat-topbar">
        <div class="chat-topbar-left">
          <div class="topbar-title">
            <div class="topbar-kicker">实时对话</div>
          </div>
        </div>
        <div class="chat-topbar-right">
          <!-- 顶部应用切换 Trigger -->
          <div class="chat-app-selector">
            <button
              type="button"
              class="current-app-trigger"
              :class="{ open: appPickerOpen }"
              @click="appPickerOpen = !appPickerOpen"
            >
              <span class="trigger-icon">🤖</span>
              <span class="trigger-meta">
                <span class="trigger-label">当前应用</span>
                <strong class="trigger-name">{{ currentAgentName }}</strong>
              </span>
              <span class="trigger-switch-badge">
                <span>切换应用</span>
                <i class="trigger-arrow" :class="{ active: appPickerOpen }">▾</i>
              </span>
            </button>

            <!-- 浮动 Popover 微型应用矩阵面板 -->
            <div v-if="appPickerOpen" class="app-picker-popover-overlay" @click.self="appPickerOpen = false">
              <div class="app-picker-popover" @click.stop>
                <div class="popover-head">
                  <div class="popover-title-row">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="popover-title-icon">
                      <rect x="3" y="3" width="7" height="7"></rect>
                      <rect x="14" y="3" width="7" height="7"></rect>
                      <rect x="14" y="14" width="7" height="7"></rect>
                      <rect x="3" y="14" width="7" height="7"></rect>
                    </svg>
                    <h4>选择业务应用</h4>
                    <span class="popover-count-badge">{{ agents.length }} 个可用</span>
                  </div>
                  <button class="popover-close-btn" title="关闭" @click="appPickerOpen = false">×</button>
                </div>

                <!-- 搜索框 (应用较多时展示) -->
                <div v-if="agents.length > 4" class="popover-search">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="search-icon">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                  </svg>
                  <input
                    v-model="appSearchQuery"
                    type="text"
                    placeholder="搜索应用名称..."
                    class="popover-search-input"
                  />
                </div>

                <!-- 应用卡片矩阵 -->
                <div class="popover-grid">
                  <div
                    v-for="a in filteredAgents"
                    :key="a.id"
                    class="app-matrix-card"
                    :class="{ active: Number(selectedAgentId) === Number(a.id) }"
                    @click="selectAgentFromMatrix(a.id)"
                  >
                    <div class="matrix-card-icon">🤖</div>
                    <div class="matrix-card-info">
                      <strong class="matrix-card-name">{{ a.name || a.agentName }}</strong>
                      <span class="matrix-card-desc">{{ a.description || '智能对话与业务自动化处理' }}</span>
                    </div>
                    <div v-if="Number(selectedAgentId) === Number(a.id)" class="matrix-card-check">
                      ✓
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <span v-if="applicationContext" class="application-context-badge">历史上下文 · {{ applicationContextLabel }}</span>
          
          <button type="button" class="chat-history-btn" title="查看历史会话" @click="historyOpen = true">
            <svg class="history-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
            <span>历史会话</span>
            <em v-if="sessionsForCurrentApp.length" class="history-badge-count">{{ sessionsForCurrentApp.length }}</em>
          </button>

          <button type="button" class="chat-new-btn" title="开启新对话" @click="resetSession">
            <i>＋</i>新对话
          </button>
        </div>
      </div>

      <!-- 流程进度 Banner（独立于对话列表外） -->
      <div v-if="workflowStatusVisible" class="workflow-status-banner" :class="workflowStatusClass(workflowState)" aria-live="polite">
        <div class="workflow-status-banner-head">
          <div class="status-summary">
            <span class="status-dot"></span>
            <div class="status-text-group">
              <span class="status-kicker">流程进度</span>
              <strong>{{ currentAgentName }}</strong>
            </div>
            <span class="workflow-status-pill">{{ workflowStatusLabel(workflowState) }}</span>
          </div>
          <div class="status-current-step">
            <small>当前节点</small>
            <strong>{{ currentNodeLabel }}</strong>
          </div>
          <button
            v-if="workflowDetail?.nodes?.length"
            type="button"
            class="toggle-graph-btn"
            @click="showWorkflowDetail = !showWorkflowDetail"
          >
            <span>{{ showWorkflowDetail ? '收起节点图' : '展开节点图' }}</span>
            <i :class="{ open: showWorkflowDetail }">▾</i>
          </button>
        </div>
        <div v-if="showWorkflowDetail && workflowDetail?.nodes?.length" class="workflow-graph" role="list" aria-label="工作流节点执行状态">
          <div v-for="(node, index) in workflowDetail.nodes" :key="node.id || index" class="workflow-graph-step">
            <div class="workflow-graph-node" role="listitem" :class="workflowStatusClass(node.status)">
              <div class="workflow-graph-icon"><span v-if="['COMPLETED', 'SUCCESS'].includes(node.status)">✓</span><span v-else-if="['FAILED', 'REJECTED'].includes(node.status)">!</span><span v-else-if="['RUNNING', 'RETRYING'].includes(node.status)" class="workflow-spinner"></span><span v-else>·</span></div>
              <strong>{{ workflowNodeName(node) }}</strong><small>{{ workflowNodeStatusLabel(node.status) }}</small>
            </div>
            <div v-if="index < workflowDetail.nodes.length - 1" class="workflow-graph-connector" :class="{ complete: ['COMPLETED', 'SUCCESS'].includes(node.status) }" aria-hidden="true"><span>›</span></div>
          </div>
        </div>
        <p v-if="workflowState === 'FAILED' || workflowState === 'REJECTED'" class="workflow-status-error">流程执行异常，请前往流程执行中心查看详情</p>
      </div>

      <!-- 主聊天气泡区 -->
      <div class="chat-stage">
        <div class="chat-bubbles-container" ref="chatContainer">

          <!-- 空态：TRAE 风格居中欢迎 -->
          <div v-if="messages.length === 0" class="chat-empty">
            <div class="empty-logo-wrap">
              <div class="empty-logo">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 2L3 7v6c0 5.1 3.6 9.8 8.4 11L12 25l.6-1C17.4 22.8 21 18.1 21 13V7l-9-5z" stroke="currentColor" stroke-width="1.6"></path>
                  <path d="M16 10.2c-.4.8-1.2 1.4-2.1 1.7.2-.6.3-1.3.1-2.1.6.2 1.4.6 1.9.7.4-1.3-.3-2.9-1.6-3.4.7-.4 1.6-.2 2.2.3.3-.3.5-.8.6-1.3.8.7 1.1 2 .6 3.1-.1.4-.4.9-.7 1" fill="currentColor" opacity=".35"></path>
                </svg>
              </div>
              <h3>你好，我是 <span>{{ currentAgentName }}</span></h3>
              <div class="empty-quick">
                <button class="chip-btn" @click="quickFill('帮我审查以下这份合同的付款条款，并给出修订建议')">
                  <svg class="chip-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="9" y1="13" x2="15" y2="13"></line><line x1="9" y1="17" x2="13" y2="17"></line></svg>
                  <span>审查合同付款条款</span>
                </button>
                <button class="chip-btn" @click="quickFill('从知识库中检索 Q3 人员招聘制度，并整理 5 条关键要点')">
                  <svg class="chip-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path><line x1="8" y1="7" x2="16" y2="7"></line><line x1="8" y1="11" x2="14" y2="11"></line></svg>
                  <span>检索 HR 招聘制度</span>
                </button>
                <button class="chip-btn" @click="quickFill('汇总最近一周的业务运行异常，给出处理优先级')">
                  <svg class="chip-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
                  <span>汇总运行异常</span>
                </button>
                <button class="chip-btn" @click="quickFill('根据多水源凝胶综述文档，给出产品研发 3 条关键启发')">
                  <svg class="chip-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 2h6v4l4 8H5l4-8V2z"></path><path d="M5 14h14"></path><path d="M12 14v8"></path><circle cx="8" cy="20" r="1"></circle><circle cx="16" cy="20" r="1"></circle></svg>
                  <span>分析文献并给出启发</span>
                </button>
              </div>
            </div>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="chat-row"
            :class="msg.role.toLowerCase()"
          >
            <div class="avatar" :class="msg.role.toLowerCase()">
              <span v-if="msg.role === 'USER'">{{ (nickname || 'U').slice(0,1).toUpperCase() }}</span>
              <span v-else>{{ (currentAgentName || 'A').slice(0,1).toUpperCase() }}</span>
            </div>
            <div class="chat-bubble">
              <div class="chat-bubble-sender">
                <span v-if="msg.role === 'USER'">{{ nickname }}</span>
                <span v-else>{{ currentAgentName }}</span>
                <em>{{ formatSenderTime(index) }}</em>
              </div>
              <div class="chat-text rich-content" v-html="renderMarkdown(msg.text)"></div>
            </div>
          </div>

          <!-- Streaming assistant bubble -->
          <div v-if="streamingText" class="chat-row assistant">
            <div class="avatar assistant"><span>{{ (currentAgentName || 'A').slice(0,1).toUpperCase() }}</span></div>
            <div class="chat-bubble">
              <div class="chat-bubble-sender">
                <span>{{ currentAgentName }}</span>
                <em>{{ formatSenderTime(messages.length) }}</em>
              </div>
              <div class="chat-text rich-content" v-html="renderMarkdown(streamingText)"></div>
            </div>
          </div>

          <div v-if="requestPending && !streamingText" class="chat-row assistant">
            <div class="avatar assistant"><span>{{ (currentAgentName || 'A').slice(0,1).toUpperCase() }}</span></div>
            <div class="chat-processing-indicator" aria-live="polite">
              <span class="processing-dots"><i></i><i></i><i></i></span>
              <div><strong>{{ processingLabel }}</strong><small>已收到请求，正在等待执行结果</small></div>
            </div>
          </div>

          <!-- Trace log indicator -->
          <div v-if="traceMsg && !isWorkflowAgent" class="chat-row trace">
            <div class="avatar trace"><span>●</span></div>
            <div class="chat-bubble trace-bubble">
              <div class="chat-text">{{ traceMsg }}</div>
            </div>
          </div>
          <!-- 错误与系统提示卡片 -->
          <div v-if="runtimeError" class="system-notice-card">
            <div class="system-notice-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
            </div>
            <div class="system-notice-content">
              <div class="system-notice-title">系统提示</div>
              <div class="system-notice-body">{{ runtimeError }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 历史会话轻量提示条 -->
      <div v-if="workflowHistorySession" class="workflow-history-notice">
        <span>当前为历史工作流会话，已结束，不可继续输入</span>
      </div>

      <!-- Composer -->
      <div v-if="!isWorkflowAgent || !workflowLocked" class="chat-composer-wrap">
        <div class="chat-composer" :class="{ disabled: workflowLocked || !selectedAgentId }">
          <textarea
            v-model="inputMsg"
            @keydown.enter.exact.prevent="sendMessage"
            rows="1"
            class="composer-input"
            autocomplete="off"
            spellcheck="false"
            :disabled="!selectedAgentId || workflowLocked || loadingSession"
            :placeholder="workflowLocked ? '该工作流已结束，请开启新会话后再次运行' : '输入你的业务问题，Enter 发送，Shift+Enter 换行...'"
            ref="composerInput"
            @input="autoResizeComposer"
          ></textarea>
          <button
            @click="sendMessage"
            class="composer-send"
            :disabled="!selectedAgentId || !inputMsg.trim() || workflowLocked"
            title="发送"
          >
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M22 2L11 13" stroke="currentColor" stroke-width="2" stroke-linecap="round"></path>
              <path d="M22 2L15 22l-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"></path>
            </svg>
          </button>
        </div>
      </div>
    </div>
    <div v-if="isWorkflowAgent" class="workflow-input-lock"><strong>当前为工作流模式</strong><p>工作流根据节点配置自动推进，历史工作流不可继续输入。请开启新会话重新运行。</p></div>
  <div v-if="historyOpen" class="history-drawer-overlay" @click.self="historyOpen = false">
    <aside class="history-drawer">
      <div class="history-drawer-head">
        <div class="drawer-title-wrap">
          <div class="drawer-title-row">
            <svg class="drawer-head-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
            <h3>历史会话</h3>
            <span v-if="sessionsForCurrentApp.length" class="drawer-head-badge">{{ sessionsForCurrentApp.length }}</span>
          </div>
          <p class="drawer-subhead">仅加载当前账号的个人历史对话 · 按需分页</p>
        </div>
        <button class="icon-close-btn" title="关闭" @click="historyOpen = false">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>

      <div v-if="runtimeConversationId" class="runtime-session-actions">
        <div class="actions-head">
          <span class="active-dot"></span>
          <span>当前活跃会话操作</span>
        </div>
        <div class="actions-btn-group">
          <button type="button" class="ghost-btn" @click="branchConversation" title="创建会话分支">
            <i>⑂</i>分支
          </button>
          <button type="button" class="ghost-btn" @click="exportConversation" title="导出当前会话">
            <i>⤓</i>导出
          </button>
          <button type="button" class="ghost-btn" @click="clearConversation" title="清理会话上下文">
            <i>↺</i>清理
          </button>
          <button type="button" class="danger-btn" @click="archiveConversation" title="归档当前会话">
            <i>✕</i>归档
          </button>
        </div>
      </div>

      <div v-if="relatedRuns.length" class="related-runs">
        <strong class="section-label">关联工作流运行</strong>
        <RouterLink v-for="run in relatedRuns" :key="run.executionId" :to="{ path: '/workflow-executions', query: { appId: run.appId, runId: run.executionId } }" class="related-run-row">
          <span><b>{{ runtimeStatusLabel(run.status) }}</b><small>{{ formatSessionTime(run.startedAt) }}</small></span>
          <span class="text-link">查看详情 →</span>
        </RouterLink>
      </div>

      <div class="drawer-body">
        <div v-if="sessionsForCurrentApp.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
          <p>暂无历史会话记录</p>
          <small>开启新对话后即可自动记录于此</small>
        </div>
        <div v-else class="history-drawer-list">
          <button
            v-for="item in visibleSessions"
            :key="item.id"
            class="session-card"
            :class="{ active: item.id === sessionId || (item.conversationId && item.conversationId === runtimeConversationId) }"
            @click="selectSession(item)"
          >
            <div class="session-card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
            </div>
            <div class="session-card-main">
              <strong class="session-card-title">{{ item.name || '未命名对话' }}</strong>
              <div class="session-card-meta">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="meta-clock-icon">
                  <circle cx="12" cy="12" r="10"></circle>
                  <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
                <span>{{ formatSessionTime(item.updatedAt || item.createdAt) }}</span>
              </div>
            </div>
            <div class="session-card-arrow">
              <span>›</span>
            </div>
          </button>

          <div v-if="hasMoreSessions" class="load-more-wrap">
            <button type="button" class="load-more-btn" @click="loadMoreSessions">
              <span>查看更多历史对话</span>
              <small>(剩余 {{ remainingSessionsCount }} 条)</small>
              <i class="load-more-icon">↓</i>
            </button>
          </div>
          <div v-else-if="showNoMoreNotice" class="no-more-notice">
            已展示全部 {{ sessionsForCurrentApp.length }} 条个人历史对话
          </div>
        </div>
      </div>
    </aside>
  </div>
</div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import SectionCard from '../components/SectionCard.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import { subscribeExecutionEvents } from '../api/execution-events'
import { apiData, apiErrorMessage, isApiSuccess, isRuntimeWaitingStatus, isTerminalRuntimeStatus, requireApiSuccess, runtimeStatusLabel } from '../api/contracts'
import { renderMarkdown } from '../utils/markdown'
import { notify } from '../utils/feedback'
import { archiveRuntimeConversation, branchRuntimeConversation, clearRuntimeConversation, createRuntimeConversation, exportRuntimeConversation, fetchRuntimeApplications, fetchRuntimeConversations, fetchRuntimeEntrypoints, fetchRuntimeRun, invokeRuntimeEntrypoint } from '../api/runtime'

const nickname = ref(localStorage.getItem('nickname') || 'Guest')
const route = useRoute()
const tenantName = ref(localStorage.getItem('tenantName') || 'Acme')
const selectedAgentId = ref(null)
const agents = ref([])
const sessionId = ref(null)
const activeRunId = ref(null)
const sessions = ref([])
const relatedRuns = ref([])
const historyOpen = ref(false)
const workflowDetail = ref(null)
const showWorkflowDetail = ref(true)
const applicationContext = ref(null)
const workflowState = ref('IDLE')
const loadingSession = ref(false)

const messages = ref([])
const inputMsg = ref('')
const streamingText = ref('')
const traceMsg = ref('')
const requestPending = ref(false)
const chatContainer = ref(null)
const runtimeConversationId = ref(null)
const runtimeError = ref('')

let stopExecutionSubscription = null

const appPickerOpen = ref(false)
const appSearchQuery = ref('')

const currentAgentName = computed(() => {
  const agent = agents.value.find(a => a.id === selectedAgentId.value)
  return agent ? (agent.name || agent.agentName) : '业务应用'
})
const filteredAgents = computed(() => {
  if (!appSearchQuery.value.trim()) return agents.value
  const query = appSearchQuery.value.toLowerCase().trim()
  return agents.value.filter(a => (a.name || a.agentName || '').toLowerCase().includes(query))
})

const selectAgentFromMatrix = (agentId) => {
  switchAgent(agentId)
  appPickerOpen.value = false
  appSearchQuery.value = ''
}

const sessionsForCurrentApp = computed(() => sessions.value.filter(item => !selectedAgentId.value || Number(item.appId) === Number(selectedAgentId.value)))

const SESSION_PAGE_SIZE = 6
const sessionDisplayLimit = ref(SESSION_PAGE_SIZE)
const visibleSessions = computed(() => sessionsForCurrentApp.value.slice(0, sessionDisplayLimit.value))
const hasMoreSessions = computed(() => sessionsForCurrentApp.value.length > sessionDisplayLimit.value)
const remainingSessionsCount = computed(() => sessionsForCurrentApp.value.length - visibleSessions.value.length)
const showNoMoreNotice = computed(() => !hasMoreSessions.value && sessionsForCurrentApp.value.length > SESSION_PAGE_SIZE)
const loadMoreSessions = () => {
  sessionDisplayLimit.value += SESSION_PAGE_SIZE
}
const isWorkflowAgent = computed(() => Boolean(selectedAgentId.value))
const workflowLocked = computed(() => isWorkflowAgent.value && (isTerminalRuntimeStatus(workflowState.value) || isRuntimeWaitingStatus(workflowState.value) || workflowState.value === 'RUNNING'))
const workflowHistorySession = computed(() => isWorkflowAgent.value && activeRunId.value && isTerminalRuntimeStatus(workflowState.value))
const workflowStatusVisible = computed(() => isWorkflowAgent.value && (workflowDetail.value || workflowState.value !== 'IDLE'))
const processingLabel = computed(() => {
  if (loadingSession.value) return '正在恢复会话'
  if (isWorkflowAgent.value) {
    if (workflowState.value === 'WAITING_APPROVAL') return '等待人工审批'
    if (workflowState.value === 'FAILED' || workflowState.value === 'REJECTED') return '工作流执行异常'
    return '工作流执行中'
  }
  return '模型正在思考'
})
const workflowNodeName = (node) => {
  if (!node) return '暂无'
  const names = { START: '开始', USER_INPUT: '用户输入', DIRECT_REPLY: '直接回复', RAG: '知识检索', LLM: '模型生成', CONDITION: '条件分支', HUMAN: '人工审批', END: '结束' }
  return node.name || names[node.nodeType] || node.nodeType || node.nodeId || '未命名节点'
}
const currentNodeLabel = computed(() => {
  const node = workflowDetail.value?.nodes?.find(item => ['RUNNING', 'WAITING_APPROVAL', 'RETRYING'].includes(item.status))
  return node ? workflowNodeName(node) : (workflowDetail.value?.status === 'COMPLETED' ? '已完成' : '暂无')
})
const workflowStatusLabel = (status) => status === 'WAITING_APPROVAL' ? '等待人工审批' : runtimeStatusLabel(status)
const workflowStatusClass = (status) => ({ RUNNING: 'workflow-running', WAITING_APPROVAL: 'workflow-waiting', COMPLETED: 'workflow-success', SUCCEEDED: 'workflow-success', SUCCESS: 'workflow-success', FAILED: 'workflow-failed', REJECTED: 'workflow-failed' }[status] || 'workflow-idle')
const workflowNodeStatusLabel = (status) => ({ RUNNING: '运行中', WAITING_APPROVAL: '等待审批', COMPLETED: '已完成', SUCCESS: '成功', FAILED: '失败', REJECTED: '已驳回', PENDING: '等待执行', RETRYING: '重试中' }[status] || status || '未开始')
const formatSessionTime = (value) => value ? new Date(value).toLocaleString('zh-CN') : ''
const parseTrace = (value) => {
  if (!value) return []
  try { const parsed = JSON.parse(value); return Array.isArray(parsed) ? parsed : [] } catch { return [] }
}

const composerInput = ref(null)
const quickFill = (text) => {
  if (!composerInput.value) { inputMsg.value = text; return }
  inputMsg.value = text
  composerInput.value.focus()
  nextTick(() => { autoResizeComposer() })
}
const formatSenderTime = (/* index */) => {
  const now = new Date()
  return `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`
}
const autoResizeComposer = () => {
  const el = composerInput.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

// 消息新增或实时输出后，将对话区域滚动到最新内容。
const scrollToBottom = () => {
  nextTick(() => {
    const container = chatContainer.value
    if (container) container.scrollTop = container.scrollHeight
  })
}

const loadAgents = async () => {
  try {
    const res = await fetchRuntimeApplications()
    const list = apiData(res, [])
    if (list.length > 0) {
      agents.value = list
      const requestedAgentId = Number(route.query.appId)
      selectedAgentId.value = list.some(agent => Number(agent.id) === requestedAgentId)
        ? requestedAgentId
        : Number(list[0].id)
    }
  } catch (e) {
    console.error('加载 Agent 列表失败', e)
  }
}

onMounted(async () => {
  await loadAgents()
  await loadSessions()
})

onUnmounted(() => {
  stopExecutionSubscription?.()
})

const ensureRuntimeConversation = async () => {
  const appId = Number(selectedAgentId.value)
  if (!appId || runtimeConversationId.value) return
  try {
    const selectedApplication = agents.value.find(item => item.id === appId)
    const response = await createRuntimeConversation({ appId, versionId: selectedApplication?.currentVersionId || null })
    if (isApiSuccess(response)) runtimeConversationId.value = response.data?.conversationId || response.data?.id || null
  } catch (error) {
    // 会话写入失败不阻断 Runtime Run 提交，但本次消息不会进入历史会话，需要在界面提示用户。
    console.warn('创建 Runtime 会话失败，继续提交 Runtime Run', error)
  }
}

const archiveConversation = async () => {
  if (!runtimeConversationId.value) return
  try {
    await archiveRuntimeConversation(runtimeConversationId.value)
    notify('Runtime 会话已归档', 'success')
    resetSession()
  } catch (error) {
    notify(error?.message || '会话归档失败', 'error')
  }
}

const clearConversation = async () => {
  if (!runtimeConversationId.value) return
  try {
    await clearRuntimeConversation(runtimeConversationId.value)
    resetSession()
    await loadSessions()
    notify('会话上下文已清理，关联 Run 仍可查看', 'success')
  } catch (error) {
    notify(apiErrorMessage(error, '清理会话上下文失败'), 'error')
  }
}

const branchConversation = async () => {
  if (!runtimeConversationId.value) return
  try {
    const response = await branchRuntimeConversation(runtimeConversationId.value)
    requireApiSuccess(response, '创建会话分支失败')
    runtimeConversationId.value = response.data?.conversationId || response.data?.id || runtimeConversationId.value
    messages.value = []
    notify('Runtime 会话分支已创建', 'success')
  } catch (error) {
    notify(error?.message || '创建会话分支失败', 'error')
  }
}

const exportConversation = async () => {
  if (!runtimeConversationId.value) return
  try {
    const response = await exportRuntimeConversation(runtimeConversationId.value)
    const conversation = requireApiSuccess(response, '会话导出失败')
    const blob = new Blob([JSON.stringify(conversation || {}, null, 2)], { type: 'application/json;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `runtime-conversation-${runtimeConversationId.value}.json`
    link.click()
    URL.revokeObjectURL(url)
    notify('Runtime 会话已导出', 'success')
  } catch (error) {
    notify(error?.message || '会话导出失败', 'error')
  }
}

const loadSessions = async () => {
  try {
    const response = await fetchRuntimeConversations()
    sessions.value = apiData(response, [])
  } catch (error) {
    notify(apiErrorMessage(error, '读取历史会话失败'), 'error')
  }
}

const loadExecutionDetail = async (id) => {
  if (!id) return
  const response = await fetchRuntimeRun(id)
  if (isApiSuccess(response)) {
    workflowDetail.value = response.data?.run || response.data
    workflowState.value = workflowDetail.value.status
    if (workflowDetail.value.status === 'SUCCEEDED') {
      const rawOutput = workflowDetail.value.output
      let output = rawOutput
      if (typeof rawOutput === 'string') {
        try { output = JSON.parse(rawOutput) } catch { output = rawOutput }
      }
      const text = typeof output === 'string' ? output : (output?.message || output?.output || '')
      // WebSocket 重连补发可能重复到达节点事件；用执行实例 ID 保证结果只插入一次。
      if (text && !messages.value.some(message => message.executionId === id)) {
        messages.value.push({ role: 'ASSISTANT', text: String(text), executionId: id })
        scrollToBottom()
      }
      streamingText.value = ''
    }
    if (isTerminalRuntimeStatus(workflowDetail.value.status) || isRuntimeWaitingStatus(workflowDetail.value.status)) requestPending.value = false
    if (isTerminalRuntimeStatus(workflowDetail.value.status) || isRuntimeWaitingStatus(workflowDetail.value.status)) stopExecutionSubscription?.()
  }
}

const trackExecution = (id) => {
  stopExecutionSubscription?.()
  stopExecutionSubscription = subscribeExecutionEvents(id, {
    onEvent: (event) => {
      if (event?.eventType === 'MODEL_DELTA') {
        try {
          const payload = JSON.parse(event.payloadJson || '{}')
          if (payload.delta) {
            streamingText.value += payload.delta
            scrollToBottom()
          }
        } catch {
          // 增量事件损坏时保留最终结果查询，不让实时界面中断。
        }
      }
      loadExecutionDetail(id)
    },
    onError: (error) => console.warn('工作流实时追踪重连中', error)
  })
}

const switchAgent = (agentId) => {
  if (Number(selectedAgentId.value) === Number(agentId)) return
  selectedAgentId.value = Number(agentId)
  resetSession()
}

const applicationContextLabel = computed(() => {
  if (!applicationContext.value) return ''
  const ctx = applicationContext.value
  const parts = []
  if (ctx.versionId) parts.push(`v_${ctx.versionId}`)
  if (ctx.status) parts.push(runtimeStatusLabel(ctx.status))
  return parts.length > 0 ? parts.join(' · ') : '活跃'
})

const selectSession = async (session) => {
  const id = session.conversationId || session.id
  historyOpen.value = false
  if (session.appId) {
    selectedAgentId.value = Number(session.appId)
  } else if (session.agentId) {
    selectedAgentId.value = Number(session.agentId)
  }
  loadingSession.value = true
  try {
    const response = await exportRuntimeConversation(id)
    requireApiSuccess(response, '读取会话失败。')
    sessionId.value = id
    activeRunId.value = null
    const aggregate = response.data || {}
    applicationContext.value = aggregate.conversation || null
    relatedRuns.value = aggregate.runs || []
    messages.value = (aggregate.messages || []).map(item => ({
      role: item.roleCode || 'ASSISTANT',
      text: parseRuntimeContent(item.contentJson),
      executionId: item.executionId
    }))
    workflowDetail.value = null
    workflowState.value = 'IDLE'
    const latestExecutionId = [...(aggregate.messages || [])].reverse().find(item => item.executionId)?.executionId
    if (latestExecutionId) {
      activeRunId.value = latestExecutionId
      const executionResponse = await fetchRuntimeRun(latestExecutionId)
      workflowDetail.value = executionResponse.data?.run || null
      workflowState.value = workflowDetail.value?.status || 'IDLE'
      if (workflowState.value === 'RUNNING') trackExecution(latestExecutionId)
    }
    scrollToBottom()
  } catch (error) {
    notify(apiErrorMessage(error, '读取历史会话失败'), 'error')
  } finally {
    loadingSession.value = false
  }
}

// Runtime 消息允许保存纯文本或 JSON，展示层只负责还原用户可读内容。
const parseRuntimeContent = (value) => {
  if (value == null) return ''
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return typeof parsed === 'string' ? parsed : (parsed.text || parsed.message || JSON.stringify(parsed))
  } catch {
    return String(value)
  }
}

const sendMessage = async () => {
  const selectedApplication = agents.value.find(item => item.id === selectedAgentId.value)
  if (!inputMsg.value.trim() || workflowLocked.value || !selectedApplication || requestPending.value) {
    return
  }

  const query = inputMsg.value
  messages.value.push({ role: 'USER', text: query })
  await ensureRuntimeConversation()
  inputMsg.value = ''
  requestPending.value = true
  traceMsg.value = '[系统] 消息已发送，等待节点调度...'
  workflowDetail.value = null
  workflowState.value = isWorkflowAgent.value ? 'QUEUED' : 'IDLE'
  scrollToBottom()

  try {
    const entrypointsResponse = await fetchRuntimeEntrypoints(selectedApplication.id)
    const conversationEntrypoint = apiData(entrypointsResponse, []).find(item => item.type === 'CONVERSATION' && item.enabled)
    if (!conversationEntrypoint) throw new Error('当前应用没有启用的对话入口')
    const response = await invokeRuntimeEntrypoint(conversationEntrypoint.id, {
      idempotencyKey: `conversation-${runtimeConversationId.value}-${Date.now()}`,
      inputs: { query }
    })
    const outputText = response?.data?.output ? (typeof response.data.output === 'object' ? JSON.stringify(response.data.output, null, 2) : String(response.data.output)) : (response?.data?.text || '处理完成')
    messages.value.push({ role: 'ASSISTANT', text: outputText })
  } catch (e) {
    runtimeError.value = e.message || '发送失败'
  } finally {
    requestPending.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
/* ===== Design Tokens — Cosmic Indigo ===== */
.chat-console-page {
  --c-primary: #4f46e5;
  --c-primary-light: #6366f1;
  --c-primary-dark: #3730a3;
  --c-primary-grad: linear-gradient(135deg, #4f46e5, #7c3aed);
  --c-accent: #f59e0b;
  --c-success: #10b981;
  --c-warning: #f59e0b;
  --c-danger: #ef4444;
  --c-text: #1e293b;
  --c-text-secondary: #64748b;
  --c-text-tertiary: #94a3b8;
  --c-text-quaternary: #cbd5e1;
  --c-border: #e2e8f0;
  --c-border-strong: #cbd5e1;
  --c-surface: #ffffff;
  --c-surface-warm: #fafafa;
  --c-surface-cool: #f8fafc;
  --c-bg-page: linear-gradient(180deg, #eef2ff 0%, #f5f3ff 35%, #f8fafc 100%);
  --shadow-sm: 0 1px 2px rgba(15,23,42,.05);
  --shadow-md: 0 4px 16px rgba(15,23,42,.07);
  --shadow-lg: 0 12px 40px rgba(15,23,42,.1);
  --shadow-primary: 0 8px 24px rgba(79,70,229,.2);
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  --font-mono: 'SF Mono', 'JetBrains Mono', Consolas, Monaco, monospace;
}

/* ===== Page Layout ===== */
.chat-console-page {
  gap: 0;
  padding: 0 !important;
  background: transparent;
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--c-text);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-feature-settings: 'cv02', 'cv03', 'cv04', 'cv11';
  letter-spacing: -0.005em;
}
.chat-shell {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 110px);
  min-height: 580px;
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 16px 20px 12px;
  background: var(--c-bg-page);
  border-radius: 20px;
  border: 1px solid var(--c-border);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  position: relative;
}

/* ===== Visual App Matrix Popover ===== */
.chat-app-selector {
  position: relative;
}
.current-app-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 4px 10px 4px 6px;
  border-radius: 12px;
  border: 1px solid var(--c-border);
  background: var(--c-surface);
  cursor: pointer;
  transition: all .2s cubic-bezier(.4,0,.2,1);
  box-shadow: 0 1px 3px rgba(15,23,42,.04);
}
.current-app-trigger:hover {
  border-color: var(--c-primary-light);
  background: var(--c-surface-warm);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79,70,229,.1);
}
.current-app-trigger.open {
  border-color: var(--c-primary);
  box-shadow: 0 0 0 3px rgba(79,70,229,.15);
}
.trigger-icon {
  display: grid;
  width: 26px; height: 26px;
  place-items: center;
  border-radius: 8px;
  background: var(--c-primary-grad);
  color: #fff;
  font-size: 13px;
  box-shadow: 0 2px 6px rgba(79,70,229,.25);
}
.trigger-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.15;
}
.trigger-label {
  color: var(--c-text-tertiary);
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: .04em;
}
.trigger-name {
  color: var(--c-text);
  font-size: 13px;
  font-weight: 700;
}
.trigger-switch-badge {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  color: var(--c-text-secondary);
  font-size: 11px;
  font-weight: 600;
  margin-left: 4px;
  transition: all .15s;
}
.current-app-trigger:hover .trigger-switch-badge {
  background: #ede9fe;
  color: #6d28d9;
}
.trigger-arrow {
  font-style: normal;
  font-size: 11px;
  transition: transform .2s ease;
  display: inline-block;
}
.trigger-arrow.active {
  transform: rotate(180deg);
}

/* Popover Modal Grid */
.app-picker-popover-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(15,23,42,.25);
  backdrop-filter: blur(4px);
  animation: fadeIn .15s ease;
}
.app-picker-popover {
  position: absolute;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  width: min(520px, 92vw);
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  padding: 16px 18px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid var(--c-border);
  box-shadow: 0 16px 48px rgba(15,23,42,.18);
  animation: popoverZoom .2s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes popoverZoom {
  from { opacity: 0; transform: translateX(-50%) scale(.95); }
  to { opacity: 1; transform: translateX(-50%) scale(1); }
}

.popover-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 12px;
}
.popover-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.popover-title-icon { width: 18px; height: 18px; color: var(--c-primary); }
.popover-head h4 { margin: 0; color: var(--c-text); font-size: 15px; font-weight: 700; }
.popover-count-badge {
  padding: 2px 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: var(--c-text-secondary);
  font-size: 11px;
  font-weight: 600;
}
.popover-close-btn {
  width: 28px; height: 28px;
  display: grid; place-items: center;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--c-text-tertiary);
  font-size: 18px;
  cursor: pointer;
  transition: all .15s;
}
.popover-close-btn:hover { background: #fee2e2; color: #dc2626; }

.popover-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid var(--c-border);
  margin-bottom: 12px;
}
.search-icon { width: 15px; height: 15px; color: var(--c-text-tertiary); }
.popover-search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  color: var(--c-text);
  font-size: 13px;
}

.popover-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  overflow-y: auto;
  max-height: 360px;
  padding: 2px;
}
.popover-grid::-webkit-scrollbar { width: 4px; }
.popover-grid::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 2px; }

.app-matrix-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid var(--c-border);
  cursor: pointer;
  transition: all .2s ease;
  position: relative;
}
.app-matrix-card:hover {
  border-color: var(--c-primary-light);
  background: #f5f3ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.08);
}
.app-matrix-card.active {
  background: #ede9fe;
  border-color: #c4b5fd;
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.12);
}
.matrix-card-icon {
  display: grid;
  width: 32px; height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border-radius: 9px;
  background: var(--c-surface-warm);
  color: var(--c-primary-dark);
  font-size: 15px;
}
.app-matrix-card.active .matrix-card-icon {
  background: var(--c-primary-grad);
  color: #ffffff;
}
.matrix-card-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.matrix-card-name {
  color: var(--c-text);
  font-size: 13px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-matrix-card.active .matrix-card-name {
  color: #4c1d95;
}
.matrix-card-desc {
  color: var(--c-text-tertiary);
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.matrix-card-check {
  width: 18px; height: 18px;
  display: grid; place-items: center;
  border-radius: 50%;
  background: var(--c-primary);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
}
.chat-shell::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--c-primary), var(--c-accent));
  opacity: .7;
}

/* ===== Topbar ===== */
.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 0 4px 12px;
  border-bottom: 1px solid var(--c-border);
}
.topbar-kicker {
  color: var(--c-primary-dark);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}
.chat-topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.app-picker-segmented {
  display: flex;
  align-items: center;
  gap: 8px;
}
.app-picker-label {
  color: var(--c-text-secondary);
  font-size: 12px;
  font-weight: 600;
}
.app-segmented-group {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px;
  border-radius: 12px;
  background: rgba(241, 245, 249, 0.85);
  border: 1px solid var(--c-border);
  overflow-x: auto;
  max-width: min(540px, 45vw);
}
.app-segmented-group::-webkit-scrollbar { height: 3px; }
.app-segmented-group::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 2px; }

.app-segment-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border-radius: 9px;
  border: none;
  background: transparent;
  color: var(--c-text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all .2s cubic-bezier(.4,0,.2,1);
}
.app-segment-btn:hover {
  color: var(--c-text);
  background: rgba(255, 255, 255, 0.65);
}
.app-segment-btn.active {
  background: #ffffff;
  color: var(--c-primary-dark);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08), 0 0 0 1px rgba(79, 70, 229, 0.15);
  font-weight: 700;
}
.app-segment-icon {
  font-size: 13px;
  line-height: 1;
}
.application-context-badge {
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--c-surface-warm);
  color: var(--c-primary-dark);
  font-size: 11.5px;
  font-weight: 600;
  border: 1px solid rgba(79,70,229,.15);
}
.chat-history-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  background: var(--c-surface);
  color: var(--c-text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all .2s ease;
}
.chat-history-btn:hover {
  border-color: var(--c-primary-light);
  color: var(--c-primary-dark);
  background: var(--c-surface-warm);
}
.history-icon {
  width: 14px;
  height: 14px;
  color: var(--c-text-tertiary);
  transition: color .2s ease;
}
.chat-history-btn:hover .history-icon {
  color: var(--c-primary);
}
.history-badge-count {
  display: inline-grid;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  place-items: center;
  border-radius: 9px;
  background: var(--c-primary);
  color: #fff;
  font-style: normal;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}
.chat-new-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  background: var(--c-surface);
  color: var(--c-text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all .2s;
}
.chat-new-btn i {
  font-style: normal;
  font-size: 13px;
  line-height: 1;
  font-weight: 700;
  color: var(--c-text-tertiary);
}
.chat-new-btn:hover {
  border-color: var(--c-primary);
  color: var(--c-primary-dark);
  background: var(--c-surface-warm);
}

/* ===== Chat Stage ===== */
.chat-stage {
  flex: 1;
  overflow: hidden;
  padding: 10px 0 0;
}
.chat-bubbles-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  overflow-y: auto;
  padding: 2px 4px 16px;
  scroll-behavior: smooth;
  position: relative;
}
.chat-bubbles-container::-webkit-scrollbar { width: 5px; }
.chat-bubbles-container::-webkit-scrollbar-thumb {
  background: #c4b5fd;
  border-radius: 3px;
}
.chat-bubbles-container::-webkit-scrollbar-thumb:hover { background: var(--c-primary); }

/* ===== Empty State ===== */
.chat-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 50%;
  padding: 32px 16px;
}
.empty-logo-wrap {
  max-width: 420px;
  text-align: center;
}
.empty-logo {
  display: grid;
  width: 56px;
  height: 56px;
  place-items: center;
  margin: 0 auto 18px;
  border-radius: 16px;
  background: var(--c-primary-grad);
  color: #fff;
  box-shadow: 0 10px 24px rgba(79,70,229,.28);
  position: relative;
}
.empty-logo::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 18px;
  background: var(--c-primary-grad);
  opacity: .2;
  filter: blur(10px);
  z-index: -1;
}
.empty-logo svg { width: 28px; height: 28px; }
.empty-logo-wrap h3 {
  margin: 0 0 18px;
  font-size: 18px;
  font-weight: 700;
  color: var(--c-text);
  letter-spacing: -0.01em;
  line-height: 1.3;
}
.empty-logo-wrap h3 span {
  background: var(--c-primary-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-weight: 700;
}
.empty-quick {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 20px;
  text-align: left;
}
.chip-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  background: var(--c-surface);
  cursor: pointer;
  transition: all .2s ease;
  text-align: left;
  font-family: var(--font-sans);
}
.chip-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: var(--c-primary);
}
.chip-btn span {
  color: var(--c-text);
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.4;
}
.chip-btn:hover {
  border-color: var(--c-primary-light);
  background: var(--c-surface-warm);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

/* ===== Chat Rows & Bubbles ===== */
.chat-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 100%;
  animation: fadeSlide .3s cubic-bezier(.4,0,.2,1);
}
@keyframes fadeSlide {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.chat-row.assistant {
  flex-direction: row;
  justify-content: flex-start;
}
.chat-row.user {
  flex-direction: row-reverse;
  justify-content: flex-start;
}
.chat-row.user .chat-bubble {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.chat-row.user .chat-bubble-sender {
  justify-content: flex-end;
  text-align: right;
}

.avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  font-size: 12.5px;
  font-weight: 700;
  background: #94a3b8;
  user-select: none;
  box-shadow: var(--shadow-sm);
}
.avatar.assistant {
  background: var(--c-primary-grad);
  box-shadow: 0 4px 12px rgba(79,70,229,.25);
}
.avatar.user {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  box-shadow: 0 4px 12px rgba(99,102,241,.22);
}
.avatar.trace {
  background: #e2e8f0;
  color: var(--c-text-secondary);
  box-shadow: none;
}

.chat-bubble {
  min-width: 0;
  max-width: min(82%, 840px);
}
.chat-row.user .chat-bubble { max-width: min(82%, 840px); }
.chat-bubble-sender {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  color: var(--c-text-tertiary);
  font-size: 11.5px;
  font-weight: 500;
}
.chat-bubble-sender span { color: var(--c-text-secondary); font-weight: 600; }
.chat-bubble-sender em {
  font-style: normal;
  color: #cbd5e1;
  font-size: 10.5px;
  font-feature-settings: 'tnum';
}
.chat-bubble .chat-text {
  padding: 12px 16px;
  border-radius: 16px;
  background: var(--c-surface);
  color: var(--c-text);
  border: 1px solid var(--c-border);
  font-size: 13.5px;
  line-height: 1.65;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(15,23,42,.04);
}
.chat-row.user .chat-bubble .chat-text {
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  color: #fff;
  border-color: transparent;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 16px rgba(79,70,229,.22);
}
.chat-row.user .chat-bubble .chat-text a { color: #e9d5ff; }
.chat-row.user .chat-bubble .chat-text .rich-inline-code { background: rgba(255,255,255,.2); color: #fef3c7; }

/* ===== System Notice Card ===== */
.system-notice-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin: 10px 0;
  padding: 12px 16px;
  border-radius: 14px;
  background: #fff5f5;
  border: 1px solid #fecaca;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.05);
  animation: fadeSlide .3s cubic-bezier(.4,0,.2,1);
}
.system-notice-icon {
  display: grid;
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  place-items: center;
  border-radius: 8px;
  background: #fee2e2;
  color: #dc2626;
}
.system-notice-icon svg {
  width: 16px;
  height: 16px;
}
.system-notice-content {
  flex: 1;
  min-width: 0;
}
.system-notice-title {
  color: #991b1b;
  font-size: 12.5px;
  font-weight: 700;
  margin-bottom: 3px;
  letter-spacing: .01em;
}
.system-notice-body {
  color: #b91c1c;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  font-weight: 500;
}

.trace-bubble .chat-text {
  background: var(--c-surface-cool);
  color: var(--c-text-secondary);
  border-style: dashed;
  font-size: 12px;
  border-radius: 12px;
}

/* ===== Processing Indicator ===== */
.chat-processing-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 300px;
  padding: 8px 12px;
  border-radius: 12px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  color: var(--c-text-secondary);
  box-shadow: var(--shadow-sm);
}
.chat-processing-indicator strong, .chat-processing-indicator small { display: block; }
.chat-processing-indicator strong { color: var(--c-text); font-size: 12.5px; font-weight: 600; }
.chat-processing-indicator small { margin-top: 2px; color: var(--c-text-tertiary); font-size: 11px; }
.processing-dots { display: inline-flex; gap: 4px; }
.processing-dots i {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--c-primary);
  animation: processing-pulse 1.2s infinite ease-in-out;
}
.processing-dots i:nth-child(2) { animation-delay: .15s; }
.processing-dots i:nth-child(3) { animation-delay: .3s; }
@keyframes processing-pulse { 0%, 80%, 100% { opacity: .35; transform: scale(.85); } 40% { opacity: 1; transform: scale(1.1); } }

/* ===== Markdown Content ===== */
.rich-content { line-height: 1.65; word-break: break-word; font-size: 13px; }
.rich-content :deep(p) { margin: 0 0 10px; }
.rich-content :deep(p:last-child) { margin-bottom: 0; }
.rich-content :deep(h3), .rich-content :deep(h4), .rich-content :deep(h5) {
  margin: 14px 0 8px;
  color: var(--c-text);
  font-weight: 700;
  line-height: 1.35;
}
.rich-content :deep(h3) { font-size: 15px; }
.rich-content :deep(h4) { font-size: 14px; }
.rich-content :deep(ul), .rich-content :deep(ol) { margin: 8px 0 12px; padding-left: 20px; }
.rich-content :deep(li) { margin: 4px 0; line-height: 1.65; }
.rich-content :deep(a) { color: var(--c-primary-dark); text-decoration: none; border-bottom: 1px solid rgba(79,70,229,.35); transition: border-color .15s; }
.rich-content :deep(a:hover) { color: var(--c-primary-dark); border-color: var(--c-primary-dark); }
.rich-content :deep(.rich-inline-code) {
  padding: 1px 6px;
  border-radius: 5px;
  background: #f4f1fb;
  color: #7c3aed;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: .85em;
  font-weight: 500;
  border: 1px solid #e9d5ff;
}
.rich-content :deep(.rich-code) {
  overflow-x: auto;
  margin: 12px 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: #1e1b4b;
  color: #e0e7ff;
  font: 12px/1.7 'JetBrains Mono', Consolas, monospace;
  white-space: pre;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,.06);
}
.rich-content :deep(blockquote) {
  margin: 12px 0;
  padding: 10px 14px;
  border-left: 3px solid var(--c-primary);
  background: var(--c-surface-warm);
  color: var(--c-primary-dark);
  border-radius: 0 10px 10px 0;
  font-size: 12.5px;
}
.rich-content :deep(.rich-table-scroll) { max-width: 100%; margin: 16px 0; overflow-x: auto; }
.rich-content :deep(table) {
  min-width: 100%;
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13.5px;
  border-radius: 12px;
  overflow: hidden;
}
.rich-content :deep(th), .rich-content :deep(td) { padding: 11px 16px; border-bottom: 1px solid #ede9fe; text-align: left; }
.rich-content :deep(th) { background: #f4f1fb; color: var(--c-text); font-weight: 600; letter-spacing: .01em; }

/* ===== History Notice ===== */
.workflow-history-notice {
  margin: 10px 4px 0;
  padding: 9px 14px;
  border-radius: 12px;
  background: #fff7ed;
  color: #92400e;
  border: 1px solid #fed7aa;
  font-size: 12.5px;
  font-weight: 500;
  text-align: center;
  letter-spacing: .01em;
}

/* ===== Composer ===== */
.chat-composer-wrap {
  margin-top: 8px;
  padding: 0;
}
.chat-composer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 10px 8px 16px;
  border-radius: 20px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
  transition: all .2s ease;
}
.chat-composer:hover {
  border-color: var(--c-border-strong);
}
.chat-composer:focus-within {
  border-color: var(--c-primary-light);
  box-shadow: 0 2px 14px rgba(79, 70, 229, 0.1);
}
.chat-composer.disabled {
  opacity: .5;
  pointer-events: none;
}
.composer-input {
  flex: 1;
  min-height: 24px;
  max-height: 140px;
  resize: none;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  background: transparent;
  color: var(--c-text);
  font-size: 13.5px;
  line-height: 1.5;
  padding: 4px 0;
  font-family: var(--font-sans);
}
.composer-input:focus, .composer-input:focus-visible {
  outline: none !important;
  border: none !important;
  box-shadow: none !important;
}
.composer-input::placeholder {
  color: var(--c-text-tertiary);
  font-size: 13px;
}
.composer-send {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  flex: 0 0 34px;
  border: 0;
  border-radius: 50%;
  background: var(--c-primary-grad);
  color: #fff;
  cursor: pointer;
  transition: all .2s ease;
  box-shadow: 0 4px 12px rgba(79,70,229,.28);
}
.composer-send:disabled {
  background: #d1d5db;
  box-shadow: none;
  cursor: not-allowed;
  color: #f3f4f6;
}
.composer-send:not(:disabled):hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(79,70,229,.35);
}
.composer-send svg { width: 15px; height: 15px; }
.composer-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 6px 8px 0;
}
.composer-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--c-text-tertiary);
  font-size: 11.5px;
  font-weight: 500;
  cursor: pointer;
  transition: color .15s;
  font-family: var(--font-sans);
}
.composer-link:hover { color: var(--c-primary); }
.badge-dot {
  display: inline-grid;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  place-items: center;
  border-radius: 9px;
  background: var(--c-primary);
  color: #fff;
  font-style: normal;
  font-size: 10px;
  font-weight: 700;
}

/* ===== Workflow Status Banner ===== */
.workflow-status-banner {
  margin: 10px 0 6px;
  padding: 10px 14px;
  border-radius: 14px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--shadow-sm);
  transition: all .25s ease;
}
.workflow-status-banner-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.status-summary {
  display: flex;
  align-items: center;
  gap: 10px;
}
.status-text-group {
  display: flex;
  flex-direction: column;
}
.status-text-group .status-kicker {
  display: block;
  color: var(--c-text-tertiary);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .06em;
  text-transform: uppercase;
}
.status-text-group strong {
  display: block;
  color: var(--c-text);
  font-size: 13.5px;
  font-weight: 600;
}
.status-kicker { display: block; margin-bottom: 2px; color: var(--c-text-tertiary); font-size: 10px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
.workflow-status-pill {
  flex: 0 0 auto;
  padding: 4px 12px;
  border-radius: 999px;
  background: #f1f5f9;
  color: var(--c-text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: .01em;
}
.workflow-running .workflow-status-pill { background: #ede9fe; color: #6d28d9; }
.workflow-waiting .workflow-status-pill { background: #fef3c7; color: #b45309; }
.workflow-success .workflow-status-pill { background: #dcfce7; color: #15803d; }
.workflow-failed .workflow-status-pill { background: #fee2e2; color: #b91c1c; }
.status-current-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px;
  border-radius: 10px;
  background: var(--c-surface-warm);
  border: 1px solid rgba(0,0,0,.04);
}
.status-current-step small { color: var(--c-text-tertiary); font-size: 11px; }
.status-current-step strong {
  overflow: hidden;
  color: var(--c-text);
  font-size: 12.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.toggle-graph-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  background: var(--c-surface);
  color: var(--c-text-secondary);
  font-size: 11.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all .2s ease;
}
.toggle-graph-btn:hover {
  background: var(--c-surface-warm);
  border-color: var(--c-primary-light);
  color: var(--c-primary-dark);
}
.toggle-graph-btn i {
  font-style: normal;
  font-size: 10px;
  transition: transform .2s ease;
  display: inline-block;
}
.toggle-graph-btn i.open {
  transform: rotate(180deg);
}
.status-dot {
  width: 11px; height: 11px;
  flex: 0 0 11px;
  border-radius: 50%;
  background: var(--c-primary);
  box-shadow: 0 0 0 3px rgba(79,70,229,.18);
}
.workflow-failed .status-dot { background: var(--c-danger); box-shadow: 0 0 0 3px rgba(239,68,68,.15); }
.workflow-success .status-dot { background: var(--c-success); box-shadow: 0 0 0 3px rgba(16,185,129,.15); }
.workflow-waiting .status-dot { background: var(--c-warning); box-shadow: 0 0 0 3px rgba(245,158,11,.15); }
.workflow-status-error {
  margin: 10px 0 0;
  color: #b91c1c;
  font-size: 12.5px;
  line-height: 1.65;
}
.workflow-graph {
  display: flex;
  align-items: stretch;
  gap: 8px;
  max-width: 100%;
  padding: 12px 4px 4px;
  overflow-x: auto;
}
.workflow-graph-node {
  display: flex;
  flex: 0 0 115px;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 7px;
  padding: 14px 12px;
  border-radius: 12px;
  background: var(--c-surface);
  text-align: center;
  transition: all .2s;
}
.workflow-graph-node strong {
  overflow: hidden;
  max-width: 100%;
  color: var(--c-text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
  letter-spacing: .01em;
}
.workflow-graph-node small { color: var(--c-text-tertiary); font-size: 10.5px; }
.workflow-graph-node.workflow-success { background: #f0fdf4; }
.workflow-graph-node.workflow-running {
  background: #ede9fe;
  box-shadow: 0 0 0 2px #c4b5fd;
}
.workflow-graph-node.workflow-waiting { background: #fffbeb; }
.workflow-graph-node.workflow-failed { background: #fef2f2; }
.workflow-graph-icon {
  display: grid;
  width: 30px; height: 30px;
  place-items: center;
  border-radius: 50%;
  background: #e5e7eb;
  color: var(--c-text-secondary);
  font-size: 15px;
  font-weight: 700;
}
.workflow-graph-node.workflow-success .workflow-graph-icon { background: var(--c-success); color: #fff; }
.workflow-graph-node.workflow-running .workflow-graph-icon { background: var(--c-primary); color: #fff; }
.workflow-graph-node.workflow-waiting .workflow-graph-icon { background: var(--c-warning); color: #fff; }
.workflow-graph-node.workflow-failed .workflow-graph-icon { background: var(--c-danger); color: #fff; }
.workflow-spinner {
  width: 13px; height: 13px;
  border: 2px solid rgba(255,255,255,.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: workflow-spin .8s linear infinite;
}
.workflow-graph-connector {
  display: flex;
  flex: 0 0 20px;
  align-items: center;
  color: #d1d5db;
  font-size: 18px;
  line-height: 1;
}
.workflow-graph-connector::before {
  width: 14px; height: 2px;
  background: #d1d5db;
  content: '';
}
.workflow-graph-connector.complete::before { background: var(--c-success); }
.workflow-graph-connector span { margin-left: -2px; }
@keyframes workflow-spin { to { transform: rotate(360deg); } }

.workflow-input-lock { display: none; }

/* ===== History Drawer ===== */
.history-drawer-overlay {
  position: fixed; inset: 0; z-index: 1100;
  display: flex; justify-content: flex-end;
  background: rgba(15, 23, 42, 0.35);
  backdrop-filter: blur(8px);
  animation: fadeIn .2s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.history-drawer {
  width: min(380px, 92vw);
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 22px 20px;
  background: linear-gradient(180deg, #ffffff 0%, #fafafa 100%);
  box-shadow: -12px 0 40px rgba(15, 23, 42, 0.15);
  border-left: 1px solid var(--c-border);
  animation: slideInRight .25s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes slideInRight { from { transform: translateX(100%); } to { transform: translateX(0); } }

.history-drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 14px;
}
.drawer-title-wrap { display: flex; flex-direction: column; gap: 3px; }
.drawer-title-row { display: flex; align-items: center; gap: 8px; }
.drawer-head-icon { width: 18px; height: 18px; color: var(--c-primary); }
.history-drawer-head h3 { margin: 0; color: var(--c-text); font-size: 17px; font-weight: 700; letter-spacing: -0.01em; }
.drawer-head-badge {
  display: inline-grid;
  place-items: center;
  min-width: 20px; height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #ede9fe;
  color: #6d28d9;
  font-size: 11px;
  font-weight: 700;
}
.drawer-subhead { margin: 0; color: var(--c-text-tertiary); font-size: 12px; }

.icon-close-btn {
  width: 32px; height: 32px;
  display: grid; place-items: center;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  background: var(--c-surface);
  color: var(--c-text-secondary);
  cursor: pointer;
  transition: all .2s ease;
}
.icon-close-btn:hover { background: #fee2e2; border-color: #fca5a5; color: #dc2626; }
.icon-close-btn svg { width: 15px; height: 15px; }

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding-right: 2px;
}
.drawer-body::-webkit-scrollbar { width: 4px; }
.drawer-body::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 2px; }

/* Active Session Action Bar */
.runtime-session-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 12px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid var(--c-border);
}
.actions-head {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--c-text-secondary);
  font-size: 11.5px;
  font-weight: 600;
}
.active-dot {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: var(--c-success);
  box-shadow: 0 0 0 2px rgba(16,185,129,.2);
}
.actions-btn-group {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}
.ghost-btn, .danger-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 28px;
  padding: 0 6px;
  border-radius: 8px;
  font-size: 11.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all .15s ease;
}
.ghost-btn i, .danger-btn i { font-style: normal; font-size: 11px; }
.ghost-btn {
  border: 1px solid var(--c-border);
  background: #ffffff;
  color: var(--c-text-secondary);
}
.ghost-btn:hover {
  border-color: var(--c-primary-light);
  color: var(--c-primary-dark);
  background: #f5f3ff;
}
.danger-btn {
  border: 1px solid #fecaca;
  background: #fff5f5;
  color: #dc2626;
}
.danger-btn:hover { background: #fee2e2; border-color: #fca5a5; }

/* Session Cards */
.history-drawer-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.session-card {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid var(--c-border);
  color: var(--c-text);
  text-align: left;
  cursor: pointer;
  transition: all .2s cubic-bezier(.4,0,.2,1);
  position: relative;
  box-shadow: 0 1px 3px rgba(15,23,42,.03);
}
.session-card:hover {
  border-color: var(--c-primary-light);
  background: #f5f3ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.08);
}
.session-card.active {
  background: #ede9fe;
  border-color: #c4b5fd;
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.12);
}
.session-card-icon {
  display: grid;
  width: 34px; height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border-radius: 10px;
  background: var(--c-surface-warm);
  color: var(--c-text-secondary);
  transition: all .2s;
}
.session-card:hover .session-card-icon {
  background: #ffffff;
  color: var(--c-primary);
}
.session-card.active .session-card-icon {
  background: var(--c-primary-grad);
  color: #ffffff;
}
.session-card-icon svg { width: 16px; height: 16px; }
.session-card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.session-card-title {
  color: var(--c-text);
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-card.active .session-card-title {
  color: #4c1d95;
}
.session-card-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--c-text-tertiary);
  font-size: 11px;
}
.meta-clock-icon { width: 11px; height: 11px; }
.session-card-arrow {
  color: var(--c-text-tertiary);
  font-size: 16px;
  line-height: 1;
  transition: transform .2s;
}
.session-card:hover .session-card-arrow {
  color: var(--c-primary);
  transform: translateX(2px);
}

/* Load More Button */
.load-more-wrap {
  margin-top: 12px;
  margin-bottom: 8px;
  text-align: center;
}
.load-more-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 36px;
  padding: 0 16px;
  border: 1px dashed var(--c-border-strong);
  border-radius: 12px;
  background: #ffffff;
  color: var(--c-primary-dark);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all .2s ease;
}
.load-more-btn:hover {
  border-color: var(--c-primary-light);
  background: #f5f3ff;
  color: var(--c-primary);
  transform: translateY(-1px);
}
.load-more-btn small {
  color: var(--c-text-tertiary);
  font-size: 11px;
  font-weight: 500;
}
.load-more-icon {
  font-style: normal;
  font-size: 12px;
  transition: transform .2s ease;
}
.load-more-btn:hover .load-more-icon {
  transform: translateY(2px);
}
.no-more-notice {
  margin-top: 12px;
  margin-bottom: 8px;
  text-align: center;
  color: var(--c-text-tertiary);
  font-size: 11.5px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  text-align: center;
  color: var(--c-text-tertiary);
}
.empty-icon { width: 36px; height: 36px; margin-bottom: 12px; color: var(--c-border-strong); }
.empty-state p { margin: 0 0 4px; font-size: 14px; font-weight: 600; color: var(--c-text-secondary); }
.empty-state small { font-size: 12px; }

.related-runs {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
}
.section-label { color: var(--c-text-secondary); font-size: 11.5px; font-weight: 600; margin-bottom: 2px; }
.related-run-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--c-surface-cool);
  transition: background .15s;
  text-decoration: none;
}
.related-run-row:hover { background: var(--c-surface-warm); }
.related-run-row span { display: flex; flex-direction: column; gap: 2px; }
.related-run-row b { color: var(--c-text); font-size: 12px; font-weight: 600; }
.related-run-row small { color: var(--c-text-tertiary); font-size: 11px; }
.text-link {
  color: var(--c-primary);
  font-size: 11.5px;
  font-weight: 500;
  text-decoration: none;
}
.text-link:hover { color: var(--c-primary-dark); }

/* ===== Responsive ===== */
@media (max-width: 860px) {
  .chat-shell { height: calc(100vh - 90px); padding: 12px 12px 8px; }
  .chat-topbar { flex-direction: column; align-items: flex-start; gap: 10px; padding-bottom: 10px; }
  .chat-topbar-right { width: 100%; }
  .chat-agent-select { flex: 1; }
  .empty-quick { grid-template-columns: 1fr; }
}
@media (max-width: 540px) {
  .chat-shell { border-radius: 18px; padding: 10px 10px 6px; }
  .chat-bubble .chat-text { padding: 8px 12px; font-size: 12.5px; }
  .avatar { width: 28px; height: 28px; flex: 0 0 28px; border-radius: 8px; font-size: 11.5px; }
  .chat-row { grid-template-columns: 28px minmax(0, 1fr); gap: 8px; }
  .chat-row.user { grid-template-columns: 1fr 28px; }
  .chat-row.user > .avatar { order: 2; }
  .chat-row.user > .chat-bubble { order: 1; }
}
</style>
