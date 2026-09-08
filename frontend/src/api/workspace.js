import http from './http'

// 工作区能力由后端按角色和租户范围计算，前端只负责呈现，不在本地复制授权规则。
export const fetchWorkspaceCapabilities = () => http.get('/project/workspace/capabilities')

/** 读取平台依赖和运行能力的就绪状态。 */
export const fetchPlatformReadiness = () => http.get('/ops/readiness')
