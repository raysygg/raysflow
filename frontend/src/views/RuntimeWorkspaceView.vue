<template>
  <div class="page-grid runtime-workspace-page">
    <PageToolbar
      eyebrow="运行应用工作区"
      :title="applicationName"
      description="用统一的运行时配置应用能力、上下文和流程；高级细节只在需要时展开。"
    >
      <template #actions>
        <RouterLink to="/applications" class="ghost-btn">返回应用目录</RouterLink>
        <RouterLink :to="{ path: `/applications/${applicationId}/launch` }" class="primary-btn">打开运行入口</RouterLink>
      </template>
    </PageToolbar>
    <ResourceContextRail label="当前应用" :name="applicationName" :state="applicationStatus.label" :hint="primaryAction.label" mark="R" />

    <PageState v-if="loading" type="loading" title="正在读取应用配置" message="正在同步主工作流、入口和发布信息。" />
    <PageState v-else-if="errorMessage" type="error" title="运行配置不可用" :message="errorMessage" action-label="重新加载" @action="loadWorkspace" />

    <template v-else>
      <section class="workspace-context-bar" aria-label="当前应用上下文">
        <div><span class="section-eyebrow">当前应用</span><strong>{{ applicationName }}</strong><small>{{ applicationCode || '编码由平台管理' }}</small></div>
        <div><span>应用状态</span><StatusBadge :label="applicationStatus.label" :tone="applicationStatus.tone" /></div>
        <div><span>当前版本</span><strong>{{ publishedVersionLabel }}</strong></div>
        <div class="workspace-context-actions"><button type="button" class="primary-btn" @click="setActiveTab(primaryAction.tab)">{{ primaryAction.label }}</button></div>
      </section>

      <section v-if="lifecycle" class="lifecycle-summary" aria-label="应用生命周期">
        <div class="lifecycle-summary-head">
          <div><div class="section-eyebrow">交付进度</div><strong>从草稿到生产运行</strong></div>
          <RouterLink v-if="lifecycleAction" :to="lifecycleRoute(lifecycleAction.route)" class="primary-btn">{{ lifecycleAction.label }}</RouterLink>
        </div>
        <div class="lifecycle-stage-grid">
          <div v-for="stage in lifecycleStages" :key="stage.key" class="lifecycle-stage" :class="stage.tone">
            <span>{{ stage.label }}</span><strong>{{ stage.value }}</strong>
          </div>
        </div>
        <div v-if="lifecycle.findings?.length" class="lifecycle-findings">
          <RouterLink v-for="finding in lifecycle.findings.slice(0, 3)" :key="finding.code" :to="lifecycleRoute(finding.route)" class="lifecycle-finding">
            <strong>{{ finding.title }}</strong><small>{{ finding.reason }}</small>
          </RouterLink>
        </div>
      </section>

      <section class="runtime-hero">
        <div>
          <span class="section-eyebrow">统一运行内核</span>
          <h3>先选运行方式，再配置它需要的能力</h3>
          <p>对话适合直接问答，循环推理适合调用工具，计划执行适合复杂任务，固定流程适合稳定流程，多智能体协作适合角色分工。</p>
        </div>
        <div class="runtime-hero-facts">
          <div><small>默认响应方式</small><strong>由入口自动决定</strong></div>
          <div><small>工作流版本</small><strong>{{ configuration.versions?.length || 0 }} 个</strong></div>
          <div><small>流程版本</small><strong>{{ configuration.versions?.length || 0 }} 个</strong></div>
        </div>
      </section>

      <div class="runtime-tab-bar" role="tablist" aria-label="运行配置区域">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="runtime-tab"
          :class="{ active: activeTab === tab.key }"
          role="tab"
          :aria-selected="activeTab === tab.key"
          @click="setActiveTab(tab.key)"
        >
          <strong>{{ tab.label }}</strong>
          <small>{{ tab.hint }}</small>
        </button>
      </div>

      <div class="runtime-content-grid">
        <section class="section-card runtime-main-panel">
          <div class="section-head">
            <div>
              <div class="section-eyebrow">{{ activeTabLabel }}</div>
              <h3>{{ activeTabDescription }}</h3>
            </div>
            <span class="runtime-readonly">配置由运行服务统一校验</span>
          </div>

          <div v-if="activeTab === 'runtime'" class="config-stack">
            <div class="runtime-capability-grid">
              <div v-for="capability in configuration.runtimeCapabilities || []" :key="capability.mode" class="runtime-mode-card" :class="{ disabled: capability.readiness !== 'PRODUCTION_READY' }">
                <span class="mode-symbol">{{ runtimeModeLabel(capability.mode).slice(0, 1) }}</span>
                <span><strong>{{ runtimeModeLabel(capability.mode) }}</strong><small>{{ runtimeModeGuidance(capability) }}</small></span>
                <StatusBadge :label="runtimeReadinessLabel(capability.readiness)" :tone="capability.readiness === 'PRODUCTION_READY' ? 'success' : 'warning'" />
              </div>
            </div>
            <div class="config-callout"><strong>应用交付方式由入口决定</strong><p>对话入口默认实时生成，短流程可以即时返回，定时和长流程进入后台处理，所有入口执行同一个应用主工作流。</p></div>
            <div class="config-list-row"><span><strong>实时生成</strong><small>立即启动工作流，持续推送节点状态、模型增量和最终结果。</small></span><StatusBadge label="对话入口默认" tone="success" /></div>
            <div class="config-list-row"><span><strong>即时返回</strong><small>适合短流程，在当前请求中完成并直接返回结果。</small></span><StatusBadge label="短流程可用" tone="neutral" /></div>
            <div class="config-list-row"><span><strong>后台处理</strong><small>适合长流程、定时任务和批量处理，完成后在运行记录中查看结果。</small></span><StatusBadge label="长流程可用" tone="neutral" /></div>
          </div>

          <div v-if="activeTab === 'runtime' && configuration.spec" class="contract-grid">
            <div class="contract-panel">
              <div class="section-eyebrow">输入契约</div>
              <strong>{{ configuration.spec.input?.contentType === 'application/json' ? '结构化数据' : (configuration.spec.input?.contentType || '结构化数据') }}</strong>
              <div v-for="field in configuration.spec.input?.fields || []" :key="`input-${field.name}`" class="contract-field">
                <span>{{ field.name }}</span>
                <small>{{ field.type }} · {{ field.required ? '必填' : '可选' }}</small>
              </div>
            </div>
            <div class="contract-panel">
              <div class="section-eyebrow">输出契约</div>
              <strong>{{ configuration.spec.output?.contentType === 'application/json' ? '结构化数据' : (configuration.spec.output?.contentType || '结构化数据') }}</strong>
              <div v-for="field in configuration.spec.output?.fields || []" :key="`output-${field.name}`" class="contract-field">
                <span>{{ field.name }}</span>
                <small>{{ field.type }} · {{ field.required ? '必填' : '可选' }}</small>
              </div>
            </div>
          </div>

          <div v-else-if="activeTab === 'prompt'" class="config-stack">
            <div class="config-callout"><strong>行为说明是应用的边界</strong><p>把角色、任务、输出格式和不能做什么写清楚，运行时会在每次执行中固定使用发布版本。</p></div>
            <div class="config-list-row"><span><strong>流程引用模型</strong><small>来源由发布快照固定，运行时不会重新猜测</small></span><strong>{{ modelDependencies.length }} 个</strong></div>
            <div v-for="item in modelDependencies" :key="`model-${item.nodeId}-${item.modelId}`" class="config-list-row dependency-row">
              <span><strong>{{ item.name || item.modelKey }}</strong><small>引用节点：{{ item.nodeId }} · {{ modelSourceLabel(item.source) }}</small></span><StatusBadge :label="dependencyStatus(item.status)" :tone="item.status === 'ACTIVE' ? 'success' : 'warning'" />
            </div>
            <div class="config-list-row"><span><strong>变量来源</strong><small>{{ promptConfiguration.variableSources?.join('、') || '输入、流程变量' }}</small></span><StatusBadge label="显式展开" tone="success" /></div>
            <div class="prompt-preview"><strong>当前草稿模板</strong><pre>{{ promptConfiguration.template || '尚未配置行为说明模板，运行时使用节点默认行为。' }}</pre></div>
            <label class="form-group"><span>行为说明预览输入</span><textarea v-model="promptPreviewInput" class="textarea-input" rows="3" placeholder="例如：请总结这份合同的付款风险" /></label>
            <button type="button" class="ghost-btn" :disabled="promptPreviewLoading || !promptPreviewInput.trim()" @click="previewPrompt">{{ promptPreviewLoading ? '正在展开...' : '展开行为说明预览' }}</button>
            <div v-if="promptPreviewError" class="config-callout prompt-preview-error"><strong>行为说明配置错误</strong><p>{{ promptPreviewError }}</p></div>
            <div v-if="promptPreview" class="prompt-preview-result">
              <div class="prompt-preview"><strong>变量展开结果</strong><pre>{{ promptPreview.renderedPrompt }}</pre></div>
              <div class="preview-meta-grid"><div><strong>上下文来源</strong><small>{{ promptPreview.contextSources?.join('、') || '无' }}</small></div><div><strong>模型参数</strong><small>{{ promptPreview.modelParameters?.modelKey }} · 随机度={{ promptPreview.modelParameters?.temperature }} · 最大输出={{ promptPreview.modelParameters?.maxTokens || '默认' }}</small></div></div>
              <div class="preview-variable-list"><strong>变量值</strong><div v-for="(value, name) in promptPreview.variables" :key="name"><span>{{ name }}</span><small>{{ String(value) }}</small></div></div>
            </div>
            <button type="button" class="ghost-btn" @click="setActiveTab('test')">使用当前配置测试</button>
          </div>

          <div v-else-if="activeTab === 'tools'" class="config-stack">
            <div class="config-list-row"><span><strong>流程引用工具</strong><small>数量来自当前工作流草稿中的真实连接器节点</small></span><strong>{{ connectorDependencies.length }} 个</strong></div>
            <div v-for="item in connectorDependencies" :key="`connector-${item.nodeId}-${item.connectorId}`" class="config-list-row dependency-row">
              <span><strong>{{ item.name || `连接器 ${item.connectorId}` }}</strong><small>引用节点：{{ item.nodeId }}</small></span><StatusBadge :label="dependencyStatus(item.status)" :tone="item.status === 'ACTIVE' ? 'success' : 'warning'" />
            </div>
            <div class="config-list-row"><span><strong>安全边界</strong><small>权限、出站域名、预算和幂等策略由后端执行</small></span><StatusBadge label="受治理" tone="success" /></div>
            <button type="button" class="ghost-btn" @click="setActiveTab('flow')">在应用内配置流程</button>
          </div>

          <div v-else-if="activeTab === 'memory'" class="config-stack">
            <div class="config-list-row"><span><strong>短期记忆</strong><small>消息窗口：{{ memoryConfiguration.windowMessages || 0 }} 条，属于当前会话运行上下文</small></span><StatusBadge :label="memoryConfiguration.shortTermEnabled ? '已启用' : '未启用'" :tone="memoryConfiguration.shortTermEnabled ? 'success' : 'neutral'" /></div>
            <div class="config-list-row"><span><strong>长期记忆</strong><small>独立于会话历史，写入策略：{{ memoryConfiguration.writePolicy || 'READ_ONLY' }}</small></span><StatusBadge :label="memoryConfiguration.longTermEnabled ? '已启用' : '只读'" :tone="memoryConfiguration.longTermEnabled ? 'success' : 'neutral'" /></div>
            <div class="config-callout"><strong>上下文优先级</strong><p>{{ contextConfiguration.context?.priority?.map(item => contextLabel(item)).join(' 低于 ') || '长期记忆 低于 短期记忆 低于 检索结果 低于 流程变量 低于 输入' }}</p></div>
          </div>

          <div v-else-if="activeTab === 'knowledge'" class="config-stack">
            <div class="config-list-row"><span><strong>流程引用文档</strong><small>当前仅展示工作流草稿明确引用的知识文档</small></span><strong>{{ documentDependencies.length }} 个</strong></div>
            <div v-for="item in documentDependencies" :key="`document-${item.nodeId}-${item.documentId}`" class="config-list-row dependency-row">
              <span><strong>{{ item.name || `知识文档 ${item.documentId}` }}</strong><small>引用节点：{{ item.nodeId }}</small></span><StatusBadge :label="dependencyStatus(item.status)" :tone="item.status === 'INDEXED' ? 'success' : 'warning'" />
            </div>
            <div class="config-list-row"><span><strong>召回策略</strong><small>{{ ragConfiguration.retrievalMode === 'HYBRID' ? '混合检索' : (ragConfiguration.retrievalMode || '未设置') }} · 返回数量 {{ ragConfiguration.topK || 0 }} · 相似度阈值 {{ ragConfiguration.scoreThreshold ?? 0 }}</small></span><StatusBadge :label="ragConfiguration.enabled ? '已启用' : '未启用'" :tone="ragConfiguration.enabled ? 'success' : 'neutral'" /></div>
            <button type="button" class="ghost-btn" @click="setActiveTab('knowledge')">在应用内查看资源</button>
          </div>

          <div v-else-if="activeTab === 'test'" class="config-stack">
            <div class="config-callout"><strong>先用真实业务问题验证</strong><p>测试会沿用当前草稿配置，生成独立测试记录，不会影响正式版本。</p></div>
            <TypedInputForm ref="testForm" v-model="testInput" :fields="inputContractFields" :resources="inputResources" />
            <button type="button" class="primary-btn" :disabled="testing || inputContractFields.length === 0" @click="runTest">{{ testing ? '提交测试中...' : '运行一次测试' }}</button>
            <div v-if="testMessage" class="test-result">
              <p class="field-help">{{ testMessage }}</p>
              <RouterLink v-if="testRunId" :to="{ path: '/workflow-executions', query: { appId: applicationId, runId: testRunId } }" class="text-link">查看运行详情</RouterLink>
            </div>
          </div>

          <div v-else-if="activeTab === 'release'" class="config-stack">
            <div v-if="latestCandidate" class="config-list-row">
              <span><strong>候选版本 #{{ latestCandidate.candidateId }}</strong><small>草稿修订 {{ latestCandidate.draftRevisionNo }} · {{ latestCandidate.status }}</small></span>
              <StatusBadge :label="candidateStatusLabel(latestCandidate.status)" :tone="latestCandidate.status === 'READY' || latestCandidate.status === 'PUBLISHED' ? 'success' : 'warning'" />
            </div>
            <div v-if="gateReport" class="gate-report">
              <div class="detail-heading"><strong>发布检查</strong><StatusBadge :label="gateLevelLabel(gateReport.overallLevel)" :tone="gateReport.overallLevel === 'PASSED' ? 'success' : 'warning'" /></div>
              <div v-for="finding in gateReport.findings || []" :key="finding.code" class="release-check-item">
                <span class="release-check-dot" :class="finding.level === 'WARNING' ? 'warning' : 'error'" />
                <span><strong>{{ finding.title }}</strong><small>{{ finding.reason }} · 修复位置：{{ finding.remediationTarget }}</small></span>
              </div>
            </div>
            <div v-else-if="latestCandidate" class="config-callout"><strong>尚未生成门禁报告</strong><p>先完成候选评测，再从发布流程执行门禁检查。</p></div>
            <div v-if="releaseCheckLoading" class="config-callout"><strong>正在读取发布检查</strong><p>正在同步当前草稿的结构、节点能力和资源校验结果。</p></div>
            <div v-else-if="releaseChecks.length === 0" class="config-callout release-check-success"><strong>当前草稿未发现阻断项</strong><p>仍需进入流程设计页确认发布版本和目标环境。</p></div>
            <div v-else class="release-check-list">
              <div v-for="issue in releaseChecks" :key="`${issue.code || issue.issueCode}-${issue.nodeId || issue.path || issue.message}`" class="release-check-item">
                <span class="release-check-dot" :class="issue.level === 'WARNING' ? 'warning' : 'error'" />
                <span><strong>{{ issue.message || issue.description || '流程检查未通过' }}</strong><small>{{ issue.nodeId || issue.path || issue.code || '流程级检查' }}</small></span>
              </div>
            </div>
            <div class="config-list-row"><span><strong>发布前检查</strong><small>结构、权限、安全、质量和依赖检查由后端统一执行</small></span><StatusBadge label="服务端校验" tone="success" /></div>
            <div v-for="version in configuration.versions || []" :key="`release-${version.versionId || version.id}`" class="config-list-row"><span><strong>版本 {{ version.versionNo || version.versionId || version.id }}</strong><small>{{ releaseStatusLabel(version.status) }} · {{ version.environment || '默认环境' }}</small></span><StatusBadge :label="releaseStatusLabel(version.status)" :tone="version.status === 'PUBLISHED' ? 'success' : 'warning'" /></div>
            <!-- 候选版本固化与更新面板 -->
            <div class="candidate-create-panel" :class="{ 'candidate-outdated-alert': isCandidateOutdated }">
              <div>
                <strong>{{ isCandidateOutdated ? '检测到草稿已更新，请重新固化候选版本' : (latestCandidate ? `当前绑定候选版本 #${latestCandidate.candidateId}` : '创建候选版本') }}</strong>
                <small>{{ isCandidateOutdated ? `当前草稿已推进至修订 ${currentDraftRevisionNo}，而候选快照仍停留在修订 ${latestCandidate.draftRevisionNo}。需先固化新候选版本，方可基于最新模型配置进行评测与发布。` : (latestCandidate ? `当前候选版本基于草稿修订 ${latestCandidate.draftRevisionNo}。如需锁定最新修改，可重新创建候选版本。` : '候选版本会固定当前草稿，后续评测和发布都基于这份快照。') }}</small>
              </div>
              <button type="button" class="primary-btn" :disabled="candidateCreating" @click="createCandidate">
                {{ candidateCreating ? '正在固化...' : (isCandidateOutdated ? '固化最新草稿为候选' : (latestCandidate ? '重新生成候选版本' : '创建候选版本')) }}
              </button>
            </div>
            <p v-if="candidateMessage" class="field-help" :class="{ 'field-error': candidateError }">{{ candidateMessage }}</p>

            <button type="button" class="primary-btn" @click="setActiveTab('flow')">返回流程检查</button>
          </div>

          <div v-else-if="activeTab === 'flow'" class="config-stack">
            <div class="config-list-row"><span><strong>已配置流程</strong><small>只有已发布版本才能进入生产运行</small></span><strong>{{ configuration.versions?.length || 0 }} 个版本</strong></div>
            <div v-for="version in configuration.versions || []" :key="version.versionId || version.id" class="config-list-row">
              <span><strong>版本 {{ version.versionNo || version.versionId || version.id }}</strong><small>{{ version.status || '已保存' }} · {{ version.environment || '默认环境' }}</small></span>
              <StatusBadge :label="releaseStatusLabel(version.status)" :tone="version.status === 'PUBLISHED' ? 'success' : 'warning'" />
            </div>
            <RouterLink :to="{ path: `/applications/${applicationId}/workflow` }" class="primary-btn">打开应用执行流程</RouterLink>
            <button type="button" class="primary-btn" @click="setActiveTab('release')">检查并准备发布</button>
            <div class="candidate-create-panel" :class="{ 'candidate-outdated-alert': isCandidateOutdated }">
              <div>
                <strong>{{ isCandidateOutdated ? `草稿已更新至修订 ${currentDraftRevisionNo}` : (latestCandidate ? `当前候选版本 #${latestCandidate.candidateId}（草稿修订 ${latestCandidate.draftRevisionNo}）` : '草稿已准备好？创建候选版本') }}</strong>
                <small>{{ isCandidateOutdated ? `候选快照仍基于旧修订 ${latestCandidate.draftRevisionNo}。点击按钮将当前修订 ${currentDraftRevisionNo} 固化为新候选版本。` : (latestCandidate ? '如需以当前最新草稿开展新一轮评测或发布，可生成新的候选版本。' : '候选版本会固定当前草稿，后续评测和发布都基于这份快照。') }}</small>
              </div>
              <button type="button" class="primary-btn" :disabled="candidateCreating" @click="createCandidate">
                {{ candidateCreating ? '正在创建...' : (isCandidateOutdated ? '固化最新草稿为候选' : (latestCandidate ? '重新生成候选版本' : '创建候选版本')) }}
              </button>
            </div>
            <p v-if="candidateMessage" class="field-help" :class="{ 'field-error': candidateError }">{{ candidateMessage }}</p>
          </div>

          <div v-else class="config-stack">
            <div class="config-callout"><strong>运行诊断集中在当前应用</strong><p>从运行结果进入任务、节点、事件和链路详情，诊断数据会沿用当前应用和版本上下文。</p></div>
            <div class="config-list-row"><span><strong>运行记录</strong><small>查看当前应用的执行结果、失败原因和重试入口</small></span><RouterLink :to="{ path: '/workflow-executions', query: { appId: applicationId } }" class="text-link">打开运行记录</RouterLink></div>
            <div class="config-list-row"><span><strong>发布检查</strong><small>查看节点能力、资源引用和输入输出契约的阻断项</small></span><button type="button" class="text-link" @click="setActiveTab('release')">查看检查</button></div>
          </div>
        </section>

        <aside class="runtime-side-column">
          <section class="section-card guide-card">
            <div class="section-eyebrow">新手引导</div>
            <h3>{{ guideTitle }}</h3>
            <p>{{ guideDescription }}</p>
            <div class="guide-fields"><span v-for="field in guideFields" :key="field">{{ field }}</span></div>
            <button type="button" class="ghost-btn" @click="advanced = !advanced">{{ advanced ? '收起高级提示' : '查看高级提示' }}</button>
            <p v-if="advanced" class="advanced-copy">{{ guideAdvancedDescription }}</p>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageState from '../components/PageState.vue'
import PageToolbar from '../components/PageToolbar.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import TypedInputForm from '../components/TypedInputForm.vue'
import { fetchRuntimeApplicationConfiguration, fetchRuntimeApplications, fetchRuntimeWorkflowChecks, previewRuntimePrompt, submitRuntimeDraftTest } from '../api/runtime'
import { apiData, apiErrorMessage, requireApiSuccess } from '../api/contracts'
import { APPLICATION_STATUS_META, WORKSPACE_TABS } from '../constants/applicationWorkspace'
import http from '../api/http'

const route = useRoute()
const router = useRouter()
const applicationId = String(route.params.id)
const applicationName = ref('业务运行应用')
const applicationCode = ref('')
const applicationStatus = ref({ label: '待配置', tone: 'neutral' })
const loading = ref(true)
const errorMessage = ref('')
const activeTab = ref(String(route.query.tab || 'runtime'))
const advanced = ref(false)
const configuration = ref({ tabs: [], versions: [], spec: null, contextConfiguration: null, dependencies: { models: [], documents: [], connectors: [] } })
const testInput = ref({})
const testForm = ref(null)
const testing = ref(false)
const testMessage = ref('')
const testRunId = ref('')
const promptPreviewInput = ref('')
const promptPreview = ref(null)
const promptPreviewLoading = ref(false)
const promptPreviewError = ref('')
const releaseChecks = ref([])
const releaseCheckLoading = ref(false)
const candidates = ref([])
const gateReport = ref(null)
const candidateCreating = ref(false)
const candidateMessage = ref('')
const candidateError = ref(false)
const latestCandidate = computed(() => candidates.value[0] || null)
const lifecycle = computed(() => configuration.value.lifecycle || null)
const lifecycleAction = computed(() => lifecycle.value?.nextActions?.find(action => action.enabled) || null)
const lifecycleRoute = routePath => String(routePath || '/applications/' + applicationId).replaceAll('{applicationId}', applicationId)
const lifecycleStages = computed(() => {
  const value = lifecycle.value || {}
  return [
    { key: 'draft', label: '草稿', value: value.draftRevisionNo ? `修订 ${value.draftRevisionNo}` : '未创建', tone: value.draftRevisionNo ? 'ready' : 'blocked' },
    { key: 'candidate', label: '候选版本', value: value.candidate ? candidateStatusLabel(value.candidate.status) : '未创建', tone: value.candidate ? 'ready' : 'blocked' },
    { key: 'evaluation', label: '评测', value: value.evaluation ? ({ SUCCEEDED: '已完成', PARTIAL: '部分完成', FAILED: '失败', RUNNING: '运行中', QUEUED: '排队中' }[value.evaluation.status] || '处理中') : '未开始', tone: value.evaluation?.status === 'SUCCEEDED' ? 'ready' : 'blocked' },
    { key: 'gate', label: '发布检查', value: value.gate ? gateLevelLabel(value.gate.overallLevel) : '未检查', tone: value.gate?.overallLevel === 'PASSED' ? 'ready' : 'blocked' },
    { key: 'release', label: '生产版本', value: value.activeRelease ? `版本 ${value.activeRelease.versionNo}` : '未发布', tone: value.activeRelease ? 'ready' : 'blocked' },
    { key: 'entrypoints', label: '运行入口', value: `${value.entrypoints?.length || 0} 个`, tone: value.entrypoints?.length ? 'ready' : 'blocked' }
  ]
})

const tabs = WORKSPACE_TABS
const runtimeModeLabels = Object.freeze({ CHAT: '对话', REACT: '循环处理', PLAN: '任务计划', WORKFLOW: '固定流程', MULTI_AGENT: '多角色协作' })
const runtimeReadinessLabels = Object.freeze({ PRODUCTION_READY: '可用于正式发布', EXPERIMENTAL: '仅供草稿试用', NOT_READY: '暂不可用' })
const runtimeModeLabel = mode => runtimeModeLabels[mode] || mode || '未命名方式'
const runtimeReadinessLabel = readiness => runtimeReadinessLabels[readiness] || '状态未知'
const runtimeModeGuidance = capability => capability.readiness === 'PRODUCTION_READY' ? '可用于正式发布' : capability.readiness === 'NOT_READY' ? '当前还不能用于正式发布' : '建议先在草稿中试用'
const candidateStatusLabel = status => ({ CREATED: '已创建', EVALUATING: '评测中', READY: '可以发布', BLOCKED: '需要修复', PUBLISHED: '已发布', RETIRED: '已停用' }[status] || status || '未知状态')
const gateLevelLabel = level => ({ PASSED: '检查通过', WARNING: '有提示', BLOCKER: '暂不能发布' }[level] || level || '未检查')
const releaseStatusLabel = status => ({ PUBLISHED: '已发布', DRAFT: '草稿', RETIRED: '已停用' }[status] || status || '草稿')
const contextConfiguration = computed(() => configuration.value.contextConfiguration || {})
const memoryConfiguration = computed(() => contextConfiguration.value.memory || {})
const promptConfiguration = computed(() => contextConfiguration.value.prompt || {})
const ragConfiguration = computed(() => contextConfiguration.value.rag || {})
const modelDependencies = computed(() => configuration.value.dependencies?.models || [])
const documentDependencies = computed(() => configuration.value.dependencies?.documents || [])
const connectorDependencies = computed(() => configuration.value.dependencies?.connectors || [])
const inputContractFields = computed(() => configuration.value.spec?.input?.fields || [])
const inputResources = computed(() => ({ MODEL: modelDependencies.value, KNOWLEDGE_DOCUMENT: documentDependencies.value, TOOL_CONNECTOR: connectorDependencies.value }))
const modelSourceLabel = source => source === 'TENANT_PRIVATE' ? '租户私有模型' : '平台共享模型'
const dependencyStatus = status => ({ ACTIVE: '已启用', INDEXED: '索引完成' }[status] || '状态未知')
const contextLabel = value => ({
  LONG_TERM_MEMORY: '长期记忆', SHORT_TERM_MEMORY: '短期记忆', RETRIEVED_KNOWLEDGE: '检索结果',
  VARIABLES: '流程变量', INPUT: '输入'
}[value] || '其他上下文')
const activeTabLabel = computed(() => tabs.find(tab => tab.key === activeTab.value)?.label || '默认响应方式')
const activeTabDescription = computed(() => ({ runtime: '选择用户能理解的执行方式', prompt: '让模型知道应该如何工作', tools: '让外部动作可控、可追踪', memory: '区分会话、短期和长期记忆', knowledge: '让回答有依据并且可引用', flow: '把确定性流程接入运行应用', test: '用真实问题验证草稿配置', release: '通过发布门禁后再投入使用', diagnostics: '定位运行、节点和模型调用问题' }[activeTab.value]))
const guideTitle = computed(() => '先把应用工作流配置完整')
const guideDescription = computed(() => '入口只负责收集输入和选择交付方式，具体执行过程统一由已发布工作流完成。')
const guideFields = computed(() => ['主工作流', '输入契约', '输出契约', '模型与知识'])
const guideAdvancedDescription = computed(() => '高级配置包括上下文预算、重试、人工等待和节点终止条件。')
// 工作区导航使用 URL 作为可恢复状态，刷新或复制链接后仍停留在同一应用上下文。
const setActiveTab = tab => {
  activeTab.value = tab
  router.replace({ query: { ...route.query, tab } })
}
const publishedVersionLabel = computed(() => {
  const published = (configuration.value.versions || []).find(version => version.status === 'PUBLISHED')
  return published ? `版本 ${published.versionNo || published.versionId || published.id}` : '尚未发布'
})
const currentDraftRevisionNo = computed(() => lifecycle.value?.draftRevisionNo || null)
const isCandidateOutdated = computed(() => {
  if (!latestCandidate.value || !currentDraftRevisionNo.value) return false
  return currentDraftRevisionNo.value > (latestCandidate.value.draftRevisionNo || 0)
})

const primaryAction = computed(() => {
  if (!latestCandidate.value) return { label: '完善配置并创建候选', tab: 'flow' }
  if (isCandidateOutdated.value) return { label: '固化最新草稿为候选', tab: 'release' }
  if (latestCandidate.value.status === 'CREATED' || latestCandidate.value.status === 'EVALUATING') return { label: '开始评测', tab: 'test' }
  if (latestCandidate.value.status === 'BLOCKED') return { label: '修复发布阻断', tab: 'release' }
  if (latestCandidate.value.status === 'READY') return { label: '发布应用', tab: 'release' }
  return { label: '查看运营状态', tab: 'diagnostics' }
})
const previewPrompt = async () => {
  promptPreviewLoading.value = true
  promptPreviewError.value = ''
  promptPreview.value = null
  try {
    const response = await previewRuntimePrompt(applicationId, {
      input: promptPreviewInput.value.trim(),
      user_message: promptPreviewInput.value.trim(),
      query: promptPreviewInput.value.trim()
    })
    promptPreview.value = requireApiSuccess(response, '行为说明预览失败')
  } catch (error) {
    promptPreviewError.value = apiErrorMessage(error, '行为说明预览失败，请检查行为说明来源和模型节点配置')
  } finally {
    promptPreviewLoading.value = false
  }
}
// 用户不配置底层执行策略；入口根据交互类型选择实时、即时或后台交付。
const runTest = async () => {
  if (!testForm.value?.validate()) {
    testMessage.value = '请先补齐所有必填输入。'
    return
  }
  testing.value = true
  testMessage.value = ''
  testRunId.value = ''
  try {
    const response = await submitRuntimeDraftTest({
      applicationId: Number(applicationId),
      idempotencyKey: `studio-test-${Date.now()}`,
      input: testForm.value.payload()
    })
    const data = requireApiSuccess(response, '测试提交失败')
    testRunId.value = String(data?.runId || data?.executionId || '')
    testMessage.value = `测试记录 ${data?.runId || data?.executionId || '已创建'}，已按实时方式启动。`
  } catch (error) {
    testMessage.value = apiErrorMessage(error, '测试 Run 提交失败，请先完成应用配置和发布检查。')
  } finally {
    testing.value = false
  }
}
const loadReleaseChecks = async () => {
  releaseCheckLoading.value = true
  try {
    const response = await fetchRuntimeWorkflowChecks(applicationId)
    releaseChecks.value = apiData(response, []) || []
  } catch (error) {
    // 发布检查失败本身也要显示出来，不能伪装成“没有问题”。
    releaseChecks.value = [{ code: 'CHECK_REQUEST_FAILED', level: 'ERROR', message: apiErrorMessage(error, '发布检查请求失败，请进入流程设计页重试') }]
  } finally {
    releaseCheckLoading.value = false
  }
}
const loadLifecycleState = async () => {
  const candidateResponse = await http.get(`/runtime/applications/${applicationId}/candidates`)
  candidates.value = candidateResponse.data || []
  gateReport.value = null
  if (!latestCandidate.value) return
  try {
    const gateResponse = await http.get(`/runtime/applications/${applicationId}/candidates/${latestCandidate.value.candidateId}/gate`)
    if (gateResponse.success) gateReport.value = gateResponse.data
  } catch (error) {
    gateReport.value = null
  }
}
const createCandidate = async () => {
  candidateCreating.value = true
  candidateMessage.value = ''
  candidateError.value = false
  try {
    const summary = currentDraftRevisionNo.value ? `从应用工作区固化草稿修订 ${currentDraftRevisionNo.value} 候选版本` : '从应用工作区创建候选版本'
    const response = await http.post(`/runtime/applications/${applicationId}/candidates`, { changeSummary: summary })
    if (!response.data) throw new Error('候选版本创建未返回数据')
    candidateMessage.value = '最新候选版本已成功固化！当前草稿配置已锁定为评测快照。'
    await loadLifecycleState()
    await loadWorkspace()
  } catch (error) {
    candidateError.value = true
    candidateMessage.value = apiErrorMessage(error, '候选版本创建失败，请先完成流程配置后重试。')
  } finally {
    candidateCreating.value = false
  }
}
const loadWorkspace = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [configurationResponse, applicationsResponse] = await Promise.all([
      fetchRuntimeApplicationConfiguration(applicationId), fetchRuntimeApplications()
    ])
    configuration.value = apiData(configurationResponse, configuration.value)
    if (!configuration.value?.spec) throw new Error('应用运行规格尚未生成，请先完成应用初始化。')
    const application = (apiData(applicationsResponse, []) || []).find(item => String(item.id) === applicationId)
    applicationName.value = application?.name || `应用 ${applicationId}`
    applicationCode.value = application?.code || application?.agentCode || ''
    applicationStatus.value = APPLICATION_STATUS_META[application?.status] || { label: application?.status || '待配置', tone: 'neutral' }
    await Promise.all([loadReleaseChecks(), loadLifecycleState()])
  } catch (error) {
    errorMessage.value = apiErrorMessage(error, '运行配置读取失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}
onMounted(loadWorkspace)
</script>

<style scoped>
.runtime-workspace-page { min-width: 0; }
.lifecycle-summary { display:grid; gap:14px; padding:18px; border:1px solid var(--line); border-radius:8px; background:var(--panel); }
.lifecycle-summary-head { display:flex; align-items:center; justify-content:space-between; gap:16px; }
.lifecycle-summary-head > div { display:grid; gap:5px; }
.lifecycle-stage-grid { display:grid; grid-template-columns:repeat(6,minmax(0,1fr)); gap:8px; }
.lifecycle-stage { display:grid; gap:5px; min-width:0; padding:10px; border:1px solid var(--line); border-radius:6px; background:var(--panel-muted); }
.lifecycle-stage span { color:var(--muted); font-size:10px; }
.lifecycle-stage strong { overflow:hidden; color:var(--text); font-size:12px; text-overflow:ellipsis; white-space:nowrap; }
.lifecycle-stage.ready { border-color:#bfe6d2; background:var(--success-soft); }
.lifecycle-stage.blocked { border-color:#f0d7a9; background:#fff9ec; }
.lifecycle-findings { display:grid; gap:8px; }
.lifecycle-finding { display:grid; gap:3px; padding:10px 12px; border-left:3px solid var(--warning, #b7791f); background:var(--panel-muted); color:inherit; text-decoration:none; }
.lifecycle-finding small { color:var(--muted); line-height:1.5; }
.runtime-hero { display:flex; justify-content:space-between; gap:24px; padding:24px; border:1px solid var(--line); border-radius:var(--radius); background:linear-gradient(125deg, #102238, #1e4866); color:#fff; }
.runtime-hero h3 { margin:8px 0; font-size:22px; }
.runtime-hero p { max-width:720px; margin:0; color:#c7d9e4; line-height:1.7; font-size:13px; }
.runtime-hero .section-eyebrow { color:#8bd9c3; }
.runtime-hero-facts { display:grid; grid-template-columns:repeat(3,minmax(86px,1fr)); align-items:center; gap:12px; min-width:330px; }
.runtime-hero-facts > div { display:grid; gap:6px; min-width:0; padding-left:12px; border-left:1px solid rgba(255,255,255,.18); }
.runtime-hero-facts small { color:#9eb8c8; font-size:11px; }
.runtime-hero-facts strong { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:15px; }
.runtime-tab-bar { display:grid; grid-template-columns:repeat(6,minmax(0,1fr)); border-bottom:1px solid var(--line); }
.runtime-tab { display:grid; gap:4px; min-width:0; padding:13px 12px; border:0; border-bottom:2px solid transparent; background:transparent; color:var(--muted); text-align:left; cursor:pointer; }
.runtime-tab:hover,.runtime-tab.active { color:var(--accent); background:var(--accent-soft); }
.runtime-tab.active { border-bottom-color:var(--accent); }
.runtime-tab strong { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:12px; }
.runtime-tab small { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:10px; }
.runtime-content-grid { display:grid; grid-template-columns:minmax(0,1fr) minmax(260px,320px); gap:18px; align-items:start; }
.runtime-main-panel { min-width:0; }
.runtime-main-panel > .section-head h3 { margin:6px 0 0; font-size:20px; line-height:1.35; }
.runtime-side-column { display:grid; gap:18px; min-width:0; }
.runtime-readonly { color:var(--muted); font-size:11px; }
.config-stack { display:grid; gap:12px; }
.runtime-mode-card { display:grid; grid-template-columns:34px minmax(0,1fr) auto; gap:10px; align-items:center; min-height:92px; padding:12px; border:1px solid var(--line); border-radius:8px; background:var(--panel); }
.runtime-mode-card.disabled { background:var(--panel-muted); }
.runtime-mode-card > span:nth-child(2) { display:grid; gap:4px; min-width:0; }
.runtime-mode-card small { color:var(--muted); line-height:1.45; }
.mode-symbol { display:grid; place-items:center; width:30px; height:30px; border-radius:50%; background:var(--accent-soft); color:var(--accent); font-weight:800; }
.config-callout { padding:15px; border:1px solid #bfe6d2; border-radius:8px; background:var(--success-soft); }
.config-callout strong { color:var(--success); }
.config-callout p { margin:5px 0 0; color:var(--muted); line-height:1.55; }
.config-list-row { display:flex; justify-content:space-between; gap:16px; align-items:center; padding:14px; border:1px solid var(--line); border-radius:8px; background:var(--panel-muted); }
.config-list-row > span { display:grid; gap:4px; min-width:0; }
.config-list-row small { color:var(--muted); line-height:1.5; overflow-wrap:anywhere; }
.candidate-create-panel { display:flex; align-items:center; justify-content:space-between; gap:16px; padding:14px 16px; border:1px solid color-mix(in srgb, var(--accent) 24%, var(--line)); border-radius:8px; background:color-mix(in srgb, var(--accent) 5%, var(--panel)); }
.candidate-create-panel.candidate-outdated-alert { border-color: #f59e0b; background: #fffbeb; }
.candidate-create-panel.candidate-outdated-alert strong { color: #b45309; }
.candidate-create-panel > div { display:grid; gap:4px; min-width:0; }
.candidate-create-panel strong { color:var(--text); font-size:13px; }
.candidate-create-panel small { color:var(--muted); line-height:1.5; }
.field-error { color:var(--danger); }
.guide-card { align-self:start; }
.guide-card h3 { margin:8px 0; font-size:18px; line-height:1.4; }
.guide-card p { color:var(--muted); line-height:1.6; }
.guide-fields { display:flex; flex-wrap:wrap; gap:7px; margin:14px 0 16px; }
.guide-fields span { padding:5px 8px; border-radius:4px; background:var(--accent-soft); color:var(--accent-deep); font-size:11px; }
.section-card .primary-btn,.section-card .ghost-btn { width:fit-content; }
.runtime-capability-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }
.workspace-context-bar { display:grid; grid-template-columns:minmax(220px,1.4fr) minmax(110px,.6fr) minmax(110px,.6fr) auto; align-items:center; gap:18px; padding:14px 18px; border:1px solid var(--line); border-radius:var(--radius); background:var(--panel); }.workspace-context-bar > div { display:grid; gap:5px; min-width:0; }.workspace-context-bar > div > span:not(.section-eyebrow) { color:var(--muted); font-size:11px; }.workspace-context-bar strong { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.workspace-context-bar small { color:var(--muted); font:11px ui-monospace, SFMono-Regular, Consolas, monospace; }.workspace-context-actions { display:flex !important; justify-content:flex-end; gap:8px; }.workspace-context-actions button { white-space:nowrap; }
.prompt-preview { display:grid; gap:8px; padding:14px; border:1px solid var(--line); border-radius:8px; background:var(--panel-muted); }
.prompt-preview pre { margin:0; max-height:180px; overflow:auto; white-space:pre-wrap; color:var(--text-primary); font:12px/1.6 ui-monospace, SFMono-Regular, Consolas, monospace; }
.prompt-preview-result { display:grid; gap:10px; }
.prompt-preview-error { border-color:#efcaca; background:#fff7f7; }
.prompt-preview-error strong { color:var(--danger); }
.preview-meta-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }
.preview-meta-grid > div,.preview-variable-list { display:grid; gap:6px; padding:12px; border:1px solid var(--line); border-radius:8px; background:var(--panel-muted); }
.preview-meta-grid small,.preview-variable-list small { color:var(--muted); line-height:1.5; overflow-wrap:anywhere; }
.preview-variable-list > div { display:grid; grid-template-columns:140px minmax(0,1fr); gap:10px; padding-top:7px; border-top:1px solid var(--line); }
@media (max-width: 620px) { .preview-meta-grid { grid-template-columns:1fr; }.preview-variable-list > div { grid-template-columns:1fr; gap:3px; } }
.release-check-success { border-color:#bfe6d2; background:var(--success-soft); }
.test-result { display:grid; gap:7px; }
.contract-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; margin-top:16px; }
.contract-panel { border:1px solid var(--border-subtle); border-radius:8px; padding:14px; background:var(--surface-muted); display:grid; gap:8px; }
.contract-panel > strong { color:var(--text-primary); font-size:13px; }
.contract-field { display:flex; justify-content:space-between; gap:12px; border-top:1px solid var(--border-subtle); padding-top:8px; color:var(--text-primary); }
.contract-field small { color:var(--text-secondary); }
@media (max-width: 760px) { .contract-grid { grid-template-columns:1fr; } }
.release-check-list { display:grid; gap:8px; }
.gate-report { display:grid; gap:8px; padding:14px; border:1px solid var(--line); border-radius:8px; background:var(--panel-muted); }
.detail-heading { display:flex; align-items:center; justify-content:space-between; gap:12px; }
.release-check-item { display:flex; align-items:flex-start; gap:9px; padding:11px 13px; border:1px solid #f0d0d2; border-radius:8px; background:#fff8f8; }
.release-check-item strong,.release-check-item small { display:block; }
.release-check-item strong { color:var(--danger); font-size:12px; line-height:1.5; }
.release-check-item small { margin-top:4px; color:var(--muted); font-size:10px; }
.release-check-dot { flex:0 0 8px; width:8px; height:8px; margin-top:5px; border-radius:50%; background:var(--danger); }
.release-check-dot.warning { background:var(--warning, #b7791f); }
@media (max-width: 900px) { .workspace-context-bar { grid-template-columns:1fr 1fr; }.workspace-context-actions { grid-column:1 / -1; justify-content:flex-start !important; }.runtime-hero,.runtime-content-grid { display:block; }.runtime-hero-facts { min-width:0; margin-top:20px; }.runtime-side-column { margin-top:18px; }.runtime-tab-bar { grid-template-columns:repeat(4, minmax(0, 1fr)); }.runtime-mode-grid,.runtime-capability-grid { grid-template-columns:1fr; } }
@media (max-width: 900px) { .lifecycle-stage-grid { grid-template-columns:repeat(3,minmax(0,1fr)); } }
@media (max-width: 580px) { .runtime-tab-bar { grid-template-columns:repeat(2, minmax(0, 1fr)); }.runtime-hero-facts { grid-template-columns:1fr 1fr; }.runtime-hero-facts div:last-child { grid-column:1 / -1; }.config-list-row { align-items:flex-start; flex-direction:column; }.candidate-create-panel { align-items:stretch; flex-direction:column; }.candidate-create-panel .primary-btn { width:100%; } }
@media (max-width: 580px) { .lifecycle-summary-head { align-items:stretch; flex-direction:column; }.lifecycle-stage-grid { grid-template-columns:repeat(2,minmax(0,1fr)); } }
@media (max-width: 420px) { .workspace-context-bar { grid-template-columns:1fr; }.workspace-context-actions { grid-column:auto; }.workspace-context-actions button { width:100%; }.runtime-hero-facts { grid-template-columns:1fr; }.runtime-hero-facts div:last-child { grid-column:auto; } }
</style>
