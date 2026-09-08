import http from './http'

/** 知识库默认每页加载记录数（20 条） */
export const KNOWLEDGE_DEFAULT_PAGE_SIZE = 20

/** 远程下拉框组件最大回显数量上限（30 条） */
export const KNOWLEDGE_REMOTE_OPTION_SIZE = 30

/** 知识库支持的自然语言枚举列表 */
export const KNOWLEDGE_LANGUAGES = Object.freeze([
  { value: 'ZH', label: '中文' },
  { value: 'EN', label: '英文' },
  { value: 'OTHER', label: '其他语言' },
])

/** 知识文档索引状态枚举列表 */
export const KNOWLEDGE_STATUSES = Object.freeze([
  { value: 'PENDING', label: '待处理' },
  { value: 'INDEXING', label: '索引中' },
  { value: 'INDEXED', label: '可检索' },
  { value: 'REINDEX_REQUIRED', label: '待重建' },
  { value: 'INDEX_FAILED', label: '索引失败' },
  { value: 'DISABLED', label: '已停用' },
])

const languageLabels = Object.freeze(Object.fromEntries(KNOWLEDGE_LANGUAGES.map(item => [item.value, item.label])))
const statusLabels = Object.freeze(Object.fromEntries(KNOWLEDGE_STATUSES.map(item => [item.value, item.label])))

/**
 * 将语言枚举编码转换为友好的中文展示文本。
 *
 * @param {string} value 语言编码（如 'ZH', 'EN'）
 * @returns {string} 可读的中文文本
 */
export const knowledgeLanguageLabel = value => languageLabels[value] || '其他语言'

/**
 * 将文档索引状态编码转换为友好的中文状态标签。
 *
 * @param {string} value 状态编码（如 'INDEXED', 'INDEX_FAILED'）
 * @returns {string} 可读的中文状态描述
 */
export const knowledgeStatusLabel = value => statusLabels[value] || '未知状态'

/**
 * 分页拉取知识库文档资产列表。
 * 说明：文档资产列表始终使用服务端分页，调用方不得通过超大 size 恢复全量加载。
 *
 * @param {Object} params 查询参数（包含 page, size, keyword, language, status）
 * @returns {Promise<Object>} 返回分页数据对象
 */
export const fetchKnowledgeDocuments = (params = {}) => http.get('/knowledge/documents', {
  params: {
    page: params.page || 1,
    size: params.size || KNOWLEDGE_DEFAULT_PAGE_SIZE,
    keyword: params.keyword || undefined,
    language: params.language || undefined,
    status: params.status || undefined,
  },
})

/** 批量获取指定的知识文档摘要选项（用于下拉框回显） */
export const fetchKnowledgeDocumentOptions = ids => http.get('/knowledge/documents/options', {
  params: { ids },
  paramsSerializer: { indexes: null },
})

/** 获取指定知识文档当前的向量索引代别与详细状态 */
export const fetchKnowledgeDocumentIndexState = documentId =>
  http.get(`/knowledge/documents/${documentId}/index-state`)

/** 查询指定知识文档历史提交的后台索引任务日志 */
export const fetchKnowledgeDocumentIndexTasks = documentId =>
  http.get(`/knowledge/documents/${documentId}/index-tasks`)

/** 获取当前生效的知识向量索引代别（Generation）元数据 */
export const fetchCurrentKnowledgeIndexGeneration = selection =>
  http.get('/knowledge/index-generations/current', { params: selectionParams(selection) })

/** 获取租户可用的 Embedding 向量模型定义列表 */
export const fetchRagEmbeddingModels = () =>
  http.get('/knowledge/embedding-models')

/** 提交全量知识文档索引的异步批量重建任务 */
export const submitKnowledgeIndexRebuild = selection =>
  http.post('/knowledge/index-generations/rebuild', null, { params: selectionParams(selection) })

/** 提交单篇知识文档的异步重新索引任务 */
export const submitKnowledgeDocumentReindex = (documentId, selection) =>
  http.post(`/knowledge/documents/${documentId}/reindex-async`, null, { params: selectionParams(selection) })

/** 根据 ID 删除指定的知识库文档条目 */
export const deleteKnowledgeDocument = documentId =>
  http.delete(`/knowledge/documents/${documentId}`)

/**
 * 上传知识库原始文件并提交向量切片任务。
 *
 * @param {File} file 待上传的文件对象
 * @param {string} language 文件的自然语言
 * @param {Object} selection 模型选择配置
 * @returns {Promise<Object>} 上传结果
 */
export const uploadKnowledgeDocument = (file, language, selection) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/knowledge/documents/upload', formData, {
    params: { language, ...selectionParams(selection) },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 执行全功能 RAG 检索、重排与上下文唤醒测试 */
export const searchKnowledge = request => http.post('/knowledge/search', request)

/** 辅助格式化模型选择参数 */
const selectionParams = selection => selection ? {
  modelSource: selection.source,
  modelId: selection.modelId || undefined,
  modelKey: selection.modelKey || undefined,
} : {}

/* 中文约定：知识资产页和流程设计器统一通过本模块访问知识接口与枚举文案。 */
