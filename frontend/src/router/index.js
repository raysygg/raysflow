import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import http, { restoreLoginSession } from '../api/http'

let menuRequest

const routes = [
  {
    path: '/quality/workbench-fixtures',
    name: 'workbench-fixtures',
    component: () => import('../views/WorkbenchAcceptanceView.vue'),
    meta: { title: '企业工作台界面验收', public: true }
  },
  {
    path: '/diagnostics',
    component: AppLayout,
    children: [
      { path: '', name: 'diagnostics', component: () => import('../views/PlatformReadinessView.vue'), meta: { title: '平台就绪诊断', requiredCapability: 'PLATFORM_AUDIT' } }
    ]
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '用户登录', guest: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { title: '企业租户自助注册', guest: true }
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '工作台', capability: '/dashboard' } },
      { path: 'applications', name: 'applications', component: () => import('../views/AgentStudioView.vue'), meta: { title: '业务应用' } },
      { path: 'applications/new', name: 'application-wizard', component: () => import('../views/ApplicationWizardView.vue'), meta: { title: '创建业务应用' } },
      { path: 'applications/:id', name: 'application-detail', component: () => import('../views/RuntimeWorkspaceView.vue'), meta: { title: '运行工作区' } },
      { path: 'applications/:id/runtime', name: 'runtime-workspace', redirect: to => ({ path: `/applications/${to.params.id}` }) },
      { path: 'applications/:id/launch', name: 'application-launch', component: () => import('../views/ApplicationLaunchView.vue'), meta: { title: '应用入口' } },
      { path: 'chat', name: 'chat', component: () => import('../views/ChatConsoleView.vue'), meta: { title: '应用工作台', capability: '/chat' } },
      { path: 'knowledge', name: 'knowledge', component: () => import('../views/KnowledgeBaseView.vue'), meta: { title: '知识库与知识检索' } },
      { path: 'applications/:id/workflow', name: 'application-workflow', component: () => import('../views/WorkflowStudioView.vue'), meta: { title: '应用执行流程'} },
      { path: 'workflow-executions', name: 'workflow-executions', component: () => import('../views/WorkflowExecutionView.vue'), meta: { title: '运行中心', requiredCapability: 'RUN_VIEW' } },
      { path: 'runtime-operations', name: 'runtime-operations', component: () => import('../views/RuntimeOperationsView.vue'), meta: { title: '故障处理', requiredCapability: 'RUN_VIEW' } },
      { path: 'ops', name: 'ops', component: () => import('../views/OpsCenterView.vue'), meta: { title: '运行观测台', requiredCapability: 'PLATFORM_AUDIT' } },
      { path: 'models', name: 'models', component: () => import('../views/ModelCenterView.vue'), meta: { title: '模型中心', requiredCapability: 'PLATFORM_RESOURCE_MANAGE' } },
      { path: 'model-prices', name: 'model-prices', component: () => import('../views/ModelPriceView.vue'), meta: { title: '模型价格规则' } },
      { path: 'organization', name: 'organization', component: () => import('../views/OrganizationCenterView.vue'), meta: { title: '组织与成员', requiredCapability: 'ORGANIZATION_MANAGE' } },
      { path: 'tenant', name: 'tenant', component: () => import('../views/TenantCenterView.vue'), meta: { title: '租户控制台', requiredCapability: 'TENANT_MANAGE' } },
      { path: 'approval', name: 'approval', component: () => import('../views/ApprovalCenterView.vue'), meta: { title: '任务中心', capability: '/approval', requiredCapability: 'TASK_APPROVE' } },
      { path: 'evaluation', name: 'evaluation', component: () => import('../views/EvaluationCenterView.vue'), meta: { title: '评测中心' } },
      { path: 'marketplace', name: 'marketplace', component: () => import('../views/MarketplaceView.vue'), meta: { title: '能力市场' } },
      { path: 'audit', name: 'audit', component: () => import('../views/AuditCenterView.vue'), meta: { title: '审计与风控' } },
      { path: 'security-sessions', name: 'security-sessions', component: () => import('../views/SecuritySessionView.vue'), meta: { title: '设备会话' } },
      { path: 'authorization', name: 'authorization', component: () => import('../views/AuthorizationCenterView.vue'), meta: { title: '角色与权限' } },
      { path: 'users', name: 'users', redirect: '/organization', meta: { title: '用户管理' } },
      { path: 'tenants', name: 'tenants', redirect: '/tenant', meta: { title: '租户管理' } },
      { path: 'identity', name: 'identity-governance', component: () => import('../views/IdentityGovernanceView.vue'), meta: { title: '企业身份' } },
      { path: 'entitlements', name: 'entitlement-usage', component: () => import('../views/EntitlementUsageView.vue'), meta: { title: '套餐、权益与用量' } },
      { path: 'data-governance', name: 'data-governance', component: () => import('../views/DataGovernanceView.vue'), meta: { title: '数据治理' } },
      { path: 'adoption', name: 'adoption-analytics', component: () => import('../views/AdoptionAnalyticsView.vue'), meta: { title: '产品采用分析' } },
      { path: 'compliance', name: 'compliance', redirect: '/data-governance', meta: { title: '数据治理' } },
      { path: 'router', name: 'router', component: () => import('../views/ModelRouterView.vue'), meta: { title: '智能路由配置', requiredCapability: 'PLATFORM_RESOURCE_MANAGE' } },
      { path: 'developer', name: 'developer', component: () => import('../views/DeveloperCenterView.vue'), meta: { title: '连接器管理', requiredCapability: 'PLATFORM_RESOURCE_MANAGE' } },
      { path: 'billing', name: 'billing', component: () => import('../views/BillingFinanceView.vue'), meta: { title: '账期与账单' } },
      { path: 'architecture', name: 'architecture', component: () => import('../views/ArchitectureView.vue'), meta: { title: '项目架构说明' } },
      { path: 'access-denied', name: 'access-denied', component: () => import('../views/PermissionDeniedView.vue'), meta: { title: '访问权限' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录鉴权守卫
router.beforeEach(async (to) => {
  if (to.meta.public) return true
  // 每次受保护导航都检查访问令牌新鲜度，页面刷新和热更新可直接使用 HttpOnly Cookie 恢复会话。
  const token = await restoreLoginSession()
  if (to.meta.guest) return token ? '/dashboard' : true
  if (!token) return '/login'
  if (!to.meta.requiredCapability) return true
  try {
    menuRequest ||= http.get('/auth/user-info')
    const response = await menuRequest
    const configuredPaths = (response.data?.menus || []).filter(item => item.status === 'ACTIVE').map(item => ({
      '/users': '/organization',
      '/tenants': '/tenant',
      // 后端历史菜单仍可能返回旧路径，统一映射到当前运行中心入口。
      '/workflow': '/workflow-executions'
    }[item.path] || item.path))
    // 入口权限以管理员在角色菜单页面保存的导航配置为准，能力码只承担后端动作授权。
    const granted = configuredPaths.includes(to.path)
    return granted ? true : { name: 'access-denied', query: { from: to.fullPath } }
  } catch {
    // 能力接口暂时不可用时保留页面访问，具体资源接口仍会执行最终授权。
    menuRequest = null
    return true
  }
})

router.afterEach((to) => {
  document.title = `${to.meta.title || 'Raysflow OS'} - Raysflow OS · 光流智能引擎`
})

export default router
