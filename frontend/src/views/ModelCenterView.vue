<template>
  <div class="page-grid model-center-container">
    <PageToolbar eyebrow="模型治理" title="模型底座管理" description="集中管理对话模型、向量模型与重排模型连接，提供连接诊断与生命周期管理。" />
    <ResourceContextRail label="模型资源" name="企业模型中心" :state="loading ? '加载中' : '可管理'" hint="配置凭证引用后验证连接并启用模型" mark="M" />

    <!-- 顶部 Metric 关键指标看板 -->
    <div class="metric-grid">
      <MetricCard label="对话大模型 (CHAT)" :value="String(chatModels.length)" note="提供自然语言对话与 Agent 推理能力" />
      <MetricCard label="向量模型 (EMBEDDING)" :value="String(embeddingModels.length)" note="提供知识库切片文本向量化能力" />
      <MetricCard label="重排模型 (RERANKER)" :value="String(rerankerModels.length)" note="提供多召回源语义重排序能力" />
      <MetricCard label="在线运行总数" :value="String(activeModels.length)" note="当前正参与全平台调度的服务连接" />
    </div>

    <!-- 三大类模型快速切片 Tab 栏 -->
    <div class="category-segmented-bar">
      <button class="cat-tab" :class="{ active: capabilityFilter === 'ALL' }" @click="capabilityFilter = 'ALL'">
        <span class="cat-name">全部模型</span>
        <span class="cat-count">{{ models.length }}</span>
      </button>
      <button class="cat-tab" :class="{ active: capabilityFilter === 'CHAT' }" @click="capabilityFilter = 'CHAT'">
        <span class="cat-icon">💬</span>
        <span class="cat-name">对话模型 (Chat)</span>
        <span class="cat-count">{{ chatModels.length }}</span>
      </button>
      <button class="cat-tab" :class="{ active: capabilityFilter === 'EMBEDDING' }" @click="capabilityFilter = 'EMBEDDING'">
        <span class="cat-icon">📐</span>
        <span class="cat-name">向量模型 (Embedding)</span>
        <span class="cat-count">{{ embeddingModels.length }}</span>
      </button>
      <button class="cat-tab" :class="{ active: capabilityFilter === 'RERANKER' }" @click="capabilityFilter = 'RERANKER'">
        <span class="cat-icon">⚡</span>
        <span class="cat-name">重排模型 (Reranker)</span>
        <span class="cat-count">{{ rerankerModels.length }}</span>
      </button>
    </div>

    <!-- 主数据面板卡片 -->
    <SectionCard>
      <!-- 自定义沉浸式 Header 工具栏 -->
      <template #action>
        <div class="panel-header-toolbar">
          <div class="panel-title-area">
            <h4 class="panel-title">{{ categoryTitleLabel }}列表</h4>
            <span class="panel-subtitle">所有模型凭证均通过 AES 加密存储，支持实时探针诊断。</span>
          </div>

          <div class="panel-controls">
            <div class="control-item search-item">
              <input v-model="searchFilter" class="text-input" placeholder="搜索模型名称或 Key..." />
            </div>

            <div class="control-item select-item">
              <select v-model="providerFilter" class="text-input">
                <option value="ALL">全部供应商 (All Providers)</option>
                <option v-for="item in PROVIDER_OPTIONS" :key="item.code" :value="item.code">
                  {{ item.label }}
                </option>
              </select>
            </div>

            <div class="segmented-control">
              <button class="seg-btn" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">卡片</button>
              <button class="seg-btn" :class="{ active: viewMode === 'table' }" @click="viewMode = 'table'">表格</button>
            </div>

            <button class="ghost-btn icon-only-btn" title="刷新列表" @click="loadModels">↻</button>
            <button v-if="canManageModels" class="ghost-btn cred-mgr-btn" @click="openCredentialManager">🔑 凭证管理</button>
            <button v-if="canManageModels" class="primary-btn add-btn" @click="openCreate">+ 新增模型连接</button>
          </div>
        </div>
      </template>

      <p v-if="loadError" class="empty-state error-state">{{ loadError }}</p>
      <div v-else-if="filteredModels.length === 0" class="empty-state">
        暂无符合筛选条件的模型连接。
      </div>

      <!-- 模式一：高颜值 Card 网格视图 -->
      <div v-else-if="viewMode === 'grid'" class="model-card-grid">
        <div v-for="model in filteredModels" :key="model.id" class="model-card" :class="{ inactive: model.status !== MODEL_STATUS_ACTIVE }">
          <div class="card-top">
            <div class="provider-pill" :class="'provider-' + model.provider.toLowerCase()">
              <span class="dot"></span>
              <span>{{ providerLabel(model.provider) }}</span>
            </div>
            <span class="scope-pill" :class="model.tenantId === SYSTEM_TENANT_ID ? 'scope-platform' : 'scope-private'">
              {{ model.tenantId === SYSTEM_TENANT_ID ? '平台共享' : '企业私有' }}
            </span>
          </div>

          <div class="card-main">
            <div class="model-heading">
              <h4 class="model-name">{{ model.modelName }}</h4>
              <div class="model-key-row">
                <code class="model-key" title="平台模型唯一标识">{{ model.modelKey }}</code>
                <span v-if="model.upstreamModelName && model.upstreamModelName !== model.modelKey" class="upstream-badge" :title="'调用上游真实模型: ' + model.upstreamModelName">
                  上游: {{ model.upstreamModelName }}
                </span>
              </div>
            </div>

            <div class="cap-row">
              <span class="cap-tag" :class="'cap-' + String(model.modelCapability || 'chat').toLowerCase()">
                {{ capabilityLabel(model.modelCapability) }}模型
              </span>
              <span v-if="model.modelCapability === 'EMBEDDING'" class="dim-tag">
                {{ model.vectorDimension ? model.vectorDimension + ' 维' : '未配置维度' }}
              </span>
            </div>

            <div class="endpoint-box" :title="model.baseUrl">
              <span class="ep-label">Endpoint:</span>
              <span class="ep-val">{{ model.baseUrl || '未配置 BaseURL' }}</span>
            </div>

            <div class="status-row">
              <span class="status-indicator" :class="model.status === MODEL_STATUS_ACTIVE ? 'status-active' : 'status-inactive'">
                <i class="status-dot"></i>{{ modelStatusLabel(model) }}
              </span>
              <span class="credential-info">{{ model.credentialConfigured ? '已绑定安全凭证' : '未绑定凭证' }}</span>
            </div>
          </div>

          <div class="card-actions">
            <template v-if="canEditModel(model)">
              <button class="act-btn test-btn" :disabled="testingId === model.id" @click="testModelDirect(model)">
                {{ testingId === model.id ? '探测中...' : '测试' }}
              </button>
              <button class="act-btn toggle-btn" @click="toggleModelStatus(model)">
                {{ model.status === MODEL_STATUS_ACTIVE ? '禁用' : '启用' }}
              </button>
              <button class="act-btn edit-btn" @click="openEdit(model)">编辑</button>
              <button class="act-btn delete-btn" @click="removeModel(model)">删除</button>
            </template>
            <span v-else class="muted-note">{{ modelManageHint(model) }}</span>
          </div>
        </div>
      </div>

      <!-- 模式二：紧凑 Table 表格视图（可展开查看详情） -->
      <table v-else class="table-card pretty-table compact-table">
        <thead>
          <tr>
            <th width="100">供应商</th>
            <th>模型名称</th>
            <th width="90">分类</th>
            <th width="90">状态</th>
            <th width="160" class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="model in filteredModels" :key="model.id">
            <tr :class="{ 'row-inactive': model.status !== MODEL_STATUS_ACTIVE, 'row-expanded': isExpanded(model.id) }">
              <td>
                <span class="provider-pill compact" :class="'provider-' + model.provider.toLowerCase()">
                  {{ providerLabel(model.provider) }}
                </span>
              </td>
              <td class="expand-cell" @click="toggleExpand(model.id)">
                <div class="name-stack">
                  <div class="name-title-row">
                    <strong class="cell-title" :title="model.modelName">{{ model.modelName }}</strong>
                    <span v-if="model.upstreamModelName && model.upstreamModelName !== model.modelKey" class="upstream-badge compact" :title="'调用上游真实模型: ' + model.upstreamModelName">
                      上游: {{ model.upstreamModelName }}
                    </span>
                  </div>
                  <code class="sub-key">{{ model.modelKey }}</code>
                </div>
                <button class="expand-toggle" :class="{ expanded: isExpanded(model.id) }">
                  {{ isExpanded(model.id) ? '收起' : '展开' }}
                </button>
              </td>
              <td>
                <span class="cap-tag compact" :class="'cap-' + String(model.modelCapability || 'chat').toLowerCase()">
                  {{ capabilityLabel(model.modelCapability) }}
                </span>
              </td>
              <td>
                <span class="status-indicator" :class="model.status === MODEL_STATUS_ACTIVE ? 'status-active' : 'status-inactive'">
                  <i class="status-dot"></i>{{ modelStatusLabel(model) }}
                </span>
              </td>
              <td class="text-right">
                <div v-if="canEditModel(model)" class="table-action-group">
                  <button class="table-link-btn" :disabled="testingId === model.id" @click="testModelDirect(model)">
                    {{ testingId === model.id ? '测试中' : '测试' }}
                  </button>
                  <button class="table-link-btn" @click="toggleModelStatus(model)">
                    {{ model.status === MODEL_STATUS_ACTIVE ? '禁用' : '启用' }}
                  </button>
                  <button class="table-link-btn" @click="openEdit(model)">编辑</button>
                  <button class="table-link-btn danger-link" @click="removeModel(model)">删除</button>
                </div>
                <span v-else class="muted-note">{{ modelManageHint(model) }}</span>
              </td>
            </tr>
            <tr v-if="isExpanded(model.id)" class="detail-row">
              <td colspan="5">
                <div class="detail-panel">
                  <div class="detail-grid">
                    <div class="detail-item">
                      <span class="detail-label">模型标识 Key</span>
                      <code>{{ model.modelKey }}</code>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">上游透传模型</span>
                      <code>{{ model.upstreamModelName || model.modelKey }}</code>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">向量维度</span>
                      <span>{{ model.modelCapability === 'EMBEDDING' ? (model.vectorDimension ? model.vectorDimension + ' 维' : '未配置') : '--' }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">作用范围</span>
                      <span class="scope-pill" :class="model.tenantId === SYSTEM_TENANT_ID ? 'scope-platform' : 'scope-private'">
                        {{ model.tenantId === SYSTEM_TENANT_ID ? '平台共享' : '企业私有' }}
                      </span>
                    </div>
                    <div class="detail-item wide">
                      <span class="detail-label">接口 Endpoint</span>
                      <code :title="model.baseUrl">{{ model.baseUrl || '未配置' }}</code>
                    </div>
                    <div class="detail-item wide">
                      <span class="detail-label">安全凭证</span>
                      <span>{{ model.credentialConfigured ? '已绑定安全凭证引用' : '未配置' }}</span>
                    </div>
                  </div>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </SectionCard>

    <!-- 新增 / 编辑模型 对话框 -->
    <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
      <div class="modal-card dialog-enhanced">
        <header class="modal-header">
          <h3>{{ editingModel ? '编辑模型底座配置' : '新增模型底座配置' }}</h3>
          <p class="modal-subtitle">
            {{ isSuperAdmin ? '当前配置为平台共享模型连接，所有企业租户继承使用。' : '当前配置为企业私有模型连接，限本企业租户内部隔离使用。' }}
          </p>
        </header>

        <div class="form-grid">
          <div class="form-group full-width">
            <label>平台显示名称 (Display Name)</label>
            <input v-model.trim="form.modelName" class="text-input" autocomplete="off" spellcheck="false" placeholder="例如 通义千问 Plus / DeepSeek-V3 / 企业问答助手" />
          </div>

          <div class="form-group">
            <label>供应商 (Provider)</label>
            <select v-model="form.provider" class="text-input" @change="handleProviderChange">
              <option v-for="item in PROVIDER_OPTIONS" :key="item.code" :value="item.code">
                {{ item.label }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>模型分类能力 (Capability)</label>
            <select v-model="form.modelCapability" class="text-input">
              <option v-for="cap in availableCapabilities" :key="cap" :value="cap">
                {{ capabilityLabel(cap) }}模型 ({{ cap }})
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>平台模型标识 (Model Key)</label>
            <input v-model.trim="form.modelKey" class="text-input code-font" autocomplete="off" spellcheck="false" placeholder="例如 deepseek-chat、gpt-5-relay1" />
            <small class="field-help">平台内路由唯一主键，同一租户内保持唯一。</small>
          </div>

          <div class="form-group">
            <label>上游真实模型标识 (Upstream Name，选填)</label>
            <input v-model.trim="form.upstreamModelName" class="text-input code-font" autocomplete="off" spellcheck="false" placeholder="例如 gpt-5.5、gpt-4o（留空同平台标识）" />
            <small class="field-help">向上游真实透传的模型名；留空则与平台标识相同。</small>
          </div>

          <div v-if="form.modelCapability === 'EMBEDDING'" class="form-group full-width">
            <label>向量维度 (Vector Dimension)</label>
            <input v-model.number="form.vectorDimension" class="text-input" type="number" min="1" step="1" placeholder="例如 1536 (OpenAI) / 1024 (BGE) / 768" />
            <small class="field-help">知识库切片向量匹配核心校验项，须与真实模型输出结果保持一致。</small>
          </div>

          <div class="form-group full-width">
            <label>接口 Endpoint (Base URL)</label>
            <input v-model.trim="form.baseUrl" class="text-input code-font" autocomplete="off" spellcheck="false" placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1" />
            <small class="field-help">遵循“配置即生效”原则，请求直接透传至此地址，不做硬编码域名篡改。</small>
          </div>

          <div class="form-group full-width">
            <div class="field-label-row">
              <label>安全凭证引用</label>
              <div class="field-quick-links">
                <button type="button" class="link-action-btn" @click="showCredentialEditor = !showCredentialEditor">
                  {{ showCredentialEditor ? '收起录入' : '+ 填写新凭证' }}
                </button>
                <span class="sep-dot">·</span>
                <button type="button" class="link-action-btn" @click="openCredentialManager">
                  管理已有凭证
                </button>
              </div>
            </div>
            <ResourceSelect
              v-model="form.credentialRefId"
              :options="credentialOptions"
              resource-label="模型凭证"
              placeholder="请选择已保存的模型凭证"
            />
          </div>
          <div v-if="showCredentialEditor" class="credential-editor form-group full-width">
            <div class="editor-header-bar">
              <span class="editor-bar-title">录入新模型凭证</span>
              <button type="button" class="editor-close-btn" @click="showCredentialEditor = false">×</button>
            </div>
            <label>新凭证名称</label>
            <input v-model.trim="newCredential.name" class="text-input" placeholder="例如：企业 OpenAI 生产凭证" />
            <label>新凭证内容 (API Key / Token)</label>
            <input v-model="newCredential.secret" class="text-input" type="password" autocomplete="new-password" placeholder="仅保存一次，后端加密存储，不会再次显示" />
            <div class="editor-action-row">
              <button class="primary-btn sm-btn" type="button" :disabled="creatingCredential" @click="createCredential">
                {{ creatingCredential ? '保存中...' : '保存并绑定当前模型' }}
              </button>
              <button class="ghost-btn sm-btn" type="button" @click="showCredentialEditor = false">取消</button>
            </div>
          </div>
        </div>

        <!-- 探针测试结果提示 -->
        <div v-if="testResult" class="test-result-banner" :class="{ success: testResult.success, fail: !testResult.success }">
          <div class="test-title">
            <span>{{ testResult.success ? '🟢 探针响应成功' : '🔴 探针响应失败' }}</span>
            <span v-if="testResult.latencyMs" class="latency">({{ testResult.latencyMs }}ms)</span>
          </div>
          <div class="test-msg">{{ testResult.message }}</div>
          <div v-if="testResult.sampleOutput" class="test-sample">输出验证: {{ testResult.sampleOutput }}</div>
        </div>

        <footer class="modal-actions">
          <button class="ghost-btn" :disabled="testingConnection" @click="testConnection">
            {{ testingConnection ? '探测中...' : '⚡ 测试连通性' }}
          </button>
          <button class="ghost-btn" @click="dialogVisible = false">取消</button>
          <button class="primary-btn" :disabled="saving" @click="saveModel">{{ saving ? '保存中...' : '保存配置' }}</button>
        </footer>
      </div>
    </div>

    <!-- 模型安全凭证独立维护弹窗 -->
    <div v-if="credentialModalVisible" class="modal-overlay" @click.self="credentialModalVisible = false">
      <div class="modal-card dialog-enhanced cred-manage-modal">
        <header class="modal-header">
          <div class="cred-header-content">
            <h3>模型安全凭证管理</h3>
            <p class="modal-subtitle">
              集中维护所有大模型 API Key 凭证引用。密钥以 AES 高强度加密存储且绝不回传明文。
            </p>
          </div>
          <button class="close-icon-btn" title="关闭" @click="credentialModalVisible = false">×</button>
        </header>

        <div class="cred-manage-container">
          <!-- 凭证录入 / 轮换编辑卡片 -->
          <div class="cred-action-card">
            <div class="cred-card-title-row">
              <span class="cred-section-title">{{ editingCredId ? '编辑 / 轮换凭证密钥' : '录入新模型安全凭证' }}</span>
              <button v-if="editingCredId" type="button" class="cancel-edit-btn" @click="resetCredForm">取消编辑返回录入</button>
            </div>
            <div class="cred-inline-form">
              <div class="inline-field name-field">
                <label>凭证名称</label>
                <input v-model.trim="credForm.name" class="text-input" placeholder="例如：阿里 DashScope 生产 Key" />
              </div>
              <div class="inline-field secret-field">
                <label>{{ editingCredId ? '新密钥内容（若不更新密钥请留空）' : '凭证密钥 (API Key)' }}</label>
                <input v-model="credForm.secret" class="text-input" type="password" autocomplete="new-password" placeholder="仅保存一次，密文安全落盘" />
              </div>
              <div class="inline-action">
                <button class="primary-btn" type="button" :disabled="savingCred" @click="saveCredentialInManager">
                  {{ savingCred ? '保存中...' : (editingCredId ? '确认更新' : '保存凭证') }}
                </button>
              </div>
            </div>
          </div>

          <!-- 凭证列表展示 -->
          <div class="cred-list-section">
            <div class="cred-list-top">
              <span class="list-heading">已保存的凭证引用 ({{ rawCredentials.length }})</span>
              <button class="ghost-btn sm-btn" type="button" :disabled="loadingCreds" @click="loadCredentialsOnly">
                {{ loadingCreds ? '加载中...' : '↻ 刷新列表' }}
              </button>
            </div>

            <div v-if="loadingCreds" class="cred-state-text">正在读取模型安全凭证...</div>
            <div v-else-if="!rawCredentials.length" class="cred-state-text empty">暂无已保存的凭证引用，请在上方直接录入。</div>
            <div v-else class="cred-item-list">
              <div v-for="item in rawCredentials" :key="item.id" class="cred-item-card" :class="{ 'editing-target': editingCredId === item.id }">
                <div class="cred-item-info">
                  <div class="cred-item-title-line">
                    <span class="cred-item-name">{{ item.name }}</span>
                    <span class="cred-status-badge" :class="item.status === 'ACTIVE' ? 'badge-ok' : 'badge-disabled'">
                      {{ item.status === 'ACTIVE' ? '可用' : '已停用' }}
                    </span>
                  </div>

                  <div class="cred-item-binding">
                    <span v-if="item.boundModelCount > 0" class="binding-tag bound">
                      🔗 已绑定 {{ item.boundModelCount }} 个模型: {{ (item.boundModelNames || []).join('、') }}
                    </span>
                    <span v-else class="binding-tag unbound">
                      ⚪ 未被任何模型引用（可安全删除）
                    </span>
                  </div>

                  <div class="cred-item-dates">
                    <span v-if="item.rotatedAt" class="date-item">最近密钥轮换: {{ formatTime(item.rotatedAt) }}</span>
                    <span v-else-if="item.createdAt" class="date-item">创建时间: {{ formatTime(item.createdAt) }}</span>
                  </div>
                </div>

                <div class="cred-item-actions">
                  <button class="ghost-btn sm-btn" type="button" @click="startEditCred(item)">
                    修改 / 轮换密钥
                  </button>
                  <button class="ghost-btn sm-btn danger-btn" type="button" :disabled="deletingCredId === item.id" @click="deleteCredInManager(item)">
                    {{ deletingCredId === item.id ? '删除中...' : '删除' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <footer class="modal-actions">
          <button class="primary-btn" type="button" @click="credentialModalVisible = false">关闭</button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import MetricCard from '../components/MetricCard.vue'
import SectionCard from '../components/SectionCard.vue'
import ResourceSelect from '../components/structured/ResourceSelect.vue'
import http from '../api/http'
import { notify, confirmAction } from '../utils/feedback'

const userRole = ref(localStorage.getItem('role') || 'ADMIN')
const isSuperAdmin = computed(() => userRole.value === 'SUPER_ADMIN')
const isTenantAdmin = computed(() => userRole.value === 'ADMIN' || isSuperAdmin.value)
const canManageModels = computed(() => isTenantAdmin.value)

const SYSTEM_TENANT_ID = 1
const MODEL_STATUS_ACTIVE = 'ACTIVE'

const PROVIDER_OPTIONS = [
  { code: 'OPENAI', label: 'OpenAI 官方', defaultBaseUrl: 'https://api.openai.com/v1', exampleModelKey: 'gpt-4o-mini', capabilities: ['CHAT', 'EMBEDDING', 'RERANKER'] },
  { code: 'ALIBABA_DASHSCOPE', label: '阿里 DashScope (通义千问)', defaultBaseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', exampleModelKey: 'qwen-plus', capabilities: ['CHAT', 'EMBEDDING', 'RERANKER'] },
  { code: 'BAIDU_QIANFAN', label: '百度千帆 (文心一言)', defaultBaseUrl: 'https://qianfan.baidubce.com/v2', exampleModelKey: 'ernie-speed-128k', capabilities: ['CHAT', 'EMBEDDING', 'RERANKER'] },
  { code: 'ZHIPU_AI', label: '智谱 GLM', defaultBaseUrl: 'https://open.bigmodel.cn/api/paas/v4', exampleModelKey: 'glm-4-flash', capabilities: ['CHAT', 'EMBEDDING'] },
  { code: 'DEEPSEEK', label: 'DeepSeek 深度求索', defaultBaseUrl: 'https://api.deepseek.com/v1', exampleModelKey: 'deepseek-chat', capabilities: ['CHAT'] },
  { code: 'DOUBAO', label: '字节豆包 (Volcengine)', defaultBaseUrl: 'https://ark.cn-beijing.volces.com/api/v3', exampleModelKey: 'doubao-pro-128k', capabilities: ['CHAT', 'EMBEDDING'] },
  { code: 'HUNYUAN', label: '腾讯混元 (Hunyuan)', defaultBaseUrl: 'https://api.hunyuan.tencentyun.com/v1', exampleModelKey: 'hunyuan-lite', capabilities: ['CHAT', 'EMBEDDING'] },
  { code: 'MOONSHOT', label: '月之暗面 (Kimi)', defaultBaseUrl: 'https://api.moonshot.cn/v1', exampleModelKey: 'moonshot-v1-8k', capabilities: ['CHAT'] },
  { code: 'ANTHROPIC_CLAUDE', label: 'Anthropic (Claude)', defaultBaseUrl: 'https://api.anthropic.com/v1', exampleModelKey: 'claude-3-5-sonnet-20241022', capabilities: ['CHAT'] },
  { code: 'GOOGLE_GEMINI', label: 'Google Gemini', defaultBaseUrl: 'https://generativelanguage.googleapis.com', exampleModelKey: 'gemini-1.5-flash', capabilities: ['CHAT', 'EMBEDDING'] },
  { code: 'XAI_GROK', label: 'xAI (Grok)', defaultBaseUrl: 'https://api.x.ai/v1', exampleModelKey: 'grok-2-latest', capabilities: ['CHAT'] },
  { code: 'OLLAMA', label: 'Ollama (本地私有化)', defaultBaseUrl: 'http://localhost:11434/v1', exampleModelKey: 'llama3:latest', capabilities: ['CHAT', 'EMBEDDING'] },
]

const models = ref([])
const credentialOptions = ref([])
const rawCredentials = ref([])
const credentialModalVisible = ref(false)
const loadingCreds = ref(false)
const savingCred = ref(false)
const deletingCredId = ref(null)
const editingCredId = ref(null)
const credForm = reactive({ name: '', secret: '' })

const showCredentialEditor = ref(false)
const creatingCredential = ref(false)
const newCredential = reactive({ name: '', secret: '' })
const loadError = ref('')
const saving = ref(false)
const dialogVisible = ref(false)
const editingModel = ref(null)
const viewMode = ref('grid')
const searchFilter = ref('')
const providerFilter = ref('ALL')
const capabilityFilter = ref('ALL') // 'ALL' | 'CHAT' | 'EMBEDDING' | 'RERANKER'
const testingId = ref(null)

const testingConnection = ref(false)
const testResult = ref(null)
const expandedIds = ref(new Set())

const form = reactive({
  modelKey: '',
  upstreamModelName: '',
  modelName: '',
  provider: 'ALIBABA_DASHSCOPE',
  modelCapability: 'CHAT',
  vectorDimension: null,
  baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  credentialRefId: null,
})

const availableCapabilities = computed(() => {
  const selectedObj = PROVIDER_OPTIONS.find(p => p.code === form.provider)
  return selectedObj ? selectedObj.capabilities : ['CHAT', 'EMBEDDING', 'RERANKER']
})

const handleProviderChange = () => {
  const selectedObj = PROVIDER_OPTIONS.find(p => p.code === form.provider)
  if (!selectedObj) return
  if (!selectedObj.capabilities.includes(form.modelCapability)) {
    form.modelCapability = selectedObj.capabilities[0] || 'CHAT'
  }
  if (!form.baseUrl || PROVIDER_OPTIONS.some(p => p.defaultBaseUrl === form.baseUrl)) {
    form.baseUrl = selectedObj.defaultBaseUrl
  }
  if (!form.modelKey || PROVIDER_OPTIONS.some(p => p.exampleModelKey === form.modelKey)) {
    form.modelKey = selectedObj.exampleModelKey
  }
}

const activeModels = computed(() => models.value.filter(model => model.status === MODEL_STATUS_ACTIVE))
const chatModels = computed(() => models.value.filter(model => (model.modelCapability || 'CHAT') === 'CHAT'))
const embeddingModels = computed(() => models.value.filter(model => model.modelCapability === 'EMBEDDING'))
const rerankerModels = computed(() => models.value.filter(model => model.modelCapability === 'RERANKER'))

const filteredModels = computed(() => {
  return models.value.filter(model => {
    const modelCap = model.modelCapability || 'CHAT'
    const matchCap = capabilityFilter.value === 'ALL' || modelCap === capabilityFilter.value
    const matchProvider = providerFilter.value === 'ALL' || model.provider === providerFilter.value
    const keyword = searchFilter.value.trim().toLowerCase()
    const matchKeyword = !keyword || (model.modelName && model.modelName.toLowerCase().includes(keyword))
      || (model.modelKey && model.modelKey.toLowerCase().includes(keyword))
    return matchCap && matchProvider && matchKeyword
  })
})

const categoryTitleLabel = computed(() => {
  return {
    ALL: '全品类模型服务',
    CHAT: '对话大模型 (Chat)',
    EMBEDDING: '向量模型 (Embedding)',
    RERANKER: '重排模型 (Reranker)'
  }[capabilityFilter.value] || '模型服务'
})

const providerLabel = code => {
  const item = PROVIDER_OPTIONS.find(p => p.code === code)
  if (item) {
    return item.label.split(' ')[0]
  }
  return code ? code.replace('_', ' ') : 'OPENAI'
}

const capabilityLabel = value => ({ CHAT: '对话', EMBEDDING: '向量', RERANKER: '重排' }[value] || '其他能力')
const canEditModel = model => {
  if (isSuperAdmin.value) {
    return true
  }
  if (isTenantAdmin.value) {
    return model.tenantId !== SYSTEM_TENANT_ID
  }
  return false
}
const modelStatusLabel = model => {
  if (model.status === MODEL_STATUS_ACTIVE) return '已启用'
  return model.credentialConfigured && model.baseUrl ? '已停用' : '待配置'
}
const modelManageHint = model => model.tenantId === SYSTEM_TENANT_ID ? '仅 SaaS 超管可配置平台共享模型' : '仅企业管理员可配置私有模型'

const isExpanded = id => expandedIds.value.has(id)
const toggleExpand = id => {
  const next = new Set(expandedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedIds.value = next
}

const loadCredentialsOnly = async () => {
  loadingCreds.value = true
  try {
    const res = await http.get('/system/model-credentials')
    rawCredentials.value = res.data || []
    credentialOptions.value = rawCredentials.value.map(item => ({
      value: item.id,
      label: item.name,
      status: item.status === 'ACTIVE' ? '可用' : '已停用'
    }))
  } catch (err) {
    notify(err.message || '凭证列表加载失败。', 'error')
  } finally {
    loadingCreds.value = false
  }
}

const openCredentialManager = async () => {
  credentialModalVisible.value = true
  await loadCredentialsOnly()
}

const resetCredForm = () => {
  editingCredId.value = null
  Object.assign(credForm, { name: '', secret: '' })
}

const startEditCred = (item) => {
  editingCredId.value = item.id
  credForm.name = item.name
  credForm.secret = ''
}

const saveCredentialInManager = async () => {
  if (!credForm.name) {
    notify('请填写凭证名称。', 'warning')
    return
  }
  if (!editingCredId.value && !credForm.secret) {
    notify('录入新凭证必须填写密钥内容。', 'warning')
    return
  }
  savingCred.value = true
  try {
    if (editingCredId.value) {
      await http.put(`/system/model-credentials/${editingCredId.value}`, credForm)
      notify('模型凭证已成功更新/轮换密钥。', 'success')
    } else {
      const res = await http.post('/system/model-credentials', credForm)
      notify('模型凭证已安全保存。', 'success')
      if (dialogVisible.value && res.data) {
        form.credentialRefId = res.data.id
      }
    }
    resetCredForm()
    await loadCredentialsOnly()
    await loadModels()
  } catch (err) {
    notify(err.message || '保存模型凭证失败。', 'error')
  } finally {
    savingCred.value = false
  }
}

const deleteCredInManager = async (item) => {
  const confirmed = await confirmAction({
    title: '删除模型安全凭证',
    message: `确定要删除凭证“${item.name}”吗？若已被模型绑定使用将无法删除。`
  })
  if (!confirmed) return
  deletingCredId.value = item.id
  try {
    await http.delete(`/system/model-credentials/${item.id}`)
    notify(`凭证“${item.name}”已安全删除。`, 'success')
    if (form.credentialRefId === item.id) {
      form.credentialRefId = null
    }
    await loadCredentialsOnly()
    await loadModels()
  } catch (err) {
    notify(err.message || '删除凭证失败。', 'error')
  } finally {
    deletingCredId.value = null
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 19)
}

const loadModels = async () => {
  loadError.value = ''
  try {
    const [response, credentialResponse] = await Promise.all([
      http.get('/system/models', { params: { includeDisabled: true } }),
      http.get('/system/model-credentials')
    ])
    models.value = response.data || []
    rawCredentials.value = credentialResponse.data || []
    credentialOptions.value = rawCredentials.value.map(item => ({
      value: item.id,
      label: item.name,
      status: item.status === 'ACTIVE' ? '可用' : '已停用'
    }))
  } catch (error) {
    loadError.value = error.message || '模型列表读取失败，请检查服务连接。'
  }
}

const resetForm = () => {
  editingModel.value = null
  testResult.value = null
  Object.assign(form, {
    modelKey: 'qwen-plus',
    upstreamModelName: '',
    modelName: '通义千问 Plus',
    provider: 'ALIBABA_DASHSCOPE',
    modelCapability: 'CHAT',
    vectorDimension: null,
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    credentialRefId: null
  })
  showCredentialEditor.value = false
  Object.assign(newCredential, { name: '', secret: '' })
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (model) => {
  editingModel.value = model
  testResult.value = null
  Object.assign(form, {
    modelKey: model.modelKey,
    upstreamModelName: model.upstreamModelName || '',
    modelName: model.modelName,
    provider: model.provider,
    modelCapability: model.modelCapability || 'CHAT',
    vectorDimension: model.vectorDimension || null,
    baseUrl: model.baseUrl || '',
    credentialRefId: model.credentialRefId || null
  })
  showCredentialEditor.value = false
  dialogVisible.value = true
}

const testConnection = async () => {
  if (!form.modelKey || !form.provider || !form.baseUrl) {
    notify('请先填写模型编码、供应商和接口地址。', 'warning')
    return
  }
  testingConnection.value = true
  testResult.value = null
  try {
    const payload = {
      id: editingModel.value ? editingModel.value.id : null,
      modelKey: form.modelKey,
      upstreamModelName: form.upstreamModelName || null,
      modelName: form.modelName || form.modelKey,
      provider: form.provider,
      modelCapability: form.modelCapability,
      baseUrl: form.baseUrl,
      credentialRefId: form.credentialRefId
    }
    const res = await http.post('/system/models/test-connection', payload)
    testResult.value = res.data || { success: false, message: '测试未返回结果' }
  } catch (err) {
    testResult.value = { success: false, message: err.message || '网络连接异常' }
  } finally {
    testingConnection.value = false
  }
}

const testModelDirect = async (model) => {
  testingId.value = model.id
  try {
    const payload = {
      id: model.id,
      modelKey: model.modelKey,
      upstreamModelName: model.upstreamModelName || null,
      modelName: model.modelName,
      provider: model.provider,
      modelCapability: model.modelCapability || 'CHAT',
      baseUrl: model.baseUrl,
      credentialRefId: model.credentialRefId
    }
    const res = await http.post('/system/models/test-connection', payload)
    if (res.data?.success) {
      notify(`模型“${model.modelName}”探针连通成功！响应耗时: ${res.data.latencyMs}ms`, 'success')
    } else {
      const failureMsg = res.data?.message || '接口连接超时或凭证失效'
      notify(`模型“${model.modelName}”探针测试失败: ${failureMsg}`, 'error')
    }
  } catch (err) {
    notify(`连通探针调用失败: ${err.message}`, 'error')
  } finally {
    testingId.value = null
  }
}

const saveModel = async () => {
  const credentialMissing = !form.credentialRefId
  if (!form.modelKey || !form.modelName || !form.provider
    || !form.baseUrl || credentialMissing
    || (form.modelCapability === 'EMBEDDING' && (!form.vectorDimension || form.vectorDimension < 1))) {
    notify('模型编码、显示名称、供应商、接口地址和安全凭证均需完整配置。', 'warning')
    return
  }
  saving.value = true
  try {
    const response = editingModel.value
      ? await http.put(`/system/models/${editingModel.value.id}`, form)
      : await http.post('/system/models', form)
    notify('模型配置保存成功！', 'success')
    dialogVisible.value = false
    await loadModels()
  } catch (error) {
    notify(error.message || '模型配置保存失败。', 'error')
  } finally {
    saving.value = false
  }
}

const createCredential = async () => {
  if (!newCredential.name || !newCredential.secret) {
    notify('请填写凭证名称和内容。', 'warning')
    return
  }
  creatingCredential.value = true
  try {
    const response = await http.post('/system/model-credentials', newCredential)
    const created = response.data
    form.credentialRefId = created.id
    showCredentialEditor.value = false
    Object.assign(newCredential, { name: '', secret: '' })
    await loadCredentialsOnly()
    notify('凭证已保存并绑定到当前模型。', 'success')
  } catch (error) {
    notify(error.message || '凭证保存失败。', 'error')
  } finally {
    creatingCredential.value = false
  }
}

const toggleModelStatus = async (model) => {
  const isCurrentlyActive = model.status === MODEL_STATUS_ACTIVE
  const targetStatus = isCurrentlyActive ? 'INACTIVE' : 'ACTIVE'
  const actionText = isCurrentlyActive ? '禁用' : '启用'
  try {
    const payload = {
      ...model,
      status: targetStatus
    }
    const response = await http.put(`/system/models/${model.id}`, payload)
    notify(`模型“${model.modelName}”已成功${actionText}。`, 'success')
    await loadModels()
  } catch (err) {
    notify(err.message || `模型${actionText}失败。`, 'error')
  }
}

const removeModel = async (model) => {
  if (!await confirmAction({ title: '删除模型配置', message: `删除“${model.modelName}”后，引用该模型的 Agent 与工作流将无法继续调用。` })) return
  await http.delete(`/system/models/${model.id}`)
  notify(`模型“${model.modelName}”已安全删除。`, 'success')
  await loadModels()
}

onMounted(loadModels)
</script>

<style scoped>
.model-center-container { display: grid; gap: 18px; }

/* 核心分类 Tab 筛选条 */
.category-segmented-bar { display: flex; align-items: center; gap: 10px; border-bottom: 1px solid #e2e8f0; padding-bottom: 12px; }
.cat-tab { border: 1px solid #cbd5e1; background: #fff; border-radius: 8px; padding: 8px 16px; font-size: 13px; font-weight: 500; color: #475569; cursor: pointer; display: inline-flex; align-items: center; gap: 8px; transition: all 0.2s; }
.cat-tab:hover { background: #f8fafc; border-color: #94a3b8; color: #0f172a; }
.cat-tab.active { background: #eff6ff; border-color: #3b82f6; color: #1d4ed8; font-weight: 600; box-shadow: 0 2px 6px rgba(59,130,246,0.12); }
.cat-icon { font-size: 14px; }
.cat-count { font-size: 11px; background: #f1f5f9; color: #64748b; padding: 1px 7px; border-radius: 10px; font-weight: 600; }
.cat-tab.active .cat-count { background: #dbeafe; color: #1d4ed8; }

/* 单行 Header 工具栏与组件对齐 */
.panel-header-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; width: 100%; flex-wrap: wrap; }
.panel-title-area { display: flex; flex-direction: column; gap: 3px; }
.panel-title { margin: 0; font-size: 15px; font-weight: 600; color: #0f172a; }
.panel-subtitle { font-size: 11px; color: #64748b; }

.panel-controls { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-item .text-input { width: 170px; height: 32px; font-size: 12px; border-radius: 6px; }
.select-item .text-input { height: 32px; font-size: 12px; border-radius: 6px; padding: 0 8px; }

.segmented-control { display: inline-flex; border: 1px solid #cbd5e1; border-radius: 6px; overflow: hidden; background: #f8fafc; }
.seg-btn { border: 0; background: transparent; padding: 5px 12px; font-size: 12px; color: #64748b; cursor: pointer; transition: all 0.15s; }
.seg-btn.active { background: var(--accent, #1890ff); color: #fff; font-weight: 600; }

.icon-only-btn { width: 32px; height: 32px; min-height: 32px; padding: 0; display: inline-grid; place-items: center; font-size: 14px; }
.add-btn { height: 32px; min-height: 32px; padding: 0 14px; font-size: 12px; white-space: nowrap; }

/* 现代 Card 网格布局 */
.model-card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(330px, 1fr)); gap: 16px; margin-top: 14px; }
.model-card { border: 1px solid #e2e8f0; border-radius: 10px; background: #fff; padding: 16px; display: flex; flex-direction: column; justify-content: space-between; gap: 14px; transition: all 0.2s ease; box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
.model-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,0.06); border-color: #cbd5e1; }
.model-card.inactive { opacity: 0.7; background: #fafbfc; border-style: dashed; }

.card-top { display: flex; align-items: center; justify-content: space-between; }
.provider-pill { display: inline-flex; align-items: center; gap: 6px; padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; background: #f1f5f9; color: #334155; }
.provider-pill .dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }

/* 供应商定制主题色彩 */
.provider-openai { background: #dcfce7; color: #15803d; }
.provider-alibaba_dashscope { background: #f3e8ff; color: #7e22ce; }
.provider-baidu_qianfan { background: #e0f2fe; color: #0369a1; }
.provider-zhipu_ai { background: #cff4fc; color: #055160; }
.provider-deepseek { background: #f1f5f9; color: #0f172a; }
.provider-doubao { background: #ffe4e6; color: #be123c; }
.provider-hunyuan { background: #e0e7ff; color: #3730a3; }
.provider-moonshot { background: #fae8ff; color: #86198f; }
.provider-anthropic_claude { background: #ffedd5; color: #c2410c; }
.provider-google_gemini { background: #fef3c7; color: #b45309; }
.provider-xai_grok { background: #f3f4f6; color: #111827; }
.provider-ollama { background: #ffedd5; color: #9a3412; }

.scope-pill { font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 4px; }
.scope-platform { background: #fffbe6; color: #ad6800; border: 1px solid #ffe58f; }
.scope-private { background: #e6f7ff; color: #096dd9; border: 1px solid #91d5ff; }

.card-main { display: grid; gap: 10px; }
.model-heading { display: flex; flex-direction: column; gap: 4px; }
.model-name { margin: 0; font-size: 15px; color: #0f172a; font-weight: 600; line-height: 1.3; }
.model-key-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.name-title-row { display: flex; align-items: center; gap: 6px; }
.model-key { font-size: 11px; color: #475569; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; display: inline-block; width: fit-content; font-family: Consolas, monospace; }
.upstream-badge { font-size: 10px; color: #0284c7; background: #e0f2fe; border: 1px solid #bae6fd; padding: 1px 6px; border-radius: 4px; font-weight: 500; font-family: Consolas, monospace; white-space: nowrap; }
.upstream-badge.compact { font-size: 9px; padding: 0 4px; line-height: 16px; }

.cap-row { display: flex; align-items: center; gap: 8px; }
.cap-tag { display: inline-flex; align-items: center; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; }
.cap-chat { background: #eff6ff; color: #1d4ed8; }
.cap-embedding { background: #faf5ff; color: #7e22ce; }
.cap-reranker { background: #fff7ed; color: #c2410c; }
.dim-tag { font-size: 10px; color: #64748b; background: #f8fafc; padding: 2px 6px; border: 1px solid #e2e8f0; border-radius: 4px; }

.endpoint-box { font-size: 11px; color: #64748b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; background: #f8fafc; padding: 6px 8px; border-radius: 6px; border: 1px solid #f1f5f9; }
.ep-label { font-weight: 600; margin-right: 4px; color: #475569; }
.ep-val { font-family: Consolas, monospace; }

.status-row { display: flex; align-items: center; justify-content: space-between; font-size: 11px; }
.status-indicator { display: inline-flex; align-items: center; gap: 6px; font-weight: 500; }
.status-indicator.status-active { color: #16a34a; }
.status-indicator.status-inactive { color: #94a3b8; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.credential-info { font-family: Consolas, monospace; font-size: 11px; color: #94a3b8; }

.card-actions { display: flex; align-items: center; gap: 6px; border-top: 1px solid #f1f5f9; padding-top: 12px; }
.act-btn { flex: 1; border: 1px solid #cbd5e1; background: #fff; padding: 6px 0; border-radius: 6px; font-size: 11px; font-weight: 600; color: #334155; cursor: pointer; transition: all 0.15s; text-align: center; }
.act-btn:hover { background: #f1f5f9; border-color: #94a3b8; color: #0f172a; }
.act-btn.test-btn { border-color: #bfdbfe; color: #1d4ed8; background: #eff6ff; }
.act-btn.test-btn:hover { background: #dbeafe; }
.act-btn.delete-btn:hover { background: #fef2f2; color: #dc2626; border-color: #fca5a5; }

/* 表格模式优雅样式 */
.pretty-table { margin-top: 14px; width: 100%; border-collapse: separate; border-spacing: 0; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; background: #fff; }
.pretty-table th { background: #f8fafc; color: #475569; font-size: 12px; font-weight: 600; padding: 12px 14px; border-bottom: 1px solid #e2e8f0; white-space: nowrap; text-align: left; }
.pretty-table td { padding: 14px 12px; font-size: 12px; border-bottom: 1px solid #f1f5f9; color: #334155; vertical-align: middle; }
.pretty-table tr:last-child td { border-bottom: 0; }
.pretty-table tr:hover td { background: #f8fafc; }
.pretty-table .cell-title { color: #0f172a; font-weight: 600; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pretty-table code { font-family: Consolas, monospace; font-size: 11px; color: #475569; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; display: inline-block; word-break: break-all; }
.row-inactive td { opacity: 0.65; }

.provider-pill, .scope-pill, .cap-tag, .status-indicator { white-space: nowrap !important; display: inline-flex; align-items: center; }

.table-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 12px; white-space: nowrap; }
.table-link-btn { border: 0; background: transparent; color: #2563eb; font-size: 12px; font-weight: 600; cursor: pointer; padding: 0; white-space: nowrap; }
.table-link-btn:hover { text-decoration: underline; }
.table-link-btn.danger-link { color: #dc2626; }
.text-right { text-align: right; }

/* 紧凑表格 + 展开详情 */
.compact-table th,
.compact-table tbody > tr:not(.detail-row) > td { white-space: nowrap; }
.compact-table .expand-cell { cursor: pointer; padding-right: 8px; }
.compact-table .expand-cell:hover { background: #f8fafc; }
.compact-table .name-stack { display: flex; flex-direction: column; gap: 3px; }
.compact-table .sub-key { font-family: Consolas, monospace; font-size: 11px; color: #64748b; background: transparent; padding: 0; }
.compact-table .expand-toggle { margin-top: 6px; font-size: 11px; color: #64748b; border: 0; background: transparent; padding: 0; cursor: pointer; }
.compact-table .expand-toggle:hover { color: #2563eb; }
.compact-table .expand-toggle.expanded { color: #2563eb; }
.compact-table tr.row-expanded td { background: #f8fafc; border-bottom: 0; }
.compact-table tr.detail-row td { padding: 0 12px 12px; border-bottom: 1px solid #f1f5f9; }
.detail-panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px 14px; }
.detail-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px 24px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; white-space: normal; }
.detail-item.wide { grid-column: 1 / -1; }
.detail-label { font-size: 11px; color: #64748b; font-weight: 500; }
.detail-item code { font-family: Consolas, monospace; font-size: 11px; color: #475569; background: #f1f5f9; padding: 3px 6px; border-radius: 4px; word-break: break-all; }
.detail-item .scope-pill { width: fit-content; }

/* 对话框表单布局 */
.dialog-enhanced { max-width: 620px; width: 92vw; border-radius: 12px; box-shadow: 0 20px 40px rgba(0,0,0,0.12); }
.modal-header h3 { margin: 0; font-size: 18px; color: #0f172a; }
.modal-subtitle { margin: 6px 0 16px; font-size: 12px; color: #64748b; line-height: 1.5; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.full-width { grid-column: span 2; }
.code-font { font-family: Consolas, monospace; font-size: 12px; }

.test-result-banner { margin: 14px 0 0; padding: 12px 14px; border-radius: 8px; font-size: 12px; line-height: 1.5; }
.test-result-banner.success { background: #f0fdf4; border: 1px solid #bbf7d0; color: #15803d; }
.test-result-banner.fail { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
.test-title { font-weight: 600; display: flex; align-items: center; gap: 8px; }
.test-msg { margin-top: 4px; word-break: break-all; }
.test-sample { margin-top: 4px; font-size: 11px; opacity: 0.85; font-family: monospace; }
.latency { font-weight: normal; opacity: 0.8; font-size: 11px; }

@media (max-width: 900px) {
  .category-segmented-bar { flex-wrap: wrap; }
  .panel-header-toolbar { flex-direction: column; align-items: stretch; }
  .panel-controls { justify-content: flex-start; }
}

/* 凭证管理相关样式 */
.cred-mgr-btn { font-size: 12px; color: #1e293b; background: #f8fafc; border: 1px solid #cbd5e1; }
.cred-mgr-btn:hover { background: #f1f5f9; border-color: #94a3b8; }

.field-label-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.field-quick-links { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.link-action-btn { border: 0; background: transparent; color: #2563eb; cursor: pointer; padding: 0; font-size: 12px; font-weight: 500; }
.link-action-btn:hover { text-decoration: underline; }
.sep-dot { color: #cbd5e1; }

.editor-header-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.editor-bar-title { font-weight: 600; font-size: 12px; color: #1e293b; }
.editor-close-btn { border: 0; background: transparent; font-size: 16px; color: #64748b; cursor: pointer; padding: 0 4px; }
.editor-action-row { display: flex; gap: 8px; margin-top: 10px; }
.sm-btn { padding: 5px 12px; font-size: 12px; }

/* 凭证维护模态弹窗 */
.cred-manage-modal { max-width: 680px; width: 94vw; }
.close-icon-btn { border: 0; background: transparent; font-size: 20px; color: #64748b; cursor: pointer; padding: 0 8px; }
.close-icon-btn:hover { color: #0f172a; }
.cred-manage-container { display: flex; flex-direction: column; gap: 16px; max-height: 65vh; overflow-y: auto; padding: 4px 2px; }

.cred-action-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; }
.cred-card-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.cred-section-title { font-size: 13px; font-weight: 600; color: #0f172a; }
.cancel-edit-btn { border: 0; background: transparent; color: #2563eb; font-size: 12px; cursor: pointer; }
.cancel-edit-btn:hover { text-decoration: underline; }

.cred-inline-form { display: flex; gap: 10px; align-items: flex-end; flex-wrap: wrap; }
.inline-field { display: flex; flex-direction: column; gap: 4px; flex: 1; min-width: 180px; }
.inline-field label { font-size: 11px; color: #64748b; font-weight: 500; }
.inline-action { display: flex; align-items: flex-end; }

.cred-list-section { display: flex; flex-direction: column; gap: 10px; }
.cred-list-top { display: flex; justify-content: space-between; align-items: center; }
.list-heading { font-size: 13px; font-weight: 600; color: #1e293b; }
.cred-state-text { font-size: 12px; color: #64748b; text-align: center; padding: 24px 0; }
.cred-state-text.empty { background: #f8fafc; border-radius: 8px; border: 1px dashed #e2e8f0; }

.cred-item-list { display: flex; flex-direction: column; gap: 8px; }
.cred-item-card { display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; transition: all 0.15s; }
.cred-item-card:hover { border-color: #cbd5e1; box-shadow: 0 2px 6px rgba(0,0,0,0.03); }
.cred-item-card.editing-target { border-color: #3b82f6; background: #eff6ff; }

.cred-item-info { display: flex; flex-direction: column; gap: 4px; flex: 1; min-width: 0; }
.cred-item-title-line { display: flex; align-items: center; gap: 8px; }
.cred-item-name { font-size: 13px; font-weight: 600; color: #0f172a; }
.cred-status-badge { font-size: 11px; padding: 1px 6px; border-radius: 4px; font-weight: 500; }
.cred-status-badge.badge-ok { background: #dcfce7; color: #15803d; }
.cred-status-badge.badge-disabled { background: #f1f5f9; color: #64748b; }

.cred-item-binding { font-size: 12px; }
.binding-tag.bound { color: #2563eb; font-weight: 500; }
.binding-tag.unbound { color: #64748b; }
.cred-item-dates { font-size: 11px; color: #94a3b8; }

.cred-item-actions { display: flex; align-items: center; gap: 8px; white-space: nowrap; margin-left: 12px; }
.danger-btn { color: #dc2626; border-color: #fecaca; }
.danger-btn:hover { background: #fef2f2; border-color: #f87171; }
</style>
