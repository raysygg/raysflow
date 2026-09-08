<template>
  <div class="knowledge-workspace">
    <PageToolbar eyebrow="知识运营" title="知识工作台" description="上传、索引和检索企业知识，并持续跟踪处理状态。">
      <template #actions><div class="header-actions">
          <el-select v-model="uploadLanguage" class="language-select" aria-label="上传文档语言">
            <el-option v-for="item in KNOWLEDGE_LANGUAGES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="uploadModelChoice" class="model-select" aria-label="上传使用的 Embedding 模型" :disabled="modelOptionsLoading" @change="loadCurrentGeneration">
            <el-option v-for="item in modelOptions" :key="modelOptionValue(item)" :label="modelOptionLabel(item)" :value="modelOptionValue(item)" />
          </el-select>
        <label class="upload-button" for="knowledge-file-input">上传文档</label>
        <input
          id="knowledge-file-input"
          class="visually-hidden"
          type="file"
          :accept="SUPPORTED_FILE_ACCEPT"
          @change="handleUpload"
        />
      <button class="ghost-btn" :disabled="batchRebuilding || !selectedUploadModel" @click="handleBatchRebuild">
          {{ batchRebuilding ? '提交中...' : '批量重建' }}
        </button>
      </div></template>
    </PageToolbar>
    <ResourceContextRail label="知识资源" name="企业知识库" state="可维护" hint="上传文档后可跟踪解析和索引进度" mark="K" />

    <nav class="task-navigation" aria-label="知识工作台任务域">
      <button
        v-for="item in KNOWLEDGE_TASKS"
        :key="item.key"
        type="button"
        :class="{ active: activeTask === item.key }"
        @click="activeTask = item.key"
      >
        {{ item.label }}
      </button>
    </nav>

    <section v-if="activeTask === 'overview'" class="overview-band">
      <div class="overview-heading">
        <div><strong>当前知识状态</strong><span>先处理影响检索的问题，再调整模型和排序参数。</span></div>
        <button type="button" class="primary-btn compact-action" @click="activeTask = nextTask.key">{{ nextTask.label }}</button>
      </div>
      <div class="overview-metrics">
        <div><small>可用文档</small><strong>{{ indexedDocumentCount }}</strong><span>已进入当前检索范围</span></div>
        <div><small>待处理文档</small><strong>{{ pendingDocumentCount }}</strong><span>包含失败和等待处理</span></div>
        <div><small>内容来源</small><strong>{{ sourceSummaries.length }}</strong><span>上传与连接来源合计</span></div>
        <div><small>当前检索</small><strong>{{ currentGeneration.generationId ? '可以使用' : '需要建立索引' }}</strong><span>{{ currentGeneration.failureCount ? `${currentGeneration.failureCount} 项异常` : '运行正常' }}</span></div>
      </div>
    </section>

    <section v-if="activeTask === 'sources'" class="operations-section">
      <div class="section-heading"><div><h4>内容来源</h4><p>查看每类内容的同步状态和最近更新时间。</p></div></div>
      <div v-if="sourceSummaries.length" class="operations-list">
        <div v-for="source in sourceSummaries" :key="source.name" class="operations-row">
          <span><strong>{{ source.label }}</strong><small>{{ source.count }} 个文档 · 最近更新 {{ formatTime(source.updatedAt) }}</small></span>
          <span class="status-chip" :class="source.failed ? 'danger' : 'success'">{{ source.failed ? '需要处理' : '同步正常' }}</span>
        </div>
      </div>
      <p v-else class="empty-state operations-empty">还没有内容来源，使用右上角“上传文档”添加第一份资料。</p>
      <div v-if="progressState.visible" class="source-progress">
        <span><strong>{{ progressState.phaseLabel }}</strong><small>{{ progressState.message }}</small></span>
        <el-progress :percentage="progressState.percent" :status="progressStatus" :stroke-width="6" />
      </div>
    </section>

    <section v-if="activeTask === 'overview' || activeTask === 'profile'" class="index-strip" aria-label="当前索引版本">
      <div class="index-primary">
        <span class="index-status" :class="indexStatusTone(currentGeneration.status)"></span>
        <div>
          <small>当前向量模型</small>
          <strong>{{ currentGeneration.embeddingModelKey || currentGeneration.profileName || '未配置' }}</strong>
        </div>
      </div>
      <div><small>重排增强</small><strong>{{ currentGeneration.rerankerModelKey ? '已启用 (' + currentGeneration.rerankerModelKey + ')' : '标准语义检索' }}</strong></div>
      <div><small>索引状态</small><strong>{{ currentGeneration.generationId ? '索引就绪' : '未创建' }}</strong></div>
      <div><small>文档 / 切块</small><strong>{{ currentGeneration.totalDocuments || 0 }} / {{ currentGeneration.indexedChunks || 0 }}</strong></div>
      <div><small>异常状态</small><strong :class="{ danger: currentGeneration.failureCount > 0 }">{{ currentGeneration.failureCount ? currentGeneration.failureCount + ' 个文档异常' : '正常' }}</strong></div>
    </section>

    <section v-if="progressState.visible" class="progress-band">
      <div class="progress-copy">
        <strong>{{ progressState.phaseLabel }}</strong>
        <span>{{ progressState.message }}</span>
      </div>
      <el-progress :percentage="progressState.percent" :status="progressStatus" :stroke-width="6" />
      <span class="progress-count">
        {{ progressState.total > 0 ? `${progressState.processed} / ${progressState.total}` : '等待切块统计' }}
      </span>
    </section>

    <section v-show="activeTask === 'documents'" class="asset-section">
      <div class="asset-toolbar">
        <el-input
          v-model="filters.keyword"
          clearable
          class="keyword-input"
          placeholder="按文档名称搜索"
          @keyup.enter="applyFilters"
          @clear="applyFilters"
        />
        <el-select v-model="filters.language" clearable placeholder="全部语言" @change="applyFilters">
          <el-option v-for="item in KNOWLEDGE_LANGUAGES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="全部状态" @change="applyFilters">
          <el-option v-for="item in KNOWLEDGE_STATUSES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <button class="primary-btn compact-action" @click="applyFilters">查询</button>
        <button class="icon-button" title="刷新文档列表" aria-label="刷新文档列表" @click="refreshAssets">↻</button>
        <span class="asset-count">共 {{ pagination.total }} 个文档</span>
      </div>

      <p v-if="loadError" class="status-message">{{ loadError }}</p>
      <el-table
        v-loading="documentsLoading"
        :data="documents"
        row-key="id"
        class="asset-table"
        empty-text="当前筛选条件下没有文档"
        @row-click="openDocumentDetail"
      >
        <el-table-column label="文档" min-width="270">
          <template #default="{ row }">
            <div class="document-cell">
              <span class="source-mark" :class="'tone-' + fileTypeInfo(row.title).tone">
                {{ fileTypeInfo(row.title).label }}
              </span>
              <div class="document-meta">
                <strong :title="row.title">{{ row.title }}</strong>
                <small>{{ row.sourceType || '未知来源' }} · 版本 {{ row.versionNo || 1 }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="语言" width="110">
          <template #default="{ row }">
            <div class="language-cell">
              <span>{{ knowledgeLanguageLabel(row.language) }}</span>
              <small v-if="!row.languageConfirmed">待确认</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="索引状态" width="130">
          <template #default="{ row }">
            <span class="status-chip" :class="statusTone(row.indexStatus || row.status)">
              {{ knowledgeStatusLabel(row.indexStatus || row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切块" width="90" align="right" />
        <el-table-column label="索引版本" width="120">
          <template #default="{ row }">{{ row.indexGenerationId ? `#${row.indexGenerationId}` : '--' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="176">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="right">
          <template #default="{ row }">
            <div class="row-action-group">
              <button class="link-btn view" title="查看详情" @click.stop="openDocumentDetail(row)">查看</button>
              <button class="link-btn delete" title="删除文档" @click.stop="removeDocumentItem(row)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <span class="total-label">Total {{ pagination.total }}</span>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="PAGE_SIZE_OPTIONS"
          layout="sizes, prev, pager, next"
          @current-change="loadDocuments"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <section v-show="activeTask === 'profile' || activeTask === 'evaluation'" class="retrieval-section">
      <div class="section-heading">
        <div><h4>{{ activeTask === 'profile' ? '检索配置验证' : '检索效果评测' }}</h4><p>{{ activeTask === 'profile' ? '确认模型、索引和排序策略是否达到预期。' : '用实际问题比较召回质量、耗时和上下文完整度。' }}</p></div>
         <el-select v-model="searchModelChoice" class="search-model" aria-label="检索使用的 Embedding 模型" :disabled="modelOptionsLoading" @change="loadSearchGeneration">
           <el-option v-for="item in modelOptions" :key="modelOptionValue(item)" :label="modelOptionLabel(item)" :value="modelOptionValue(item)" />
         </el-select>
      </div>
      <div class="search-row">
        <el-input v-model="searchForm.query" placeholder="输入需要验证的自然语言问题（支持跨语言自动匹配）" @keyup.enter="runSearch" />
        <button class="primary-btn" :disabled="searching || !searchModelReady" @click="runSearch">{{ searching ? '检索中...' : '执行检索' }}</button>
      </div>
      <p v-if="searchModelChoice && !searchModelReady" class="model-index-warning">提示：选择的检索模型“{{ selectedSearchModel?.modelName || '未构建模型' }}”尚无可用向量索引（当前生效索引模型为：{{ currentGeneration.embeddingModelKey || '其它模型' }}）。如需使用该模型测试，请先提交【批量重建】。</p>
      <p v-if="searchError" class="status-message">{{ searchError }}</p>

      <div v-if="searchOutcome" class="retrieval-summary">
        <div><small>初召回数</small><strong>{{ searchOutcome.initialCandidateCount || 0 }}</strong></div>
        <div><small>重排候选</small><strong>{{ searchOutcome.rerankedCandidateCount || 0 }}</strong></div>
        <div><small>最终命中</small><strong>{{ searchOutcome.hitCount || 0 }}</strong></div>
        <div><small>关联匹配度</small><strong>{{ formatScore(searchOutcome.topScore) }}</strong></div>
        <div><small>上下文完整度</small><strong>{{ formatPercent(searchOutcome.parentCoverage) }}</strong></div>
        <div><small>检索模式</small><strong>{{ rerankModeLabel(searchOutcome.rerankMode) }}</strong></div>
        <div><small>检索耗时</small><strong>{{ searchOutcome.elapsedMs || 0 }} ms</strong></div>
        <span v-if="searchOutcome.queryRewritten" class="summary-flag">已智能扩充</span>
        <span v-if="searchOutcome.noHit" class="summary-flag neutral">无高相关结果</span>
      </div>
      <details v-if="searchOutcome" class="retrieval-debug">
        <summary>高级检索摘要</summary>
        <div class="debug-grid">
          <span>模型来源：{{ modelSourceLabel(searchOutcome.modelSource) }}</span>
          <span>召回通道：{{ channelLabels(searchOutcome.channels) }}</span>
          <span>降级状态：{{ degradeStatusLabel(searchOutcome) }}</span>
          <span>通道状态：{{ channelExecutionLabels(searchOutcome.debugSummary?.channelExecutions) }}</span>
          <span>上下文跳过：{{ contextSkipLabels(searchOutcome.debugSummary?.skippedContexts) }}</span>
          <span>首位差值：{{ formatScore(searchOutcome.scoreGap) }}</span>
          <span>Embedding：{{ searchOutcome.stageTimings?.embeddingMs || 0 }} ms</span>
          <span>重排：{{ searchOutcome.stageTimings?.rerankMs || 0 }} ms</span>
          <span>Dense 最高分：{{ formatScore(searchOutcome.debugSummary?.scoreLayers?.dense) }}</span>
          <span>Sparse 最高分：{{ formatScore(searchOutcome.debugSummary?.scoreLayers?.sparse) }}</span>
          <span>Fusion 最高分：{{ formatScore(searchOutcome.debugSummary?.scoreLayers?.fusion) }}</span>
        </div>
      </details>

      <p v-if="searchOutcome && searchResults.length === 0" class="empty-state">本次检索没有通过当前 Profile 的无命中校准。</p>
      <div v-else-if="searchResults.length" class="result-list">
        <article v-for="(item, index) in searchResults" :key="`${item.documentId}-${item.parentChunkId}-${index}`" class="result-row">
          <div class="result-rank">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="result-main">
            <div class="result-head">
              <div><strong>{{ item.source || `文档 ${item.documentId}` }}</strong><small>{{ item.sectionPath || '未提供章节路径' }} · 切块 {{ chunkRange(item) }} · {{ channelLabels(item.channels || [item.channel]) }}</small></div>
              <span>{{ formatScore(item.score) }}</span>
            </div>
            <div class="result-content rich-content" v-html="renderMarkdown(item.text)"></div>
          </div>
        </article>
      </div>
    </section>

    <section v-if="activeTask === 'quality'" class="operations-section">
      <div class="section-heading"><div><h4>质量待办</h4><p>集中处理解析、语言、切块和索引问题。</p></div></div>
      <div v-if="qualityFindings.length" class="operations-list">
        <div v-for="finding in qualityFindings" :key="finding.id" class="operations-row">
          <span><strong>{{ finding.title }}</strong><small>{{ finding.reason }}</small></span>
          <button type="button" class="link-btn" @click="activeTask = finding.target">去处理</button>
        </div>
      </div>
      <p v-else class="empty-state operations-empty">当前没有需要处理的质量问题。</p>
    </section>

    <el-drawer v-model="detailVisible" size="min(560px, 94vw)" :with-header="false" destroy-on-close>
      <div class="detail-drawer">
        <header class="drawer-header">
          <div><div class="page-kicker">文档索引详情</div><h4>{{ selectedDocument?.title || '文档详情' }}</h4></div>
          <button class="icon-button" title="关闭详情" aria-label="关闭详情" @click="detailVisible = false">×</button>
        </header>
        <div v-loading="detailLoading" class="drawer-content">
          <section class="detail-grid">
            <div><small>语言</small><strong>{{ knowledgeLanguageLabel(selectedDocument?.language) }}</strong></div>
            <div><small>语言确认</small><strong>{{ selectedDocument?.languageConfirmed ? '已确认' : '待确认' }}</strong></div>
            <div><small>文档状态</small><strong>{{ knowledgeStatusLabel(selectedDocument?.status) }}</strong></div>
            <div><small>索引状态</small><strong>{{ knowledgeStatusLabel(documentDetail.indexStatus) }}</strong></div>
            <div><small>切块数量</small><strong>{{ selectedDocument?.chunkCount || 0 }}</strong></div>
            <div><small>索引版本</small><strong>{{ documentDetail.indexGenerationId ? `#${documentDetail.indexGenerationId}` : '--' }}</strong></div>
          </section>
          <section class="detail-block">
            <h5>模型配置</h5>
            <dl>
              <div><dt>Profile</dt><dd>{{ documentDetail.embeddingProfileName || '--' }}</dd></div>
              <div><dt>Embedding</dt><dd>{{ documentDetail.embeddingModelKey || '--' }}</dd></div>
              <div><dt>Reranker</dt><dd>{{ documentDetail.rerankerModelKey || '未配置' }}</dd></div>
              <div><dt>完成时间</dt><dd>{{ formatTime(documentDetail.indexedAt) }}</dd></div>
            </dl>
          </section>
          <section v-if="documentDetail.lastError" class="detail-error">
            <strong>最近失败原因</strong><p>{{ documentDetail.lastError }}</p>
          </section>
          <section class="detail-block">
            <div class="detail-block-head"><h5>索引任务</h5><button class="link-btn" @click="reindexSelectedDocument">重新索引</button></div>
            <p v-if="indexTasks.length === 0" class="empty-state compact-empty">暂无索引任务记录。</p>
            <div v-for="task in indexTasks" :key="task.id" class="task-row">
              <div><strong>#{{ task.id }} · {{ task.status }}</strong><small>{{ formatTime(task.createdAt) }}</small></div>
              <span>{{ task.retryCount || 0 }} / {{ task.maxRetries || 0 }} 次重试</span>
              <p v-if="task.errorMessage">{{ task.errorMessage }}</p>
            </div>
          </section>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  KNOWLEDGE_DEFAULT_PAGE_SIZE,
  KNOWLEDGE_LANGUAGES,
  KNOWLEDGE_STATUSES,
  deleteKnowledgeDocument,
  fetchCurrentKnowledgeIndexGeneration,
  fetchKnowledgeDocumentIndexState,
  fetchKnowledgeDocumentIndexTasks,
  fetchKnowledgeDocuments,
  fetchRagEmbeddingModels,
  knowledgeLanguageLabel,
  knowledgeStatusLabel,
  searchKnowledge,
  submitKnowledgeDocumentReindex,
  submitKnowledgeIndexRebuild,
  uploadKnowledgeDocument,
} from '../api/knowledge'
import { apiErrorMessage, requireApiSuccess } from '../api/contracts'
import { subscribeKnowledgeProgress } from '../api/knowledge-progress'
import { confirmAction, notify } from '../utils/feedback'
import { renderMarkdown } from '../utils/markdown'

const SUPPORTED_FILE_ACCEPT = '.txt,.md,.markdown,.csv,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.html,.htm'
const PAGE_SIZE_OPTIONS = Object.freeze([10, KNOWLEDGE_DEFAULT_PAGE_SIZE, 50, 100])
const TERMINAL_PROGRESS_PHASES = new Set(['COMPLETED', 'FAILED'])
const RETRIEVAL_CHANNEL_FAILURE = 'RETRIEVAL_CHANNEL_FAILURE'
const CHANNEL_EXECUTION_STATUS_LABELS = Object.freeze({ SUCCESS: '正常', FAILED: '不可用', SKIPPED: '未触发' })
const CONTEXT_SKIP_REASON_LABELS = Object.freeze({ TOKEN_BUDGET: 'Token 预算', EMPTY_CONTENT: '空内容' })
const KNOWLEDGE_TASKS = Object.freeze([
  { key: 'overview', label: '概览' },
  { key: 'sources', label: '来源' },
  { key: 'documents', label: '文档' },
  { key: 'profile', label: '检索配置' },
  { key: 'quality', label: '质量' },
  { key: 'evaluation', label: '评测' },
])

const activeTask = ref('overview')
const documents = ref([])
const documentsLoading = ref(false)
const loadError = ref('')
  const uploadLanguage = ref('OTHER')
const modelOptions = ref([])
const modelOptionsLoading = ref(false)
const uploadModelChoice = ref('')
const searchModelChoice = ref('')
const batchRebuilding = ref(false)
const currentGeneration = ref({})
const searchGeneration = ref({})
const filters = reactive({ keyword: '', language: '', status: '' })
const pagination = reactive({ page: 1, size: KNOWLEDGE_DEFAULT_PAGE_SIZE, total: 0 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const selectedDocument = ref(null)
const documentDetail = ref({})
const indexTasks = ref([])

const searchForm = reactive({ query: '', queryLanguage: 'ZH' })
const searching = ref(false)
const searchError = ref('')
const searchOutcome = ref(null)
const searchResults = computed(() => searchOutcome.value?.results || [])
const selectedUploadModel = computed(() => modelOptions.value.find(item => modelOptionValue(item) === uploadModelChoice.value) || null)
const selectedSearchModel = computed(() => modelOptions.value.find(item => modelOptionValue(item) === searchModelChoice.value) || null)
const searchModelReady = computed(() => Number(searchGeneration.value?.indexedChunks || 0) > 0)
const indexedDocumentCount = computed(() => documents.value.filter(item => ['INDEXED', 'COMPLETED'].includes(item.indexStatus || item.status)).length)
const pendingDocumentCount = computed(() => Math.max(0, Number(pagination.total || 0) - indexedDocumentCount.value))
const sourceSummaries = computed(() => {
  const summaries = new Map()
  documents.value.forEach(item => {
    const name = item.sourceType || 'UPLOAD'
    const current = summaries.get(name) || { name, label: sourceTypeLabel(name), count: 0, failed: false, updatedAt: null }
    current.count += 1
    current.failed ||= ['FAILED', 'INDEX_FAILED'].includes(item.indexStatus || item.status)
    if (!current.updatedAt || new Date(item.updatedAt || 0) > new Date(current.updatedAt || 0)) current.updatedAt = item.updatedAt
    summaries.set(name, current)
  })
  return [...summaries.values()]
})
const qualityFindings = computed(() => {
  const findings = []
  const failed = documents.value.filter(item => ['FAILED', 'INDEX_FAILED'].includes(item.indexStatus || item.status)).length
  const unconfirmed = documents.value.filter(item => !item.languageConfirmed).length
  if (failed) findings.push({ id: 'index-failed', title: `${failed} 个文档处理失败`, reason: '重新索引前先查看文档失败原因。', target: 'documents' })
  if (unconfirmed) findings.push({ id: 'language', title: `${unconfirmed} 个文档语言待确认`, reason: '确认语言后可以获得更稳定的关键词召回。', target: 'documents' })
  if (Number(currentGeneration.value?.failureCount || 0) > 0) findings.push({ id: 'generation', title: '当前索引存在异常', reason: '检查失败文档后再提交重建。', target: 'profile' })
  return findings
})
const nextTask = computed(() => {
  if (qualityFindings.value.length) return { key: 'quality', label: '处理质量问题' }
  if (!currentGeneration.value?.generationId) return { key: 'profile', label: '建立检索索引' }
  return { key: 'evaluation', label: '验证检索效果' }
})

const progressState = reactive({
  visible: false,
  documentId: null,
  phase: '',
  phaseLabel: '等待处理',
  percent: 0,
  processed: 0,
  total: 0,
  message: '',
})
const progressStatus = computed(() => progressState.phase === 'FAILED' ? 'exception' : (progressState.phase === 'COMPLETED' ? 'success' : ''))
let stopProgressRealtime = null

const modelOptionValue = option => `${option.source}:${option.modelId || option.modelKey}`
const modelOptionLabel = option => `${option.modelName} · ${option.sourceLabel} · ${option.vectorDimension}维`
const loadEmbeddingModels = async () => {
  modelOptionsLoading.value = true
  try {
    modelOptions.value = requireApiSuccess(await fetchRagEmbeddingModels(), '读取 Embedding 模型失败。') || []
  } catch (error) {
    notify(apiErrorMessage(error, '读取 Embedding 模型失败，请检查模型中心配置。'), 'warning')
  } finally {
    modelOptionsLoading.value = false
  }
}

const syncModelChoicesWithActiveGeneration = () => {
  if (!modelOptions.value.length) return
  // 优先匹配当前文档已有生效索引的模型；无生效索引时才回退至默认推荐模型
  const activeModelKey = currentGeneration.value?.embeddingModelKey
  const activeOption = (activeModelKey && modelOptions.value.find(item => item.modelKey === activeModelKey))
    || modelOptions.value.find(item => item.recommended)
    || modelOptions.value[0]
  if (activeOption) {
    const val = modelOptionValue(activeOption)
    if (!uploadModelChoice.value) uploadModelChoice.value = val
    if (!searchModelChoice.value) searchModelChoice.value = val
  }
}

const loadDocuments = async () => {
  documentsLoading.value = true
  loadError.value = ''
  try {
    const page = requireApiSuccess(await fetchKnowledgeDocuments({ ...filters, ...pagination }), '读取知识文档失败。')
    documents.value = page.items || []
    pagination.total = page.total || 0
  } catch (error) {
    loadError.value = apiErrorMessage(error, '读取知识文档失败，请检查网络或后端服务。')
  } finally {
    documentsLoading.value = false
  }
}

const removeDocumentItem = async (doc) => {
  if (!doc || !doc.id) return
  if (!await confirmAction({ title: '删除知识文档', message: `确定要彻底删除文档“${doc.title || doc.id}”吗？关联的切块索引与向量数据也将同步删除。` })) return
  try {
    requireApiSuccess(await deleteKnowledgeDocument(doc.id), '文档删除失败。')
    notify(`文档“${doc.title || doc.id}”已成功删除。`, 'success')
    await loadDocuments()
  } catch (error) {
    notify(apiErrorMessage(error, '文档删除失败。'), 'error')
  }
}

const loadCurrentGeneration = async () => {
  try {
    currentGeneration.value = requireApiSuccess(await fetchCurrentKnowledgeIndexGeneration(selectedUploadModel.value), '读取索引版本失败。') || {}
  } catch (error) {
    notify(apiErrorMessage(error, '读取索引版本失败。'), 'warning')
  }
}

const loadSearchGeneration = async () => {
  searchGeneration.value = {}
  if (!selectedSearchModel.value) return
  try {
    searchGeneration.value = requireApiSuccess(
      await fetchCurrentKnowledgeIndexGeneration(selectedSearchModel.value),
      '读取检索模型索引失败。',
    ) || {}
  } catch (error) {
    notify(apiErrorMessage(error, '读取检索模型索引失败。'), 'warning')
  }
}

const refreshAssets = async () => {
  await loadEmbeddingModels()
  await loadCurrentGeneration()
  syncModelChoicesWithActiveGeneration()
  await Promise.all([loadDocuments(), loadSearchGeneration()])
}
const applyFilters = () => { pagination.page = 1; loadDocuments() }
const handlePageSizeChange = () => { pagination.page = 1; loadDocuments() }

const openDocumentDetail = async row => {
  selectedDocument.value = row
  detailVisible.value = true
  detailLoading.value = true
  documentDetail.value = {}
  indexTasks.value = []
  try {
    const [detailResponse, taskResponse] = await Promise.all([
      fetchKnowledgeDocumentIndexState(row.id),
      fetchKnowledgeDocumentIndexTasks(row.id),
    ])
    documentDetail.value = requireApiSuccess(detailResponse, '读取文档索引详情失败。') || {}
    indexTasks.value = requireApiSuccess(taskResponse, '读取索引任务失败。') || []
  } catch (error) {
    notify(apiErrorMessage(error, '读取文档索引详情失败。'), 'error')
  } finally {
    detailLoading.value = false
  }
}

const handleUpload = async event => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  try {
    const document = requireApiSuccess(await uploadKnowledgeDocument(file, uploadLanguage.value, selectedUploadModel.value), '上传文档失败。')
    notify(`文档“${file.name}”已提交索引。`, 'success')
    if (document?.id) connectProgressRealtime(document.id)
    await refreshAssets()
  } catch (error) {
    notify(apiErrorMessage(error, '文档上传或索引提交失败。'), 'error')
  }
}

const handleBatchRebuild = async () => {
  const confirmed = await confirmAction({
    title: '批量重建知识索引',
    message: '后台会按文档拆分任务。当前可用索引会继续服务，单个文档失败不会中断其他任务。',
    confirmText: '提交重建',
    tone: 'warning',
  })
  if (!confirmed) return
  batchRebuilding.value = true
  try {
    const submission = requireApiSuccess(await submitKnowledgeIndexRebuild(selectedUploadModel.value), '提交批量重建失败。')
    notify(`${submission.message || '批量重建任务已提交'} 任务 #${submission.taskId}`, 'success')
  } catch (error) {
    notify(apiErrorMessage(error, '提交批量重建失败。'), 'error')
  } finally {
    batchRebuilding.value = false
  }
}

const reindexSelectedDocument = async () => {
  if (!selectedDocument.value?.id) return
  try {
    requireApiSuccess(await submitKnowledgeDocumentReindex(selectedDocument.value.id, selectedUploadModel.value), '提交重建失败。')
    notify('文档已进入重建队列。', 'success')
    connectProgressRealtime(selectedDocument.value.id)
    detailVisible.value = false
  } catch (error) {
    notify(apiErrorMessage(error, '提交文档重建失败。'), 'error')
  }
}

const runSearch = async () => {
  if (!searchForm.query.trim()) {
    notify('请输入需要验证的问题。', 'warning')
    return
  }
  searching.value = true
  searchError.value = ''
  searchOutcome.value = null
  try {
    searchOutcome.value = requireApiSuccess(await searchKnowledge({
      query: searchForm.query.trim(),
      queryLanguage: searchForm.queryLanguage,
      modelSelection: selectedSearchModel.value,
    }), '知识检索失败。')
  } catch (error) {
    searchError.value = apiErrorMessage(error, '知识检索失败，请检查模型和 Qdrant 服务。')
  } finally {
    searching.value = false
  }
}

/** WebSocket 只传递进度快照；终态后重新读取 MySQL，避免页面把实时消息当作资产事实源。 */
const connectProgressRealtime = documentId => {
  stopProgressRealtime?.()
  progressState.visible = true
  progressState.documentId = documentId
  progressState.phase = 'QUEUED'
  progressState.phaseLabel = '等待处理'
  progressState.percent = 0
  progressState.message = '正在建立实时连接...'
  stopProgressRealtime = subscribeKnowledgeProgress(documentId, {
    onProgress: data => {
      progressState.phase = data.phase || ''
      progressState.phaseLabel = data.phaseLabel || knowledgeStatusLabel(data.phase)
      progressState.percent = Number(data.progressPercent || 0)
      progressState.processed = Number(data.processedChunks || 0)
      progressState.total = Number(data.totalChunks || 0)
      progressState.message = data.message || ''
      if (TERMINAL_PROGRESS_PHASES.has(data.phase)) {
        window.setTimeout(async () => {
          stopProgressRealtime?.()
          stopProgressRealtime = null
          await refreshAssets()
        }, 1200)
      }
    },
    onError: error => { progressState.message = error.message || '实时连接异常，客户端将自动恢复。' },
  })
}

const sourceMark = source => String(source || 'DOC').slice(0, 4).toUpperCase()
const sourceTypeLabel = source => ({ UPLOAD: '上传资料', TEXT: '文本资料', CSV: '表格资料', CONNECTOR: '外部连接' }[source] || '其他来源')
const fileTypeInfo = title => {
  const ext = String(title || '').split('.').pop()?.toLowerCase() || ''
  const map = {
    txt: { label: 'TXT', tone: 'text' },
    md: { label: 'MD', tone: 'markdown' },
    markdown: { label: 'MD', tone: 'markdown' },
    csv: { label: 'CSV', tone: 'sheet' },
    xls: { label: 'XLS', tone: 'sheet' },
    xlsx: { label: 'XLSX', tone: 'sheet' },
    pdf: { label: 'PDF', tone: 'pdf' },
    doc: { label: 'DOC', tone: 'doc' },
    docx: { label: 'DOCX', tone: 'doc' },
    ppt: { label: 'PPT', tone: 'doc' },
    pptx: { label: 'PPTX', tone: 'doc' },
    html: { label: 'HTML', tone: 'web' },
    htm: { label: 'HTM', tone: 'web' },
  }
  return map[ext] || { label: sourceMark(title), tone: 'default' }
}
const formatTime = value => value ? new Date(value).toLocaleString('zh-CN') : '--'
const formatScore = value => Number(value || 0).toFixed(4)
const formatPercent = value => `${(Number(value || 0) * 100).toFixed(0)}%`
const chunkRange = item => Array.isArray(item.chunkNumbers) && item.chunkNumbers.length ? item.chunkNumbers.join('、') : (item.chunkNo || '--')
const rerankModeLabel = mode => ({ STANDARD_HYBRID: '标准混合检索', MULTILINGUAL_RERANKER: '多语言重排', HYBRID_FALLBACK: '混合检索回退', VECTOR_FALLBACK: '标准混合检索' }[mode] || '标准混合检索')
const modelSourceLabel = source => ({ TENANT_PRIVATE: '租户模型', PLATFORM_SHARED: '平台模型', LOCAL: '平台本地模型' }[source] || '平台模型')
const channelLabels = channels => (channels || []).map(channel => ({ VECTOR: 'Dense', LEXICAL_ZH: '中文 Sparse', LEXICAL_EN: '英文 Sparse', LEXICAL_GENERAL: '通用 Sparse', EXACT: '精确编号' }[channel] || channel)).join('、') || '无'
const degradeStatusLabel = outcome => !outcome?.degraded ? '未降级' : outcome.degradeReason === RETRIEVAL_CHANNEL_FAILURE ? '部分检索通道不可用' : '已按策略回退'
const channelExecutionLabels = executions => (executions || []).map(item => `${channelLabels([item.channel])}：${CHANNEL_EXECUTION_STATUS_LABELS[item.status] || item.status}`).join('；') || '未执行'
const contextSkipLabels = skipped => !(skipped || []).length ? '无' : `${skipped.length} 个（${[...new Set(skipped.map(item => CONTEXT_SKIP_REASON_LABELS[item.reason] || item.reason))].join('、')}）`
const statusTone = status => ({ INDEXED: 'success', COMPLETED: 'success', INDEXING: 'running', QUEUED: 'running', REINDEX_REQUIRED: 'warning', PENDING: 'warning', INDEX_FAILED: 'danger', FAILED: 'danger', DISABLED: 'neutral' }[status] || 'neutral')
const indexStatusTone = status => statusTone(status === 'ACTIVE' ? 'INDEXED' : status)

onMounted(refreshAssets)
onBeforeUnmount(() => stopProgressRealtime?.())
</script>

<style scoped>
.knowledge-workspace { display: grid; gap: 18px; min-width: 0; }
.workspace-header { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 2px 0 2px; }
.workspace-header h3, .section-heading h4, .drawer-header h4 { margin: 6px 0 0; color: var(--text); font-size: 20px; font-weight: 600; }
.page-kicker { color: var(--accent); font-size: 12px; font-weight: 700; letter-spacing: 0; }
.header-actions { display: flex; align-items: center; gap: 10px; }
.task-navigation { display: flex; gap: 2px; min-height: 42px; padding: 4px; border: 1px solid #e2e8f0; border-radius: 7px; background: #f8fafc; }
.task-navigation button { flex: 1; min-width: 92px; border: 0; border-radius: 5px; background: transparent; color: #64748b; font-size: 13px; font-weight: 600; cursor: pointer; }
.task-navigation button:hover { background: #eef4fa; color: #1e3a5f; }
.task-navigation button.active { background: #fff; color: #1d4ed8; box-shadow: 0 1px 3px rgba(15, 23, 42, .1); }
.overview-band, .operations-section { border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; overflow: hidden; }
.overview-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 16px 18px; border-bottom: 1px solid #e2e8f0; }
.overview-heading > div, .operations-row > span, .source-progress > span { display: grid; gap: 4px; min-width: 0; }
.overview-heading strong, .operations-row strong, .source-progress strong { color: #0f172a; font-size: 13px; }
.overview-heading span, .operations-row small, .source-progress small { color: #64748b; font-size: 12px; }
.overview-metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.overview-metrics > div { display: grid; gap: 5px; min-width: 0; padding: 16px 18px; border-right: 1px solid #e2e8f0; }
.overview-metrics > div:last-child { border-right: 0; }
.overview-metrics small { color: #64748b; font-size: 11px; }
.overview-metrics strong { overflow: hidden; color: #0f172a; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.overview-metrics span { color: #94a3b8; font-size: 11px; }
.operations-section > .section-heading { padding: 16px 18px; border-bottom: 1px solid #e2e8f0; }
.operations-list { display: grid; }
.operations-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 62px; padding: 12px 18px; border-bottom: 1px solid #edf2f7; }
.operations-row:last-child { border-bottom: 0; }
.operations-empty { margin: 0; padding: 28px 18px; }
.source-progress { display: grid; grid-template-columns: minmax(180px, .6fr) minmax(260px, 1.4fr); align-items: center; gap: 20px; padding: 14px 18px; border-top: 1px solid #e2e8f0; background: #f8fafc; }
.asset-toolbar { display: flex; align-items: center; gap: 10px; padding: 16px; border-bottom: 1px solid #edf2f7; background: #fff; }
.search-row { display: flex; align-items: center; gap: 10px; }
.header-actions :deep(.el-select),
.asset-toolbar :deep(.el-select) { height: 36px; }
.header-actions :deep(.el-input__wrapper),
.asset-toolbar :deep(.el-input__wrapper),
.header-actions :deep(.el-select .el-select__wrapper),
.asset-toolbar :deep(.el-select .el-select__wrapper) {
  box-shadow: inset 0 0 0 1px #d9e2ec;
  border-radius: 6px;
  background: #fff;
}
.header-actions :deep(.el-select:hover .el-select__wrapper),
.asset-toolbar :deep(.el-select:hover .el-select__wrapper),
.header-actions :deep(.el-input__wrapper:hover),
.asset-toolbar :deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px #94a3b8;
}
.language-select { width: 160px; }
.model-select { width: 280px; }
.upload-button { display: inline-flex; align-items: center; justify-content: center; gap: 4px; height: 36px; padding: 0 18px; border-radius: 6px; background: var(--accent); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; transition: background .15s; }
.upload-button:hover { filter: brightness(1.05); }
.upload-button::before { content: '+'; font-size: 15px; line-height: 1; font-weight: 700; }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
.index-strip { display: flex; border: 1px solid #edf2f7; border-radius: 10px; overflow: hidden; background: #fff; }
.index-strip > div { display: flex; min-width: 0; flex: 1; flex-direction: column; justify-content: center; gap: 5px; min-height: 76px; padding: 14px 20px; border-right: 1px solid #edf2f7; }
.index-strip > div:last-child { flex: 0 0 auto; width: 120px; background: #f0f4f8; border-right: 0; }
.index-strip small, .detail-grid small, .retrieval-summary small { color: #64748b; font-size: 11px; font-weight: 500; }
.index-strip strong, .detail-grid strong, .retrieval-summary strong { overflow: hidden; color: #0f172a; font-size: 14px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.index-primary { flex: 1.4 !important; flex-direction: row !important; align-items: center; justify-content: flex-start !important; gap: 12px; }
.index-primary > div { display: grid; gap: 5px; min-width: 0; }
.index-status { flex: 0 0 auto; width: 12px; height: 12px; border-radius: 50%; background: #94a3b8; box-shadow: 0 0 0 3px rgba(148,163,184,0.18); }
.index-status.success { background: #22c55e; box-shadow: 0 0 0 4px rgba(34,197,94,0.15); }
.index-status.running { background: #3b82f6; box-shadow: 0 0 0 4px rgba(59,130,246,0.15); }
.index-status.warning { background: #f59e0b; box-shadow: 0 0 0 4px rgba(245,158,11,0.15); }
.index-status.danger { background: #ef4444; box-shadow: 0 0 0 4px rgba(239,68,68,0.15); }
.danger { color: var(--danger) !important; }
.progress-band { display: grid; grid-template-columns: minmax(220px, .8fr) minmax(260px, 1.4fr) auto; align-items: center; gap: 16px; padding: 10px 14px; border: 1px solid #cfe0f0; border-radius: 7px; background: #f5f9fd; }
.progress-copy { display: grid; gap: 3px; }.progress-copy strong { color: #234a6e; font-size: 12px; }.progress-copy span, .progress-count { color: var(--muted); font-size: 11px; }
.asset-section, .retrieval-section { border: 1px solid #edf2f7; border-radius: 10px; background: #fff; overflow: hidden; }
.asset-toolbar { display: flex; align-items: center; gap: 12px; padding: 18px; border-bottom: 1px solid #edf2f7; background: #fff; }
.asset-toolbar :deep(.el-select) { width: 160px; }
.keyword-input { width: min(400px, 40vw); }
.compact-action { min-height: 36px; padding: 0 20px; border-radius: 6px; font-weight: 600; font-size: 13px; }
.icon-button { display: inline-grid; width: 36px; height: 36px; place-items: center; border: 1px solid #d9e2ec; border-radius: 6px; background: #fff; color: #475569; font-size: 16px; cursor: pointer; transition: all .15s; }
.icon-button:hover { border-color: #94a3b8; background: #f8fafc; color: #0f172a; }
.asset-count { margin-left: auto; color: #475569; font-size: 13px; font-weight: 500; }
.asset-table { width: 100%; cursor: pointer; --el-table-row-hover-bg-color: #f8fafc; }
.asset-table :deep(.el-table__row) { transition: background .15s; }
.asset-table :deep(.el-table__cell) { padding: 16px 0 !important; }
.asset-table :deep(.el-table__header th) { background: #fff; color: #475569; font-size: 12px; font-weight: 600; }
.document-cell { display: flex; align-items: center; gap: 14px; min-width: 0; }
.document-meta { min-width: 0; }
.source-mark { display: grid; flex: 0 0 auto; width: 46px; height: 46px; place-items: center; border-radius: 8px; background: #f1f5f9; color: #475569; font: 700 10px/1.2 Consolas, monospace; }
.source-mark.tone-text { background: #ecfdf5; color: #059669; }
.source-mark.tone-markdown { background: #f5f3ff; color: #8b5cf6; }
.source-mark.tone-sheet { background: #ecfdf5; color: #059669; }
.source-mark.tone-pdf { background: #fef2f2; color: #ef4444; }
.source-mark.tone-doc { background: #eff6ff; color: #2563eb; }
.source-mark.tone-web { background: #fff7ed; color: #f97316; }
.source-mark.tone-default { background: #f1f5f9; color: #475569; }
.document-cell strong, .document-cell small, .language-cell small { display: block; }
.document-cell strong { overflow: hidden; color: #0f172a; font-size: 14px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.document-cell small, .result-head small { margin-top: 5px; color: #64748b; font-size: 12px; }
.language-cell { display: flex; flex-direction: column; gap: 3px; }
.language-cell small { color: #d97706; font-size: 10px; font-weight: 600; }
.status-chip { display: inline-flex; align-items: center; gap: 6px; padding: 5px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; }
.status-chip::before { content: ''; width: 7px; height: 7px; border-radius: 50%; background: currentColor; opacity: .7; }
.status-chip.success { background: #dcfce7; color: #15803d; }
.status-chip.running { background: #dbeafe; color: #1d4ed8; }
.status-chip.warning { background: #fef3c7; color: #b45309; }
.status-chip.danger { background: #fee2e2; color: #b91c1c; }
.status-chip.neutral { background: #f1f5f9; color: #475569; }
.pagination-row { display: flex; justify-content: flex-end; align-items: center; gap: 8px; padding: 14px 18px; border-top: 1px solid #edf2f7; }
.pagination-row :deep(.el-pagination) { --el-pagination-hover-color: var(--accent); }
.pagination-row .total-label { color: #475569; font-size: 13px; font-weight: 500; margin-right: 8px; }
.row-action-group { display: flex; align-items: center; gap: 12px; justify-content: flex-end; }
.row-action-group .link-btn { border: 0; background: transparent; padding: 0; cursor: pointer; font-size: 13px; font-weight: 500; }
.row-action-group .link-btn.view { color: #2563eb; }
.row-action-group .link-btn.view:hover { text-decoration: underline; }
.row-action-group .link-btn.delete { color: #ef4444; }
.row-action-group .link-btn.delete:hover { text-decoration: underline; }
.retrieval-section { padding: 20px; }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; }
.section-heading h4 { font-size: 16px; font-weight: 600; color: #0f172a; }
.section-heading p { margin: 6px 0 0; color: #64748b; font-size: 13px; }
.search-language { width: 130px; }
.search-model { width: 320px; }
.search-row { margin-top: 16px; gap: 12px; }
.search-row :deep(.el-input) { flex: 1; }
.section-heading :deep(.el-select .el-select__wrapper) {
  box-shadow: inset 0 0 0 1px #d9e2ec;
  border-radius: 6px;
  background: #fff;
}
.section-heading :deep(.el-select:hover .el-select__wrapper) {
  box-shadow: inset 0 0 0 1px #94a3b8;
}
.search-row :deep(.el-input__wrapper) { border-radius: 8px; min-height: 44px; padding: 0 14px; box-shadow: inset 0 0 0 1px #d9e2ec; }
.search-row :deep(.el-input__wrapper:hover) { box-shadow: inset 0 0 0 1px #94a3b8; }
.search-row .primary-btn { min-height: 44px; padding: 0 24px; border-radius: 8px; font-weight: 600; font-size: 14px; }
.model-index-warning { margin: 12px 0 0; padding: 10px 12px; border-radius: 6px; background: #fffbeb; color: #92400e; font-size: 12px; }
.retrieval-summary { position: relative; display: flex; margin-top: 18px; border: 1px solid #edf2f7; border-radius: 10px; overflow: hidden; background: #fff; }
.retrieval-summary > div { display: grid; gap: 5px; flex: 1; min-width: 0; padding: 14px 16px; border-right: 1px solid #edf2f7; }
.retrieval-summary > div:last-of-type { border-right: 0; }
.retrieval-summary strong { font-size: 14px; }
.retrieval-debug { margin-top: 10px; padding: 10px 14px; border: 1px solid #e2e8f0; border-radius: 8px; background: #f8fafc; color: #475569; font-size: 12px; }
.retrieval-debug summary { cursor: pointer; color: #334155; font-weight: 600; }
.debug-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px 16px; margin-top: 10px; }
.summary-flag { position: absolute; top: 10px; right: 10px; padding: 3px 9px; border-radius: 12px; background: #dbeafe; color: #1d4ed8; font-size: 10px; font-weight: 600; }
.summary-flag.warning { right: 100px; background: #fef3c7; color: #b45309; }
.summary-flag.neutral { right: 180px; background: #f1f5f9; color: #475569; }
.result-list { display: grid; gap: 0; margin-top: 18px; border: 1px solid #edf2f7; border-radius: 10px; overflow: hidden; }
.result-row { display: grid; grid-template-columns: 34px minmax(0, 1fr); gap: 14px; padding: 16px 20px; border-bottom: 1px solid #edf2f7; background: #fff; transition: background .15s; }
.result-row:last-child { border-bottom: 0; }
.result-row:hover { background: #f8fafc; }
.result-rank { display: grid; width: 32px; height: 32px; place-items: center; border-radius: 6px; background: #f1f5f9; color: #475569; font: 700 11px/1 Consolas, monospace; }
.result-head { display: flex; justify-content: space-between; gap: 14px; align-items: flex-start; }
.result-head strong { display: block; color: #0f172a; font-size: 14px; font-weight: 600; }
.result-head small { color: #64748b; font-size: 12px; }
.result-head > span { padding: 3px 10px; border-radius: 12px; background: #eff6ff; color: #1d4ed8; font: 700 12px Consolas, monospace; }
.result-content { margin-top: 10px; color: #334155; font-size: 13px; line-height: 1.75; }
.result-content :deep(p) { margin: 0 0 8px; }
.detail-drawer { min-height: 100%; }
.drawer-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding-bottom: 16px; border-bottom: 1px solid #e2e8f0; }
.drawer-content { min-height: 320px; padding-top: 18px; }
.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1px; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; background: #e2e8f0; }
.detail-grid > div { display: grid; gap: 5px; padding: 12px; background: #f8fafc; }
.detail-block { margin-top: 18px; }
.detail-block h5 { margin: 0 0 12px; color: var(--text); font-size: 13px; font-weight: 600; }
.detail-block-head { display: flex; align-items: center; justify-content: space-between; }
.detail-block dl { margin: 0; border-top: 1px solid #e2e8f0; }
.detail-block dl > div { display: grid; grid-template-columns: 105px minmax(0, 1fr); gap: 12px; padding: 10px 0; border-bottom: 1px solid #e2e8f0; font-size: 12px; }
.detail-block dt { color: #64748b; }
.detail-block dd { margin: 0; overflow-wrap: anywhere; color: #0f172a; }
.detail-error { margin-top: 16px; padding: 12px; border-radius: 6px; border-left: 3px solid #ef4444; background: #fef2f2; color: #991b1b; }
.detail-error strong { font-size: 12px; }
.detail-error p { margin: 6px 0 0; font-size: 12px; line-height: 1.6; }
.task-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 5px 12px; padding: 12px 0; border-top: 1px solid #e2e8f0; }
.task-row strong, .task-row small { display: block; }
.task-row strong { color: var(--text); font-size: 12px; }
.task-row small, .task-row span { margin-top: 3px; color: #64748b; font-size: 11px; }
.task-row p { grid-column: 1 / -1; margin: 0; color: var(--danger); font-size: 11px; }
.compact-empty { margin: 0; padding: 12px 0; }
.status-message { margin: 12px 16px; color: var(--danger); font-size: 12px; }
.empty-state { color: var(--muted); font-size: 12px; }
.link-btn { padding: 0; border: 0; background: transparent; color: var(--accent); cursor: pointer; font-size: 12px; font-weight: 500; }
.link-btn:hover { text-decoration: underline; }
@media (max-width: 1080px) { .index-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); }.index-strip > div:nth-child(4) { border-left: 0; border-top: 1px solid #e2e8f0; }.index-strip > div:nth-child(n+4) { border-top: 1px solid #e2e8f0; }.retrieval-summary { grid-template-columns: repeat(4, minmax(0, 1fr)); }.progress-band { grid-template-columns: 1fr; }.overview-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }.overview-metrics > div:nth-child(2) { border-right: 0; }.overview-metrics > div:nth-child(n+3) { border-top: 1px solid #e2e8f0; } }
@media (max-width: 720px) { .workspace-header, .header-actions, .asset-toolbar, .section-heading, .search-row, .overview-heading { align-items: stretch; flex-direction: column; }.header-actions > *, .language-select, .asset-toolbar :deep(.el-select), .keyword-input, .search-language { width: 100%; }.task-navigation { overflow-x: auto; }.task-navigation button { flex: 0 0 88px; }.asset-count { margin-left: 0; }.index-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }.index-strip > div:nth-child(odd) { border-left: 0; }.index-strip > div:nth-child(n+3) { border-top: 1px solid #e2e8f0; }.retrieval-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }.debug-grid, .source-progress { grid-template-columns: 1fr; }.detail-grid, .overview-metrics { grid-template-columns: 1fr; }.overview-metrics > div { border-right: 0; border-top: 1px solid #e2e8f0; }.overview-metrics > div:first-child { border-top: 0; }.operations-row { align-items: flex-start; }.pagination-row { overflow-x: auto; justify-content: flex-start; } }
</style>
