<template>
  <div class="node-editor-fields rag-node-editor">
    <div class="capability-summary">
      <div><span>输入</span><strong>检索问句 / 变量引用</strong></div>
      <div><span>输出</span><strong>包含语言对齐的上下文</strong></div>
      <div><span>作用</span><strong>知识库向量检索与重排</strong></div>
    </div>

    <label>检索问句表达</label>
    <el-input v-model="node.config.query" placeholder="默认使用 {{sys.query}}，支持自定义 Prompt 变量" />
    <small class="field-help">留空时自动读取工作流系统输入或当前问句变量。</small>

    <label>关联文档范围</label>
    <el-radio-group v-model="node.config.retrievalScope" class="segmented-control">
      <el-radio-button value="VISIBLE_DOCUMENTS">全库可见文档</el-radio-button>
      <el-radio-button value="EXPLICIT_DOCUMENTS">指定具体文档</el-radio-button>
    </el-radio-group>

    <template v-if="node.config.retrievalScope === 'EXPLICIT_DOCUMENTS'">
      <label>选择关联文档</label>
      <el-select
        v-model="node.config.knowledgeDocumentIds"
        multiple
        filterable
        remote
        reserve-keyword
        collapse-tags
        collapse-tags-tooltip
        :max-collapse-tags="3"
        :remote-method="searchDocuments"
        :loading="documentsLoading"
        placeholder="输入文档名称远程搜索"
      >
        <el-option v-for="item in documentOptions" :key="item.id" :label="documentLabel(item)" :value="String(item.id)">
          <div class="document-option">
            <span>{{ item.title }}</span>
            <small>{{ knowledgeLanguageLabel(item.language) }}{{ item.languageConfirmed ? '' : ' · 待确认' }}</small>
          </div>
        </el-option>
      </el-select>
      <small v-if="!documentsLoading && documentOptions.length === 0" class="field-help">没有找到已完成索引的文档。</small>
    </template>

    <label>最终召回数量 (TopK)</label>
    <el-input-number v-model="node.config.topK" :min="1" :max="20" controls-position="right" />
    <small class="field-help">控制最终提供给后置大模型节点的切块数量上限。</small>

    <div class="runtime-note">
      <strong>节点工作流程</strong>
      <span>自动感知语言 ➔ 跨语言全量召回 ➔ 智能重排 ➔ 上下文与语言约束组装</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  KNOWLEDGE_LANGUAGES,
  KNOWLEDGE_REMOTE_OPTION_SIZE,
  fetchRagEmbeddingModels,
  fetchKnowledgeDocumentOptions,
  fetchKnowledgeDocuments,
  knowledgeLanguageLabel,
} from '../../../api/knowledge'
import { apiData } from '../../../api/contracts'

const props = defineProps({ node: { type: Object, required: true } })
const documentOptions = ref([])
const documentsLoading = ref(false)
const modelOptions = ref([])
const modelsLoading = ref(false)
const modelChoice = ref('')

const selectedIds = computed(() => Array.isArray(props.node.config.knowledgeDocumentIds)
  ? props.node.config.knowledgeDocumentIds.map(String)
  : [])
const selectedDocuments = computed(() => documentOptions.value.filter(item => selectedIds.value.includes(String(item.id))))
const hasUnconfirmedDocument = computed(() => selectedDocuments.value.some(item => !item.languageConfirmed))

const modelOptionValue = option => `${option.source}:${option.modelId || option.modelKey}`
const modelOptionLabel = option => `${option.modelName} · ${option.sourceLabel} · ${option.vectorDimension}维`
const configuredModelValue = () => {
  const source = props.node.config.embeddingModelSource
  const identity = props.node.config.embeddingModelId || props.node.config.embeddingModelKey
  return source && identity ? `${source}:${identity}` : ''
}

const applyModelSelection = () => {
  const option = modelOptions.value.find(item => modelOptionValue(item) === modelChoice.value)
  if (!option) return
  props.node.config.embeddingModelSource = option.source
  props.node.config.embeddingModelId = option.modelId || null
  props.node.config.embeddingModelKey = option.modelKey
}

const loadEmbeddingModels = async () => {
  modelsLoading.value = true
  try {
    modelOptions.value = apiData(await fetchRagEmbeddingModels(), [])
    const configured = configuredModelValue()
    const selected = modelOptions.value.find(item => modelOptionValue(item) === configured)
      || modelOptions.value.find(item => item.recommended)
      || modelOptions.value[0]
    modelChoice.value = selected ? modelOptionValue(selected) : ''
    applyModelSelection()
  } finally {
    modelsLoading.value = false
  }
}

const mergeOptions = items => {
  const merged = new Map(documentOptions.value.map(item => [String(item.id), item]))
  ;(items || []).forEach(item => merged.set(String(item.id), item))
  documentOptions.value = [...merged.values()]
}

const loadSelectedOptions = async () => {
  if (selectedIds.value.length === 0) return
  const response = await fetchKnowledgeDocumentOptions(selectedIds.value)
  mergeOptions(apiData(response, []))
}

/** 远程搜索始终使用小页，并保留已选项；文档数量增长时不会把全租户资产装入设计器内存。 */
const searchDocuments = async keyword => {
  documentsLoading.value = true
  try {
    const response = await fetchKnowledgeDocuments({
      page: 1,
      size: KNOWLEDGE_REMOTE_OPTION_SIZE,
      keyword: String(keyword || '').trim(),
      status: 'INDEXED',
    })
    mergeOptions(apiData(response, {}).items || [])
  } finally {
    documentsLoading.value = false
  }
}

const documentLabel = item => `${item.title}（${knowledgeLanguageLabel(item.language)}）`

watch(() => props.node.id, async () => {
  documentOptions.value = []
  await Promise.all([loadEmbeddingModels(), loadSelectedOptions(), searchDocuments('')])
})

onMounted(async () => {
  props.node.config.retrievalScope ||= 'VISIBLE_DOCUMENTS'
  props.node.config.languageStrategy ||= 'AUTO'
  props.node.config.queryLanguage ||= 'OTHER'
  props.node.config.topK = Number(props.node.config.topK || 5)
  props.node.config.knowledgeDocumentIds = selectedIds.value
  await Promise.all([loadEmbeddingModels(), loadSelectedOptions(), searchDocuments('')])
})
</script>

<style scoped>
.capability-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1px; border: 1px solid #dce7f1; border-radius: 7px; overflow: hidden; background: #dce7f1; }
.capability-summary > div { display: grid; gap: 3px; min-width: 0; padding: 8px; background: #f8fbfd; }
.capability-summary span { color: var(--muted); font-size: 9px; }.capability-summary strong { overflow: hidden; color: var(--text); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.segmented-control { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); width: 100%; }
.language-strategy { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.segmented-control :deep(.el-radio-button), .segmented-control :deep(.el-radio-button__inner) { width: 100%; }
.document-option { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-width: 0; }.document-option span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.document-option small { flex: 0 0 auto; color: var(--muted); font-size: 10px; }
.field-warning { color: #a66a0a; font-size: 10px; line-height: 1.5; }
.runtime-note { display: grid; gap: 4px; margin-top: 5px; padding: 9px; border-left: 2px solid #4f91ce; background: #f3f8fc; }.runtime-note strong { color: #285d8b; font-size: 10px; }.runtime-note span { color: #60788e; font-size: 10px; line-height: 1.5; }
.rag-node-editor :deep(.el-input-number), .rag-node-editor :deep(.el-select) { width: 100%; }
</style>
