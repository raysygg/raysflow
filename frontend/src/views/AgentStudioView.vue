<template>
  <div class="page-grid application-directory">
    <SectionCard
      eyebrow="应用目录"
      title="业务应用"
      description="从一个入口查看企业可用的业务应用、运行状态与下一步动作。"
    >
      <template #action>
        <RouterLink to="/applications/new" class="primary-btn">创建业务应用</RouterLink>
      </template>
    </SectionCard>

    <PageState v-if="loadError" type="error" title="应用暂时无法加载" :message="loadError" action-label="重新加载" @action="loadApplications" />
    <PageState v-else-if="loading" type="loading" title="正在读取应用" message="正在同步当前企业的可用入口" />
    <PageState v-else-if="applications.length === 0" type="empty" title="还没有业务应用" message="从标准流程设计开始，发布后应用会出现在这里。" action-label="进入设计" @action="goToDesign" />

    <div v-else class="agent-grid application-grid">
      <article v-for="application in applications" :key="application.id" class="agent-card application-card">
        <header class="card-head">
          <span class="app-kicker">{{ application.triggerLabel }}</span>
          <StatusBadge :label="application.statusLabel" :tone="application.statusTone" />
        </header>

        <div class="card-body">
          <h3 class="app-title">{{ application.name }}</h3>
          <code class="app-code">{{ application.code }}</code>
        </div>

        <div class="card-health">
          <span class="health-dot" :class="`health-${application.statusTone}`"></span>
          <span class="health-text">{{ application.healthLabel }}</span>
          <span class="health-updated">更新于 {{ application.updatedLabel }}</span>
        </div>

        <ul class="card-meta">
          <li><em>负责人</em><span>{{ application.owner || '待指定' }}</span></li>
          <li><em>发布版本</em><span>{{ application.release || '尚未发布' }}</span></li>
          <li><em>默认交付</em><span>{{ application.deliveryLabel }}</span></li>
        </ul>

        <div class="card-actions">
          <RouterLink :to="{ path: `/applications/${application.id}` }" class="ghost-btn">查看应用</RouterLink>
          <RouterLink :to="{ path: `/applications/${application.id}` }" class="primary-btn">进入运行应用</RouterLink>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import SectionCard from '../components/SectionCard.vue'
import PageState from '../components/PageState.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { fetchRuntimeApplications } from '../api/runtime'
import { APPLICATION_STATUS_META } from '../constants/applicationWorkspace'

const router = useRouter()
const applications = ref([])
const loading = ref(true)
const loadError = ref('')
const statusMeta = status => APPLICATION_STATUS_META[status] || { label: status || '待配置', tone: 'neutral', health: '需要关注' }

const loadApplications = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const response = await fetchRuntimeApplications()
    if (!Array.isArray(response.data)) throw new Error('应用列表数据格式不正确')
    applications.value = (response.data || []).map(item => {
      const meta = statusMeta(item.status)
      return {
        id: item.id,
        name: item.name || item.agentName || '未命名应用',
        code: item.code || item.agentCode || `application-${item.id}`,
        owner: item.ownerTeam,
        release: item.workflowCode ? '当前生产版本' : null,
        triggerLabel: item.agentType === 'WORKFLOW' ? '业务流程' : '对话入口',
        deliveryLabel: item.currentVersionNo ? '由入口自动决定' : '发布后决定',
        statusLabel: meta.label,
        statusTone: meta.tone,
        healthLabel: meta.health,
        updatedLabel: item.updatedAt ? new Date(item.updatedAt).toLocaleDateString('zh-CN') : '最近'
      }
    })
  } catch (error) {
    loadError.value = error?.message || '应用列表读取失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const goToDesign = () => router.push('/applications/new')
onMounted(loadApplications)
</script>

<style scoped>
.application-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.application-card {
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 18px 14px;
  border-radius: 10px;
  border: 1px solid #edf2f7;
  background: #fff;
  transition: all .15s;
}
.application-card:hover {
  border-color: #d7e3ef;
  box-shadow: 0 2px 8px rgba(24, 56, 88, .05);
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.app-kicker {
  color: #059669;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .04em;
}
.card-body {
  margin-top: 10px;
  min-height: 0;
}
.app-title {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.3;
}
.app-code {
  display: inline-block;
  margin-top: 5px;
  color: #64748b;
  font-size: 11px;
  font-family: Consolas, monospace;
}
.card-health {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 12px;
  padding: 7px 10px;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 11px;
  font-weight: 500;
}
.health-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #94a3b8;
}
.health-success { background: #22c55e; box-shadow: 0 0 0 3px rgba(34,197,94,.14); }
.health-warning { background: #f59e0b; box-shadow: 0 0 0 3px rgba(245,158,11,.14); }
.health-neutral { background: #64748b; }
.health-text { flex: 1; }
.health-updated { color: #94a3b8; font-weight: 500; }
.card-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 16px;
  margin: 12px 0 0;
  padding: 12px 0 0;
  list-style: none;
  border-top: 1px dashed #e2e8f0;
}
.card-meta li {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.card-meta em {
  font-style: normal;
  color: #94a3b8;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: .02em;
}
.card-meta span {
  color: #334155;
  font-size: 11px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}
.card-actions .ghost-btn,
.card-actions .primary-btn {
  flex: 1;
  min-height: 30px;
  height: 30px;
  padding: 0 12px;
  font-size: 12px;
  border-radius: 6px;
}
@media (max-width: 880px) {
  .application-grid { grid-template-columns: 1fr; }
}
</style>
