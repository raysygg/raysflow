<template>
  <div class="page-grid">
    <PageToolbar eyebrow="平台治理" title="租户控制台" description="管理企业租户、套餐和运行范围。" />
    <ResourceContextRail label="平台治理" name="企业租户目录" state="超级管理员" hint="查看租户状态、套餐和运行范围" mark="T" />
    <SectionCard title="平台租户管理" description="查看和管理已注册企业的状态、配额与运行范围。仅平台超级管理员可访问。">
      <template #action>
        <div class="page-toolbar-actions">
          <RouterLink to="/models" class="primary-btn">配置模型底座</RouterLink>
          <div class="status-chip platform-mode-chip">
            超级管理员模式
          </div>
        </div>
      </template>

      <p v-if="loadError" class="status-message">{{ loadError }}</p>
      <p v-else-if="tenants.length === 0" class="empty-state">暂无企业租户注册数据。</p>
      
      <table v-else class="table-card">
        <thead>
          <tr>
            <th>ID</th>
            <th>企业/组织名称</th>
            <th>租户编码 (Code)</th>
            <th>订阅套餐</th>
            <th>用户上限数</th>
            <th>当前状态</th>
            <th>创建时间</th>
            <th>配置操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in tenants" :key="item.id">
            <td>{{ item.id }}</td>
            <td><b>{{ item.tenantName }}</b></td>
            <td><code>{{ item.tenantCode }}</code></td>
            <td><span class="pill">{{ item.planCode }}</span></td>
            <td>{{ item.userLimit }} 用户</td>
            <td>
              <span class="badge" :style="{ 
                color: item.status === 'ACTIVE' ? '#10b981' : '#ef4444', 
                borderColor: item.status === 'ACTIVE' ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)' 
              }">
                {{ item.status === 'ACTIVE' ? '运行中 (ACTIVE)' : '已冻结 (FROZEN)' }}
              </span>
            </td>
            <td>{{ formatTime(item.createdAt) }}</td>
            <td class="row-actions">
              <button 
                @click="openQuotaModal(item)" 
                class="ghost-btn compact-btn"
              >
                配置额度
              </button>
              <button 
                @click="toggleTenantStatus(item.id, item.status)" 
                class="ghost-btn" 
                :style="{ 
                  color: item.status === 'ACTIVE' ? '#ef4444' : '#10b981', 
                  borderColor: item.status === 'ACTIVE' ? 'rgba(239, 68, 68, 0.2)' : 'rgba(16, 185, 129, 0.2)' 
                }"
              >
                {{ item.status === 'ACTIVE' ? '冻结' : '解冻' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </SectionCard>

    <!-- Quota Editing Modal -->
    <div v-if="showQuotaModal" class="modal-overlay">
      <div class="modal-card">
        <h3>管理租户运行限额 - {{ selectedTenant.name }}</h3>
        
        <div class="form-group">
          <label>每月最大 API 消耗 Token 限额</label>
          <input type="number" v-model="quotaForm.tokenLimit" class="text-input" autocomplete="off" spellcheck="false" placeholder="例如: 1000000" />
        </div>

        <div class="form-group">
          <label>每月工作流运行最大执行次数</label>
          <input type="number" v-model="quotaForm.workflowLimit" class="text-input" autocomplete="off" spellcheck="false" placeholder="例如: 1000" />
        </div>

        <div class="form-group">
          <label>企业云存储空间限制 (MB)</label>
          <input type="number" v-model="quotaForm.storageLimit" class="text-input" autocomplete="off" spellcheck="false" placeholder="例如: 100" />
        </div>

        <div class="modal-actions">
          <button @click="showQuotaModal = false" class="ghost-btn">取消</button>
          <button @click="handleUpdateQuota" class="primary-btn">确认更新</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import SectionCard from '../components/SectionCard.vue'
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import http from '../api/http'
import { confirmAction, notify } from '../utils/feedback'

const tenants = ref([])
const loadError = ref('')
const showQuotaModal = ref(false)
const selectedTenant = ref({})

const quotaForm = reactive({
  tokenLimit: 1000000,
  workflowLimit: 1000,
  storageLimit: 100
})

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return timeStr.replace('T', ' ').substring(0, 19)
}

const loadTenants = async () => {
  try {
    const response = await http.get('/system/tenants')
    tenants.value = response.data || []
  } catch (error) {
    loadError.value = '无法读取租户数据，您不是平台超级管理员，或后端服务未启动。'
  }
}

const toggleTenantStatus = async (id, currentStatus) => {
  const nextStatus = currentStatus === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const actionText = currentStatus === 'ACTIVE' ? '冻结' : '解冻'
  if (await confirmAction({ title: `${actionText}租户`, message: `确定要${actionText}该租户的所有系统和服务吗？` })) {
    await http.put(`/system/tenants/${id}/status?status=${nextStatus}`)
    await loadTenants()
  }
}

const openQuotaModal = (tenant) => {
  selectedTenant.value = tenant
  // 新建租户表单的默认值，实际租户数据仍由后端返回。
  quotaForm.tokenLimit = 1000000
  quotaForm.workflowLimit = 1000
  quotaForm.storageLimit = 100
  showQuotaModal.value = true
}

const handleUpdateQuota = async () => {
  await http.put(`/system/tenants/${selectedTenant.value.id}/quota`, quotaForm)
  showQuotaModal.value = false
  notify('用量限额配置更新成功。', 'success')
  await loadTenants()
}

onMounted(() => {
  loadTenants()
})
</script>

<style scoped>
.platform-mode-chip { margin: 0; border: 1px solid #f0c9cc; background: #fff4f5; color: #b33e48; }
</style>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(16, 34, 56, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(3px);
}

.modal-card {
  width: 100%;
  max-width: 440px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 28px;
  box-shadow: 0 18px 50px rgba(20, 42, 66, 0.18);
}

.modal-card h3 {
  margin: 0 0 16px 0;
  color: var(--text);
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}
</style>
