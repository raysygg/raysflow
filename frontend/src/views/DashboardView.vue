<template>
  <div class="page-grid dashboard-page">
    <PageToolbar
      eyebrow="企业工作台"
      :title="workbench?.projectName || '企业运营工作台'"
      :description="isPlatformScope ? '掌握租户健康、应用发布与跨企业运行风险。' : '从可运行应用和待办开始，快速推进今天的业务。'"
    >
      <template #actions>
        <RouterLink v-if="!isPlatformScope" to="/applications" class="primary-btn">查看业务应用</RouterLink>
        <RouterLink to="/workflow-executions" class="ghost-btn">打开运行中心</RouterLink>
      </template>
    </PageToolbar>

    <PageState v-if="loading" type="loading" title="正在准备工作台" message="正在同步应用、待办和运行状态" />
    <PageState v-else-if="loadError" type="error" title="工作台暂时不可用" :message="loadError" action-label="重新加载" @action="loadWorkbench" />

    <template v-else>
      <section v-if="!isPlatformScope" class="dashboard-focus">
        <div class="focus-copy">
          <span class="eyebrow">今日工作</span>
          <h3>先处理最重要的业务动作</h3>
          <p>应用入口、待办任务和最近请求集中在这里，技术细节会在对应工作区展开。</p>
        </div>
        <div class="focus-stats">
          <div><strong>{{ availableApplications.length }}</strong><span>可运行应用</span></div>
          <div><strong>{{ assignedTasks.length }}</strong><span>待处理任务</span></div>
          <div><strong>{{ myRequests.length }}</strong><span>我的请求</span></div>
        </div>
      </section>

      <div v-if="!isPlatformScope" class="dashboard-columns">
        <SectionCard title="可运行应用" description="按业务入口启动已经发布的应用。">
          <div v-if="availableApplications.length" class="dashboard-list">
            <RouterLink v-for="item in availableApplications.slice(0, 5)" :key="item.appId" :to="{ path: `/applications/${item.appId}` }" class="dashboard-row">
              <span class="row-mark">↗</span>
              <span class="row-main"><strong>{{ item.name || '未命名应用' }}</strong><small>最近更新 {{ formatDate(item.updatedAt) }}</small></span>
              <StatusBadge label="可运行" tone="success" />
            </RouterLink>
          </div>
          <PageState v-else type="empty" title="还没有可运行应用" message="发布第一个业务应用后，它会出现在这里。" action-label="进入设计" @action="goToDesign" />
        </SectionCard>

        <SectionCard title="我的待办" description="需要你继续推进的审批与业务任务。">
          <div v-if="assignedTasks.length" class="dashboard-list">
            <RouterLink v-for="item in assignedTasks.slice(0, 5)" :key="item.id" to="/approval" class="dashboard-row">
              <span class="row-mark row-mark-warning">!</span>
              <span class="row-main"><strong>{{ item.title || '待处理任务' }}</strong><small>{{ item.owner || '待分配' }} · {{ item.risk || '常规' }}</small></span>
              <StatusBadge label="待处理" tone="warning" />
            </RouterLink>
          </div>
          <PageState v-else type="empty" title="暂时没有待办" message="新的审批和人工任务会在这里提醒。" />
        </SectionCard>
      </div>

      <div v-else class="dashboard-columns">
        <SectionCard title="平台运行概况" description="跨租户服务与发布状态。">
          <div class="platform-metrics">
            <MetricCard label="租户数" :value="String(metrics.tenants ?? 0)" note="当前平台租户" />
            <MetricCard label="已发布应用" :value="String(metrics.publishedApps ?? 0)" note="正式运行入口" />
            <MetricCard label="运行中" :value="String(metrics.activeExecutions ?? 0)" note="执行实例" />
            <MetricCard label="待审批" :value="String(metrics.pendingApprovals ?? 0)" note="待处理事项" />
          </div>
        </SectionCard>
        <SectionCard title="应用健康" description="需要关注的企业应用。">
          <div v-if="applicationHealth.length" class="dashboard-list">
            <RouterLink v-for="item in applicationHealth.slice(0, 5)" :key="item.appId" to="/applications" class="dashboard-row">
              <span class="row-mark">•</span><span class="row-main"><strong>{{ item.name || '未命名应用' }}</strong><small>{{ item.healthStatus === 'AVAILABLE' ? '运行正常' : '等待发布' }}</small></span><StatusBadge :label="item.healthStatus === 'AVAILABLE' ? '正常' : '待发布'" :tone="item.healthStatus === 'AVAILABLE' ? 'success' : 'warning'" />
            </RouterLink>
          </div>
          <PageState v-else type="empty" title="暂无应用健康数据" message="发布应用后可在此查看状态。" />
        </SectionCard>
      </div>

      <SectionCard title="运行与请求" description="业务状态摘要，进入运行中心查看完整时间线。">
        <div v-if="myRequests.length || activeExecutions.length" class="dashboard-list">
          <RouterLink v-for="item in (myRequests.length ? myRequests : activeExecutions).slice(0, 6)" :key="item.executionId" to="/workflow-executions" class="dashboard-row">
            <span class="row-mark row-mark-cyan">→</span><span class="row-main"><strong>{{ item.applicationName || item.appName || '业务应用请求' }}</strong><small>{{ requestSummary(item) }}</small></span><StatusBadge :label="businessStatus(item.status)" :tone="statusTone(item.status)" />
          </RouterLink>
        </div>
        <PageState v-else type="empty" title="还没有运行请求" message="启动业务应用后，请求状态会在这里汇总。" />
      </SectionCard>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import MetricCard from '../components/MetricCard.vue'
import PageState from '../components/PageState.vue'
import PageToolbar from '../components/PageToolbar.vue'
import SectionCard from '../components/SectionCard.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { fetchWorkbench } from '../api/overview'

const router = useRouter()
const workbench = ref(null)
const loading = ref(true)
const loadError = ref('')
const isPlatformScope = computed(() => workbench.value?.scope === 'PLATFORM')
const metrics = computed(() => workbench.value?.metrics || {})
const availableApplications = computed(() => workbench.value?.availableApplications || [])
const assignedTasks = computed(() => workbench.value?.assignedTasks || [])
const myRequests = computed(() => workbench.value?.myRequests || [])
const activeExecutions = computed(() => workbench.value?.activeExecutions || [])
const applicationHealth = computed(() => workbench.value?.applicationHealth || [])

const loadWorkbench = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const response = await fetchWorkbench()
    workbench.value = response.data || {}
  } catch (error) {
    loadError.value = error?.message || '工作台数据读取失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}
const goToDesign = () => router.push('/applications/new')
const formatDate = value => value ? new Date(value).toLocaleDateString('zh-CN') : '最近'
const businessStatus = status => ({ QUEUED: '排队中', RUNNING: '处理中', WAITING_APPROVAL: '等待审批', SUCCEEDED: '已完成', FAILED: '需要处理' }[status] || status || '处理中')
const requestSummary = item => {
  const nodeLabel = ({ start: '已开始', END: '已结束', end: '已结束' }[item.currentNodeId] || (item.currentNodeId ? '处理中' : '等待下一步处理'))
  return `运行请求 · ${nodeLabel}`
}
const statusTone = status => status === 'FAILED' ? 'danger' : status === 'SUCCEEDED' ? 'success' : status === 'WAITING_APPROVAL' ? 'warning' : 'info'

onMounted(loadWorkbench)
</script>

<style scoped>
.dashboard-page { max-width: 1380px; }
.dashboard-focus { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 24px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--graphite); color: #fff; }
.focus-copy { max-width: 620px; }.focus-copy h3 { margin: 7px 0 6px; font-size: 24px; }.focus-copy p { margin: 0; color: #c8d7d1; font-size: 13px; line-height: 1.6; }.focus-copy .eyebrow { color: #7ce0bd; }
.focus-stats { display: flex; gap: 26px; }.focus-stats div { display: grid; gap: 4px; min-width: 84px; }.focus-stats strong { font-size: 28px; }.focus-stats span { color: #c8d7d1; font-size: 11px; }
.dashboard-columns { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }.dashboard-list { display: grid; gap: 7px; }.dashboard-row { display: flex; align-items: center; gap: 10px; min-height: 54px; padding: 9px 10px; border: 1px solid var(--line); border-radius: 7px; color: var(--text); text-decoration: none; }.dashboard-row:hover { border-color: var(--accent); background: var(--panel-muted); }.row-mark { display: grid; flex: 0 0 26px; place-items: center; width: 26px; height: 26px; border-radius: 6px; background: rgba(30,156,120,.12); color: var(--success); font-weight: 800; }.row-mark-warning { background: rgba(214,168,79,.16); color: #9c731a; }.row-mark-cyan { background: rgba(46,111,128,.13); color: var(--info); }.row-main { display: grid; flex: 1; min-width: 0; gap: 4px; }.row-main strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }.row-main small { color: var(--muted); font-size: 11px; }.platform-metrics { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
@media (max-width: 760px) { .dashboard-focus, .dashboard-columns { display: block; }.focus-stats { margin-top: 20px; gap: 14px; }.focus-stats div { min-width: 0; flex: 1; }.dashboard-columns > * + * { margin-top: 16px; }.platform-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
