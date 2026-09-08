<template>
  <div class="node-editor-fields schema-node-editor">
    <div class="node-help">
      <strong>{{ nodeLabel }}</strong>
      <p>{{ nodeDescription }}</p>
    </div>
    <div class="descriptor-meta">
      <span class="status-ready">{{ descriptor?.status === 'ACTIVE' ? '可用于发布' : '暂不可发布' }}</span>
      <span>{{ fields.length }} 项配置</span>
    </div>
    <div v-if="descriptor" class="node-contract" aria-label="节点能力说明">
      <div><small>输入</small><strong>{{ portText(descriptor.inputPorts) }}</strong></div>
      <div><small>输出</small><strong>{{ portText(descriptor.outputPorts) }}</strong></div>
      <div><small>运行影响</small><strong>{{ sideEffectText(descriptor.sideEffect) }}</strong></div>
      <div><small>控制行为</small><strong>{{ controlSignalText(descriptor.controlSignals) }}</strong></div>
    </div>
    <p v-if="runtimeGuardText" class="runtime-guard"><b>运行保障</b>{{ runtimeGuardText }}</p>
    <p v-if="descriptor?.status !== 'ACTIVE' || descriptor?.executable === false" class="field-warning">
      这个节点还没有可用的运行能力，保存草稿不受影响，但发布前需要先完成能力配置。
    </p>
    <div v-if="requiredFields.length" class="field-section-title">基本配置 <span>必填项</span></div>
    <button v-if="advancedFields.length" type="button" class="advanced-toggle" @click="showAdvanced = !showAdvanced">
      <span>高级设置</span><small>{{ showAdvanced ? '收起' : `${advancedFields.length} 项可选` }}</small>
    </button>
    <template v-for="field in fields" :key="field.name">
      <div v-if="isVisible(field) && (requiredFields.includes(field.name) || showAdvanced)" class="config-field" :class="{ 'is-advanced': !requiredFields.includes(field.name) }">
        <label>
          {{ fieldLabel(field) }}
          <span v-if="requiredFields.includes(field.name)" class="required-mark">必填</span>
        </label>
        <select v-if="isNodeSelect(field)" v-model="node.config[field.name]" class="text-input">
          <option value="">请选择{{ fieldLabel(field) }}</option>
          <option v-for="option in nodeOptions(field)" :key="String(option.value)" :value="option.value">{{ option.label }}</option>
        </select>
        <select v-else-if="isResourceField(field)" v-model="node.config[field.name]" class="text-input" :multiple="isArrayField(field)">
          <option v-if="resourceOptions(field).length === 0" value="" disabled>{{ resourceEmptyShort(field) }}</option>
          <option v-for="option in resourceOptions(field)" :key="String(option.value)" :value="option.value">{{ option.label }}</option>
        </select>
        <select v-else-if="isSelect(field)" v-model="node.config[field.name]" class="text-input" :multiple="isMultiSelect(field)">
          <option value="">请选择{{ fieldLabel(field) }}</option>
          <option v-for="option in options(field)" :key="String(option.value)" :value="option.value">{{ option.label }}</option>
        </select>
        <select v-else-if="isVariableReference(field)" v-model="node.config[field.name]" class="text-input">
          <option value="">请选择输入变量</option>
          <option v-for="variable in variables" :key="variable" :value="variable">{{ variableLabel(variable) }}</option>
        </select>
        <KeyValueEditor v-else-if="field.widget === 'key-value'" :model-value="keyValueRows(field)" @update:model-value="value => commitKeyValue(field, value)" />
        <FieldSchemaEditor v-else-if="field.widget === 'json-schema'" :model-value="schemaFields(field)" @update:model-value="value => commitSchema(field, value)" />
        <RepeatableListEditor v-else-if="listWidgets.has(field.widget)" :model-value="listValue(field)" @update:model-value="value => node.config[field.name] = value" />
        <AssignmentBuilder v-else-if="field.widget === 'assignment-list'" :model-value="listValue(field)" :sources="variableOptions" :targets="assignmentTargets" @update:model-value="value => node.config[field.name] = value" />
        <ConditionBuilder v-else-if="field.widget === 'expression'" :model-value="conditionValue(field)" :fields="variableOptions" @update:model-value="value => node.config[field.name] = value" />
        <DiagnosticJsonEditor v-else-if="field.widget === 'json'" :model-value="diagnosticValue(field)" @update:model-value="value => node.config[field.name] = value" />
        <textarea v-else-if="isLongText(field)" v-model="textValues[field.name]" class="textarea-input" autocomplete="off" spellcheck="false" :placeholder="placeholder(field)" @input="commitText(field)" />
        <input v-else-if="isNumber(field)" v-model.number="node.config[field.name]" type="number" class="text-input" autocomplete="off" spellcheck="false" />
        <label v-else-if="isBoolean(field)" class="switch-field">
          <input v-model="node.config[field.name]" type="checkbox" />
          <span>{{ booleanText(field) }}</span>
        </label>
        <input v-else v-model="node.config[field.name]" class="text-input" autocomplete="off" spellcheck="false" :placeholder="placeholder(field)" />
        <small v-if="fieldHelp(field)" class="field-help">{{ fieldHelp(field) }}</small>
        <small v-if="isResourceField(field) && resourceOptions(field).length === 0" class="field-help field-warning-text">{{ resourceEmptyText(field) }}</small>
      </div>
    </template>
    <small v-if="fields.length === 0" class="field-help empty-fields">这个节点暂时不需要额外配置。</small>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { WORKFLOW_VARIABLES, nodeOutputReference } from '../../../constants/workflowVariables'
import AssignmentBuilder from '../../structured/AssignmentBuilder.vue'
import ConditionBuilder from '../../structured/ConditionBuilder.vue'
import DiagnosticJsonEditor from '../../structured/DiagnosticJsonEditor.vue'
import FieldSchemaEditor from '../../structured/FieldSchemaEditor.vue'
import KeyValueEditor from '../../structured/KeyValueEditor.vue'
import RepeatableListEditor from '../../structured/RepeatableListEditor.vue'

const props = defineProps({
  node: { type: Object, required: true }, descriptor: { type: Object, default: null },
  models: { type: Array, default: () => [] }, documents: { type: Array, default: () => [] },
  agents: { type: Array, default: () => [] }, organizationUnits: { type: Array, default: () => [] },
  nodes: { type: Array, default: () => [] }, connectors: { type: Array, default: () => [] }
})
const textValues = reactive({})
const showAdvanced = ref(false)
const fields = computed(() => props.descriptor?.configSchema?.fields || [])
const requiredFields = computed(() => props.descriptor?.configSchema?.required || props.descriptor?.requiredConfigFields || [])
const advancedFields = computed(() => fields.value.filter(field => !requiredFields.value.includes(field.name)))
// 同一套 schema 同时驱动控件、必填标记、帮助文案和资源选择，避免节点之间出现行为分叉。
// 变量只能来自运行时输入或上游节点输出，禁止把配置字段名伪装成可运行变量。
const variables = computed(() => [WORKFLOW_VARIABLES.INPUT, WORKFLOW_VARIABLES.USER_MESSAGE, WORKFLOW_VARIABLES.QUERY, ...props.nodes
  .filter(node => node.id !== props.node.id)
  .map(node => nodeOutputReference(node.id))
])
const variableOptions = computed(() => variables.value.map(variable => ({ value: variable, label: variableLabel(variable), type: 'any' })))
const assignmentTargets = computed(() => [{ value: 'output', label: '节点输出', type: 'any' }, ...fields.value.filter(field => field.name).map(field => ({ value: field.name, label: fieldLabel(field), type: field.type || 'any' }))])
const listWidgets = new Set(['list', 'category-list', 'variable-list'])
const isLongText = field => ['prompt-editor', 'markdown-editor', 'code-editor', 'jinja-editor'].includes(field.widget)
const isNumber = field => ['number', 'duration'].includes(field.widget) || ['integer', 'number', 'long'].includes(field.type)
const isBoolean = field => ['switch', 'boolean'].includes(field.widget) || field.type === 'boolean'
const isSelect = field => ['select', 'enum'].includes(field.widget)
const isNodeSelect = field => ['node-select', 'operation-select', 'tool-select'].includes(field.widget)
const isVariableReference = field => ['variable-select', 'variable-reference'].includes(field.widget)
const isArrayField = field => field.type === 'array' || field.name.endsWith('Ids')
const isMultiSelect = field => field.widget === 'multi-select' || isArrayField(field)
const isResourceField = field => ['resource-select', 'credential-select'].includes(field.widget)
const isVisible = field => !field.visibleWhen || String(props.node.config?.[field.visibleWhen.field] ?? '') === String(field.visibleWhen.equals ?? props.node.config?.[field.visibleWhen.field])
const listValue = field => Array.isArray(props.node.config?.[field.name]) ? props.node.config[field.name] : []
const conditionValue = field => Array.isArray(props.node.config?.[field.name]) ? props.node.config[field.name] : []
const keyValueRows = field => Array.isArray(props.node.config?.[field.name]) ? props.node.config[field.name] : Object.entries(props.node.config?.[field.name] || {}).map(([key, value]) => ({ key, value }))
const commitKeyValue = (field, value) => { props.node.config[field.name] = Object.fromEntries(value.filter(item => item.key).map(item => [item.key, item.value])) }
const schemaFields = field => {
  const value = props.node.config?.[field.name]
  if (Array.isArray(value)) return value
  const required = new Set(value?.required || [])
  return Object.entries(value?.properties || {}).map(([key, property]) => ({ key, type: property.type || 'string', description: property.description || '', required: required.has(key) }))
}
const commitSchema = (field, value) => { props.node.config[field.name] = { type: 'object', properties: Object.fromEntries(value.filter(item => item.key).map(item => [item.key, { type: item.type, description: item.description || undefined }])), required: value.filter(item => item.key && item.required).map(item => item.key) } }
const diagnosticValue = field => { const value = props.node.config?.[field.name]; return value && typeof value === 'object' ? value : {} }
const PLACEHOLDERS = {
  promptTemplate: '例如：根据用户问题和知识库内容，给出准确、简洁的答复。',
  systemMessage: '例如：你是企业内部政策助手，只回答有依据的问题。',
  url: '例如：https://api.example.com/orders/{{variables.orderId}}',
  structuredOutputSchema: '例如：{"answer":{"type":"string"}}',
  headers: '例如：{"Content-Type":"application/json"}',
  body: '例如：{"question":"{{variables.input}}"}',
  retryPolicy: '例如：{"maxAttempts":3,"backoffMs":500}'
}
const placeholder = field => PLACEHOLDERS[field.name] || (field.widget === 'code-editor' ? '请输入受控执行代码' : field.widget === 'json-schema' ? '{"type":"object","properties":{}}' : `请输入${fieldLabel(field)}`)
const FIELD_LABELS = {
  modelId: '主模型', modelKey: '主模型编码', backupModelId: '备用模型', backupModelKey: '备用模型编码', promptTemplate: '提示词', systemMessage: '系统提示词', memoryWindow: '记忆窗口',
  maxContextChars: '最大上下文长度', imageVariables: '图像变量', structuredOutputSchema: '结构化输出结构', inputVariables: '输入变量',
  categories: '分类', language: '执行语言', arguments: '工具参数', operationId: '操作', toolName: '工具', retrievalScope: '检索文档范围', knowledgeDocumentIds: '指定文档（多选）',
  languageStrategy: '检索语言策略', queryLanguage: '查询语言', retrievalStrategy: '检索策略', topK: '召回数量', agentId: 'Agent',
  approvalTitle: '审批标题', approvalDescription: '审批说明', approvalGroupId: '审批组', operator: '匹配操作符', trueTarget: '满足时分支',
  falseTarget: '不满足时分支', inputVariable: '输入变量', bodyNodeId: '循环体节点', itemVariable: '当前项变量', parallel: '并行处理',
  errorStrategy: '出错处理方式', flattenOutput: '合并输出', maxItems: '最多处理数量', parameterSchema: '参数结构', instruction: '提取指令',
  code: '代码', connectorId: '连接器', outputSchema: '输出结构', template: '模板', variables: '聚合变量', fileVariable: '文件变量',
  assignments: '赋值规则', url: '请求地址', method: '请求方法', authRef: '认证凭据', headers: '请求头', body: '请求体', timeout: '超时时间', retryPolicy: '重试策略'
  ,inputVariables: '输入变量', outputVariable: '输出变量', separator: '信息分隔符', maxChars: '最大内容长度', template: '模板内容',
  window: '保留对话轮数', framework: '运行框架', connectorId: '运行时连接器', agents: '团队角色', graph: '子图定义', path: '运行时路径',
  idempotencyKey: '幂等变量', inputReference: '输入内容', inputSchema: '输入格式', messageTemplate: '回复模板', ruleSetId: '路由规则',
  embeddingModelSource: '向量模型来源', embeddingModelId: '向量模型', embeddingModelKey: '向量模型编码', query: '检索问题', queryLanguage: '查询语言',
  verifySsl: '验证安全证书', baseUrl: '服务地址', connectorId: '连接器', operationId: '操作', toolName: '工具名称', sourceSystem: '来源系统',
  externalId: '外部成员标识', userName: '账号', displayName: '显示名称', email: '邮箱', inputVariable: '输入变量', outputVariable: '输出变量',
  riskLevel: '风险等级', message: '消息内容', name: '名称', description: '说明'
}
const NODE_LABELS = {
  START: '开始', END: '结束', RAG: '知识检索', LLM: '模型生成', AGENT: 'Agent 调用', CONDITION: '条件分支', PARALLEL: '并行分支', JOIN: '并行聚合', LOOP: '循环处理', TRANSFORM: '数据转换', HUMAN: '人工审批',
  USER_INPUT: '用户输入', DIRECT_REPLY: '直接回复', ITERATION: '迭代处理', QUESTION_CLASSIFIER: '问题分类', PARAMETER_EXTRACTOR: '参数提取', ROUTER: '路由分支',
  CODE: '代码处理', TEMPLATE_TRANSFORM: '模板转换', VARIABLE_AGGREGATOR: '变量聚合', DOCUMENT_EXTRACTOR: '文档提取', VARIABLE_ASSIGNMENT: '变量赋值', LIST_OPERATOR: '列表处理',
  CONTEXT_BUILDER: '上下文整理', PROMPT_TEMPLATE: '提示词模板', SESSION_MEMORY: '会话记忆', AGENT_TEAM: '智能体团队', GRAPH_ORCHESTRATOR: '图编排运行时',
  HTTP_REQUEST: 'HTTP 请求', OPENAPI: 'OpenAPI 调用', MCP: 'MCP 工具', WEBHOOK: 'Webhook 调用', INTERNAL_API: '内部 API', WEB_CRAWLER: '网页抓取'
}
const NODE_DESCRIPTIONS = {
  ITERATION: '对一个列表中的每一项重复执行指定节点。', QUESTION_CLASSIFIER: '根据用户问题自动选择对应的分类分支。',
  PARAMETER_EXTRACTOR: '从输入内容中提取结构化参数，供后续节点使用。', ROUTER: '按照已配置的路由规则选择执行分支。',
  HTTP_REQUEST: '通过已配置的连接器调用外部 HTTP 服务。', CODE: '在受控执行环境中运行一段代码。'
}
const FIELD_HELP = {
  modelId: '选择一个已在模型底座启用的模型。', backupModelId: '主模型不可用时自动切换，可不设置。',
  promptTemplate: '描述这个节点要完成的任务，可使用上游节点输出。', systemMessage: '设定模型的固定行为和边界，可不设置。',
  memoryWindow: '保留最近几轮对话，数值越大消耗的上下文越多。', maxContextChars: '限制发送给模型的上下文长度。',
  imageVariables: '需要传给模型的图片变量，没有图片时留空。', structuredOutputSchema: '需要模型返回固定格式时填写，不需要时留空。',
  retryPolicy: '请求失败后的重试规则，不确定时留空使用系统默认值。',
  inputVariable: '选择一个数组变量，节点会逐项处理其中的内容。', bodyNodeId: '选择每一项要执行的节点。',
  itemVariable: '当前项在循环体内的变量名称，默认即可。', parallel: '打开后会并行处理多个项目，关闭则按顺序处理。',
  errorStrategy: '某一项失败时如何继续：立即停止，或跳过错误继续处理。', flattenOutput: '打开后将每项结果合并成一个列表，便于后续节点使用。', maxItems: '限制本次最多处理的项目数量，避免数据量过大。',
  topK: '最多返回多少条相关知识片段。', retrievalScope: '选择指定文档或当前用户可见文档。', knowledgeDocumentIds: '指定发布版本允许检索的文档。', languageStrategy: '选择按文档、按查询或自动识别语言。', queryLanguage: '语言策略为按查询时使用。', agentId: '选择一个已配置完成的 Agent。',
  approvalGroupId: '审批任务将发送给这个组织或审批组。', url: '支持变量引用的请求地址。', timeout: '请求超过该时间仍未完成时自动失败。',
  inputVariables: '选择需要进入上下文或提示词的上游结果。', outputVariable: '后续节点通过这个变量名引用结果。',
  maxChars: '限制上下文大小，避免模型输入过长。', window: '建议从 6-20 轮开始，过大可能降低回答聚焦度。',
  framework: '选择已部署并通过连接器授权的运行框架。', connectorId: '连接器负责鉴权、审计、超时和重试。',
  agents: '用 JSON 描述角色、职责和协作顺序；发布前会随运行请求一并发送。', graph: '用 JSON 描述子图入口、节点和边。',
  path: '默认调用 /invoke，也可以按运行时约定修改。'
}
const containsChinese = value => /[\u3400-\u9fff]/.test(String(value || ''))
const fieldLabel = field => FIELD_LABELS[field.name] || (containsChinese(field.label) ? field.label : '其他配置')
const fieldHelp = field => field.help || FIELD_HELP[field.name] || ''
const portText = value => Array.isArray(value) && value.length ? value.join('、') : '默认数据'
const SIDE_EFFECT_LABELS = Object.freeze({
  NONE: '无外部副作用', MODEL_CALL: '调用模型', KNOWLEDGE_RETRIEVAL: '检索知识', EXTERNAL_CALL: '调用外部服务',
  HUMAN_WAIT: '等待人工处理', CHILD_RUN: '创建子运行'
})
const CONTROL_SIGNAL_LABELS = Object.freeze({
  CONTINUE: '继续', SELECT_PORT: '选择分支', FORK: '并行分叉', JOIN_WAIT: '等待汇聚', ITERATE: '迭代处理',
  WAIT_APPROVAL: '等待审批', CHILD_RUN: '执行子流程', COMPLETE: '完成流程'
})
const sideEffectText = value => SIDE_EFFECT_LABELS[value] || '无外部副作用'
const controlSignalText = values => Array.isArray(values) && values.length
  ? values.map(value => CONTROL_SIGNAL_LABELS[value] || value).join('、') : '继续'
const runtimeGuardText = computed(() => {
  const names = fields.value.map(field => field.name)
  const parts = []
  if (names.includes('timeout')) parts.push('可设置超时')
  if (names.includes('retryPolicy')) parts.push('可设置重试')
  if (names.includes('errorStrategy')) parts.push('可设置失败后的处理方式')
  return parts.length ? `${parts.join('、')}。发布检查会验证相关配置。` : ''
})
const booleanText = field => field.name === 'verifySsl' ? '验证安全证书' : field.name === 'parallel' ? '并行处理' : fieldLabel(field)
const optionLabels = { FAIL_FAST: '立即停止', CONTINUE_ON_ERROR: '忽略错误继续', SCORE: '按相关性排序', DIVERSITY: '兼顾内容多样性', GET: 'GET 获取', POST: 'POST 创建', PUT: 'PUT 更新', PATCH: 'PATCH 修改', DELETE: 'DELETE 删除' }
const options = field => (field.options || (field.name === 'method' ? ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'] : [])).map(option => {
  const value = typeof option === 'object' ? option.value : option
  return { value, label: optionLabels[value] || (typeof option === 'object' ? option.label : option) || value }
})
const nodeOptions = field => {
  const candidates = props.nodes.filter(node => !field.excludeSelf || node.id !== props.node.id)
  return candidates.map((node, index) => ({ value: node.id, label: `${node.name || node.id} · ${nodeLabelForType(node.type)}${candidates.filter(item => item.name === node.name).length > 1 ? `（${index + 1}）` : ''}` }))
}
const nodeLabelForType = type => NODE_LABELS[type] || type
const variableLabel = variable => {
  if (variable === WORKFLOW_VARIABLES.INPUT) return '用户输入'
  if (variable === WORKFLOW_VARIABLES.USER_MESSAGE) return '用户消息'
  if (variable === WORKFLOW_VARIABLES.QUERY) return '查询内容'
  const node = props.nodes.find(item => nodeOutputReference(item.id) === variable)
  return node ? `${node.name || node.id} · 输出` : fieldLabel({ name: variable })
}
const nodeLabel = computed(() => NODE_LABELS[props.node.type] || NODE_LABELS[props.descriptor?.nodeType] || (containsChinese(props.descriptor?.label) ? props.descriptor.label : '扩展节点'))
const nodeDescription = computed(() => NODE_DESCRIPTIONS[props.node.type] || props.descriptor?.description || '在这里设置这个节点运行时需要的参数。')
const resourceOptions = field => {
  const list = field.resourceType === 'MODEL' ? props.models : field.resourceType === 'KNOWLEDGE_DOCUMENT' ? props.documents : field.resourceType === 'AGENT' ? props.agents : field.resourceType === 'ORGANIZATION_UNIT' ? props.organizationUnits : field.resourceType === 'TOOL_CONNECTOR' ? props.connectors : []
  return list.map(item => ({ value: field.resourceType === 'MODEL' ? String(item.id) : String(item.id), label: item.modelName ? `${item.modelName}${item.modelKey ? `（${item.modelKey}）` : ''}` : item.title || item.name || item.orgName || String(item.id) }))
}
const resourceEmptyText = field => ({ MODEL: '暂无可用模型，请先到“模型底座”完成配置。', KNOWLEDGE_DOCUMENT: '暂无可用知识文档，请先创建并发布知识库。', AGENT: '暂无可用 Agent，请先完成 Agent 配置。', ORGANIZATION_UNIT: '暂无可用审批组，请先到组织管理中配置。', TOOL_CONNECTOR: '暂无可用连接器，请先完成能力接入。' }[field.resourceType] || '暂无可用资源，请先完成资源配置。')
const resourceEmptyShort = field => ({ MODEL: '请先配置模型', KNOWLEDGE_DOCUMENT: '请先创建知识文档', AGENT: '请先配置 Agent', ORGANIZATION_UNIT: '请先配置审批组', TOOL_CONNECTOR: '请先接入连接器' }[field.resourceType] || '暂无可选资源')
const syncText = () => fields.value.forEach(field => {
  if (field.resourceType === 'MODEL' && props.models?.length && props.node.config?.[field.name]) {
    const cur = String(props.node.config[field.name])
    const match = props.models.find(m => String(m.id) === cur || m.modelKey === cur)
    if (match) {
      props.node.config[field.name] = String(match.id)
      const keyField = field.name === 'backupModelId' ? 'backupModelKey' : field.name === 'modelId' ? 'modelKey' : field.name === 'embeddingModelId' ? 'embeddingModelKey' : null
      if (keyField && match.modelKey) props.node.config[keyField] = match.modelKey
    }
  }
  if (!isLongText(field)) return
  const value = props.node.config?.[field.name]
  textValues[field.name] = value === undefined || value === null || value === '' ? '' : typeof value === 'string' ? value : JSON.stringify(value, null, 2)
})
// 提示词、Markdown 等长文本必须实时回写配置；否则保存时只会序列化旧的 node.config。
const commitText = field => {
  props.node.config[field.name] = textValues[field.name]
}
watch(() => props.node.type, syncText, { immediate: true })
watch([() => props.node.config, () => props.models], syncText, { deep: true, immediate: true })
</script>

<style scoped>
.node-help { padding: 2px 0 6px; }
.node-help strong { color: #182b3d; font-size: 14px; }
.node-help p { margin: 5px 0 0; color: #62788d; font-size: 12px; line-height: 1.55; }
.descriptor-meta { display: flex; gap: 6px; margin: 6px 0 14px; color: #71869a; font-size: 10px; }
.descriptor-meta span { padding: 4px 7px; border: 1px solid #dce7f1; border-radius: 4px; background: #f7fafc; }
.descriptor-meta .status-ready { border-color: #bfe6d2; background: #f0fbf5; color: #14764b; }
.node-contract { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 5px; margin: 0 0 10px; }
.node-contract > div { min-width: 0; padding: 7px; border: 1px solid #e1e9ef; border-radius: 5px; background: #f8fbfd; }
.node-contract small { display: block; color: #8496a6; font-size: 9px; }
.node-contract strong { display: block; margin-top: 3px; overflow: hidden; color: #36546b; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.runtime-guard { margin: 0 0 11px; padding: 8px 9px; border-left: 2px solid #2e6f80; background: #f0f7f8; color: #5e7481; font-size: 10px; line-height: 1.5; }
.runtime-guard b { margin-right: 4px; color: #2e6f80; }
.field-warning { margin: 0 0 14px; padding: 9px 10px; border: 1px solid #f0d7a9; border-radius: 6px; background: #fff9ec; color: #8b601a; font-size: 11px; line-height: 1.55; }
.field-section-title { display: flex; align-items: center; gap: 7px; margin: 0 0 5px; color: #304b63; font-size: 11px; font-weight: 700; }
.advanced-title { margin-top: 15px; padding-top: 14px; border-top: 1px solid #dfe8f0; }
.advanced-title span { color: #8193a3; }
.advanced-toggle { display: flex; justify-content: space-between; width: 100%; margin-top: 12px; padding: 9px 0; border: 0; border-top: 1px solid #dfe8f0; background: transparent; color: #304b63; cursor: pointer; font-size: 11px; font-weight: 700; }
.advanced-toggle small { color: #7890a4; font-weight: 400; }
.field-section-title span { color: #b42318; font-size: 10px; font-weight: 500; }
.config-field { padding: 10px 0; border-bottom: 1px solid #e8eef3; }
.config-field.is-advanced { padding-top: 12px; }
.config-field > label:first-child { display: flex; align-items: center; gap: 6px; margin: 0 0 6px; color: #304b63; font-size: 12px; font-weight: 650; }
.required-mark { color: #b42318; font-size: 10px; font-weight: 500; }
.field-help { display: block; margin-top: 5px; color: #7890a4; font-size: 10px; line-height: 1.5; }
.field-warning-text { color: #9a6817; }
.switch-field { display: flex !important; align-items: center; gap: 8px; margin: 0 !important; font-weight: 400 !important; }
.switch-field input { accent-color: #287fd3; }
.schema-node-editor select[multiple] { min-height: 74px; height: 88px; padding: 5px 8px; }
.schema-node-editor select[multiple] option { padding: 5px 2px; }
.schema-node-editor textarea { min-height: 88px; resize: vertical; }
.empty-fields { padding: 12px 0; }
</style>
