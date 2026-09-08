<template>
  <div class="page-grid task-inbox">
    <PageToolbar eyebrow="待办队列" title="任务中心" description="集中处理审批、补充材料、转交和可恢复运行任务。">
      <template #actions><button class="ghost-btn" type="button" @click="loadTasks">刷新</button></template>
    </PageToolbar>
    <div class="task-summary-row"><div><strong>{{ total }}</strong><span>待处理任务</span></div><div><strong>{{ counts.APPROVAL }}</strong><span>审批</span></div><div><strong>{{ counts.SUPPLEMENT }}</strong><span>补充材料</span></div><div><strong>{{ recoverableTasks.length }}</strong><span>可恢复运行</span></div></div>
    <div class="task-tabs" role="tablist"><button v-for="tab in tabs" :key="tab.value" type="button" :class="{ active: activeType === tab.value }" @click="activeType = tab.value">{{ tab.label }}</button></div>
    <PageState v-if="loading" type="loading" title="正在读取任务" message="正在同步当前用户可处理的任务" />
    <PageState v-else-if="loadError" type="error" title="任务暂时不可用" :message="loadError" action-label="重新加载" @action="loadTasks" />
    <PageState v-else-if="visibleTasks.length === 0 && recoverableTasks.length === 0" type="empty" title="暂时没有待处理任务" message="新的审批、补充材料或运行恢复任务会出现在这里。" />
    <section v-if="visibleTasks.length" class="task-list-panel">
      <article v-for="task in visibleTasks" :key="`${task.source}-${task.id}`" class="task-row">
        <div class="task-row-main"><div class="task-row-title"><StatusBadge :status="task.taskType" /><strong>{{ task.title }}</strong></div><p>{{ task.description || '需要业务人员继续处理该任务。' }}</p><small>{{ task.owner || '当前工作区' }} · {{ task.nextAction === 'APPROVE' ? '等待处理' : '查看详情' }}</small></div>
        <div class="task-row-actions">
          <details v-if="task.request || task.analysis || task.ragContext" class="task-details"><summary>查看上下文</summary><p v-if="task.request"><b>业务请求</b>{{ task.request }}</p><p v-if="task.analysis"><b>分析摘要</b>{{ task.analysis }}</p><p v-if="task.ragContext"><b>知识依据</b>{{ task.ragContext }}</p></details>
          <template v-if="task.status === 'PENDING' && task.taskType === 'APPROVAL'"><textarea v-model="decisionComments[task.id]" class="decision-input" rows="2" placeholder="填写处理意见" /><div><button class="primary-btn" type="button" @click="handleAction(task.id, 'APPROVED')">通过</button><button class="ghost-btn danger-btn" type="button" @click="handleAction(task.id, 'REJECTED')">驳回</button></div></template>
          <span v-else class="muted-action">{{ task.status === 'PENDING' ? '待补充后继续' : '已处理' }}</span>
        </div>
      </article>
    </section>
    <section v-if="activeType === 'ALL' || activeType === 'RECOVERY'" class="recovery-panel">
      <PageToolbar title="可恢复运行" description="从中断节点继续，或取消不再需要的运行。" />
      <div v-if="recoverableTasks.length === 0" class="muted-action">当前没有可恢复运行。</div>
      <article v-for="item in recoverableTasks" :key="item.executionId" class="task-row"><div><strong>{{ item.executionId }}</strong><p>{{ item.status }} · {{ item.currentNodeId || '等待调度' }}</p></div><div class="task-row-actions"><button class="primary-btn" type="button" @click="controlRecovery(item.executionId, 'RESUME')">继续运行</button><button class="ghost-btn danger-btn" type="button" @click="controlRecovery(item.executionId, 'CANCEL')">取消运行</button></div></article>
    </section>
    <div v-if="hasNext" class="pagination-bar"><button class="ghost-btn" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button><span>第 {{ page }} 页</span><button class="ghost-btn" type="button" :disabled="!hasNext" @click="changePage(page + 1)">下一页</button></div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import PageState from '../components/PageState.vue'
import PageToolbar from '../components/PageToolbar.vue'
import StatusBadge from '../components/StatusBadge.vue'
import http from '../api/http'
import { controlRuntimeRun, fetchRuntimeRuns } from '../api/runtime'
import { notify } from '../utils/feedback'

const tabs = [{ value: 'ALL', label: '全部' }, { value: 'APPROVAL', label: '审批' }, { value: 'SUPPLEMENT', label: '补充材料' }, { value: 'TRANSFER', label: '转交' }, { value: 'RECOVERY', label: '运行恢复' }]
const activeType = ref('ALL')
const page = ref(1)
const total = ref(0)
const hasNext = ref(false)
const loading = ref(false)
const loadError = ref('')
const tasks = ref([])
const recoverableTasks = ref([])
const decisionComments = ref({})
const counts = computed(() => tasks.value.reduce((result, item) => { result[item.taskType] = (result[item.taskType] || 0) + 1; return result }, { APPROVAL: 0, SUPPLEMENT: 0, TRANSFER: 0 }))
const visibleTasks = computed(() => activeType.value === 'ALL' || activeType.value === 'RECOVERY' ? tasks.value : tasks.value.filter(item => item.taskType === activeType.value))

const loadTasks = async () => {
  loading.value = true
  loadError.value = ''
  try {
    let taskResponse
    taskResponse = await http.get('/commercial/tasks', { params: { page: page.value, size: 20 } })
    const recoveryResponse = await fetchRuntimeRuns()
    if (!taskResponse.success) throw new Error(taskResponse.message || '任务读取失败')
    const result = taskResponse.data || {}
    tasks.value = result.items || []
    total.value = result.total || 0
    hasNext.value = Boolean(result.hasNext)
    if (recoveryResponse.success) recoverableTasks.value = (recoveryResponse.data?.items || recoveryResponse.data || []).filter(item => ['QUEUED', 'RUNNING', 'PAUSING'].includes(item.status))
  } catch (error) { loadError.value = error.message || '任务读取失败，请稍后重试' } finally { loading.value = false }
}
const changePage = nextPage => { page.value = nextPage; loadTasks() }
const handleAction = async (id, action) => { const comment = (decisionComments.value[id] || '').trim(); if (!comment) { notify('请先填写处理意见', 'warning'); return } try { await http.post(`/commercial/approvals/${id}/action`, { action, comment }); notify(action === 'APPROVED' ? '任务已通过' : '任务已驳回', 'success'); await loadTasks() } catch (error) { notify(error.message || '任务处理失败', 'error') } }
const controlRecovery = async (executionId, action) => { try { await controlRuntimeRun(executionId, { action }); notify(action === 'RESUME' ? '运行已重新排队' : '运行已取消', 'success'); await loadTasks() } catch (error) { notify(error.message || '运行控制失败', 'error') } }
watch(activeType, () => { if (activeType.value !== 'RECOVERY') loadTasks() })
onMounted(loadTasks)
</script>

<style scoped>
.task-summary-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }.task-summary-row div { padding: 14px 16px; border: 1px solid var(--line); background: var(--panel); border-radius: var(--radius); }.task-summary-row strong, .task-summary-row span { display: block; }.task-summary-row strong { color: var(--text); font-size: 21px; }.task-summary-row span { margin-top: 4px; color: var(--muted); font-size: 12px; }.task-tabs { display: flex; gap: 4px; border-bottom: 1px solid var(--line); }.task-tabs button { border: 0; border-bottom: 2px solid transparent; padding: 10px 14px; background: transparent; color: var(--muted); cursor: pointer; }.task-tabs button.active { border-bottom-color: var(--accent); color: var(--text); font-weight: 700; }.task-list-panel, .recovery-panel { display: grid; gap: 10px; }.task-row { display: flex; justify-content: space-between; gap: 18px; padding: 16px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--panel); }.task-row-title { display: flex; align-items: center; gap: 8px; }.task-row-main { min-width: 0; }.task-row-main p, .task-row p { margin: 7px 0; color: var(--muted); font-size: 12px; line-height: 1.5; }.task-row small { color: var(--muted); }.task-row-actions { display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex-wrap: wrap; min-width: 220px; }.task-row-actions > div { display: flex; gap: 8px; }.decision-input { width: 190px; resize: vertical; }.task-details { width: 100%; color: var(--muted); font-size: 12px; }.task-details summary { cursor: pointer; color: var(--accent); }.task-details p b { display: block; color: var(--text); }.recovery-panel { padding-top: 10px; border-top: 1px solid var(--line); }.pagination-bar { display: flex; justify-content: flex-end; align-items: center; gap: 10px; color: var(--muted); font-size: 12px; }
@media (max-width: 760px) { .task-summary-row { grid-template-columns: repeat(2, 1fr); }.task-row { align-items: stretch; flex-direction: column; }.task-row-actions { justify-content: flex-start; min-width: 0; }.decision-input { width: 100%; flex: 1 1 100%; }.task-details { width: 100%; } }
</style>
