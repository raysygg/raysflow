<template>
  <div class="page-grid">
    <PageToolbar eyebrow="运行治理" title="运行观测" description="查看平台运行健康、队列状态和审计线索。" />
    <ResourceContextRail label="观测范围" name="平台运行状态" state="持续监测" hint="先检查平台就绪状态，再处理运行异常" mark="O" />
    <div class="readiness-link-row">
      <span>发布前先确认平台依赖和执行能力</span>
      <RouterLink to="/diagnostics" class="ghost-btn compact-btn">打开平台就绪诊断</RouterLink>
    </div>
    <div class="tab-list">
      <button 
        v-for="tab in tabs" 
        :key="tab.value" 
        class="tab-btn" 
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 运行状态与指标 -->
    <div v-if="activeTab === 'status'" class="page-grid">
      <div class="metric-grid">
        <div class="metric-card">
          <span class="metric-label">今日会话消息数</span>
          <span class="metric-value">{{ opsSnapshot.todayRequests || '暂无数据' }}</span>
            <span class="pill">数据库实时统计</span>
        </div>
        <div class="metric-card">
          <span class="metric-label">企业已分配配额</span>
          <span class="metric-value">{{ billing.monthlyTokenLimit ? (billing.monthlyTokenLimit / 10000).toFixed(0) + '万' : '未配置' }}</span>
            <span class="pill">Token 配额</span>
        </div>
        <div class="metric-card">
          <span class="metric-label">工作流已用额度</span>
          <span class="metric-value">{{ billing.monthlyWorkflowLimit ? (billing.monthlyWorkflowUsed || 0) + ' / ' + billing.monthlyWorkflowLimit : '未配置' }}</span>
            <span class="pill">工作流用量</span>
        </div>
        <div class="metric-card">
          <span class="metric-label">云存储限额空间</span>
          <span class="metric-value">{{ billing.storageLimitMb ? billing.storageLimitMb + ' MB' : '未配置' }}</span>
            <span class="pill">存储配额</span>
        </div>
      </div>

      <div class="split-grid">
        <SectionCard title="服务监控快照">
          <div class="info-list">
            <div class="info-row">
              <strong>系统合规网关规则数</strong>
              <span>已部署 {{ complianceRules.length }} 条过滤规则</span>
            </div>
            <div class="info-row">
              <strong>当前企业标识</strong>
              <span>{{ tenantCode }}</span>
            </div>
            <div class="info-row">
              <strong>模型连接</strong>
              <RouterLink to="/models" class="link-btn">前往模型中心查看</RouterLink>
            </div>
            <div class="info-row">
              <strong>今日平均延迟</strong>
              <span>{{ opsSnapshot.averageLatency || '暂无数据' }}</span>
            </div>
            <div class="info-row">
              <strong>今日 RAG 命中率</strong>
              <span>{{ opsSnapshot.ragHitRate || '暂无数据' }}</span>
            </div>
            <div class="info-row">
              <strong>今日调用失败率</strong>
              <span>{{ opsSnapshot.failureRate || '暂无数据' }}</span>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="运行观测说明">
          <p class="section-description ops-description">
            本控制台运行数据、配置和监控完全对接数据库真实接口。多租户下各租户仅能管理及查阅属于自身租户环境的数据。
            <br/><br/>
            大模型底座支持<b>动态云端实例化</b>。企业录入有效凭证后，模型注册中心会缓存连接并优先加载企业私有模型；平台共享模型由超管统一维护，未配置有效凭证的模型不会被当作可用服务。
          </p>
        </SectionCard>
      </div>

      <SectionCard title="模型用量与成本">
        <div class="info-list">
          <div class="info-row">
            <strong>统计周期</strong>
            <span>最近 30 天，共 {{ usageSummary.totalCalls || 0 }} 次模型调用</span>
          </div>
          <div class="info-row">
            <strong>Token 用量</strong>
            <span>输入 {{ usageSummary.inputTokens || 0 }} / 输出 {{ usageSummary.outputTokens || 0 }}</span>
          </div>
          <div class="info-row">
            <strong>真实成本</strong>
            <span>{{ usageSummary.costAvailable ? usageSummary.totalCost : '暂无价格规则或真实 Token 数据' }}</span>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="成本分组明细" description="仅展示已经落库的真实模型调用指标，成本不可用时保留为零并以调用记录为准。">
        <div class="breakdown-toolbar">
          <select v-model="breakdownDimension" class="text-input" @change="loadBreakdown">
            <option value="MODEL">按模型</option><option value="AGENT">按 Agent</option><option value="DATE">按日期</option>
          </select>
        </div>
        <table class="table-card" v-if="usageBreakdown.length">
          <thead><tr><th>统计维度</th><th>调用次数</th><th>成功 / 失败</th><th>输入 Token</th><th>输出 Token</th><th>成本</th></tr></thead>
          <tbody><tr v-for="item in usageBreakdown" :key="item.dimension"><td>{{ item.dimension }}</td><td>{{ item.calls }}</td><td>{{ item.successCalls }} / {{ item.failedCalls }}</td><td>{{ item.inputTokens }}</td><td>{{ item.outputTokens }}</td><td>{{ item.totalCost || '暂无价格' }}</td></tr></tbody>
        </table>
        <p v-else class="empty-state">当前周期暂无可分组的真实调用记录。</p>
      </SectionCard>
    </div>

    <!-- 用户管理 -->
    <div v-if="activeTab === 'users'" class="page-grid">
      <SectionCard title="租户用户列表" description="管理当前企业组织内的子账户和访问权限。">
        <template #action>
          <button @click="showAddUserModal = true" class="primary-btn">+ 添加用户</button>
        </template>

        <table class="table-card">
          <thead>
            <tr>
              <th>ID</th>
              <th>账号名称</th>
              <th>显示昵称</th>
              <th>绑定角色</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td>{{ u.id }}</td>
              <td><b>{{ u.username }}</b></td>
              <td>{{ u.nickname }}</td>
              <td><span class="pill">{{ u.role }}</span></td>
              <td>
                <span class="badge" :style="{ color: u.status === 'ACTIVE' ? '#10b981' : '#ef4444', borderColor: u.status === 'ACTIVE' ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)' }">
                  {{ u.status === 'ACTIVE' ? '正常启用' : '禁用' }}
                </span>
              </td>
              <td>{{ formatTime(u.createdAt) }}</td>
              <td>
                <button 
                  @click="toggleUserStatus(u.id, u.status)" 
                  class="ghost-btn compact-btn"
                >
                  {{ u.status === 'ACTIVE' ? '禁用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </SectionCard>

      <!-- Add User modal -->
      <div v-if="showAddUserModal" class="modal-overlay">
        <div class="modal-card">
          <h3>新增子用户账号</h3>
          <div class="form-group">
            <label>登录账号 (唯一)</label>
            <input type="text" v-model="newUser.username" class="text-input" placeholder="输入用户名" />
          </div>
          <div class="form-group">
            <label>登录密码</label>
            <input type="password" v-model="newUser.password" class="text-input" placeholder="输入密码" />
          </div>
          <div class="form-group">
            <label>用户显示昵称</label>
            <input type="text" v-model="newUser.nickname" class="text-input" placeholder="输入姓名" />
          </div>
          <div class="form-group">
            <label>绑定系统角色</label>
            <select v-model="newUser.role">
              <option value="ADMIN">企业管理员</option>
              <option value="OPERATOR">业务运营</option>
              <option value="STAFF">普通员工</option>
            </select>
          </div>
          <div class="modal-actions">
            <button @click="showAddUserModal = false" class="ghost-btn">取消</button>
            <button @click="handleAddUser" class="primary-btn">确定创建</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 智能路由 -->
    <div v-if="activeTab === 'routers'" class="page-grid">
      <SectionCard title="智能路由策略" description="根据请求复杂度选择合适的模型，降低成本并提高响应稳定性。">
        <template #action>
          <button @click="showAddRouterModal = true" class="primary-btn">+ 添加路由策略</button>
        </template>

        <table class="table-card">
          <thead>
            <tr>
              <th>规则名称</th>
              <th>指令匹配正则 (Regex Pattern)</th>
              <th>首选主大模型 (Primary)</th>
              <th>热备降级模型 (Backup)</th>
              <th>规则状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in routerRules" :key="r.id">
              <td><b>{{ r.ruleName }}</b></td>
              <td><code>{{ r.patternRegex }}</code></td>
              <td><span class="pill">{{ r.primaryModelKey }}</span></td>
              <td><span class="pill muted-pill">{{ r.backupModelKey || '无' }}</span></td>
              <td>
                <span class="badge status-success">
                  {{ r.status }}
                </span>
              </td>
              <td>
                <button 
                  @click="handleDeleteRouter(r.id)" 
                  class="ghost-btn compact-btn danger-btn"
                >
                  注销
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </SectionCard>

      <div v-if="showAddRouterModal" class="modal-overlay">
        <div class="modal-card">
          <h3>新增智能路由策略</h3>
          <div class="form-group">
            <label>策略规则显示名称</label>
            <input type="text" v-model="newRouter.ruleName" class="text-input" placeholder="例如: 财务分析意图分流" />
          </div>
          <div class="form-group">
            <label>关键字/意图匹配正则表达式</label>
            <input type="text" v-model="newRouter.patternRegex" class="text-input" placeholder="例如: .*(退款|赔偿|账单).*" />
          </div>
          <div class="form-group">
            <label>首选指向大模型 Key</label>
            <select v-model="newRouter.primaryModelKey" class="text-input"><option value="">请选择模型中心中的模型</option><option v-for="model in availableModels" :key="`primary-${model.id}`" :value="model.modelKey">{{ model.modelName }}（{{ model.modelKey }}）</option></select>
          </div>
          <div class="form-group">
            <label>容灾备用大模型 Key</label>
            <select v-model="newRouter.backupModelKey" class="text-input"><option value="">不配置备用模型</option><option v-for="model in availableModels" :key="`backup-${model.id}`" :value="model.modelKey">{{ model.modelName }}（{{ model.modelKey }}）</option></select>
          </div>
          <div class="modal-actions">
            <button @click="showAddRouterModal = false" class="ghost-btn">取消</button>
            <button @click="handleAddRouter" class="primary-btn">创建策略</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 安全合规 -->
    <div v-if="activeTab === 'compliance'" class="page-grid">
      <SectionCard title="企业合规安全网关 (Content Guardrails)" description="过滤或脱敏敏感企业数据。当用户输入或模型生成内容触发规则时，将自动进行拦截（记录高危审计日志）或掩码脱敏。">
        <template #action>
          <button @click="showAddComplianceModal = true" class="primary-btn">+ 部署合规过滤器</button>
        </template>

        <table class="table-card">
          <thead>
            <tr>
              <th>安全策略名称</th>
              <th>敏感词/匹配正则</th>
              <th>处置动作 (Action)</th>
              <th>网关状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in complianceRules" :key="c.id">
              <td><b>{{ c.ruleName }}</b></td>
              <td><code>{{ c.sensitiveWord }}</code></td>
              <td>
                <span class="badge" :style="{ color: c.actionType === 'BLOCK' ? '#ef4444' : '#f59e0b', borderColor: c.actionType === 'BLOCK' ? 'rgba(239, 68, 68, 0.3)' : 'rgba(245, 158, 11, 0.3)' }">
                  {{ c.actionType === 'BLOCK' ? '拦截请求' : '掩码脱敏 (*)' }}
                </span>
              </td>
              <td><span class="status-dot"></span> 实时拦截中</td>
              <td>
                <button 
                  @click="handleDeleteCompliance(c.id)" 
                  class="ghost-btn compact-btn danger-btn"
                >
                  移除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </SectionCard>

      <div v-if="showAddComplianceModal" class="modal-overlay">
        <div class="modal-card">
          <h3>新增合规安全网关规则</h3>
          <div class="form-group">
            <label>合规策略显示名称</label>
            <input type="text" v-model="newCompliance.ruleName" class="text-input" autocomplete="off" spellcheck="false" placeholder="例如: 手机号掩码脱敏, 机密言论拦截" />
          </div>
          <div class="form-group">
            <label>拦截词或匹配正则表达式</label>
            <input type="text" v-model="newCompliance.sensitiveWord" class="text-input" autocomplete="off" spellcheck="false" placeholder="多个用逗号隔开，或填写正则: (13[0-9]|15[0-9])\d{8}" />
          </div>
          <div class="form-group">
            <label>处置动作类型 (Action Type)</label>
            <select v-model="newCompliance.actionType">
              <option value="BLOCK">BLOCK (直接拦截并记录高危日志)</option>
              <option value="DESENSITIZE">DESENSITIZE (对匹配字符进行掩码脱敏)</option>
            </select>
          </div>
          <div class="modal-actions">
            <button @click="showAddComplianceModal = false" class="ghost-btn">取消</button>
            <button @click="handleAddCompliance" class="primary-btn">部署规则</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 开发者 API 秘钥 -->
    <div v-if="activeTab === 'apikeys'" class="page-grid">
      <SectionCard title="开发者 API 密钥" description="用于企业系统通过 API 接入本平台的 Agent 或工作流。密钥只在创建成功时展示一次。">
        <template #action>
          <button @click="handleCreateApiKey" class="primary-btn">新密钥申请</button>
        </template>

        <table class="table-card">
          <thead>
            <tr>
              <th>ID</th>
              <th>所有者</th>
              <th>密钥掩码 (Display Mask)</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>过期时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="k in apiKeys" :key="k.id">
              <td>{{ k.id }}</td>
              <td>{{ k.ownerUser }}</td>
              <td><code>{{ k.apiKeyMask }}</code></td>
              <td><span class="badge status-success">{{ k.status }}</span></td>
              <td>{{ formatTime(k.createdAt) }}</td>
              <td>{{ formatTime(k.expiresAt) }}</td>
              <td>
                <button 
                  @click="handleDeleteApiKey(k.id)" 
                  class="ghost-btn compact-btn danger-btn"
                >
                  吊销
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </SectionCard>

      <div v-if="revealedApiKey" class="reveal-key-box api-key-reveal">
        <h3>API Key 申请成功！</h3>
        <p>请妥善保管此秘钥，安全起见，<b>明文仅展示这一次</b>：</p>
        <div class="key-field">
          <code>{{ revealedApiKey }}</code>
        </div>
      </div>
    </div>

    <!-- 系统审计日志 -->
    <div v-if="activeTab === 'audit'" class="page-grid">
      <SectionCard title="企业行为审计日志" description="记录管理员操作、API 密钥签发、工作流变更和安全拦截。">
        <table class="table-card">
          <thead>
            <tr>
              <th>日志ID</th>
              <th>操作账户</th>
              <th>动作类型</th>
              <th>目标实体</th>
              <th>风险评级</th>
              <th>详细信息</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in auditLogs" :key="l.id">
              <td>{{ l.id }}</td>
              <td><b>{{ l.operatorId }}</b></td>
              <td><code>{{ l.actionType }}</code></td>
              <td>{{ l.targetType }} : {{ l.targetId }}</td>
              <td>
                <span class="badge" :style="{ 
                  color: l.riskLevel === 'P1' ? '#c24149' : (l.riskLevel === 'P2' ? '#a46d13' : '#64778b'), 
                  borderColor: l.riskLevel === 'P1' ? 'rgba(194, 65, 73, 0.3)' : (l.riskLevel === 'P2' ? 'rgba(164, 109, 19, 0.3)' : 'rgba(100, 119, 139, 0.24)'),
                  background: l.riskLevel === 'P1' ? 'rgba(239, 68, 68, 0.05)' : 'none'
                }">
                  {{ l.riskLevel }}
                </span>
              </td>
              <td class="truncate-cell audit-detail-cell">
                {{ l.detailJson }}
              </td>
              <td>{{ formatTime(l.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </SectionCard>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import SectionCard from '../components/SectionCard.vue'
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import http from '../api/http'
import { confirmAction, notify } from '../utils/feedback'

const tenantCode = ref(localStorage.getItem('tenantCode') || '')
const route = useRoute()
const activeTab = ref(String(route.query.tab || ({ '/compliance': 'compliance', '/router': 'routers', '/developer': 'apikeys', '/billing': 'status' }[route.path] || 'status')))

const tabs = [
  { value: 'status', label: '运行状态' },
  { value: 'users', label: '用户管理' },
  { value: 'routers', label: '智能路由' },
  { value: 'compliance', label: '安全合规' },
  { value: 'apikeys', label: '开发秘钥' },
  { value: 'audit', label: '行为审计' }
]

// 数据状态
const opsSnapshot = ref({})
const usageSummary = ref({})
const usageBreakdown = ref([])
const breakdownDimension = ref('MODEL')
const billing = ref({})
const users = ref([])
const routerRules = ref([])
const complianceRules = ref([])
const apiKeys = ref([])
const auditLogs = ref([])
const availableModels = ref([])

// 弹窗开关
const showAddUserModal = ref(false)
const showAddRouterModal = ref(false)
const showAddComplianceModal = ref(false)
const revealedApiKey = ref('')

// 表单状态
const newUser = reactive({ username: '', password: '', nickname: '', role: 'STAFF' })
const newRouter = reactive({ ruleName: '', patternRegex: '', primaryModelKey: '', backupModelKey: '' })
const newCompliance = reactive({ ruleName: '', sensitiveWord: '', actionType: 'BLOCK' })

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return timeStr.replace('T', ' ').substring(0, 19)
}

const loadBreakdown = async () => {
  const response = await http.get('/metrics/breakdown', { params: { days: 30, dimension: breakdownDimension.value } })
  usageBreakdown.value = response.data || []
}

const loadData = async () => {
  try {
    const snapshotRes = await http.get('/ops/snapshot')
    opsSnapshot.value = snapshotRes.data
    const usageRes = await http.get('/metrics/summary', { params: { days: 30 } })
    usageSummary.value = usageRes.data
    await loadBreakdown()
    const modelsRes = await http.get('/system/models')
    availableModels.value = (modelsRes.data || []).filter(model => model.status === 'ACTIVE' && model.credentialConfigured)

    // 读取真实计费配额和系统配置，不在前端补造统计数据。
    const billRes = await http.get('/system/billing')
    billing.value = billRes.data

    // 2. 读取用户列表
    const usersRes = await http.get('/system/users')
    users.value = usersRes.data

    // 3. 读取路由规则
    const routerRes = await http.get('/system/routers')
    routerRules.value = routerRes.data

    // 4. 读取合规规则
    const compRes = await http.get('/system/compliance')
    complianceRules.value = compRes.data

    // 5. 读取开发者秘钥
    const keysRes = await http.get('/system/api-keys')
    apiKeys.value = keysRes.data

    // 6. 读取审计日志
    const auditRes = await http.get('/system/audit-logs')
    auditLogs.value = auditRes.data
  } catch (e) {
    console.error('加载系统配置失败', e)
  }
}

// 用户管理操作
const toggleUserStatus = async (id, currentStatus) => {
  const nextStatus = currentStatus === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await http.put(`/system/users/${id}/status?status=${nextStatus}`)
  await loadData()
}

const handleAddUser = async () => {
  await http.post('/system/users', newUser)
  showAddUserModal.value = false
  newUser.username = ''
  newUser.password = ''
  newUser.nickname = ''
  newUser.role = 'STAFF'
  await loadData()
}

// 路由规则操作
const handleAddRouter = async () => {
  await http.post('/system/routers', newRouter)
  showAddRouterModal.value = false
  newRouter.ruleName = ''
  newRouter.patternRegex = ''
  newRouter.primaryModelKey = ''
  newRouter.backupModelKey = ''
  await loadData()
}

const handleDeleteRouter = async (id) => {
  if (await confirmAction({ title: '注销智能路由', message: '注销后，新的模型请求将不再使用这条路由规则。' })) {
    await http.delete(`/system/routers/${id}`)
    await loadData()
  }
}

// 合规规则操作
const handleAddCompliance = async () => {
  await http.post('/system/compliance', newCompliance)
  showAddComplianceModal.value = false
  newCompliance.ruleName = ''
  newCompliance.sensitiveWord = ''
  newCompliance.actionType = 'BLOCK'
  await loadData()
}

const handleDeleteCompliance = async (id) => {
  if (await confirmAction({ title: '删除合规规则', message: '删除后，该合规规则将不再拦截新的请求。' })) {
    await http.delete(`/system/compliance/${id}`)
    await loadData()
  }
}

// 开发者秘钥操作
const handleCreateApiKey = async () => {
  const res = await http.post('/system/api-keys')
  revealedApiKey.value = res.data.apiKey
  await loadData()
}

const handleDeleteApiKey = async (id) => {
  if (await confirmAction({ title: '吊销 API 密钥', message: '吊销后该密钥立即失效，此操作无法恢复。' })) {
    await http.delete(`/system/api-keys/${id}`)
    await loadData()
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.ops-description { line-height: 1.7; }
.api-key-reveal { margin-top: 20px; }
.audit-detail-cell { max-width: 280px; }
.readiness-link-row { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:12px 14px; border:1px solid var(--line); border-radius:var(--radius); background:var(--panel-muted); color:var(--muted); font-size:12px; }
@media (max-width: 680px) { .readiness-link-row { align-items:flex-start; flex-direction:column; } }
</style>
