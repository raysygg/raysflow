import http from './http'

/** 获取当前租户下可用的 Runtime 应用列表 */
export const fetchRuntimeApplications = () => http.get('/runtime/applications')

/** 获取指定 Runtime 应用的完整配置信息 */
export const fetchRuntimeApplicationConfiguration = applicationId =>
  http.get(`/runtime/applications/${applicationId}/configuration`)

/** 展开当前草稿 Prompt 渲染效果，不实际调用大模型 */
export const previewRuntimePrompt = (applicationId, input) =>
  http.post(`/runtime/applications/${applicationId}/prompt-preview`, { input })

/** 读取应用当前草稿的流程校验与合法性检查结果 */
export const fetchRuntimeWorkflowChecks = applicationId =>
  http.get(`/orchestration/apps/${applicationId}/checks`)

/** 获取应用的对外访问入口列表（如 API 接入、对话窗口、表单） */
export const fetchRuntimeEntrypoints = applicationId =>
  http.get(`/runtime/applications/${applicationId}/entrypoints`)

/** 为应用新建对外访问入口 */
export const createRuntimeEntrypoint = (applicationId, payload) =>
  http.post(`/runtime/applications/${applicationId}/entrypoints`, payload)

/** 更新现有的应用对外访问入口配置 */
export const updateRuntimeEntrypoint = (applicationId, entrypointId, payload) =>
  http.put(`/runtime/applications/${applicationId}/entrypoints/${entrypointId}`, payload)

/** 轮转刷新应用对外访问入口的 API Secret 凭证 */
export const rotateRuntimeEntrypointSecret = (applicationId, entrypointId) =>
  http.post(`/runtime/applications/${applicationId}/entrypoints/${entrypointId}/rotate-secret`)

/** 外部调用指定入口运行 Agent 或工作流 */
export const invokeRuntimeEntrypoint = (entrypointId, payload) =>
  http.post(`/runtime/entrypoints/${entrypointId}/invoke`, payload)

/** 增量轮询或查询指定 Run 运行实例的实时状态与执行日志 */
export const fetchRuntimeRun = (runId, afterSequence = 0) =>
  http.get(`/runtime/runs/${runId}`, { params: { afterSequence } })

/** 分页查询 Runtime 运行任务实例历史记录 */
export const fetchRuntimeRuns = (params = {}) => http.get('/runtime/runs', { params })

/** 查询运行失败与死信队列中的异常任务 */
export const fetchRuntimeFailures = (params = {}) => http.get('/runtime/operations/failures', { params })

/** 查询运行中心与引擎算力节点的健康指标 */
export const fetchRuntimeOperationsHealth = () => http.get('/runtime/operations/health')

/** 获取指定 Run 任务的节点级时间线轨迹 */
export const fetchRuntimeTimeline = runId => http.get(`/runtime/operations/runs/${runId}/timeline`)

/** 预览失败任务的断点恢复效果 */
export const previewRuntimeRecovery = payload => http.post('/runtime/operations/recovery/preview', payload)

/** 重新播放或死信重试指定的运行任务 */
export const replayRuntimeRun = payload => http.post('/runtime/operations/replay', payload)

/** 批量处理或标记消除失败的任务死信 */
export const disposeRuntimeFailures = payload => http.post('/runtime/operations/failures/batch', payload)

/** 查询 SLO 引擎与告警阻断策略 */
export const fetchRuntimeSloPolicies = params => http.get('/runtime/operations/slo-policies', { params })

/** 按链路维度读取持久化运行事件，供诊断页与可观测中心使用 */
export const queryRuntimeEvents = params => http.get('/runtime/runs/events/query', { params })

/** 提交画布流程图的 DEBUG 调试单体运行 */
export const submitRuntimeDraftTest = payload => http.post('/runtime/runs/draft-test', payload)

/** 对正在运行或挂起的 Run 任务发送控制指令（暂停、恢复、取消） */
export const controlRuntimeRun = (runId, request) =>
  http.post(`/runtime/runs/${runId}/control`, request)

/** 新建对话会话实例 */
export const createRuntimeConversation = payload =>
  http.post('/runtime/conversations', payload)

/** 查询当前租户的所有活动对话会话列表 */
export const fetchRuntimeConversations = () => http.get('/runtime/conversations')

/** 归档指定的对话会话 */
export const archiveRuntimeConversation = conversationId =>
  http.post(`/runtime/conversations/${conversationId}/archive`)

/** 清理会话消息上下文，服务端保留关联 Run 和审计事实 */
export const clearRuntimeConversation = conversationId =>
  http.post(`/runtime/conversations/${conversationId}/clear`)

/** 恢复已归档的会话 */
export const restoreRuntimeConversation = conversationId =>
  http.post(`/runtime/conversations/${conversationId}/restore`)

/** 基于历史节点衍生分支新对话 */
export const branchRuntimeConversation = conversationId =>
  http.post(`/runtime/conversations/${conversationId}/branch`)

/** 导出指定会话的完整对话记录 JSON/Markdown */
export const exportRuntimeConversation = conversationId =>
  http.get(`/runtime/conversations/${conversationId}/export`)

/* 中文约定：页面统一通过本模块访问 Runtime 接口，不重复拼装 URL 或猜测参数语义。 */

