<template>
  <div class="page-grid workflow-page">
    <ResourceContextRail label="当前工作流" :name="form.workflowName || '未命名工作流'" state="草稿编排" hint="配置节点并完成发布检查后即可运行" mark="W" />
    <section class="workflow-editor section-card">
      <header class="editor-toolbar">
        <div class="workflow-meta">
          <label class="toolbar-field"><span>工作流名称</span><input v-model.trim="form.workflowName" class="text-input" placeholder="例如：人事政策答复流程" /><small>给团队成员看的名称</small></label>
          <div class="toolbar-field system-id-note"><span>系统标识</span><strong>自动生成</strong><small>保存时由系统生成，名称修改不会影响已创建的工作流</small></div>
          <div class="toolbar-field system-id-note"><span>执行过程</span><strong>应用主工作流</strong><small>对话、表单和接口入口均执行当前发布版本</small></div>
        </div>
        <div class="editor-actions">
          <button class="ghost-btn" @click="resetCanvas">清空画布</button>
          <button class="secondary-btn toolbar-btn" :disabled="!editingWorkflowId" @click="runPublishChecks">发布检查</button>
          <button class="primary-btn" @click="submitWorkflow">保存草稿</button>
          <button class="primary-btn publish-btn" :disabled="!editingWorkflowId" @click="publishDraft">发布上线</button>
        </div>
      </header>
      <div v-if="submitMessage" class="editor-feedback" :class="`tone-${submitTone}`" role="status">
        <span class="feedback-dot" aria-hidden="true"></span>{{ submitMessage }}
      </div>
          <div class="sop-templates" aria-label="标准流程起步模板">
        <span class="template-label">从标准流程开始</span>
        <button v-for="template in availableSopTemplates" :key="template.id" class="template-btn" type="button" @click="applySopTemplate(template)">
          <strong>{{ template.name }}</strong><small>{{ template.description }}</small>
        </button>
      </div>
      <nav class="workflow-tabs" role="tablist" aria-label="标准流程工作区">
        <button class="workflow-tab" :class="{ active: editorTab === 'canvas' }" type="button" role="tab" :aria-selected="editorTab === 'canvas'" @click="editorTab = 'canvas'">
          <span>01</span><strong>设计画布</strong><small>节点、连接和配置</small>
        </button>
        <button class="workflow-tab" :class="{ active: editorTab === 'release' }" type="button" role="tab" :aria-selected="editorTab === 'release'" @click="editorTab = 'release'">
          <span>02</span><strong>发布与调试</strong><small>检查、测试和上线</small>
        </button>
        <button class="workflow-tab" :class="{ active: editorTab === 'versions' }" type="button" role="tab" :aria-selected="editorTab === 'versions'" @click="editorTab = 'versions'">
          <span>03</span><strong>工作流版本</strong><small>草稿、正式版本和入口</small>
        </button>
      </nav>

      <div v-show="editorTab === 'canvas'" class="workflow-tab-panel canvas-tab-panel">
      <div class="editor-layout">
        <aside class="node-palette">
          <div class="palette-head">
            <div>
              <div class="panel-title">节点库</div>
            <p class="panel-hint">点击添加到画布，拖动可直接放置</p>
            </div>
            <span class="palette-count">{{ filteredPaletteTypes.length }}</span>
          </div>
          <input v-model.trim="paletteSearch" class="text-input palette-search" autocomplete="off" spellcheck="false" placeholder="搜索节点" />
          <div class="palette-mode-switch" role="group" aria-label="编排模式">
            <button type="button" :class="{ active: !advancedMode }" @click="setPaletteMode(false)">基础模式</button>
            <button type="button" :class="{ active: advancedMode }" @click="setPaletteMode(true)">高级模式</button>
          </div>
          <div class="palette-tabs" role="tablist" aria-label="节点分类">
            <button v-for="category in paletteCategories" :key="category.value" class="palette-tab" :class="{ active: paletteCategory === category.value }" type="button" @click="paletteCategory = category.value">
              {{ category.label }}
            </button>
          </div>
          <p v-if="paletteTypes.length === 0 && !loadError" class="config-empty">正在读取后端节点目录...</p>
          <p v-else-if="paletteTypes.length === 0" class="config-empty">节点目录暂不可用，请检查服务连接或权限。</p>
          <p v-else-if="filteredPaletteTypes.length === 0" class="config-empty">没有匹配的节点，请更换分类或搜索词。</p>
          <p v-if="unavailableNodeCount" class="capability-note">{{ unavailableNodeCount }} 种尚未完成执行闭环的节点已隐藏。</p>
          <button
            v-for="type in filteredPaletteTypes"
            :key="type.value"
            :title="`${type.label}：${type.description || '点击添加到画布'}`"
            class="palette-node"
            draggable="true"
            @dragstart="startPaletteDrag($event, type.value)"
            @click="addNode(type.value)"
          >
            <span class="node-symbol" :class="`node-${type.value.toLowerCase()}`">{{ type.symbol }}</span>
            <span><strong>{{ type.label }}</strong><small>{{ type.description }}</small></span>
          </button>

          <div class="panel-title connection-title">连接节点</div>
          <p class="panel-hint">选择上游和下游节点，或直接拖动画布上的连接点。</p>
          <select v-model="connection.source" class="text-input">
            <option value="">选择上游节点</option>
            <option v-for="node in nodes" :key="`source-${node.id}`" :value="node.id">{{ connectionNodeLabel(node) }}</option>
          </select>
          <select v-model="connection.target" class="text-input">
            <option value="">选择下游节点</option>
            <option v-for="node in nodes" :key="`target-${node.id}`" :value="node.id">{{ connectionNodeLabel(node) }}</option>
          </select>
          <button class="secondary-btn" @click="addConnection">添加连线</button>
          <div v-if="edges.length" class="edge-list">
            <div v-for="(edge, index) in edges" :key="edge.edgeId || `${edge.source}-${edge.target}-${index}`" class="edge-item">
              <span>{{ edgeLabel(edge) }}</span>
              <button class="icon-text-btn" title="删除连线" @click="removeConnection(index)">×</button>
            </div>
          </div>
        </aside>

        <div
          ref="canvasRef"
          class="workflow-canvas"
          :class="{ 'is-panning': canvasInteraction.mode === 'pan', 'is-connecting': canvasInteraction.mode === 'connect' }"
          @pointerdown="startCanvasPan"
          @pointermove="moveCanvasPan"
          @pointerup="stopCanvasInteraction"
          @pointercancel="stopCanvasInteraction"
          @wheel.prevent="zoomCanvas"
          @dragover.prevent
          @drop="dropPaletteNode"
        >
          <div class="canvas-grid" :style="gridStyle"></div>
          <div v-if="nodes.length === 0" class="canvas-empty">从左侧添加节点开始编排</div>
          <div class="canvas-stage" :style="stageStyle">
            <svg class="edge-layer" aria-hidden="true">
              <defs>
                <marker id="workflow-arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
                  <path d="M0,0 L8,4 L0,8 z" fill="#6f9bc5" />
                </marker>
              </defs>
              <line v-for="edge in edgeLines" :key="edge.edgeId || `${edge.source}-${edge.target}`" v-bind="edge" marker-end="url(#workflow-arrow)" />
              <line v-if="connectionPreview" v-bind="connectionPreview" class="connection-preview" />
            </svg>
            <div
              v-for="node in nodes"
              :key="node.id"
              class="canvas-node"
              :class="[{ selected: selectedNode?.id === node.id, 'has-problem': nodeProblemCount(node) > 0 }, `canvas-node-${node.type.toLowerCase()}`]"
              :style="{ left: `${node.x}px`, top: `${node.y}px` }"
              @pointerdown.stop="startNodeDrag($event, node)"
              @click.stop="selectedNode = node"
            >
              <button
                v-if="node.type !== 'START'"
                class="connection-port input-port"
                title="连接到此节点"
                @pointerup.stop="finishConnection($event, node)"
              />
              <div class="canvas-node-head">
                <span class="node-symbol" :class="`node-${node.type.toLowerCase()}`">{{ nodeSymbol(node.type) }}</span>
                <strong>{{ node.name }}</strong>
                <button class="node-remove" title="删除节点" @pointerdown.stop @click.stop="removeNode(node.id)">×</button>
              </div>
              <div class="canvas-node-type">{{ nodeTypeLabel(node.type) }}</div>
              <span v-if="nodeProblemCount(node)" class="node-problem-count" :title="nodeProblems(node).join('；')">{{ nodeProblemCount(node) }}</span>
              <button
                v-if="node.type !== 'END'"
                class="connection-port output-port"
                title="从此节点连线"
                @pointerdown.stop="startConnection($event, node)"
              />
            </div>
          </div>
          <div class="canvas-controls" aria-label="画布视图控制">
            <button class="canvas-control reset-control" title="自动排列节点" @click.stop="autoLayout">自动排列</button>
            <button class="canvas-control reset-control" title="显示全部节点" @click.stop="fitViewport">适应视图</button>
            <button class="canvas-control" title="缩小" @click.stop="changeZoom(-0.1)">−</button>
            <span>{{ Math.round(viewport.scale * 100) }}%</span>
            <button class="canvas-control" title="放大" @click.stop="changeZoom(0.1)">+</button>
            <button class="canvas-control reset-control" title="重置画布视图" @click.stop="resetViewport">重置</button>
          </div>
        </div>

        <aside v-if="selectedNode" class="node-config">
          <div class="config-panel-head">
            <div class="panel-title">节点配置</div>
            <button type="button" class="config-close" title="关闭节点配置" aria-label="关闭节点配置" @click="selectedNode = null">×</button>
          </div>
          <div class="selected-node-title">
            <span class="node-symbol" :class="`node-${selectedNode.type.toLowerCase()}`">{{ nodeSymbol(selectedNode.type) }}</span>
            <div>
              <strong>{{ selectedNode.name }}</strong>
              <small v-if="selectedDescriptor">{{ selectedDescriptor.status === 'ACTIVE' ? '可用于发布' : '暂不可发布' }}</small>
              <small>{{ nodeTypeLabel(selectedNode.type) }}</small>
            </div>
          </div>
          <div class="node-contract-summary">
            <div><span>输入端口</span><strong>{{ selectedIncomingEdges.length ? selectedIncomingEdges.map(edge => nodeName(edge.source)).join('、') : '等待上游连接' }}</strong></div>
            <div><span>输出端口</span><strong>{{ selectedOutgoingEdges.length ? selectedOutgoingEdges.map(edge => nodeName(edge.target)).join('、') : '等待下游连接' }}</strong></div>
            <div><span>变量来源</span><strong>{{ selectedVariableReferences.length ? selectedVariableReferences.map(item => item.sourceNodeId).join('、') : '当前没有显式映射' }}</strong></div>
          </div>
          <div v-if="nodeProblems(selectedNode).length" class="node-validation-errors">
            <strong>保存前需要修正</strong>
            <span v-for="problem in nodeProblems(selectedNode)" :key="problem">{{ problem }}</span>
          </div>
          <div class="form-group">
            <label>显示名称</label>
            <input v-model.trim="selectedNode.name" class="text-input" />
            <small class="field-help">这是画布和执行记录中显示的名称，不影响节点类型。</small>
          </div>
          <component
            :is="selectedNodeEditor"
            :node="selectedNode"
            :descriptor="selectedDescriptor"
            :models="models"
            :agents="agents"
            :organization-units="organizationUnits"
            :connectors="connectors"
            :nodes="nodes"
          />
        </aside>
      </div>
      </div>

      <div v-show="editorTab === 'release'" class="workflow-tab-panel release-tab-panel">
      <section class="publish-panel">
        <div class="publish-head">
          <div>
            <div class="panel-title">发布检查</div>
            <p class="panel-hint">发布前查看阻断项、草稿修订号和正式环境当前版本。当前检查结果来自后端 `/checks`。</p>
          </div>
          <div class="check-summary">
            <span class="check-pill neutral">草稿 r{{ editingRevisionNo || 0 }}</span>
            <span class="check-pill" :class="editedWorkflow?.currentVersionNo ? 'pass' : 'warning'">
              {{ editedWorkflow?.currentVersionNo ? `正式 v${editedWorkflow.currentVersionNo}` : '正式版本未发布' }}
            </span>
            <span v-if="checkLoaded" class="check-pill" :class="checkIssues.length === 0 ? 'pass' : 'blocking'">
              {{ checkIssues.length === 0 ? '检查通过' : `${checkIssues.length} 项阻断` }}
            </span>
          </div>
        </div>
        <p v-if="checkMessage" class="check-message" :class="`tone-${checkTone}`">{{ checkMessage }}</p>
        <div v-if="!editingWorkflowId" class="config-empty">请先保存草稿，再查看发布检查清单。</div>
        <div v-else-if="checkLoading" class="config-empty">正在读取发布检查清单...</div>
        <div v-else-if="checkLoaded && checkIssues.length === 0" class="check-pass">
          <strong>当前草稿没有阻断项。</strong>
          <p>现在可以发布上线；正式入口仍然读取生产环境绑定版本，不会直接运行未发布草稿。</p>
        </div>
        <div v-else-if="checkLoaded" class="check-list">
          <article v-for="(issue, index) in checkIssues" :key="`${issue.code}-${issue.nodeId}-${issue.fieldPath}-${index}`" class="check-item" :class="{ actionable: issue.nodeId }" @click="focusIssue(issue)">
            <div class="check-item-head">
              <span class="check-level">{{ issue.level === 'BLOCKING' ? '阻断' : issue.level }}</span>
              <code>{{ issue.code }}</code>
            </div>
            <strong>{{ issue.message }}</strong>
            <p v-if="issue.nodeId || issue.fieldPath" class="check-meta">
              节点：{{ issue.nodeId || '全局' }}
              <span v-if="issue.fieldPath"> · 字段：{{ issue.fieldPath }}</span>
            </p>
            <p v-if="issue.suggestion" class="check-suggestion">建议：{{ issue.suggestion }}</p>
          </article>
        </div>
        <div v-else class="config-empty">保存草稿后可查看后端校验返回的结构化检查项。</div>
      </section>

      <section class="debug-panel">
        <div class="publish-head">
          <div>
            <div class="panel-title">运行预览</div>
            <p class="panel-hint">先用草稿调试单个节点；发布后才能执行完整工作流。</p>
          </div>
          <div class="debug-modes" aria-label="运行环境">
            <span class="debug-mode active">草稿调试 · r{{ editingRevisionNo || 0 }}</span>
            <span class="debug-mode" :class="{ ready: editedWorkflow?.currentVersionNo }">正式运行 · {{ editedWorkflow?.currentVersionNo ? `PRODUCTION v${editedWorkflow.currentVersionNo}` : '尚未发布' }}</span>
          </div>
          <div class="editor-actions">
            <button class="secondary-btn toolbar-btn" :disabled="!editingWorkflowId || !selectedNode || debugLoading" @click="debugSelectedNode">调试当前节点</button>
            <button class="primary-btn toolbar-btn" :disabled="!editingWorkflowId || !editedWorkflow?.currentVersionNo || debugLoading" @click="executePublished">运行已发布版本</button>
          </div>
        </div>
        <div class="debug-grid">
          <div>
            <label class="debug-label">测试输入</label>
            <textarea v-model="debugInput" class="textarea-input debug-textarea" spellcheck="false" />
          </div>
          <div>
            <label class="debug-label">运行结果</label>
            <pre class="debug-output">{{ debugOutput || '填写测试输入后开始运行' }}</pre>
          </div>
        </div>
        <p v-if="debugMessage" class="check-message" :class="`tone-${debugTone}`">{{ debugMessage }}</p>
      </section>

      <details class="graph-preview">
        <summary>查看工作流 JSON</summary>
        <pre>{{ graphJson }}</pre>
      </details>
      </div>
    </section>

    <section v-show="editorTab === 'versions'" class="workflow-tab-panel version-tab-panel">
    <SectionCard title="工作流列表" description="草稿用于调试和继续编辑，正式入口始终依赖生产环境当前版本。">
      <p v-if="loadError" class="empty-state">{{ loadError }}</p>
      <p v-else-if="workflows.length === 0" class="empty-state">当前租户暂无工作流。</p>
      <table v-else class="table-card">
        <thead><tr><th>工作流编码</th><th>工作流名称</th><th>草稿 / 正式版本</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in workflows" :key="item.id">
            <td><code>{{ item.code }}</code></td>
            <td>
              <strong>{{ item.name }}</strong>
              <p class="entry-hint">图类型：{{ item.graphType }}</p>
            </td>
            <td class="version-cell">
              <span class="pill">草稿 r{{ item.revision }}</span>
              <span v-if="item.currentVersionNo" class="pill version-pill">正式 v{{ item.currentVersionNo }}</span>
              <span v-else class="version-placeholder">正式版本未发布</span>
              <small v-if="item.currentVersionReleasedAt" class="entry-hint">{{ item.environmentCode || 'PRODUCTION' }} · {{ formatTime(item.currentVersionReleasedAt) }}</small>
            </td>
            <td><span class="status-badge" :class="`status-${item.status.toLowerCase()}`">{{ workflowStatusText(item) }}</span></td>
            <td class="row-actions">
              <button class="link-btn" @click="editWorkflow(item.id)">{{ item.currentVersionNo ? '继续调试' : '编辑草稿' }}</button>
              <RouterLink v-if="item.currentVersionNo" :to="{ path: '/chat', query: { appId: item.id } }" class="link-btn">正式入口</RouterLink>
              <span v-else class="version-placeholder">正式入口不可用</span>
            </td>
          </tr>
        </tbody>
      </table>
    </SectionCard>
    <section v-if="editingWorkflowId" class="version-history">
      <div class="publish-head">
        <div><div class="panel-title">发布版本</div><p class="panel-hint">历史版本保持不可变，可以比较差异、派生新草稿或切换生产版本。</p></div>
        <button class="secondary-btn" :disabled="versionLoading" @click="loadVersions">刷新版本</button>
      </div>
      <p v-if="versionMessage" class="check-message" :class="`tone-${versionTone}`">{{ versionMessage }}</p>
      <div v-if="versions.length === 0" class="config-empty">当前应用还没有发布版本。</div>
      <table v-else class="table-card version-history-table">
        <thead><tr><th>版本</th><th>状态</th><th>发布时间</th><th>快照</th><th>操作</th></tr></thead>
        <tbody><tr v-for="version in versions" :key="version.versionId">
          <td><strong>v{{ version.versionNo }}</strong><small class="entry-hint">{{ version.versionId }}</small></td>
          <td><span class="status-badge" :class="version.current ? 'status-published' : 'status-draft'">{{ version.current ? '生产使用中' : '历史版本' }}</span></td>
          <td>{{ formatTime(version.releasedAt) }}</td>
          <td><code>{{ version.releaseBundleHash ? version.releaseBundleHash.slice(0, 12) : '缺少快照' }}</code></td>
          <td class="row-actions">
            <button class="link-btn" @click="createDraftFromVersion(version)">创建草稿</button>
            <button v-if="!version.current" class="link-btn" @click="rollbackVersion(version)">切换生产版本</button>
          </td>
        </tr></tbody>
      </table>
      <div v-if="versions.length > 1" class="version-compare-controls">
        <label>基准版本<select v-model="compareLeft" class="text-input"><option v-for="version in versions" :key="`left-${version.versionId}`" :value="version.versionId">v{{ version.versionNo }}</option></select></label>
        <label>目标版本<select v-model="compareRight" class="text-input"><option v-for="version in versions" :key="`right-${version.versionId}`" :value="version.versionId">v{{ version.versionNo }}</option></select></label>
        <button class="secondary-btn" :disabled="compareLeft === compareRight" @click="compareVersions">比较版本</button>
      </div>
      <div v-if="versionComparison" class="version-diff">
        <strong>v{{ versionComparison.left.versionNo }} 与 v{{ versionComparison.right.versionNo }} 的差异</strong>
        <span>新增节点：{{ versionComparison.addedNodeIds.join('、') || '无' }}</span>
        <span>删除节点：{{ versionComparison.removedNodeIds.join('、') || '无' }}</span>
        <span>修改节点：{{ versionComparison.modifiedNodeIds.join('、') || '无' }}</span>
        <span>连线：{{ versionComparison.edgesChanged ? '有变化' : '无变化' }}；输入契约：{{ versionComparison.inputContractChanged ? '有变化' : '无变化' }}；输出契约：{{ versionComparison.outputContractChanged ? '有变化' : '无变化' }}；依赖：{{ versionComparison.dependenciesChanged ? '有变化' : '无变化' }}</span>
      </div>
    </section>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { WORKFLOW_VARIABLES, nodeOutputReference } from '../constants/workflowVariables'
import SectionCard from '../components/SectionCard.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import StartEndNodeConfig from '../components/workflow/nodes/StartEndNodeConfig.vue'
import SchemaNodeConfig from '../components/workflow/nodes/SchemaNodeConfig.vue'
import RagNodeConfig from '../components/workflow/nodes/RagNodeConfig.vue'
import http from '../api/http'
import { subscribeExecutionEvents } from '../api/execution-events'
import { fetchRuntimeApplications, fetchRuntimeRun, submitRuntimeDraftTest } from '../api/runtime'
import { confirmAction } from '../utils/feedback'
import { useRoute } from 'vue-router'

const PRODUCTION_ENV = 'PRODUCTION'
const route = useRoute()
const NODE_SYMBOLS = {
  START: '▶',
  END: '■',
  LLM: '✦',
  RAG: '⌕',
  HUMAN: '✓',
  CONDITION: '?',
  AGENT: '◎',
  PARALLEL: '∥',
  JOIN: '⋈',
  LOOP: '↺',
  TRANSFORM: '⇄',
  CONTEXT_BUILDER: '▣',
  PROMPT_TEMPLATE: '✎',
  SESSION_MEMORY: '◌',
  AGENT_TEAM: '◎',
  GRAPH_ORCHESTRATOR: '◇'
}
const NODE_LABELS = {
  START: '开始', END: '结束', USER_INPUT: '用户输入', DIRECT_REPLY: '直接回复',
  LLM: '模型生成', RAG: '知识检索', AGENT: 'Agent 调用', QUESTION_CLASSIFIER: '问题分类',
  ROUTER: '路由分支', PARAMETER_EXTRACTOR: '参数提取', CONDITION: '条件分支', ITERATION: '迭代',
  PARALLEL: '并行分支', JOIN: '并行聚合', LOOP: '循环', TRANSFORM: '数据转换', HUMAN: '人工审批',
  CODE: '代码执行', TEMPLATE_TRANSFORM: '模板转换', VARIABLE_AGGREGATOR: '变量聚合',
  DOCUMENT_EXTRACTOR: '文档提取', VARIABLE_ASSIGNMENT: '变量赋值', LIST_OPERATOR: '列表处理',
  HTTP_REQUEST: 'HTTP 请求', OPENAPI: 'OpenAPI', MCP: 'MCP 工具', WEBHOOK: 'Webhook',
  INTERNAL_API: '内部 API', WEB_CRAWLER: '网页抓取', CONTEXT_BUILDER: '上下文整理',
  PROMPT_TEMPLATE: '提示词模板', SESSION_MEMORY: '会话记忆', AGENT_TEAM: '智能体团队',
  GRAPH_ORCHESTRATOR: '图编排运行时'
}
// 节点分组面向业务用户命名；具体节点类型只在这里映射一次。
const NODE_GROUPS = Object.freeze({
  BASIC: { label: '基础', types: ['START', 'END', 'USER_INPUT', 'DIRECT_REPLY', 'LLM'] },
  CONTROL: { label: '控制流', types: ['CONDITION', 'ROUTER', 'ITERATION', 'PARALLEL', 'JOIN', 'LOOP', 'HUMAN'] },
  CONTEXT: { label: '上下文', types: ['RAG', 'CONTEXT_BUILDER', 'PROMPT_TEMPLATE', 'SESSION_MEMORY'] },
  TOOLS: { label: '工具', types: ['HTTP_REQUEST', 'OPENAPI', 'MCP', 'WEBHOOK', 'INTERNAL_API', 'WEB_CRAWLER', 'TRANSFORM', 'CODE', 'TEMPLATE_TRANSFORM', 'VARIABLE_AGGREGATOR', 'DOCUMENT_EXTRACTOR', 'VARIABLE_ASSIGNMENT', 'LIST_OPERATOR'] },
  AGENTS: { label: '多智能体', types: ['AGENT', 'QUESTION_CLASSIFIER', 'PARAMETER_EXTRACTOR', 'AGENT_TEAM', 'GRAPH_ORCHESTRATOR'] }
})
const NODE_GROUP_ORDER = Object.freeze(['BASIC', 'CONTROL', 'CONTEXT', 'TOOLS', 'AGENTS'])
const NODE_GROUP_BY_TYPE = Object.freeze(Object.fromEntries(
  NODE_GROUP_ORDER.flatMap(group => NODE_GROUPS[group].types.map(type => [type, group]))
))
const SOP_TEMPLATES = [
  {
    id: 'conversation-reply', name: '基础对话', description: '接收用户消息并返回可编辑答复', graphType: 'APPLICATION_WORKFLOW',
    requiredTypes: ['START', 'USER_INPUT', 'DIRECT_REPLY', 'END'],
    nodes: [
      { id: 'start', type: 'START', name: '开始', x: 80, y: 190 },
      { id: 'user-input', type: 'USER_INPUT', name: '接收用户消息', x: 300, y: 190 },
      { id: 'reply', type: 'DIRECT_REPLY', name: '返回答复', x: 520, y: 190, config: { messageTemplate: '已收到：{{variables.user_message}}' } },
      { id: 'end', type: 'END', name: '结束', x: 740, y: 190 }
    ],
    edges: [['start', 'user-input'], ['user-input', 'reply'], ['reply', 'end']]
  },
  {
    id: 'conditional-sop', name: '条件分流', description: '按输入是否为空走两条明确分支', graphType: 'APPLICATION_WORKFLOW',
    requiredTypes: ['START', 'USER_INPUT', 'CONDITION', 'DIRECT_REPLY', 'END'],
    nodes: [
      { id: 'start', type: 'START', name: '开始', x: 60, y: 250 },
      { id: 'user-input', type: 'USER_INPUT', name: '接收用户消息', x: 250, y: 250 },
      { id: 'condition', type: 'CONDITION', name: '判断是否有消息', x: 460, y: 250, config: { operator: 'NOT_EMPTY', inputReference: WORKFLOW_VARIABLES.USER_MESSAGE, trueTarget: 'reply-yes', falseTarget: 'reply-no' } },
      { id: 'reply-yes', type: 'DIRECT_REPLY', name: '消息有效', x: 680, y: 130, config: { messageTemplate: '已收到：{{variables.user_message}}' } },
      { id: 'reply-no', type: 'DIRECT_REPLY', name: '消息为空', x: 680, y: 370, config: { messageTemplate: '请补充需要处理的信息。' } },
      { id: 'end', type: 'END', name: '结束', x: 900, y: 250 }
    ],
    edges: [['start', 'user-input'], ['user-input', 'condition'], ['condition', 'reply-yes', 'true'], ['condition', 'reply-no', 'false'], ['reply-yes', 'end'], ['reply-no', 'end']]
  },
  {
    id: 'approval-sop', name: '人工审批', description: '将业务请求提交指定组织审批', graphType: 'APPLICATION_WORKFLOW',
    requiredTypes: ['START', 'HUMAN', 'END'],
    nodes: [
      { id: 'start', type: 'START', name: '开始', x: 130, y: 190 },
      { id: 'approval', type: 'HUMAN', name: '部门审批', x: 400, y: 190, config: { approvalTitle: '业务审批', approvalDescription: '请审核本次业务请求。', approvalGroupId: '' } },
      { id: 'end', type: 'END', name: '结束', x: 670, y: 190 }
    ],
    edges: [['start', 'approval'], ['approval', 'end']]
  }
]
const PALETTE_CATEGORIES = NODE_GROUP_ORDER.map(value => ({ value, label: NODE_GROUPS[value].label }))
const RESOURCE_EMPTY_STATE = {
  MODEL: '当前账号暂无可用模型，请先在模型中心完成配置。',
  KNOWLEDGE_DOCUMENT: '当前租户暂无可用知识文档。',
  AGENT: '当前租户暂无可用 Agent。',
  ORGANIZATION_UNIT: '当前没有可选组织，请先在组织架构中创建团队。'
}
const LEGACY_CONFIG_FIELDS = [
  'modelId',
  'modelKey',
  'backupModelId',
  'backupModelKey',
  'promptTemplate',
  'agentId',
  'operator',
  'value',
  'trueTarget',
  'falseTarget',
  'approvalTitle',
  'approvalDescription',
  'approvalGroupId',
  'approvalGroup',
  'riskLevel'
]

const nodeTypes = ref([])
const workflows = ref([])
const versions = ref([])
const versionLoading = ref(false)
const versionMessage = ref('')
const versionTone = ref('info')
const compareLeft = ref('')
const compareRight = ref('')
const versionComparison = ref(null)
const models = ref([])
const agents = ref([])
const organizationUnits = ref([])
const connectors = ref([])
const loadError = ref('')
const submitMessage = ref('')
const submitTone = ref('success')
const editorTab = ref('canvas')
const checkMessage = ref('')
const checkTone = ref('info')
const checkLoading = ref(false)
const checkLoaded = ref(false)
const checkIssues = ref([])
const sourcesLoaded = ref(false)
const paletteType = ref('')
const paletteSearch = ref('')
const paletteCategory = ref('BASIC')
const advancedMode = ref(false)
const connection = reactive({ source: '', target: '' })
const form = reactive({ workflowCode: '', workflowName: '' })
const editingWorkflowId = ref(null)
const editingRevisionNo = ref(null)
const graphType = ref('APPLICATION_WORKFLOW')
const graphInputSchema = ref({ type: 'object', properties: {} })
const graphOutputSchema = ref({ type: 'object', properties: {} })
const debugInput = ref('{}')
const debugOutput = ref('')
const debugMessage = ref('')
const debugTone = ref('info')
const debugLoading = ref(false)
const canvasRef = ref(null)
const selectedNode = ref(null)
const viewport = reactive({ x: 0, y: 0, scale: 1 })
const canvasInteraction = reactive({ mode: 'idle', node: null, offsetX: 0, offsetY: 0, pointerId: null, captureTarget: null, sourceNode: null, pointerX: 0, pointerY: 0 })
const nodeWidth = 164
const nodePortY = 34

const nodes = ref([])
const edges = ref([])

const nodeTypeMap = computed(() => Object.fromEntries(nodeTypes.value.map(item => [item.nodeType || item.value, item])))
const unavailableNodeCount = computed(() => nodeTypes.value.filter(item => item.status !== 'ACTIVE' || item.executable === false).length)
const paletteTypes = computed(() => nodeTypes.value
  .filter(item => item.status === 'ACTIVE' && item.executable !== false && supportsGraphType(item, graphType.value))
  .map(item => ({
    ...item,
    value: item.nodeType || item.value,
    label: localizedNodeLabel(item),
    description: item.description || item.capabilities?.join('、') || '已注册节点',
    symbol: nodeSymbolForType(item.nodeType || item.value, item.symbol)
  })))
const paletteCategories = computed(() => advancedMode.value ? PALETTE_CATEGORIES : PALETTE_CATEGORIES.slice(0, 1))
const filteredPaletteTypes = computed(() => {
  const query = paletteSearch.value.toLowerCase()
  return paletteTypes.value.filter(item => {
    const group = NODE_GROUP_BY_TYPE[item.value] || 'BASIC'
    const categoryMatch = group === paletteCategory.value
    const text = `${item.label} ${item.value} ${item.description}`.toLowerCase()
    return (advancedMode.value || group === 'BASIC') && categoryMatch && (!query || text.includes(query))
  })
})
const availableSopTemplates = computed(() => SOP_TEMPLATES.filter(template => template.requiredTypes.every(type => {
  const descriptor = nodeTypeMap.value[type]
  return descriptor?.status === 'ACTIVE' && descriptor.executable !== false && supportsGraphType(descriptor, template.graphType)
})))
const editedWorkflow = computed(() => workflows.value.find(item => item.id === editingWorkflowId.value) || null)
const selectedDescriptor = computed(() => (selectedNode.value ? nodeTypeMap.value[selectedNode.value.type] : null))
const selectedIncomingEdges = computed(() => selectedNode.value ? edges.value.filter(edge => edge.target === selectedNode.value.id) : [])
const selectedOutgoingEdges = computed(() => selectedNode.value ? edges.value.filter(edge => edge.source === selectedNode.value.id) : [])
const nodeEditorMap = { START: StartEndNodeConfig, END: StartEndNodeConfig, RAG: RagNodeConfig }
const selectedNodeEditor = computed(() => nodeEditorMap[selectedNode.value?.type] || SchemaNodeConfig)

// 从节点配置和连线生成统一变量引用，确保页面配置可以被运行时识别。
const workflowVariableReferences = computed(() => {
  const references = new Map()
  const addReference = sourceNodeId => {
    if (!sourceNodeId || !nodes.value.some(node => node.id === sourceNodeId)) return
    references.set(sourceNodeId, {
      sourceNodeId,
      outputPath: '$.output',
      dataType: 'any',
      sensitive: false
    })
  }
  const collect = value => {
    if (typeof value === 'string') {
      const match = value.match(/^(.+?)\.output(?:\.|$)/)
      if (match) addReference(match[1])
      return
    }
    if (Array.isArray(value)) {
      value.forEach(collect)
      return
    }
    if (value && typeof value === 'object') Object.values(value).forEach(collect)
  }
  nodes.value.forEach(node => collect(buildSerializedConfig(node)))
  edges.value.forEach(edge => addReference(edge.source))
  return [...references.values()]
})
const selectedVariableReferences = computed(() => selectedNode.value
  ? workflowVariableReferences.value.filter(item => selectedIncomingEdges.value.some(edge => edge.source === item.sourceNodeId))
  : [])
const graphPayload = computed(() => ({
  graphType: graphType.value,
  schemaVersion: '1.0',
  inputSchema: graphInputSchema.value,
  outputSchema: graphOutputSchema.value,
  nodes: nodes.value.map(serializeNode),
  edges: edges.value.map(serializeEdge),
  variables: workflowVariableReferences.value
}))
const graphJson = computed(() => JSON.stringify(graphPayload.value, null, 2))
const edgeLines = computed(() => edges.value.flatMap(edge => {
  const source = nodes.value.find(node => node.id === edge.source)
  const target = nodes.value.find(node => node.id === edge.target)
  if (!source || !target) return []
  return [{ edgeId: edge.edgeId, source: edge.source, target: edge.target, x1: source.x + nodeWidth, y1: source.y + nodePortY, x2: target.x, y2: target.y + nodePortY }]
}))
const stageStyle = computed(() => ({ transform: `translate(${viewport.x}px, ${viewport.y}px) scale(${viewport.scale})` }))
const gridStyle = computed(() => ({ backgroundPosition: `${viewport.x}px ${viewport.y}px`, backgroundSize: `${18 * viewport.scale}px ${18 * viewport.scale}px` }))
const connectionPreview = computed(() => {
  if (!canvasInteraction.sourceNode) return null
  return { x1: canvasInteraction.sourceNode.x + nodeWidth, y1: canvasInteraction.sourceNode.y + nodePortY, x2: canvasInteraction.pointerX, y2: canvasInteraction.pointerY }
})

const supportsGraphType = (descriptor, graphType) => {
  const graphTypes = descriptor?.supportedGraphTypes || []
  return graphTypes.includes('*') || graphTypes.includes(graphType)
}
const nodeSymbolForType = (type, descriptorSymbol) => descriptorSymbol && descriptorSymbol !== '*' && String(descriptorSymbol).length <= 2 ? descriptorSymbol : ({
  START: 'S', END: 'E', USER_INPUT: 'IN', DIRECT_REPLY: 'OUT', LLM: 'AI', RAG: 'KB', HUMAN: 'OK', CONDITION: '?',
  AGENT: 'A', QUESTION_CLASSIFIER: 'Q', PARAMETER_EXTRACTOR: 'P', ROUTER: 'R', PARALLEL: '||', JOIN: '+',
  ITERATION: 'FOR', LOOP: 'LOOP', TRANSFORM: 'T', CODE: '</>', TEMPLATE_TRANSFORM: 'J', VARIABLE_AGGREGATOR: 'Σ',
  DOCUMENT_EXTRACTOR: 'D', VARIABLE_ASSIGNMENT: '=', LIST_OPERATOR: '[]', HTTP_REQUEST: '↗', OPENAPI: 'A',
  MCP: 'M', WEBHOOK: 'W', INTERNAL_API: 'I', WEB_CRAWLER: 'W'
}[type] || 'N')
const nodeSymbol = (type) => nodeSymbolForType(type, nodeTypeMap.value[type]?.symbol)
const localizedNodeLabel = (descriptor) => NODE_LABELS[descriptor?.nodeType || descriptor?.value] || descriptor?.label || descriptor?.nodeType || descriptor?.value || '节点'
const nodeTypeLabel = (type) => localizedNodeLabel(nodeTypeMap.value[type]) || type
const statusText = (status) => ({ ACTIVE: '可用', DRAFT: '草稿', DISABLED: '已停用' }[status] || status || '未知状态')
const workflowStatusText = (item) => {
  if (item.status === 'DRAFT' && item.currentVersionNo) return '草稿调试中'
  return ({ DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用' }[item.status] || item.status)
}
const nodeName = (id) => nodes.value.find(node => node.id === id)?.name || '尚未选择'
const connectionNodeLabel = (node) => `${node.name || nodeTypeLabel(node.type)} · ${nodeTypeLabel(node.type)}`
const edgeLabel = (edge) => {
  const source = nodes.value.find(node => node.id === edge.source)
  if (source?.type !== 'CONDITION') return `${edge.source} → ${edge.target}`
  if (source.config?.trueTarget === edge.target) return `${edge.source} · 满足（是） → ${edge.target}`
  if (source.config?.falseTarget === edge.target) return `${edge.source} · 不满足（否） → ${edge.target}`
  return `${edge.source} → ${edge.target}`
}
const formatTime = (value) => value ? new Date(value).toLocaleString('zh-CN') : ''
const setPaletteMode = advanced => {
  advancedMode.value = advanced
  paletteCategory.value = 'BASIC'
}

const setSubmitMessage = (message, tone = 'success') => {
  submitMessage.value = message
  submitTone.value = tone
}
const setCheckMessage = (message, tone = 'info') => {
  checkMessage.value = message
  checkTone.value = tone
}
const normalizeId = (value) => value === undefined || value === null || value === '' ? '' : String(value)
const toCoordinate = (value, fallback) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}
const isBlank = (value) => value === undefined || value === null || String(value).trim() === ''
const readError = (error, fallback) => {
  const code = error?.response?.data?.code || error?.code
  if (code === 'FORBIDDEN') return '当前账号没有执行该编排操作或访问依赖资源的权限，请联系管理员。'
  if (code === 'VERSION_UNAVAILABLE') return '正式环境当前没有可用版本或依赖资源已失效，请重新检查后再发布。'
  if (code === 'CONFLICT') return '草稿已被其他用户更新，请重新加载最新修订后再继续。'
  const raw = error?.response?.data?.message || error?.message || fallback
  if (raw.includes('修订冲突') || raw.includes('已过期')) {
    return '检测到草稿已被其他人更新，请重新加载最新草稿后再继续保存或发布。'
  }
  if (raw.includes('没有该编排资源权限')) {
    return '当前账号没有该编排应用的访问权限，请联系管理员授予。'
  }
  return raw
}
const schemaFieldsForType = (type) => nodeTypeMap.value[type]?.configSchema?.fields || []
const applyFieldDefaults = (type, config = {}) => {
  const next = { ...config }
  schemaFieldsForType(type).forEach(field => {
    if ((next[field.name] === undefined || next[field.name] === null || next[field.name] === '') && field.default !== undefined) {
      next[field.name] = field.default
    }
  })
  return next
}
const buildSerializedConfig = (node) => {
  const config = applyFieldDefaults(node.type, { ...(node.config || {}) })
  if (node.type === 'LLM') {
    config.modelId = normalizeId(config.modelId)
    const model = models.value.find(item => String(item.id) === config.modelId)
    config.modelKey = config.modelId ? (model?.modelKey || config.modelKey || '') : ''
    config.backupModelId = normalizeId(config.backupModelId)
    const backupModel = models.value.find(item => String(item.id) === config.backupModelId)
    config.backupModelKey = config.backupModelId ? (backupModel?.modelKey || config.backupModelKey || '') : ''
  }
  if (node.type === 'RAG') {
    config.embeddingModelId = normalizeId(config.embeddingModelId)
    config.knowledgeDocumentIds = Array.isArray(config.knowledgeDocumentIds)
      ? config.knowledgeDocumentIds.map(normalizeId).filter(Boolean)
      : []
    config.retrievalScope = config.retrievalScope || 'VISIBLE_DOCUMENTS'
    config.languageStrategy = config.languageStrategy || 'AUTO'
  }
  if (node.type === 'AGENT') {
    config.agentId = normalizeId(config.agentId)
  }
  if (node.type === 'HUMAN') {
    config.approvalGroupId = normalizeId(config.approvalGroupId)
    const unit = organizationUnits.value.find(item => String(item.id) === config.approvalGroupId)
    config.approvalGroup = config.approvalGroupId ? (unit?.orgName || config.approvalGroup || '') : ''
  }
  if (node.type === 'CONDITION') {
    config.trueTarget = normalizeId(config.trueTarget)
    config.falseTarget = normalizeId(config.falseTarget)
    if (!config.operator) config.operator = 'CONTAINS'
  }
  return config
}
const syncField = (node) => {
  if (!node) return
  const next = buildSerializedConfig(node)
  Object.keys(next).forEach(key => {
    node.config[key] = next[key]
  })
}
const isFieldVisible = (node, field) => {
  if (!field?.visibleWhen) return true
  const currentValue = node?.config?.[field.visibleWhen.field]
  if (field.visibleWhen.equals !== undefined) return String(currentValue || '') === String(field.visibleWhen.equals)
  if (field.visibleWhen.notEquals !== undefined) return String(currentValue || '') !== String(field.visibleWhen.notEquals)
  return true
}
const normalizeNode = (raw, index = 0) => {
  const type = raw.type || raw.nodeType || 'TRANSFORM'
  const config = { ...(raw.config || {}) }
  LEGACY_CONFIG_FIELDS.forEach(field => {
    if (config[field] === undefined && raw[field] !== undefined) config[field] = raw[field]
  })
  const node = {
    id: raw.id || raw.nodeId || `${type.toLowerCase()}-${index + 1}`,
    type,
    name: raw.name || raw.title || nodeTypeLabel(type),
    x: toCoordinate(raw.x, 120 + index * 40),
    y: toCoordinate(raw.y, 120 + index * 28),
    config: applyFieldDefaults(type, config),
    inputSchema: raw.inputSchema || {},
    outputSchema: raw.outputSchema || {}
  }
  node.config = buildSerializedConfig(node)
  return node
}
const normalizeEdge = (edge, index = 0) => ({
  edgeId: edge.edgeId || `edge-${index + 1}`,
  source: edge.source || edge.sourceNodeId || '',
  sourcePort: edge.sourcePort || 'default',
  target: edge.target || edge.targetNodeId || '',
  targetPort: edge.targetPort || 'default'
})
const syncAllNodes = () => {
  const selectedId = selectedNode.value?.id
  nodes.value = nodes.value.map((node, index) => normalizeNode(node, index))
  selectedNode.value = selectedId ? nodes.value.find(node => node.id === selectedId) || null : null
}
const nextNodeId = (type) => {
  let sequence = 1
  let candidate = `${type.toLowerCase()}-${sequence}`
  while (nodes.value.some(node => node.id === candidate)) {
    sequence += 1
    candidate = `${type.toLowerCase()}-${sequence}`
  }
  return candidate
}
const fieldOptions = (field, node) => {
  if (!field) return []
  if (field.widget === 'select') return field.options || []
  if (field.widget === 'node-select') {
    return nodes.value
      .filter(item => !field.excludeSelf || item.id !== node.id)
      .map(item => ({ value: item.id, label: item.name }))
  }
  if (field.widget !== 'resource-select') return []
  if (field.resourceType === 'MODEL') {
    return models.value
      .filter(item => !field.excludeSelectedField || String(item.id) !== String(node.config?.[field.excludeSelectedField] || ''))
      .map(item => ({ value: String(item.id), label: `${item.modelName}（${item.modelKey}）` }))
  }
  // 知识文档由 RAG 专用编辑器远程分页搜索，通用 schema 不加载全租户文档。
  if (field.resourceType === 'KNOWLEDGE_DOCUMENT') return []
  if (field.resourceType === 'AGENT') {
    return agents.value.map(item => ({ value: String(item.id), label: `${item.name}（${item.code}）` }))
  }
  if (field.resourceType === 'ORGANIZATION_UNIT') {
    return organizationUnits.value.map(item => ({ value: String(item.id), label: item.orgName }))
  }
  return []
}
const fieldEmptyState = (field) => RESOURCE_EMPTY_STATE[field?.resourceType] || '当前没有可选资源。'

const serializeNode = (node) => ({
  nodeId: node.id,
  nodeType: node.type,
  title: node.name,
  x: Number(Number(node.x || 0).toFixed(2)),
  y: Number(Number(node.y || 0).toFixed(2)),
  config: buildSerializedConfig(node),
  inputSchema: node.inputSchema || {},
  outputSchema: node.outputSchema || {}
})
const serializeEdge = (edge, index) => ({
  edgeId: edge.edgeId || `edge-${index + 1}`,
  sourceNodeId: edge.source,
  sourcePort: sourcePortForEdge(edge),
  targetNodeId: edge.target,
  targetPort: edge.targetPort || 'default'
})
const sourcePortForEdge = (edge) => {
  const source = nodes.value.find(node => node.id === edge.source)
  if (source?.type !== 'CONDITION') return edge.sourcePort || 'default'
  const config = buildSerializedConfig(source)
  if (config.trueTarget === edge.target) return 'true'
  if (config.falseTarget === edge.target) return 'false'
  return edge.sourcePort || 'default'
}

const loadSources = async () => {
  // 节点 schema 和可引用资源都从后端读取，页面只负责将真实数据映射成控件。
  const [modelResponse, organizationResponse, nodeTypeResponse, agentResponse, connectorResponse] = await Promise.allSettled([
    http.get('/system/models'),
    http.get('/organization/units'),
    http.get('/orchestration/node-types'),
    fetchRuntimeApplications(),
    http.get('/orchestration/resources/connectors')
  ])
  if (modelResponse.status === 'fulfilled') models.value = modelResponse.value.data || []
  if (organizationResponse.status === 'fulfilled') organizationUnits.value = organizationResponse.value.data || []
  if (nodeTypeResponse.status === 'fulfilled') nodeTypes.value = nodeTypeResponse.value.data || []
  if (agentResponse.status === 'fulfilled') agents.value = agentResponse.value.data || []
  if (connectorResponse.status === 'fulfilled') connectors.value = connectorResponse.value.data || []
  sourcesLoaded.value = true
  syncAllNodes()
}

const loadWorkflows = async () => {
  loadError.value = ''
  try {
    const response = await http.get('/orchestration/apps')
    workflows.value = response.data || []
  } catch (error) {
    loadError.value = readError(error, '工作流读取失败，请检查服务连接。')
  }
}

const loadVersions = async () => {
  if (!editingWorkflowId.value) return
  versionLoading.value = true
  versionMessage.value = ''
  try {
    const response = await http.get(`/orchestration/apps/${editingWorkflowId.value}/versions`)
    versions.value = response.data || []
    compareLeft.value = versions.value[1]?.versionId || versions.value[0]?.versionId || ''
    compareRight.value = versions.value[0]?.versionId || ''
  } catch (error) {
    versionMessage.value = readError(error, '发布版本读取失败。')
    versionTone.value = 'error'
  } finally { versionLoading.value = false }
}

const createDraftFromVersion = async version => {
  if (!await confirmAction({ title: '从版本创建草稿', message: `将从正式版本 v${version.versionNo} 创建新的草稿修订，当前生产版本不会变化。` })) return
  try {
    const response = await http.post(`/orchestration/apps/${editingWorkflowId.value}/versions/${version.versionId}/draft`)
    versionMessage.value = `已从 v${version.versionNo} 创建草稿 r${response.data.revisionNo}。`
    versionTone.value = 'success'
    await editWorkflow(editingWorkflowId.value)
    editorTab.value = 'canvas'
  } catch (error) { versionMessage.value = readError(error, '创建草稿失败。'); versionTone.value = 'error' }
}

const rollbackVersion = async version => {
  if (!await confirmAction({ title: '切换生产版本', message: `生产入口将切换到 v${version.versionNo}。此操作只影响之后的新调用，历史运行仍保留原版本证据，当前草稿不会被修改。` })) return
  try {
    const response = await http.post(`/orchestration/apps/${editingWorkflowId.value}/rollback`, {
      versionId: version.versionId, environmentCode: PRODUCTION_ENV, reason: '从版本历史切换生产版本'
    })
    versionMessage.value = `生产入口已切换到 v${version.versionNo}。之后的新调用会使用该版本，历史运行不会改变。`
    versionTone.value = 'success'
    await Promise.all([loadVersions(), loadWorkflows()])
  } catch (error) { versionMessage.value = readError(error, '生产版本切换失败。'); versionTone.value = 'error' }
}

const compareVersions = async () => {
  if (!compareLeft.value || !compareRight.value || compareLeft.value === compareRight.value) return
  try {
    const response = await http.get(`/orchestration/apps/${editingWorkflowId.value}/versions/compare`, { params: { left: compareLeft.value, right: compareRight.value } })
    versionComparison.value = response.data
  } catch (error) { versionMessage.value = readError(error, '版本比较失败。'); versionTone.value = 'error' }
}

const createNode = (type, x = 120, y = 120) => normalizeNode({
  id: nextNodeId(type),
  type,
  name: nodeTypeLabel(type),
  x,
  y,
  config: {},
  inputSchema: {},
  outputSchema: {}
}, nodes.value.length)

const addNode = (type, x = 120, y = 120) => {
  const descriptor = nodeTypeMap.value[type]
  if (!descriptor || descriptor.status !== 'ACTIVE' || descriptor.executable === false || !supportsGraphType(descriptor, graphType.value)) {
    setSubmitMessage('该节点当前没有可用的运行能力，或不适用于当前流程类型。', 'warning')
    return false
  }
  // 节点先进入可编辑草稿，保存并通过发布检查后才进入正式运行链路。
  const node = createNode(type, x, y)
  nodes.value.push(node)
  selectedNode.value = node
  return true
}

const startPaletteDrag = (event, type) => {
  paletteType.value = type
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('text/plain', type)
}

const dropPaletteNode = (event) => {
  const type = event.dataTransfer.getData('text/plain') || paletteType.value
  if (!type) return
  const rect = event.currentTarget.getBoundingClientRect()
  addNode(type, Math.max(18, (event.clientX - rect.left - viewport.x) / viewport.scale - 75), Math.max(18, (event.clientY - rect.top - viewport.y) / viewport.scale - 25))
  paletteType.value = ''
}

const startNodeDrag = (event, node) => {
  // 选中和拖动共用 pointer 事件，按下节点时即可打开配置浮层。
  if (event.button !== 0 || event.target.closest('button')) return
  selectedNode.value = node
  const rect = event.currentTarget.getBoundingClientRect()
  canvasInteraction.mode = 'node'
  canvasInteraction.node = node
  canvasInteraction.offsetX = (event.clientX - rect.left) / viewport.scale
  canvasInteraction.offsetY = (event.clientY - rect.top) / viewport.scale
  canvasInteraction.pointerId = event.pointerId
  canvasInteraction.captureTarget = event.currentTarget
  event.currentTarget.setPointerCapture(event.pointerId)
  event.preventDefault()
}

const moveNode = (event) => {
  if (canvasInteraction.mode !== 'node' || !canvasInteraction.node) return
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  canvasInteraction.node.x = Math.max(12, (event.clientX - rect.left - viewport.x) / viewport.scale - canvasInteraction.offsetX)
  canvasInteraction.node.y = Math.max(12, (event.clientY - rect.top - viewport.y) / viewport.scale - canvasInteraction.offsetY)
}

const startCanvasPan = (event) => {
  if (event.button !== 0 || event.target.closest('.canvas-node') || event.target.closest('.canvas-controls')) return
  canvasInteraction.mode = 'pan'
  canvasInteraction.pointerId = event.pointerId
  canvasInteraction.pointerX = event.clientX
  canvasInteraction.pointerY = event.clientY
  canvasInteraction.captureTarget = event.currentTarget
  event.currentTarget.setPointerCapture(event.pointerId)
}

const moveCanvasPan = (event) => {
  if (canvasInteraction.mode === 'node') return moveNode(event)
  if (canvasInteraction.mode === 'connect') {
    const rect = canvasRef.value?.getBoundingClientRect()
    if (rect) {
      canvasInteraction.pointerX = (event.clientX - rect.left - viewport.x) / viewport.scale
      canvasInteraction.pointerY = (event.clientY - rect.top - viewport.y) / viewport.scale
    }
    return
  }
  if (canvasInteraction.mode !== 'pan') return
  viewport.x += event.clientX - canvasInteraction.pointerX
  viewport.y += event.clientY - canvasInteraction.pointerY
  canvasInteraction.pointerX = event.clientX
  canvasInteraction.pointerY = event.clientY
}

const stopCanvasInteraction = () => {
  if (canvasInteraction.pointerId !== null && canvasInteraction.captureTarget?.hasPointerCapture?.(canvasInteraction.pointerId)) {
    canvasInteraction.captureTarget.releasePointerCapture(canvasInteraction.pointerId)
  }
  canvasInteraction.mode = 'idle'
  canvasInteraction.node = null
  canvasInteraction.sourceNode = null
  canvasInteraction.pointerId = null
  canvasInteraction.captureTarget = null
}

const startConnection = (event, node) => {
  if (event.button !== 0 || node.type === 'END') return
  canvasInteraction.mode = 'connect'
  canvasInteraction.sourceNode = node
  canvasInteraction.pointerId = event.pointerId
  canvasInteraction.captureTarget = canvasRef.value
  canvasRef.value?.setPointerCapture(event.pointerId)
  const rect = canvasRef.value?.getBoundingClientRect()
  if (rect) {
    canvasInteraction.pointerX = (event.clientX - rect.left - viewport.x) / viewport.scale
    canvasInteraction.pointerY = (event.clientY - rect.top - viewport.y) / viewport.scale
  }
  event.preventDefault()
}

const finishConnection = (event, node) => {
  if (canvasInteraction.mode !== 'connect' || !canvasInteraction.sourceNode || node.type === 'START') return stopCanvasInteraction()
  addConnectionBetween(canvasInteraction.sourceNode.id, node.id)
  stopCanvasInteraction()
}

const addConnectionBetween = (source, target) => {
  if (!source || !target || source === target) return
  if (edges.value.some(edge => edge.source === source && edge.target === target)) return
  const sourceNode = nodes.value.find(node => node.id === source)
  if (sourceNode?.type === 'CONDITION') {
    const config = sourceNode.config || (sourceNode.config = {})
    if (!config.trueTarget) config.trueTarget = target
    else if (!config.falseTarget && config.trueTarget !== target) config.falseTarget = target
    else if (config.trueTarget !== target && config.falseTarget !== target) {
      setSubmitMessage(`${sourceNode.name} 只能连接“满足”和“不满足”两条分支。`, 'warning')
      return
    }
  }
  const edge = { edgeId: `edge-${source}-${target}`, source, sourcePort: 'default', target, targetPort: 'default' }
  edge.sourcePort = sourcePortForEdge(edge)
  edges.value.push(edge)
}

const zoomCanvas = (event) => changeZoom(event.deltaY > 0 ? -0.1 : 0.1, event)
const changeZoom = (delta, event = null) => {
  const nextScale = Math.min(1.6, Math.max(0.5, Number((viewport.scale + delta).toFixed(2))))
  if (event && canvasRef.value) {
    const rect = canvasRef.value.getBoundingClientRect()
    const cursorX = event.clientX - rect.left
    const cursorY = event.clientY - rect.top
    viewport.x = cursorX - (cursorX - viewport.x) * nextScale / viewport.scale
    viewport.y = cursorY - (cursorY - viewport.y) * nextScale / viewport.scale
  }
  viewport.scale = nextScale
}

const resetViewport = () => {
  viewport.x = 0
  viewport.y = 0
  viewport.scale = 1
}

const fitViewport = () => {
  const canvas = canvasRef.value
  if (!canvas || nodes.value.length === 0) return resetViewport()
  const bounds = nodes.value.reduce((result, node) => ({
    minX: Math.min(result.minX, node.x),
    minY: Math.min(result.minY, node.y),
    maxX: Math.max(result.maxX, node.x + nodeWidth),
    maxY: Math.max(result.maxY, node.y + 76)
  }), { minX: Infinity, minY: Infinity, maxX: -Infinity, maxY: -Infinity })
  const padding = 72
  const scale = Math.min(1.2, Math.max(0.5, Math.min(
    (canvas.clientWidth - padding * 2) / Math.max(1, bounds.maxX - bounds.minX),
    (canvas.clientHeight - padding * 2) / Math.max(1, bounds.maxY - bounds.minY)
  )))
  viewport.scale = Number(scale.toFixed(2))
  viewport.x = (canvas.clientWidth - (bounds.maxX - bounds.minX) * viewport.scale) / 2 - bounds.minX * viewport.scale
  viewport.y = (canvas.clientHeight - (bounds.maxY - bounds.minY) * viewport.scale) / 2 - bounds.minY * viewport.scale
}

const autoLayout = () => {
  if (nodes.value.length === 0) return
  const incoming = new Map(nodes.value.map(node => [node.id, 0]))
  const outgoing = new Map(nodes.value.map(node => [node.id, []]))
  edges.value.forEach(edge => {
    if (!incoming.has(edge.target) || !outgoing.has(edge.source)) return
    incoming.set(edge.target, incoming.get(edge.target) + 1)
    outgoing.get(edge.source).push(edge.target)
  })
  const queue = nodes.value.filter(node => incoming.get(node.id) === 0).map(node => ({ id: node.id, level: 0 }))
  const levels = new Map()
  while (queue.length) {
    const current = queue.shift()
    levels.set(current.id, Math.max(levels.get(current.id) || 0, current.level))
    outgoing.get(current.id).forEach(target => {
      incoming.set(target, incoming.get(target) - 1)
      if (incoming.get(target) === 0) queue.push({ id: target, level: current.level + 1 })
    })
  }
  nodes.value.forEach(node => { if (!levels.has(node.id)) levels.set(node.id, Math.max(0, levels.size)) })
  const rows = new Map()
  nodes.value.forEach(node => {
    const level = levels.get(node.id)
    const row = rows.get(level) || 0
    node.x = 64 + level * 236
    node.y = 64 + row * 116
    rows.set(level, row + 1)
  })
  window.requestAnimationFrame(fitViewport)
}

const nodeProblems = (node) => {
  const problems = []
  const config = buildSerializedConfig(node)
  const missing = schemaFieldsForType(node.type)
    .filter(field => field.required && isFieldVisible({ ...node, config }, field) && isBlank(config[field.name]))
  if (missing.length) problems.push(`缺少${missing.map(field => field.label || field.name).join('、')}`)
  if (node.type !== 'START' && !edges.value.some(edge => edge.target === node.id)) problems.push('没有上游连线')
  if (node.type !== 'END' && !edges.value.some(edge => edge.source === node.id)) problems.push('没有下游连线')
  const mappingError = invalidMappingForNode(node, config)
  if (mappingError) problems.push(mappingError)
  return problems
}
const nodeProblemCount = node => nodeProblems(node).length

// 保存前校验变量、节点和资源引用，避免草稿进入运行时后才暴露配置错误。
const invalidMappingForNode = (node, config = buildSerializedConfig(node)) => {
  const descriptor = nodeTypeMap.value[node.type]
  for (const field of descriptor?.configSchema?.fields || []) {
    if (!isFieldVisible({ ...node, config }, field) || isBlank(config[field.name])) continue
    if (['variable-select', 'variable-reference'].includes(field.widget)) {
      const allowed = [WORKFLOW_VARIABLES.INPUT, WORKFLOW_VARIABLES.USER_MESSAGE, WORKFLOW_VARIABLES.QUERY,
        ...nodes.value.filter(item => item.id !== node.id).map(item => nodeOutputReference(item.id))]
      if (!allowed.includes(String(config[field.name]))) return `${node.name} 的 ${field.label || field.name} 不是有效变量`
    }
    if (field.widget === 'node-select' && !nodes.value.some(item => String(item.id) === String(config[field.name]))) {
      return `${node.name} 的 ${field.label || field.name} 引用了不存在的节点`
    }
    if (field.widget === 'resource-select') {
      // 文档存在性和索引状态由发布检查统一校验，避免设计器仅凭当前搜索页误判已选文档。
      if (field.resourceType === 'KNOWLEDGE_DOCUMENT') continue
      const resourceList = field.resourceType === 'MODEL' ? models.value
        : field.resourceType === 'AGENT' ? agents.value
          : field.resourceType === 'ORGANIZATION_UNIT' ? organizationUnits.value : connectors.value
      const resourceId = String(config[field.name])
      const exists = resourceList.some(item => String(item.id) === resourceId || item.modelKey === resourceId)
      if (resourceList && resourceList.length > 0  && !exists) return `${node.name} 的 ${field.label || field.name} 引用了不可用资源`
    }
  }
  return ''
}

const focusNode = (nodeId) => {
  const node = nodes.value.find(item => item.id === nodeId)
  const canvas = canvasRef.value
  if (!node || !canvas) return
  selectedNode.value = node
  viewport.x = canvas.clientWidth / 2 - (node.x + nodeWidth / 2) * viewport.scale
  viewport.y = canvas.clientHeight / 2 - (node.y + nodePortY) * viewport.scale
}
const focusIssue = issue => { if (issue?.nodeId) focusNode(issue.nodeId) }

const removeNode = async (id) => {
  const node = nodes.value.find(item => item.id === id)
  if (node && !await confirmAction({ title: '删除流程节点', message: `删除“${node.name}”后，与它相连的连线也会一起删除。` })) return
  nodes.value = nodes.value.filter(node => node.id !== id)
  edges.value = edges.value.filter(edge => edge.source !== id && edge.target !== id)
  if (selectedNode.value?.id === id) selectedNode.value = null
}

const addConnection = () => {
  addConnectionBetween(connection.source, connection.target)
  connection.source = ''
  connection.target = ''
}

const removeConnection = (index) => edges.value.splice(index, 1)
const applySopTemplate = async (template) => {
  if (nodes.value.length > 0 && !await confirmAction({ title: '替换当前画布', message: '应用模板会替换当前未保存的节点和连线。' })) return
  graphType.value = template.graphType
  nodes.value = template.nodes.map((node, index) => normalizeNode({ ...node, config: { ...(node.config || {}) } }, index))
  edges.value = template.edges.map(([source, target, sourcePort], index) => ({ edgeId: `edge-${index + 1}`, source, target, sourcePort: sourcePort || 'default', targetPort: 'default' }))
  selectedNode.value = nodes.value.find(node => node.type !== 'START') || null
  checkLoaded.value = false
  checkIssues.value = []
  setSubmitMessage(`已应用“${template.name}”模板，请补充节点配置后保存草稿。`, 'info')
  nextTick(() => fitViewport())
}
const resetCanvas = async () => {
  if (nodes.value.length > 0 && !await confirmAction({ title: '清空流程画布', message: '当前未保存的节点和连线都会被删除。' })) return
  nodes.value = []
  edges.value = []
  selectedNode.value = null
  checkLoaded.value = false
  checkIssues.value = []
  checkMessage.value = ''
}

const validateWorkflow = () => {
  if (nodeTypes.value.length === 0) return '节点目录尚未加载完成，请稍后再试。'
  const startNodes = nodes.value.filter(node => node.type === 'START')
  const endNodes = nodes.value.filter(node => node.type === 'END')
  if (startNodes.length !== 1 || endNodes.length !== 1) return '工作流必须且只能包含一个开始节点和一个结束节点。'
  if (nodes.value.some(node => node.type !== 'START' && !edges.value.some(edge => edge.target === node.id))) return '存在没有入边的节点，请从开始节点连接到所有业务节点。'
  if (nodes.value.some(node => node.type !== 'END' && !edges.value.some(edge => edge.source === node.id))) return '存在没有出边的节点，请确保流程最终可以到达结束节点。'
  for (const node of nodes.value) {
    const descriptor = nodeTypeMap.value[node.type]
    if (!descriptor || descriptor.status !== 'ACTIVE' || descriptor.executable === false || !supportsGraphType(descriptor, graphType.value)) {
      return `${node.name} 节点当前不可用于此流程，请替换为可用节点。`
    }
    const config = buildSerializedConfig(node)
    const missingField = schemaFieldsForType(node.type)
      .filter(field => field.required && isFieldVisible({ ...node, config }, field))
      .find(field => isBlank(config[field.name]))
    if (missingField) return `${node.name} 节点必须配置${missingField.label}。`
    const mappingError = invalidMappingForNode(node, config)
    if (mappingError) return mappingError
    if (node.type === 'LLM' && config.backupModelId && config.backupModelId === config.modelId) {
      return `${node.name} 节点的备用模型不能与主模型相同。`
    }
    if (node.type === 'CONDITION') {
      if (config.operator !== 'NOT_EMPTY' && isBlank(config.value)) return `${node.name} 节点必须填写匹配文本。`
      if (!config.trueTarget || !config.falseTarget || config.trueTarget === config.falseTarget) return `${node.name} 节点必须配置两个不同的后继节点。`
      const branchPorts = edges.value.filter(edge => edge.source === node.id).map(sourcePortForEdge)
      if (branchPorts.filter(port => port === 'true').length !== 1 || branchPorts.filter(port => port === 'false').length !== 1) return `${node.name} 节点必须分别连接“满足”和“不满足”两条分支。`
    }
  }
  return ''
}

const loadPublishChecks = async (appId, quiet = false) => {
  if (!appId) {
    checkLoaded.value = false
    checkIssues.value = []
    setCheckMessage('请先保存草稿，再查看发布检查清单。', 'warning')
    return []
  }
  checkLoading.value = true
  if (!quiet) setCheckMessage('正在读取发布检查清单...', 'info')
  try {
    const response = await http.get(`/orchestration/apps/${appId}/checks`)
    checkIssues.value = response.data || []
    checkLoaded.value = true
    if (checkIssues.value.length === 0) setCheckMessage('当前草稿没有阻断项，可以发布正式版本。', 'success')
    else setCheckMessage(`发现 ${checkIssues.value.length} 项阻断问题，请先修正后再发布。`, 'warning')
    return checkIssues.value
  } catch (error) {
    checkLoaded.value = false
    checkIssues.value = []
    setCheckMessage(readError(error, '发布检查读取失败。'), 'error')
    return []
  } finally {
    checkLoading.value = false
  }
}

const runPublishChecks = async () => {
  await loadPublishChecks(editingWorkflowId.value)
}

const submitWorkflow = async () => {
  if (!form.workflowName) {
    setSubmitMessage('请先填写工作流编码和名称。', 'warning')
    return
  }
  const validationMessage = validateWorkflow()
  if (validationMessage) {
    setSubmitMessage(validationMessage, 'warning')
    return
  }
  setSubmitMessage('保存中...', 'info')
  try {
    const request = {
      appId: editingWorkflowId.value,
      appCode: editingWorkflowId.value ? form.workflowCode : null,
      appName: form.workflowName,
      graphType: graphType.value,
      graphJson: graphJson.value,
      expectedRevisionNo: editingRevisionNo.value
    }
    const response = await http.post('/orchestration/drafts', request)
    editingWorkflowId.value = response.data?.appId || editingWorkflowId.value
    editingRevisionNo.value = response.data?.revisionNo || editingRevisionNo.value
    form.workflowCode = response.data?.appCode || form.workflowCode
    setSubmitMessage(`工作流草稿 r${editingRevisionNo.value} 已保存。`, 'success')
    await loadWorkflows()
    await loadPublishChecks(editingWorkflowId.value, true)
  } catch (error) {
    const message = readError(error, '工作流保存失败。')
    setSubmitMessage(message, message.includes('重新加载最新草稿') ? 'warning' : 'error')
  }
}

const publishDraft = async () => {
  if (!editingWorkflowId.value) {
    setSubmitMessage('请先保存草稿，再发布上线。', 'warning')
    return
  }
  const issues = await loadPublishChecks(editingWorkflowId.value, true)
  if (issues.length > 0) {
    setSubmitMessage('发布检查未通过，请先根据清单修正问题。', 'warning')
    return
  }
  setSubmitMessage('发布中...', 'info')
  try {
    const response = await http.post(`/orchestration/apps/${editingWorkflowId.value}/publish`, {
      environmentCode: PRODUCTION_ENV,
      expectedRevisionNo: editingRevisionNo.value
    })
    setSubmitMessage(`正式版本 v${response.data?.versionNo || ''} 已发布，${PRODUCTION_ENV} 环境入口已切换。`, 'success')
    await loadWorkflows()
    await loadVersions()
    await loadPublishChecks(editingWorkflowId.value, true)
  } catch (error) {
    setSubmitMessage(readError(error, '工作流发布失败。'), 'error')
  }
}

const editWorkflow = async (id) => {
  try {
    if (!sourcesLoaded.value) await loadSources()
    const response = await http.get(`/orchestration/apps/${id}/draft`)
    const detail = response.data
    const graph = JSON.parse(detail.graphJson || '{}')
    editingWorkflowId.value = detail.appId
    editingRevisionNo.value = detail.revisionNo
    form.workflowCode = detail.appCode || ''
    form.workflowName = detail.appName || ''
    graphType.value = graph.graphType || 'APPLICATION_WORKFLOW'
    graphInputSchema.value = graph.inputSchema || { type: 'object', properties: {} }
    graphOutputSchema.value = graph.outputSchema || { type: 'object', properties: {} }
    nodes.value = (graph.nodes || []).map((node, index) => normalizeNode(node, index))
    edges.value = (graph.edges || []).map((edge, index) => normalizeEdge(edge, index))
    selectedNode.value = null
    resetViewport()
    setSubmitMessage(`正在调试草稿 r${detail.revisionNo}。正式入口仍使用生产环境已发布版本。`, 'info')
    await loadPublishChecks(detail.appId, true)
    await loadVersions()
  } catch (error) {
    setSubmitMessage(readError(error, '工作流读取失败。'), 'error')
  }
}

const readDebugInput = () => {
  try { return JSON.parse(debugInput.value || '{}') } catch { throw new Error('测试输入不是有效的 JSON，请检查括号、引号和逗号。') }
}

const debugSelectedNode = async () => {
  if (!editingWorkflowId.value || !selectedNode.value) return
  debugLoading.value = true
  debugMessage.value = ''
  try {
    const response = await http.post('/runtime/runs/debug-node', {
      applicationId: Number(editingWorkflowId.value),
      nodeType: selectedNode.value.type,
      config: buildSerializedConfig(selectedNode.value),
      input: readDebugInput()
    })
    debugOutput.value = JSON.stringify(response.data, null, 2)
    debugMessage.value = '节点调试完成。'
    debugTone.value = 'success'
  } catch (error) {
    debugMessage.value = readError(error, '节点调试失败。')
    debugTone.value = 'error'
  } finally { debugLoading.value = false }
}

const executePublished = async () => {
  if (!editingWorkflowId.value) return
  debugLoading.value = true
  debugMessage.value = ''
  try {
    const input = readDebugInput()
    const response = await submitRuntimeDraftTest({ applicationId: Number(editingWorkflowId.value), idempotencyKey: `studio-${Date.now()}`, input })
    if (!response.data?.runId) throw new Error('未能创建工作流执行实例。')
    const executionId = response.data.runId
    debugMessage.value = `执行已提交，正在跟踪实例 ${executionId.slice(0, 8)}。`
    debugTone.value = 'success'
    debugOutput.value = JSON.stringify(response.data, null, 2)
    // 编辑器不再维持单独的流式执行协议，统一使用持久化执行事件。
    const stop = subscribeExecutionEvents(executionId, {
      onEvent: async () => {
        const detail = await fetchRuntimeRun(executionId)
        debugOutput.value = JSON.stringify(detail.data?.run || detail.data, null, 2)
        const status = (detail.data?.run || detail.data).status
        if (['SUCCEEDED', 'FAILED', 'CANCELLED', 'REJECTED', 'WAITING_APPROVAL'].includes(status)) {
          debugMessage.value = status === 'WAITING_APPROVAL' ? '执行正在等待人工审批，请前往执行中心处理。' : `工作流执行${status === 'SUCCEEDED' ? '成功' : '结束'}。`
          debugTone.value = status === 'SUCCEEDED' ? 'success' : status === 'WAITING_APPROVAL' ? 'warning' : 'error'
          stop()
        }
      },
      onError: () => { debugMessage.value = '实时跟踪连接暂时中断，执行记录仍可在运行中心查看。' }
    })
  } catch (error) {
    debugMessage.value = readError(error, '工作流运行失败。')
    debugTone.value = 'error'
  } finally { debugLoading.value = false }
}

const initialize = async () => {
  await loadSources()
  await loadWorkflows()
  const applicationId = String(route.params.id || route.query.appId || '')
  if (/^\d+$/.test(applicationId)) {
    // 从应用详情进入时自动打开该应用的主工作流，避免用户重新寻找流程对象。
    await editWorkflow(Number(applicationId))
  }
}

onMounted(() => { initialize() })
onUnmounted(stopCanvasInteraction)
</script>

<style scoped>
.system-id-note strong { display: block; color: var(--accent); font-size: 12px; }
.editor-toolbar { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: end; gap: 18px; padding-bottom: 14px; border-bottom: 1px solid var(--line); }
.editor-feedback { display: flex; align-items: center; gap: 7px; min-height: 30px; margin-top: 10px; padding: 6px 10px; border: 1px solid #cfe2f4; border-radius: 6px; background: #f4f9fd; color: #2269a7; font-size: 11px; }
.feedback-dot { width: 6px; height: 6px; flex: 0 0 auto; border-radius: 50%; background: currentColor; }
.editor-feedback.tone-success { border-color: #cce8d8; background: #f3fcf7; color: var(--success); }
.editor-feedback.tone-warning { border-color: #f1d6a5; background: #fff8ea; color: #9c6a13; }
.editor-feedback.tone-error { border-color: #f0c6cb; background: #fff4f5; color: var(--danger); }
.workflow-tabs { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin: 18px 0 0; padding: 5px; border: 1px solid var(--line); border-radius: 8px; background: var(--panel-muted); }
.workflow-tab { display: grid; grid-template-columns: 28px minmax(0, 1fr); gap: 2px 8px; align-items: center; padding: 10px 12px; border: 1px solid transparent; border-radius: 6px; background: transparent; color: var(--muted); cursor: pointer; text-align: left; }
.workflow-tab > span { grid-row: span 2; display: grid; place-items: center; width: 25px; height: 25px; border-radius: 6px; background: color-mix(in srgb, var(--accent) 10%, transparent); color: var(--accent); font-size: 10px; font-weight: 700; }
.workflow-tab strong { color: var(--text); font-size: 12px; }
.workflow-tab small { color: var(--muted); font-size: 10px; }
.workflow-tab:hover { border-color: color-mix(in srgb, var(--accent) 30%, transparent); background: var(--panel); }
.version-history { margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--line); }
.version-history-table { margin-top: 14px; }
.version-history-table td:first-child { display: grid; gap: 4px; }
.version-compare-controls { display: grid; grid-template-columns: minmax(160px, 1fr) minmax(160px, 1fr) auto; gap: 10px; align-items: end; margin-top: 16px; }
.version-compare-controls label { display: grid; gap: 6px; color: var(--muted); font-size: 11px; }
.version-diff { display: grid; gap: 7px; margin-top: 12px; padding: 14px 0; border-top: 1px solid var(--line); color: var(--muted); font-size: 12px; }
.version-diff strong { color: var(--text); }
.workflow-tab.active { border-color: color-mix(in srgb, var(--accent) 40%, transparent); background: var(--panel); box-shadow: 0 3px 10px color-mix(in srgb, var(--accent) 10%, transparent); }
.workflow-tab.active > span { background: var(--accent); color: #fff; }
.workflow-tab-panel { min-width: 0; }
.release-tab-panel { padding-top: 2px; }
.version-tab-panel > .section-card { margin-top: 18px; }
.workflow-meta, .editor-actions { display: flex; align-items: center; gap: 8px; }
.workflow-meta { min-width: 0; align-items: flex-start; }
.toolbar-field { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 4px; color: #50677c; font-size: 11px; font-weight: 650; }
.toolbar-field .text-input { margin-top: 0; }
.toolbar-field small { color: #91a0ae; font-size: 10px; font-weight: 400; line-height: 1.35; }
.editor-actions { flex-wrap: wrap; justify-content: flex-end; }
.editor-message, .check-message { font-size: 12px; }
.sop-templates { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; padding: 10px 0 2px; }
.template-label { margin-right: 2px; color: var(--muted); font-size: 12px; font-weight: 700; }
.template-btn { display: inline-flex; flex-direction: column; align-items: flex-start; gap: 2px; min-width: 132px; padding: 8px 10px; border: 1px solid #d6e3ed; border-radius: 6px; background: #fff; color: #304b63; cursor: pointer; text-align: left; }
.template-btn:hover { border-color: #7eafd5; background: #f5faff; }
.template-btn strong { font-size: 12px; }.template-btn small { color: #71869a; font-size: 10px; }
.editor-message.tone-success, .check-message.tone-success { color: var(--success); }
.editor-message.tone-info, .check-message.tone-info { color: #2269a7; }
.editor-message.tone-warning, .check-message.tone-warning { color: #b76a00; }
.editor-message.tone-error, .check-message.tone-error { color: var(--danger); }
.editor-layout { position: relative; display: grid; grid-template-columns: 184px minmax(0, 1fr); height: clamp(620px, calc(100vh - 260px), 840px); min-height: 620px; margin: 18px -20px 0; border: 1px solid #e0e8f0; border-radius: 9px; overflow: hidden; background: #f8fafc; }
.workflow-editor { padding: 16px; }
.node-palette, .node-config { padding: 14px; background: #f8fafc; }
.node-palette { border-right: 1px solid var(--line); overflow-y: auto; scrollbar-gutter: stable; }
.node-config { position: absolute; inset: 0 0 0 auto; z-index: 8; display: block; width: min(330px, 42%); border-left: 1px solid #cbdbea; overflow: auto; background: #f8fafc; box-shadow: -10px 0 26px rgba(34,72,111,.12); }
.config-panel-head { position: sticky; top: -14px; z-index: 4; display: flex; align-items: center; justify-content: space-between; gap: 10px; margin: -14px -14px 0; padding: 14px; border-bottom: 1px solid #e1eaf2; background: rgba(248,250,252,.97); }
.config-close { display: inline-grid; place-items: center; width: 28px; height: 28px; border: 1px solid #d5e1ed; border-radius: 6px; background: #fff; color: #6f8295; font-size: 20px; line-height: 1; cursor: pointer; }
.config-close:hover { border-color: #b7d3ed; background: #eef6ff; color: #2269a7; }
.palette-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.palette-count { min-width: 22px; padding: 3px 6px; border-radius: 999px; background: #eaf3ff; color: #287fd3; font-size: 11px; font-weight: 700; text-align: center; }
.palette-search { width: 100%; margin-top: 10px; }
.palette-tabs { display: flex; flex-wrap: wrap; gap: 4px; margin: 10px 0 4px; }
.palette-tab { padding: 4px 7px; border: 1px solid transparent; border-radius: 999px; background: transparent; color: var(--muted); font-size: 11px; cursor: pointer; }
.palette-tab:hover { color: var(--accent); background: #eef6ff; }
.palette-tab.active { border-color: #c7def5; background: #eaf3ff; color: #2269a7; font-weight: 700; }
.panel-title { color: var(--text); font-size: 13px; font-weight: 700; }
.panel-hint, .config-empty, .entry-hint { color: var(--muted); font-size: 11px; line-height: 1.6; }
.palette-node { display: flex; align-items: center; gap: 9px; width: 100%; min-height: 42px; margin-top: 2px; padding: 7px 8px; border: 0; border-bottom: 1px solid #e8eef4; border-radius: 0; background: transparent; color: var(--text); text-align: left; cursor: grab; }
.palette-node:hover { background: #eef6ff; color: #145d9c; }
.palette-node:focus-visible, .palette-tab:focus-visible, .palette-mode-switch button:focus-visible, .workflow-tab:focus-visible, .canvas-control:focus-visible, .connection-port:focus-visible, .node-remove:focus-visible, .config-close:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
.capability-note { margin: 8px 0; padding: 7px 8px; border-left: 2px solid #d49b32; background: #fff9ed; color: #805d1c; font-size: 10px; line-height: 1.5; }
.palette-node strong, .palette-node small, .selected-node-title small { display: block; }
.palette-mode-switch { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; margin: 8px 0; padding: 3px; border: 1px solid #dce7f1; border-radius: 6px; background: #f7fafc; }
.palette-mode-switch button { padding: 6px 4px; border: 0; border-radius: 4px; background: transparent; color: var(--muted); font-size: 11px; cursor: pointer; }
.palette-mode-switch button.active { background: var(--accent); color: #fff; }
.palette-node strong { display: block; overflow: hidden; font-size: 12px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.palette-node small { display: none; }
.node-symbol { display: inline-grid; flex: 0 0 auto; width: 26px; height: 26px; place-items: center; border-radius: 6px; font-size: 13px; font-weight: 700; }
.node-palette > .palette-node { width: 100%; display: flex; vertical-align: initial; margin-right: 0; }
.node-start { background: #e8f7f0; color: #168258; }
.node-rag { background: #eaf3ff; color: #1677ff; }
.node-human { background: #fff4df; color: #a46d13; }
.node-condition { background: #fff0f3; color: #c2415a; }
.node-llm { background: #f1eaff; color: #7953c6; }
.node-end { background: #f0f2f5; color: #637286; }
.node-agent { background: #ecf7f1; color: #1b7a57; }
.node-parallel, .node-join, .node-loop, .node-transform { background: #eef4fb; color: #335f8a; }
.connection-title { margin-top: 18px; padding-top: 14px; border-top: 1px solid #dfe8f0; }
.node-palette select { margin-top: 8px; }
.branch-preview { margin: -3px 0 10px; padding: 6px 8px; border-radius: 5px; font-size: 11px; font-weight: 600; }
.branch-true { background: #eef9f3; color: #18794e; }
.branch-false { background: #fff3f0; color: #a54032; }
.secondary-btn { width: 100%; margin-top: 8px; padding: 8px; border: 1px solid #bfd7ef; border-radius: 6px; background: #eef6ff; color: #2269a7; font-size: 12px; cursor: pointer; }
.secondary-btn:disabled, .primary-btn:disabled { cursor: not-allowed; opacity: .6; }
.toolbar-btn { width: auto; margin-top: 0; }
.publish-btn { background: #0b7d68; }
.edge-list { display: flex; flex-direction: column; gap: 5px; margin-top: 12px; }
.edge-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 8px; border-radius: 4px; background: #fff; color: var(--muted); font-family: Consolas, monospace; font-size: 10px; }
.icon-text-btn { border: 0; background: none; color: var(--danger); cursor: pointer; font-size: 16px; }
.workflow-canvas { position: relative; min-height: 620px; overflow: hidden; background: #fff; cursor: grab; touch-action: none; isolation: isolate; }
.workflow-canvas.is-panning { cursor: grabbing; }
.workflow-canvas.is-connecting { cursor: crosshair; }
.edge-layer { position: absolute; inset: 0; z-index: 0; width: 100%; height: 100%; pointer-events: none; overflow: visible; }
.canvas-stage { position: absolute; inset: 0; transform-origin: 0 0; will-change: transform; }
.edge-layer line { stroke: #6f9bc5; stroke-width: 2; stroke-dasharray: 5 4; }
.edge-layer .connection-preview { stroke: #287fd3; stroke-dasharray: 4 3; }
.canvas-grid { position: absolute; inset: 0; background-image: radial-gradient(#dbe6f0 1px, transparent 1px); background-size: 18px 18px; opacity: .7; }
.canvas-empty { position: absolute; inset: 0; display: grid; place-items: center; color: var(--muted); font-size: 13px; }
.canvas-empty::before { content: '+'; display: grid; width: 28px; height: 28px; margin: 0 auto 8px; place-items: center; border: 1px dashed #9bb9d4; border-radius: 7px; color: var(--accent); font-size: 20px; }
.canvas-node { position: absolute; z-index: 1; width: 164px; min-height: 68px; padding: 10px 11px; border: 1px solid #cbd9e8; border-radius: 9px; background: #fff; box-shadow: 0 6px 16px rgba(34, 72, 111, .1); cursor: grab; user-select: none; transition: border-color .15s, box-shadow .15s; }
.canvas-node:active { cursor: grabbing; }
.canvas-node.selected { border-color: var(--accent); box-shadow: 0 0 0 3px #eaf3ff, 0 6px 16px rgba(22, 119, 255, .12); }
.canvas-node-head { display: flex; align-items: center; gap: 7px; color: var(--text); font-size: 12px; }
.canvas-node.has-problem { border-color: #d9a23b; }
.node-problem-count { position: absolute; top: -8px; right: -8px; display: grid; place-items: center; width: 20px; height: 20px; border: 2px solid #fff; border-radius: 50%; background: #b76a00; color: #fff; font-size: 10px; font-weight: 700; }
.canvas-node-head strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.canvas-node-head .node-symbol { width: 22px; height: 22px; font-size: 11px; }
.node-remove { margin-left: auto; border: 0; background: transparent; color: #9aaabc; cursor: pointer; font-size: 16px; }
.node-remove:hover { color: var(--danger); }
.canvas-node-type { margin: 7px 0 0 29px; color: var(--muted); font-size: 10px; letter-spacing: .05em; }
.connection-port { position: absolute; z-index: 2; width: 12px; height: 12px; padding: 0; border: 2px solid #fff; border-radius: 50%; background: #6f9bc5; box-shadow: 0 0 0 1px #6f9bc5; cursor: crosshair; }
.connection-port:hover { background: #287fd3; box-shadow: 0 0 0 3px #dcecff; }
.input-port { top: 28px; left: -7px; }
.output-port { top: 28px; right: -7px; }
.canvas-controls { position: absolute; right: 12px; bottom: 12px; z-index: 3; display: flex; align-items: center; gap: 4px; padding: 4px; border: 1px solid #d5e1ed; border-radius: 6px; background: rgba(255,255,255,.94); color: var(--muted); font-size: 11px; box-shadow: 0 3px 10px rgba(34,72,111,.08); }
.canvas-control { min-width: 26px; height: 26px; padding: 0 6px; border: 0; border-radius: 4px; background: transparent; color: var(--text); cursor: pointer; }
.canvas-control:hover { background: #eef6ff; color: var(--accent); }
.reset-control { border-left: 1px solid var(--line); }
.selected-node-title { display: flex; align-items: center; gap: 9px; padding: 10px; margin: 12px 0 14px; border: 1px solid #dce7f1; border-radius: 8px; background: #fff; }
.node-contract-summary { display: grid; gap: 7px; margin: 0 0 14px; padding: 10px; border: 1px solid #dce7f1; border-radius: 8px; background: #f8fbfd; }
.node-contract-summary div { display: grid; gap: 3px; min-width: 0; }
.node-contract-summary span { color: var(--muted); font-size: 10px; }
.node-contract-summary strong { overflow: hidden; color: var(--text); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.node-validation-errors { display: grid; gap: 4px; margin: 0 0 14px; padding: 9px 10px; border: 1px solid #f0c6cb; border-radius: 7px; background: #fff4f5; color: #a33746; font-size: 11px; line-height: 1.5; }
.node-validation-errors strong { color: #8f2636; }
.selected-node-title strong { color: var(--text); font-size: 13px; }
.selected-node-title small { margin-top: 3px; color: var(--muted); font-size: 10px; }
.publish-panel, .debug-panel { margin-top: 18px; padding: 16px; border: 1px solid #dfe8f1; border-radius: 10px; background: #fff; }
.debug-modes { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.debug-mode { padding: 5px 8px; border: 1px solid #e2eaf0; border-radius: 5px; background: #f7fafc; color: #8b9baa; font-size: 10px; }
.debug-mode.active { border-color: #b9d8ed; background: #edf7fd; color: #246b98; }
.debug-mode.ready { border-color: #bfe6d2; background: #f0fbf5; color: #26764e; }
.debug-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin-top: 14px; }
.debug-label { display: block; margin-bottom: 6px; color: var(--text); font-size: 12px; font-weight: 600; }
.debug-textarea { min-height: 150px; font-family: Consolas, monospace; font-size: 11px; }
.debug-output { min-height: 150px; max-height: 260px; margin: 0; overflow: auto; padding: 11px; border: 1px solid #dce7f1; border-radius: 6px; background: #172b40; color: #d4e1ed; font: 11px/1.6 Consolas, monospace; white-space: pre-wrap; word-break: break-word; }
.publish-head { display: flex; justify-content: space-between; gap: 12px; }
.check-summary { display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }
.check-pill { display: inline-flex; align-items: center; padding: 4px 9px; border-radius: 999px; border: 1px solid #d5e1ed; background: #f7fafc; color: #53708a; font-size: 11px; font-weight: 600; }
.check-pill.pass { border-color: #bfe6d2; background: #f0fbf5; color: #137a4d; }
.check-pill.warning { border-color: #f1d6a5; background: #fff8ea; color: #9c6a13; }
.check-pill.blocking { border-color: #f0c6cb; background: #fff4f5; color: #b33a49; }
.check-pill.neutral { border-color: #d5e1ed; background: #f7fafc; color: #53708a; }
.check-pass { padding: 14px 16px; border: 1px solid #cce8d8; border-radius: 8px; background: #f3fcf7; color: #2a6f50; }
.check-pass strong { display: block; color: #14563b; }
.check-pass p { margin: 4px 0 0; font-size: 12px; }
.check-list { display: flex; flex-direction: column; gap: 10px; }
.check-item { padding: 12px 14px; border: 1px solid #f1d4d8; border-radius: 8px; background: #fff7f8; }
.check-item-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.check-item.actionable { cursor: pointer; }
.check-item.actionable:hover { border-color: #b8d4ec; background: #f5faff; }
.check-level { display: inline-flex; padding: 2px 7px; border-radius: 999px; background: #c0394b; color: #fff; font-size: 10px; font-weight: 700; }
.check-item code { padding: 2px 6px; border-radius: 4px; background: #f4ecee; color: #99505b; font-size: 10px; }
.check-meta, .check-suggestion { margin: 5px 0 0; color: #6d5960; font-size: 11px; line-height: 1.6; }
.graph-preview { margin-top: 14px; color: var(--muted); font-size: 12px; }
.graph-preview summary { cursor: pointer; }
.graph-preview pre { max-height: 180px; overflow: auto; padding: 12px; border: 1px solid var(--line); border-radius: 6px; background: #f8fafc; color: #526b84; font-size: 11px; }
.status-badge { display: inline-flex; padding: 3px 8px; border: 1px solid; border-radius: 4px; font-size: 11px; font-weight: 600; }
.status-draft { border-color: #cfe2f8; background: #eef6ff; color: #2670b8; }
.status-published { border-color: #bfe6d2; background: #f0fbf5; color: var(--success); }
.status-disabled { border-color: #f4c8cc; background: #fff4f5; color: var(--danger); }
.version-cell { min-width: 180px; }
.version-cell .pill { margin-right: 6px; }
.version-pill { background: #f0fbf5; color: #14764b; }
.version-placeholder { color: var(--muted); font-size: 11px; }
.row-actions { white-space: nowrap; }
.link-btn { border: 0; background: transparent; color: var(--accent); cursor: pointer; font-size: 12px; }
.field-help { margin-top: 4px; color: var(--muted); font-size: 11px; line-height: 1.6; }
.node-editor-fields { display: flex; flex-direction: column; gap: 8px; }
.node-editor-fields label { margin-top: 4px; color: var(--text); font-size: 12px; font-weight: 600; }
.node-editor-fields .field-help { margin-top: -2px; }
.node-editor-fields .text-input, .node-editor-fields .textarea-input { width: 100%; box-sizing: border-box; }
.node-editor-fields textarea { min-height: 86px; resize: vertical; }
.node-editor-fields select[multiple] { min-height: 74px; padding-block: 6px; }
@media (max-width: 1180px) {
  .editor-layout { grid-template-columns: 176px minmax(420px, 1fr); }
  .node-config { width: min(330px, 48%); }
}
@media (max-width: 760px) {
  .editor-toolbar, .workflow-meta, .editor-actions, .publish-head { align-items: stretch; flex-direction: column; }
  .workflow-editor { padding: 12px; }
  .editor-actions > button, .editor-actions > a { width: 100%; box-sizing: border-box; text-align: center; }
  .workflow-tabs { grid-template-columns: 1fr; }
  .workflow-tab { grid-template-columns: 30px minmax(0, 1fr); }
  .sop-templates { align-items: stretch; flex-direction: column; }.template-btn { width: 100%; box-sizing: border-box; }
  .check-summary { justify-content: flex-start; }
  .editor-layout { display: block; height: auto; min-height: 0; margin: 12px -12px 0; border-right: 0; border-left: 0; border-radius: 0; }
  .node-palette { border: 0; border-bottom: 1px solid var(--line); }
  .node-config { position: relative; inset: auto; width: auto; max-height: none; border: 0; border-bottom: 1px solid var(--line); box-shadow: none; }
  .workflow-canvas { min-height: 420px; }
  .canvas-controls { right: 8px; bottom: 8px; max-width: calc(100% - 16px); overflow-x: auto; }
  .debug-grid { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .editor-toolbar { gap: 10px; }
  .workflow-meta { gap: 10px; }
  .runtime-tab, .workflow-tab { padding: 9px; }
  .node-palette { padding: 12px; }
  .palette-tabs { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .palette-tab { min-height: 30px; }
  .canvas-controls { left: 8px; right: 8px; justify-content: center; }
}
</style>
