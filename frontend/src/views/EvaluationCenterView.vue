<template>
  <div class="page-grid eval-page-grid">
    <PageToolbar eyebrow="质量验证" title="应用评测" description="用真实样例验证应用回答是否准确、可靠。" />
    <ResourceContextRail class="eval-context-rail" label="评测应用" :name="selectedApplicationName" :state="loading ? '加载中' : '可评测'" hint="选择评测集和候选版本后发起评测" mark="Q" />
    <SectionCard class="eval-main-card" title="企业应用评测" description="根据验收样例执行真实测试，完成后展示结果和运行报告。">
      <template #action>
        <div class="action-row">
          <button v-if="hasFailedEvaluations" @click="handleCleanEvaluations" class="danger-outline-btn" title="清理所有未通过的评测任务">清空未通过记录</button>
          <button @click="startNewSuite" class="ghost-btn">新建评测集</button>
          <button @click="showLaunchModal = true" class="primary-btn">发起评测</button>
        </div>
      </template>

      <!-- 紧凑型应用评测状态条（替代原先臃肿的多行大块卡片） -->
      <div v-if="form.applicationId" class="eval-config-banner">
        <div class="config-facts">
          <div class="config-fact-item">
            <span class="fact-label">当前应用</span>
            <span class="fact-value app-name">{{ selectedApplicationName }}</span>
          </div>
          <div class="config-fact-divider"></div>
          <div class="config-fact-item">
            <span class="fact-label">可用评测集</span>
            <span class="fact-value">{{ suites.length }} 个</span>
          </div>
          <div class="config-fact-divider"></div>
          <div class="config-fact-item">
            <span class="fact-label">候选版本</span>
            <span class="fact-value">{{ candidates.length }} 个</span>
          </div>
          <template v-if="suites.length">
            <div class="config-fact-divider"></div>
            <div class="config-fact-item suite-meta-item">
              <span class="fact-label">当前评测集</span>
              <span class="fact-value suite-name">
                {{ suites[0].suiteName }}
                <small class="suite-version-tag">第 {{ suites[0].latestVersionNo }} 版 · {{ suites[0].caseCount }} 样例</small>
              </span>
            </div>
          </template>
        </div>
        <div v-if="suites.length" class="config-actions">
          <button type="button" class="btn-edit-suite" @click="editSuite(suites[0])">
            ✏️ 编辑样例并创建新版
          </button>
        </div>
      </div>

      <PageState v-if="loadError" type="error" title="评测数据加载失败" :message="loadError" action-label="重新加载" @action="loadApplications" />
      <PageState v-else-if="loading" type="loading" message="正在读取评测任务和候选版本..." />
      <PageState v-else-if="evaluations.length === 0" type="empty" message="目前尚未执行过回答质量评测。" />

      <!-- 紧凑现代的评测任务列表表格 -->
      <table v-else class="compact-eval-table">
        <thead>
          <tr>
            <th>评测任务</th>
            <th>测试集版本</th>
            <th>综合结果</th>
            <th>测试状态</th>
            <th class="actions-header">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in evaluations" :key="item.runId" :class="{ 'row-active': selectedDetail?.run?.runId === item.runId }">
            <td class="task-name-cell">
              <span class="candidate-tag">候选版本 #{{ item.candidateId }}</span>
            </td>
            <td>
              <span class="suite-ver-badge">版本 #{{ item.suiteVersionId }}</span>
            </td>
            <td class="score-cell">
              <span v-if="item.successRate !== undefined && item.successRate !== null" class="score-pill">
                {{ percent(item.successRate) }}
              </span>
              <span v-else class="text-muted">-</span>
            </td>
            <td>
              <span class="status-badge" :class="statusBadgeClass(item.status)">
                <i class="status-dot"></i>
                {{ statusText(item.status) }}
              </span>
            </td>
            <td class="actions-cell">
              <div class="row-actions">
                <button type="button" class="action-btn btn-view" @click="openDetail(item)" title="查看评测报告与测试证据">
                  查看证据
                </button>
                <button
                  v-if="['PARTIAL', 'FAILED'].includes(item.status)"
                  type="button"
                  class="action-btn btn-retry"
                  :disabled="mutation.isPending(`evaluation-retry-${item.runId}`)"
                  @click="retryEvaluation(item)"
                  title="为失败用例重新发起评测"
                >
                  {{ mutation.isPending(`evaluation-retry-${item.runId}`) ? '重试中...' : '重试' }}
                </button>
                <button
                  type="button"
                  class="action-btn btn-delete"
                  @click="handleDeleteEvaluation(item)"
                  title="删除此条评测记录"
                >
                  删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="selectedDetail" class="evaluation-detail">
        <div class="detail-heading">
          <div class="detail-title-wrap">
            <strong>评测证据报告</strong>
            <span class="badge" :style="{
              color: ['SUCCEEDED', 'PASS'].includes(selectedDetail.run.status) ? '#10b981' : '#f59e0b',
              borderColor: ['SUCCEEDED', 'PASS'].includes(selectedDetail.run.status) ? 'rgba(16, 185, 129, 0.3)' : 'rgba(245, 158, 11, 0.3)'
            }">
              {{ statusText(selectedDetail.run.status) }}
            </span>
            <span class="muted-note">候选版本 #{{ selectedDetail.run.candidateId }}</span>
          </div>
          <button type="button" class="ghost-btn" @click="selectedDetail = null">关闭详情</button>
        </div>

        <div class="metric-strip">
          <span>候选通过率 <b>{{ percent(selectedDetail.aggregateReport.successRate) }}</b></span>
          <span v-if="selectedDetail.aggregateReport.baselineSuccessRate !== undefined">基线通过率 <b>{{ percent(selectedDetail.aggregateReport.baselineSuccessRate) }}</b></span>
          <span>回归样例 <b>{{ selectedDetail.aggregateReport.regressedCaseIds?.length || 0 }}</b></span>
          <span>平均延迟 <b>{{ selectedDetail.aggregateReport.averageLatencyMs || 0 }} ms</b></span>
          <span>样例总数 <b>{{ selectedDetail.cases?.length || 0 }} 个</b></span>
        </div>

        <div class="evidence-card-list">
          <div v-for="evidence in selectedDetail.cases" :key="`${evidence.caseId}-${evidence.evaluationVariant}`" class="evidence-card">
            <!-- 头部状态栏：清晰的评测达标与相似度徽标 -->
            <div class="evidence-card-head">
              <div class="card-head-left">
                <span class="case-tag">样例 #{{ evidence.caseId }}</span>
                <span class="variant-pill">{{ variantText(evidence.evaluationVariant) }}</span>
                <span class="status-pill" :class="evidence.status === 'SUCCEEDED' ? 'pill-success' : 'pill-fail'">
                  <span class="status-dot"></span>
                  {{ evidence.status === 'SUCCEEDED' ? '评测达标' : '未达标' }}
                </span>
                <span v-if="evidence.similarityScore !== null && evidence.similarityScore !== undefined" class="similarity-pill" :class="evidence.status === 'SUCCEEDED' ? 'sim-pass' : 'sim-fail'">
                  综合相似度: {{ percent(evidence.similarityScore) }}
                </span>
              </div>
              <div class="card-head-right">
                <span v-if="evidence.latencyMs" class="stat-badge">⏱️ {{ evidence.latencyMs }} ms</span>
                <span v-if="evidence.outputTokens" class="stat-badge">📊 Token: {{ evidence.outputTokens }}</span>
              </div>
            </div>

            <!-- 未命中/未达标中文诊断提示 -->
            <div v-if="evidence.status !== 'SUCCEEDED'" class="diagnostic-alert">
              <div class="alert-icon-wrap">⚠️</div>
              <div class="alert-body">
                <div class="alert-title">相似度分析诊断</div>
                <div class="alert-msg">{{ evidence.failureMessage || evidence.matchDetails || '大模型实际回答与期望相似度未达标' }}</div>
              </div>
            </div>

            <!-- 最佳匹配核心段落引用条 -->
            <div v-if="evidence.bestSegment" class="best-segment-bar">
              <div class="segment-icon">🎯</div>
              <div class="segment-body">
                <span class="segment-label">模型命中最高契合段落：</span>
                <span class="segment-quote" v-html="renderHighlightedOutput(evidence.bestSegment, evidence.expectedContent, null)"></span>
              </div>
            </div>

            <div class="evidence-grid">
              <!-- 业务输入 -->
              <div class="grid-item">
                <div class="item-title">
                  <span>业务输入内容 (提问)</span>
                  <span class="bubble-tag">输入</span>
                </div>
                <div class="item-content input-content">{{ evidence.inputContent || '（无输入文本）' }}</div>
              </div>

              <!-- 期望规则 -->
              <div class="grid-item">
                <div class="item-title">
                  <span>期望参考标准 (语义相似度判定)</span>
                  <span class="rule-hint-pill">高亮比对基准</span>
                </div>
                <div class="item-content expected-content">
                  <div class="expected-chip">
                    <span class="chip-dot"></span>
                    <span class="chip-text">{{ evidence.expectedContent || '（无特定期望）' }}</span>
                  </div>
                </div>
              </div>

              <!-- 大模型真实回答展示面板 -->
              <div class="grid-item full-width">
                <div class="output-panel-header">
                  <div class="panel-header-left">
                    <span class="ai-sparkle">✨</span>
                    <span class="panel-title">大模型智能回答 (真实输出)</span>
                    <span class="output-count-badge" v-if="extractPureLlmOutput(evidence.actualOutput)">{{ extractPureLlmOutput(evidence.actualOutput).length }} 字符</span>
                    <span class="highlight-badge">已高亮命中关键指标</span>
                  </div>
                  <div class="panel-header-right">
                    <span v-if="evidence.matchDetails && evidence.status === 'SUCCEEDED'" class="match-success-text">
                      ✓ {{ evidence.matchDetails }}
                    </span>
                    <button type="button" class="mini-copy-btn" @click="copyToClipboard(extractPureLlmOutput(evidence.actualOutput))" title="复制回答文本">
                      复制纯文本
                    </button>
                  </div>
                </div>
                <div class="item-content output-content rich-content">
                  <div class="output-rendered-text" v-html="renderHighlightedOutput(evidence.actualOutput, evidence.expectedContent, evidence.bestSegment)"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </SectionCard>

    <!-- 创建评测任务弹窗 -->
    <div v-if="showLaunchModal" class="modal-overlay">
      <div class="modal-card">
        <h3>发起应用评测</h3>
        
        <div class="form-group">
          <label>应用</label>
          <select v-model="form.applicationId" class="text-input" @change="loadApplicationContext">
            <option :value="null">请选择应用</option>
            <option v-for="application in applications" :key="application.id" :value="application.id">{{ application.name || application.appName }}</option>
          </select>
          <p v-if="applications.length === 0" class="field-help">当前租户暂无应用。</p>
        </div>

        <div class="form-group">
          <label>候选版本</label>
          <select v-model="form.candidateId" class="text-input">
            <option :value="null">请选择候选版本</option>
            <option v-for="candidate in candidates" :key="candidate.candidateId" :value="candidate.candidateId">{{ candidate.snapshotFingerprint }}</option>
          </select>
        </div>

        <div class="form-group">
          <label>评测集</label>
          <select v-model="form.suiteVersionId" class="text-input">
            <option :value="null">请选择评测集</option>
            <option v-for="suite in suites" :key="suite.suiteId" :value="suite.latestVersionId">{{ suite.suiteName }} · 第 {{ suite.latestVersionNo }} 版 · {{ suite.caseCount }} 个样例</option>
          </select>
          <p v-if="suites.length === 0" class="field-help">当前应用还没有评测集，请先关闭窗口并新建评测集。</p>
        </div>

        <div class="form-group">
          <label>对照版本</label>
          <select v-model="form.baselineReleaseId" class="text-input"><option value="">不使用基线</option><option v-for="version in releaseVersions" :key="version.versionId" :value="version.versionId">第 {{ version.versionNo }} 版{{ version.current ? '（当前生产）' : '' }}</option></select>
        </div>

        <div class="modal-actions">
          <button @click="showLaunchModal = false" class="ghost-btn">取消</button>
          <button @click="handleLaunch" class="primary-btn">开始评测</button>
        </div>
      </div>
    </div>

    <div v-if="showSuiteModal" class="modal-overlay">
      <div class="modal-card suite-modal">
        <h3>{{ suiteForm.suiteId ? '创建评测集新版本' : '新建评测集' }}</h3>
        <div class="form-group"><label>评测集名称</label><input v-model.trim="suiteForm.name" class="text-input" :disabled="Boolean(suiteForm.suiteId)" placeholder="例如：招聘助手核心业务验收" /></div>
        <div class="sample-list">
          <div v-for="(sample, index) in suiteForm.samples" :key="index" class="sample-row">
            <div class="sample-head"><strong>样例 {{ index + 1 }}</strong><button v-if="suiteForm.samples.length > 1" type="button" class="text-link" @click="suiteForm.samples.splice(index, 1)">删除</button></div>
            <label><span>业务输入</span><textarea v-model.trim="sample.input" rows="3" placeholder="输入一条真实业务请求，例如：请帮我看一下 HR-REQ-2026-002 这个招聘需求" /></label>
            <label>
              <div class="label-with-tip">
                <span>期望参考标准 (语义相似度分析)</span>
                <small class="tip-inline">系统基于语义相似度多维打分，无需强行限制固定用词</small>
              </div>
              <input v-model.trim="sample.expected" placeholder="例如：需说明预算月薪范围在 18,000~28,000 元区间，并进入人工审批流" />
            </label>
          </div>
        </div>
        <button type="button" class="ghost-btn" @click="suiteForm.samples.push({ input: '', expected: '' })">添加样例</button>
        <div class="modal-actions"><button type="button" class="ghost-btn" @click="showSuiteModal = false">取消</button><button type="button" class="primary-btn" :disabled="mutation.isPending('evaluation-suite-create')" @click="createSuite">创建评测集</button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, reactive } from 'vue'
import { useRoute } from 'vue-router'
import SectionCard from '../components/SectionCard.vue'
import PageToolbar from '../components/PageToolbar.vue'
import PageState from '../components/PageState.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import http from '../api/http'
import { notify, confirmAction } from '../utils/feedback'
import { fetchRuntimeApplicationConfiguration, fetchRuntimeApplications } from '../api/runtime'
import { useMutationState } from '../composables/useMutationState.js'
import { renderMarkdown } from '../utils/markdown'

const evaluations = ref([])
const applications = ref([])
const candidates = ref([])
const suites = ref([])
const releaseVersions = ref([])
const loadError = ref('')
const loading = ref(false)
const mutation = useMutationState()
const route = useRoute()
const showLaunchModal = ref(false)
const showSuiteModal = ref(false)
const selectedDetail = ref(null)
const inputFieldName = ref('request')
const suiteForm = reactive({ suiteId: null, name: '', samples: [{ input: '', expected: '' }] })

const form = reactive({
  applicationId: route.query.applicationId ? Number(route.query.applicationId) : null,
  candidateId: null,
  suiteVersionId: null,
  candidateFingerprint: '',
  baselineReleaseId: ''
})

const statusText = status => ({
  QUEUED: '排队中',
  RUNNING: '评测中',
  SUCCEEDED: '评测通过',
  PASS: '评测通过',
  FAILED: '评测未通过',
  PARTIAL: '部分完成'
}[status] || status || '未知状态')
const selectedApplicationName = computed(() => applications.value.find(item => String(item.id) === String(form.applicationId))?.name || '当前应用')

const loadEvaluations = async () => {
  try {
    if (!form.applicationId) return
    const response = await http.get(`/runtime/applications/${form.applicationId}/evaluations`)
    evaluations.value = response.data || []
  } catch (error) {
    loadError.value = '无法读取评测数据，请检查网络或后端服务配置。'
  }
}

const loadApplications = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const response = await fetchRuntimeApplications()
    applications.value = response.data || []
    if (!form.applicationId && applications.value.length > 0) form.applicationId = applications.value[0].id
    if (form.applicationId) await loadApplicationContext()
  } catch (error) {
    loadError.value = error?.message || '应用与评测数据加载失败，请稍后重试。'
  } finally { loading.value = false }
}

const loadCandidates = async () => {
  if (!form.applicationId) return
  const response = await http.get(`/runtime/applications/${form.applicationId}/candidates`)
  candidates.value = response.data || []
}

const loadSuites = async () => {
  if (!form.applicationId) return
  const response = await http.get(`/runtime/applications/${form.applicationId}/evaluation-suites`)
  suites.value = response.data || []
  if (!suites.value.some(item => String(item.latestVersionId) === String(form.suiteVersionId))) form.suiteVersionId = suites.value[0]?.latestVersionId || null
}

const loadApplicationContext = async () => {
  form.candidateId = null
  form.suiteVersionId = null
  selectedDetail.value = null
  const configurationResponse = await fetchRuntimeApplicationConfiguration(form.applicationId)
  inputFieldName.value = configurationResponse.data?.spec?.input?.fields?.[0]?.name || 'request'
  releaseVersions.value = (configurationResponse.data?.versions || []).filter(item => item.status === 'PUBLISHED')
  await Promise.all([loadCandidates(), loadSuites(), loadEvaluations()])
}

const createSuite = async () => {
  if (!form.applicationId || !suiteForm.name || suiteForm.samples.some(item => !item.input || !item.expected)) {
    notify('请填写评测集名称以及每个样例的输入和期望内容。', 'warning')
    return
  }
  const payload = {
    scoringPolicy: { correctness: true },
    cases: suiteForm.samples.map((item, index) => ({ caseCode: `case-${index + 1}`, input: { [inputFieldName.value]: item.input }, expectedRule: { expected: item.expected }, metricApplicability: { correctness: true } }))
  }
  try {
    const request = suiteForm.suiteId
      ? () => http.post(`/runtime/applications/${form.applicationId}/evaluation-suites/${suiteForm.suiteId}/versions`, payload)
      : () => http.post(`/runtime/applications/${form.applicationId}/evaluation-suites`, { ...payload, suiteCode: `suite-${Date.now()}`, suiteName: suiteForm.name })
    await mutation.run('evaluation-suite-create', request, loadSuites)
    showSuiteModal.value = false
    suiteForm.suiteId = null
    suiteForm.name = ''
    suiteForm.samples = [{ input: '', expected: '' }]
    notify('评测集已创建，可以直接发起评测。', 'success')
  } catch (error) { notify(error?.message || '评测集创建失败。', 'error') }
}

const startNewSuite = () => {
  suiteForm.suiteId = null
  suiteForm.name = ''
  suiteForm.samples = [{ input: '', expected: '' }]
  showSuiteModal.value = true
}

const editSuite = async suite => {
  try {
    const response = await http.get(`/runtime/applications/${form.applicationId}/evaluation-suite-versions/${suite.latestVersionId}/cases`)
    const cases = response.data || []
    suiteForm.suiteId = suite.suiteId
    suiteForm.name = suite.suiteName
    suiteForm.samples = cases.map(item => ({ input: String(Object.values(item.input || {})[0] || ''), expected: String(item.expectedRule?.expected || '') }))
    if (suiteForm.samples.length === 0) suiteForm.samples = [{ input: '', expected: '' }]
    showSuiteModal.value = true
  } catch (error) { notify(error?.message || '评测样例读取失败。', 'error') }
}

const openDetail = async (item) => {
  if (!form.applicationId || !item?.runId) return
  const response = await http.get(`/runtime/applications/${form.applicationId}/evaluations/${item.runId}`)
  selectedDetail.value = response.data
}

const retryEvaluation = async item => {
  try {
    await mutation.run(`evaluation-retry-${item.runId}`, () => http.post(`/runtime/applications/${form.applicationId}/evaluations/${item.runId}/retry`), loadEvaluations)
    notify('新的评测重试任务已创建，原有证据保持不变。', 'success')
  } catch (error) { notify(error?.message || '评测重试失败。', 'error') }
}

const statusBadgeClass = (status) => {
  if (['SUCCEEDED', 'PASS'].includes(status)) return 'badge-success'
  if (['RUNNING', 'EVALUATING'].includes(status)) return 'badge-running'
  if (status === 'QUEUED') return 'badge-queued'
  return 'badge-failed'
}

const hasFailedEvaluations = computed(() => {
  return evaluations.value.some(item => ['PARTIAL', 'FAILED', 'QUEUED'].includes(item.status))
})

const handleDeleteEvaluation = async (item) => {
  const confirmed = await confirmAction({
    title: '删除评测任务',
    message: `确定要删除候选版本 #${item.candidateId} 的这条评测任务记录吗？关联的测试打分数据将被一并清理。`,
    confirmText: '确定删除',
    cancelText: '取消',
    tone: 'danger'
  })
  if (!confirmed) return

  try {
    await http.delete(`/runtime/applications/${form.applicationId}/evaluations/${item.runId}`)
    notify('评测记录已删除。', 'success')
    if (selectedDetail.value?.run?.runId === item.runId) {
      selectedDetail.value = null
    }
    await loadEvaluations()
  } catch (error) {
    notify(error?.message || '删除评测任务失败。', 'error')
  }
}

const handleCleanEvaluations = async () => {
  const confirmed = await confirmAction({
    title: '清空未通过记录',
    message: '确定要清理当前应用下所有未通过、失败或排队中的历史评测记录吗？测试通过的记录将予以保留。',
    confirmText: '确定清空',
    cancelText: '取消',
    tone: 'danger'
  })
  if (!confirmed) return

  try {
    const res = await http.delete(`/runtime/applications/${form.applicationId}/evaluations?scope=failed`)
    notify(res.data || '已成功清理历史评测任务。', 'success')
    selectedDetail.value = null
    await loadEvaluations()
  } catch (error) {
    notify(error?.message || '清空历史任务失败。', 'error')
  }
}

const percent = value => value === null || value === undefined ? '暂无数据' : `${(Number(value) * 100).toFixed(1)}%`
const variantText = variant => ({ CANDIDATE: '候选版本', BASELINE: '当前版本' }[variant] || variant || '评测结果')

const escapeHtml = str => {
  if (!str) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

// 提取纯净的大模型回答，剥离一切 RAG 知识库调试上下文与原始数据表
const extractPureLlmOutput = (raw) => {
  if (!raw) return ''
  let text = String(raw).trim()

  // 1. 如果为结构化 JSON 字符串
  if ((text.startsWith('{') && text.endsWith('}')) || (text.startsWith('[') && text.endsWith(']'))) {
    try {
      const obj = JSON.parse(text)
      if (typeof obj === 'object' && obj !== null) {
        // 查找 llm 相关字段（如 nodes.llm-1.output）
        for (const [k, v] of Object.entries(obj)) {
          const lk = k.toLowerCase()
          if (lk.includes('llm') && (lk.includes('output') || lk.includes('result') || lk.includes('text'))) {
            if (typeof v === 'string' && v.trim()) return v.trim()
            if (v && typeof v === 'object' && v.output) return String(v.output).trim()
            if (v && typeof v === 'object' && v.text) return String(v.text).trim()
          }
        }
        // 检查嵌套 nodes 对象
        if (obj.nodes && typeof obj.nodes === 'object') {
          for (const [k, v] of Object.entries(obj.nodes)) {
            if (k.toLowerCase().includes('llm') && v) {
              if (typeof v === 'string') return v.trim()
              if (v.output) return String(v.output).trim()
              if (v.text) return String(v.text).trim()
            }
          }
        }
        // 顶层常规字段
        for (const key of ['result', 'answer', 'reply', 'output_text', 'response']) {
          if (typeof obj[key] === 'string' && obj[key].trim()) return obj[key].trim()
        }
        if (typeof obj.output === 'string') {
          return extractPureLlmOutput(obj.output)
        }
      }
    } catch (e) {
      // 非纯净 JSON 继续向下匹配
    }
  }

  // 2. 正则兜底提取内嵌的 "nodes.llm-1.output": "..."
  const llmMatch = text.match(/"nodes\.llm[^"]*\.output"\s*:\s*"((?:[^"\\]|\\.)*)"/s)
  if (llmMatch && llmMatch[1]) {
    try {
      return JSON.parse(`"${llmMatch[1]}"`)
    } catch (e) {
      return llmMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"')
    }
  }

  return text
}

// 在优雅的 Markdown 格式化渲染基础上，对命中的关键词和段落做高光标记
const renderHighlightedOutput = (actualText, expectedText, bestSegment) => {
  const pureText = extractPureLlmOutput(actualText)
  if (!pureText) return '<div class="empty-output-tip">（未捕获到大模型回答或执行异常）</div>'

  // 先转为排版优雅的结构化 HTML
  let html = renderMarkdown(pureText)

  // 收集并清洗期望关键词
  const terms = new Set()
  if (expectedText && expectedText.trim()) {
    const rawExpected = expectedText.trim()
    terms.add(rawExpected)
    const words = rawExpected.split(/[\s,，、。；;：:!！?？《》\(\)（）]+/).filter(w => w.length >= 2)
    words.forEach(w => terms.add(w))
  }

  const sortedTerms = Array.from(terms).filter(Boolean).sort((a, b) => b.length - a.length)
  if (sortedTerms.length === 0) {
    return html
  }

  try {
    const escapedTerms = sortedTerms.map(t => escapeHtml(t).replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
    // 匹配 HTML 标签或关键词，只高亮纯文本部分，坚决不破坏 HTML 标签属性与骨架
    const regex = new RegExp(`(<[^>]+>)|(${escapedTerms.join('|')})`, 'gi')
    return html.replace(regex, (match, tag, term) => {
      if (tag) return tag
      return `<mark class="eval-hit-term">${term}</mark>`
    })
  } catch (e) {
    return html
  }
}

const copyToClipboard = async (text) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    notify('已成功复制大模型回答内容', 'success')
  } catch (err) {
    notify('复制操作未成功，请手动选中文本复制', 'warning')
  }
}

const handleLaunch = async () => {
  if (!form.applicationId || !form.candidateId || !form.suiteVersionId) {
    notify('请选择应用、Candidate 和 Suite 版本。', 'warning')
    return
  }

  try {
    const selected = candidates.value.find(item => item.candidateId === form.candidateId)
    await mutation.run('evaluation-launch', () => http.post(`/runtime/applications/${form.applicationId}/evaluations`, {
      candidateId: form.candidateId,
      candidateFingerprint: form.candidateFingerprint || selected?.snapshotFingerprint,
      suiteVersionId: form.suiteVersionId,
      baselineReleaseId: form.baselineReleaseId || null
    }), loadEvaluations)
      showLaunchModal.value = false
      form.candidateId = null
      form.suiteVersionId = null
      notify('评测任务已创建，等待真实评测执行器处理。', 'success')
  } catch (e) {
    notify(e?.message || '发起评测任务失败。', 'error')
  }
}

onMounted(loadApplications)
</script>

<style scoped>
.score-value { color: var(--accent); font-size: 14px; font-weight: 700; }
.action-row { display: flex; align-items: center; gap: 8px; }

/* 页面级别空间紧凑化（消灭巨大空白间隔） */
.eval-page-grid { gap: 12px; }
.eval-context-rail { margin-bottom: 0 !important; }
.eval-main-card { margin-top: 0; }

/* 危险操作线框按钮 */
.danger-outline-btn {
  padding: 5px 12px;
  border: 1px solid #fecdd3;
  border-radius: 6px;
  background: #fff1f2;
  color: #be123c;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}
.danger-outline-btn:hover {
  background: #ffe4e6;
  border-color: #fda4af;
  color: #9f1239;
}

/* 紧凑型应用评测状态条（替代原先臃肿的多行大块卡片） */
.eval-config-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 9px 16px;
  margin-bottom: 14px;
  background: linear-gradient(90deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.config-facts {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.config-fact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.fact-label {
  color: #64748b;
  font-size: 11px;
}

.fact-value {
  color: #1e293b;
  font-weight: 600;
}

.config-fact-divider {
  width: 1px;
  height: 14px;
  background: #cbd5e1;
}

.suite-version-tag {
  font-size: 10px;
  margin-left: 5px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #e2e8f0;
  color: #475569;
  font-weight: normal;
}

/* 优雅的“编辑样例并创建新版”按钮 */
.btn-edit-suite {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #ffffff;
  color: #334155;
  font-size: 11.5px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.15s ease;
}

.btn-edit-suite:hover {
  border-color: #6366f1;
  background: #f5f7ff;
  color: #4338ca;
  box-shadow: 0 2px 6px rgba(99, 102, 241, 0.15);
}

/* 紧凑现代的评测任务列表表格 */
.compact-eval-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 6px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
}

.compact-eval-table th {
  padding: 9px 14px;
  background: #f8fafc;
  color: #475569;
  font-size: 11.5px;
  font-weight: 600;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
}

.compact-eval-table td {
  padding: 9px 14px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 12px;
  color: #334155;
  vertical-align: middle;
}

.compact-eval-table tr:hover td {
  background: #f8fafc;
}

.compact-eval-table tr.row-active td {
  background: #eff6ff;
}

.candidate-tag {
  font-weight: 600;
  color: #0f172a;
}

.suite-ver-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
}

.score-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}

.text-muted {
  color: #94a3b8;
}

/* 现代状态胶囊 */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 9px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.badge-success {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}
.badge-success .status-dot { background: #10b981; }

.badge-running {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}
.badge-running .status-dot { background: #3b82f6; }

.badge-queued {
  background: #f8fafc;
  color: #64748b;
  border: 1px solid #e2e8f0;
}
.badge-queued .status-dot { background: #94a3b8; }

.badge-failed {
  background: #fff1f2;
  color: #e11d48;
  border: 1px solid #fecdd3;
}
.badge-failed .status-dot { background: #f43f5e; }

/* 现代操作按钮组 */
.actions-header {
  text-align: right !important;
  padding-right: 18px !important;
}

.actions-cell {
  text-align: right;
  padding-right: 18px !important;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.btn-view {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}
.btn-view:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #1e40af;
}

.btn-retry {
  border: 1px solid #fde68a;
  background: #fffbeb;
  color: #b45309;
}
.btn-retry:hover {
  background: #fef3c7;
  border-color: #fcd34d;
  color: #92400e;
}

.btn-delete {
  border: 1px solid #fecdd3;
  background: #fff1f2;
  color: #be123c;
}
.btn-delete:hover {
  background: #ffe4e6;
  border-color: #fda4af;
  color: #9f1239;
}

.sample-list { display:grid; gap:12px; max-height:48vh; overflow:auto; }
.sample-row { display:grid; gap:10px; padding:12px; border:1px solid var(--line); border-radius:7px; background:var(--panel-muted); }
.sample-row label { display:grid; gap:6px; }
.label-with-tip { display:flex; align-items:center; justify-content:space-between; }
.tip-inline { color:var(--muted); font-size:11px; }
.sample-row input,.sample-row textarea { width:100%; border:1px solid var(--line); border-radius:7px; padding:9px 10px; background:var(--panel); color:var(--text); font:inherit; }
.sample-head { display:flex; justify-content:space-between; align-items: center; gap:10px; }
.text-link {
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.text-link:hover {
  color: #1d4ed8;
}
.suite-modal { max-width:640px; }

/* 评测证据容器 */
.evaluation-detail { margin-top: 20px; padding: 20px; border: 1px solid var(--line); border-radius: 10px; background: var(--panel-muted, #f8fafc); display: grid; gap: 16px; }
.detail-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.detail-title-wrap { display: flex; align-items: center; gap: 10px; }
.muted-note { font-size: 12px; color: var(--muted); }

.metric-strip { display: flex; flex-wrap: wrap; gap: 20px; padding: 12px 16px; background: var(--panel); border: 1px solid var(--line); border-radius: 8px; color: var(--muted); font-size: 12px; }
.metric-strip b { color: var(--text); margin-left: 5px; font-weight: 600; }

/* 证据卡片列表 */
.evidence-card-list { display: grid; gap: 16px; }
.evidence-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  padding: 18px 20px;
  display: grid;
  gap: 14px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
  transition: all 0.2s ease;
}
.evidence-card:hover {
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.07);
  border-color: #cbd5e1;
}

.evidence-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}
.card-head-left { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.case-tag { font-weight: 700; font-size: 13px; color: #0f172a; }
.variant-pill { font-size: 11px; padding: 2px 8px; border-radius: 12px; background: #f1f5f9; color: #475569; font-weight: 500; }

.status-pill {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }
.pill-success { background: #ecfdf5; color: #059669; border: 1px solid #a7f3d0; }
.pill-success .status-dot { background: #10b981; box-shadow: 0 0 6px #10b981; }
.pill-fail { background: #fff1f2; color: #e11d48; border: 1px solid #fecdd3; }
.pill-fail .status-dot { background: #f43f5e; }

.similarity-pill {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: 0.2px;
}
.sim-pass { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.sim-fail { background: #fffbeb; color: #b45309; border: 1px solid #fde68a; }

.card-head-right { font-size: 11px; color: #64748b; display: flex; gap: 14px; align-items: center; }
.stat-badge { background: #f8fafc; padding: 2px 8px; border-radius: 6px; border: 1px solid #e2e8f0; }

/* 最佳匹配段落展示条（现代引用条风格） */
.best-segment-bar {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  background: linear-gradient(90deg, #eff6ff 0%, #f8fafc 100%);
  border: 1px solid #bfdbfe;
  border-left: 4px solid #2563eb;
  border-radius: 6px;
  font-size: 12px;
}
.segment-icon { font-size: 14px; line-height: 1.4; }
.segment-body { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; line-height: 1.5; }
.segment-label { font-weight: 700; color: #1e40af; }
.segment-quote { color: #1e293b; font-style: normal; }

/* 诊断提示条（更柔和分层的警示设计） */
.diagnostic-alert {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 6px;
  background: #fff1f2;
  border: 1px solid #fecdd3;
  font-size: 12px;
}
.alert-icon-wrap { font-size: 14px; line-height: 1.4; }
.alert-body { display: grid; gap: 2px; }
.alert-title { font-weight: 700; color: #be123c; font-size: 12px; }
.alert-msg { color: #9f1239; line-height: 1.5; word-break: break-all; }

/* 对比网格 */
.evidence-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.grid-item { display: grid; gap: 8px; }
.grid-item.full-width { grid-column: 1 / -1; }
.item-title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.bubble-tag { font-size: 10px; background: #e2e8f0; color: #475569; padding: 1px 6px; border-radius: 4px; font-weight: normal; }
.rule-hint-pill { font-size: 10px; background: #e0e7ff; color: #3730a3; padding: 1px 6px; border-radius: 4px; font-weight: 500; }

.item-content {
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid #e2e8f0;
}
.input-content { background: #f8fafc; color: #1e293b; word-break: break-word; white-space: pre-wrap; font-family: inherit; }
.expected-content { background: #f8fafc; display: flex; align-items: center; }

.expected-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  padding: 4px 10px;
  border-radius: 6px;
}
.chip-dot { width: 6px; height: 6px; border-radius: 50%; background: #4f46e5; }
.chip-text { font-family: inherit; font-weight: 600; color: #3730a3; font-size: 12px; word-break: break-all; }

/* 大模型回答面板与工具栏 */
.output-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: -2px;
}
.panel-header-left { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.ai-sparkle { font-size: 14px; }
.panel-title { font-size: 12px; font-weight: 700; color: #1e293b; }
.output-count-badge { font-size: 11px; background: #f1f5f9; color: #64748b; padding: 1px 8px; border-radius: 10px; font-weight: 500; }
.highlight-badge { font-size: 11px; background: #fef9c3; color: #854d0e; padding: 1px 8px; border-radius: 10px; border: 1px solid #fef08a; font-weight: 500; }

.panel-header-right { display: flex; align-items: center; gap: 10px; }
.match-success-text { color: #059669; font-size: 11px; font-weight: 600; }
.mini-copy-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  font-weight: 500;
}
.mini-copy-btn:hover { background: #f8fafc; color: #0f172a; border-color: #94a3b8; }

.output-content {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-left: 4px solid #6366f1;
  max-height: 380px;
  overflow-y: auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
  padding: 16px 20px;
}
.output-rendered-text {
  font-size: 13px;
  color: #1e293b;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  line-height: 1.7;
}

/* 优雅的 Markdown 内容深度排版 */
.rich-content :deep(h3),
.rich-content :deep(h4),
.rich-content :deep(h5) {
  margin: 16px 0 8px 0;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 4px;
}
.rich-content :deep(h3) { font-size: 15px; }
.rich-content :deep(h4) { font-size: 14px; }
.rich-content :deep(h5) { font-size: 13px; }

.rich-content :deep(p) {
  margin: 0 0 10px 0;
  color: #334155;
  line-height: 1.7;
}
.rich-content :deep(p:last-child) {
  margin-bottom: 0;
}

.rich-content :deep(ul),
.rich-content :deep(ol) {
  margin: 8px 0 12px 0;
  padding-left: 22px;
}
.rich-content :deep(li) {
  margin: 4px 0;
  color: #334155;
  line-height: 1.65;
}
.rich-content :deep(strong) {
  color: #0f172a;
  font-weight: 600;
}
.rich-content :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 14px;
  border-left: 3px solid #94a3b8;
  background: #f8fafc;
  color: #475569;
  border-radius: 0 6px 6px 0;
  font-size: 12.5px;
}

/* 关键词与段落命中高光样式 */
.eval-hit-term {
  background: #fef08a !important;
  color: #854d0e !important;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
  border-bottom: 2px solid #eab308;
  box-shadow: 0 1px 3px rgba(234, 179, 8, 0.2);
  display: inline;
}

.empty-output-tip {
  color: #94a3b8;
  font-style: italic;
  padding: 8px 0;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(16, 34, 56, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(3px);
}

.modal-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 28px;
  box-shadow: 0 18px 50px rgba(20, 42, 66, 0.18);
}

.modal-card h3 {
  margin: 0 0 16px 0;
  color: var(--text);
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

@media (max-width: 720px) {
  .evidence-grid { grid-template-columns: 1fr; }
  .suite-summary { grid-template-columns: 1fr; }
  .metric-strip { gap: 10px; }
}

@media (max-width: 560px) {
  .modal-card { max-height: calc(100vh - 32px); overflow-y: auto; padding: 18px; }
}
</style>
