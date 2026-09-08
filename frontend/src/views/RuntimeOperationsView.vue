<template>
  <main class="runtime-ops page-grid">
    <PageToolbar eyebrow="运行保障" title="故障处理" description="查看失败任务、处理积压并确认平台运行状态。">
      <template #action><button class="ghost-btn compact-btn" :disabled="loading" @click="loadAll">刷新</button></template>
    </PageToolbar>
    <ResourceContextRail label="运行保障" name="平台运行队列" :state="loading ? '加载中' : '可处理'" hint="选择问题查看处理建议和时间线" mark="O" />

    <section class="health-strip" aria-label="运行状态">
      <div v-for="item in health.components || []" :key="item.code" class="health-item">
        <span class="health-dot" :class="item.status.toLowerCase()"></span>
        <div><strong>{{ item.label }}</strong><small>{{ item.message }}<template v-if="item.value">（{{ item.value }}）</template></small></div>
      </div>
    </section>

    <section class="work-grid">
      <div class="queue-pane">
        <div class="section-heading"><div><h2>待处理问题</h2><p>按条件定位需要人工介入的失败记录。</p></div></div>
        <div class="filters">
          <select v-model="filters.status" class="text-input" @change="search"><option value="">全部状态</option><option value="OPEN">待处理</option><option value="ASSIGNED">处理中</option><option value="REPLAYED">已重放</option><option value="RESOLVED">已解决</option></select>
          <input v-model.trim="filters.releaseId" class="text-input" placeholder="发布版本" @keyup.enter="search" />
          <input v-model.trim="filters.errorCategory" class="text-input" placeholder="问题类型" @keyup.enter="search" />
          <button class="primary-btn compact-btn" @click="search">查询</button>
        </div>
        <PageState v-if="errorMessage" type="error" title="运行数据加载失败" :message="errorMessage" action-label="重新加载" @action="loadAll" /><PageState v-else-if="loading" type="loading" message="正在加载运行记录..." />
        <div v-else-if="!failures.length" class="empty-state">当前没有待处理问题。</div>
        <div v-else class="failure-list">
          <button v-for="item in failures" :key="item.id" class="failure-row" :class="{ active: selected?.id === item.id }" @click="select(item)">
            <span class="status-mark" :class="item.status.toLowerCase()"></span>
            <span class="failure-main"><strong>{{ statusLabel(item.status) }} · {{ item.errorCategory || '运行失败' }}</strong><small>{{ item.safeSummary || '暂无安全摘要' }}</small></span>
            <span class="failure-meta">{{ shortId(item.runId) }}</span>
          </button>
        </div>
        <div class="pager" v-if="total > pageSize"><button class="ghost-btn compact-btn" :disabled="page <= 1" @click="changePage(-1)">上一页</button><span>第 {{ page }} 页，共 {{ Math.ceil(total / pageSize) }} 页</span><button class="ghost-btn compact-btn" :disabled="page * pageSize >= total" @click="changePage(1)">下一页</button></div>
      </div>

      <aside class="detail-pane">
        <template v-if="selected">
          <div class="section-heading"><div><h2>处理详情</h2><p>运行 {{ shortId(selected.runId) }}</p></div></div>
          <dl class="facts"><div><dt>当前状态</dt><dd>{{ statusLabel(selected.status) }}</dd></div><div><dt>发布版本</dt><dd>{{ selected.releaseId || '未记录' }}</dd></div><div><dt>负责人</dt><dd>{{ selected.assignedTo || '未分派' }}</dd></div><div><dt>后续运行</dt><dd>{{ selected.replayRunId ? shortId(selected.replayRunId) : '尚未创建' }}</dd></div></dl>
          <div class="action-bar"><button class="primary-btn" :disabled="submitting" @click="replay">创建新运行</button><button class="ghost-btn" :disabled="submitting" @click="resolve">标记已解决</button></div>
          <div class="timeline"><h3>处理时间线</h3><div v-if="!timeline.length" class="empty-state compact">暂无时间线记录。</div><div v-for="(event, index) in timeline" :key="`${event.occurredAt}-${index}`" class="timeline-row"><span></span><div><strong>{{ eventLabel(event.stage) }}</strong><small>{{ event.safeSummary || statusLabel(event.status) }}</small></div><time>{{ formatTime(event.occurredAt) }}</time></div></div>
        </template>
        <div v-else class="empty-state detail-empty">选择左侧问题查看处理建议和时间线。</div>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import PageState from '../components/PageState.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import { disposeRuntimeFailures, fetchRuntimeFailures, fetchRuntimeOperationsHealth, fetchRuntimeTimeline, replayRuntimeRun } from '../api/runtime'
import { confirmAction, notify } from '../utils/feedback'
import { useMutationState } from '../composables/useMutationState.js'

const PAGE_SIZE = 20
const loading = ref(false), submitting = ref(false), failures = ref([]), selected = ref(null), timeline = ref([]), health = ref({ components: [] })
const errorMessage = ref('')
const mutation = useMutationState()
const page = ref(1), pageSize = PAGE_SIZE, total = ref(0)
const filters = reactive({ status: 'OPEN', releaseId: '', errorCategory: '' })
const statusLabel = value => ({ OPEN: '待处理', ASSIGNED: '处理中', REPLAYED: '已创建新运行', RESOLVED: '已解决', DISMISSED: '已忽略', SUCCEEDED: '成功', FAILED: '失败' }[value] || '未知状态')
const eventLabel = value => ({ ATTEMPT: '任务尝试', MANUAL_ACTION: '人工处理', NODE_STARTED: '步骤开始', NODE_COMPLETED: '步骤完成', NODE_FAILED: '步骤失败' }[value] || '运行事件')
const shortId = value => value ? String(value).slice(0, 8) : '-'
const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 19) : '-'

async function loadFailures() { const response = await fetchRuntimeFailures({ ...filters, page: page.value, pageSize }); failures.value = response.data?.records || []; total.value = response.data?.total || 0 }
async function loadHealth() { const response = await fetchRuntimeOperationsHealth(); health.value = response.data || { components: [] } }
async function loadAll() { loading.value = true; errorMessage.value = ''; try { await Promise.all([loadFailures(), loadHealth()]) } catch (error) { errorMessage.value = error?.message || '运行数据加载失败，请稍后重试。' } finally { loading.value = false } }
async function select(item) { selected.value = item; try { const response = await fetchRuntimeTimeline(item.runId); timeline.value = response.data || [] } catch (error) { errorMessage.value = error?.message || '运行时间线加载失败，请稍后重试。' } }
function search() { page.value = 1; loadFailures() }
function changePage(offset) { page.value += offset; loadFailures() }
async function replay() { if (!await confirmAction({ title: '创建新运行', message: '将使用原发布版本和安全快照创建新运行，原运行记录保持不变。' })) return; submitting.value = true; try { await mutation.run(`runtime-replay-${selected.value.id}`, () => replayRuntimeRun({ runId: selected.value.runId, deadLetterId: selected.value.id, confirmed: true }), loadAll); notify('新运行已创建', 'success') } catch (error) { errorMessage.value = error?.message || '创建新运行失败，请稍后重试。' } finally { submitting.value = false } }
async function resolve() { if (!await confirmAction({ title: '标记已解决', message: '该记录将从待处理列表移出，并保留审计记录。' })) return; submitting.value = true; try { await mutation.run(`runtime-resolve-${selected.value.id}`, () => disposeRuntimeFailures({ deadLetterIds: [selected.value.id], type: 'RESOLVE', nextStatus: 'RESOLVED', reason: '运营人员确认问题已解决', confirmed: true }), async () => { selected.value = null; timeline.value = []; await loadAll() }); notify('已标记为解决', 'success') } catch (error) { errorMessage.value = error?.message || '标记解决失败，请稍后重试。' } finally { submitting.value = false } }
onMounted(loadAll)
</script>

<style scoped>
.runtime-ops { --ops-line: #dbe2e8; }
.health-strip { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); border: 1px solid var(--ops-line); background: #fff; }
.health-item { min-width: 0; display: flex; gap: 9px; align-items: flex-start; padding: 12px 14px; border-right: 1px solid var(--ops-line); }
.health-item:last-child { border-right: 0; }.health-item strong,.health-item small { display:block; }.health-item strong { font-size:13px; }.health-item small { margin-top:3px;color:var(--muted);font-size:11px;line-height:1.4; }
.health-dot { width:8px;height:8px;margin-top:4px;border-radius:50%;background:#2d8a63;flex:none; }.health-dot.degraded{background:#c2872c}.health-dot.unavailable{background:#c64d59}
.work-grid { display:grid;grid-template-columns:minmax(0,1.15fr) minmax(320px,.85fr);min-height:560px;border:1px solid var(--ops-line);background:#fff; }
.queue-pane,.detail-pane { min-width:0;padding:18px; }.detail-pane{border-left:1px solid var(--ops-line);background:#fafbfc}.section-heading h2{font-size:16px;margin:0}.section-heading p{font-size:12px;color:var(--muted);margin:4px 0 0}
.filters{display:grid;grid-template-columns:150px 1fr 1fr auto;gap:8px;margin:16px 0 12px}.filters .text-input{height:34px;font-size:12px}
.failure-list{border-top:1px solid var(--ops-line)}.failure-row{width:100%;display:grid;grid-template-columns:8px 1fr auto;gap:10px;align-items:center;padding:12px 8px;border:0;border-bottom:1px solid var(--ops-line);background:transparent;text-align:left;cursor:pointer}.failure-row:hover,.failure-row.active{background:#f3f7f8}.status-mark{width:7px;height:28px;background:#c64d59}.status-mark.assigned{background:#c2872c}.status-mark.resolved,.status-mark.replayed{background:#2d8a63}.failure-main strong,.failure-main small{display:block}.failure-main strong{font-size:13px}.failure-main small{font-size:11px;color:var(--muted);margin-top:4px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:520px}.failure-meta{font:11px monospace;color:var(--muted)}
.facts{margin:16px 0;border-top:1px solid var(--ops-line)}.facts div{display:flex;justify-content:space-between;gap:16px;padding:10px 0;border-bottom:1px solid var(--ops-line);font-size:12px}.facts dt{color:var(--muted)}.facts dd{margin:0;text-align:right}.action-bar{display:flex;gap:8px;margin:16px 0}.timeline h3{font-size:13px;margin:22px 0 10px}.timeline-row{display:grid;grid-template-columns:8px 1fr auto;gap:10px;padding:9px 0}.timeline-row>span{width:7px;height:7px;border-radius:50%;background:#608b91;margin-top:5px}.timeline-row strong,.timeline-row small{display:block;font-size:12px}.timeline-row small,.timeline-row time{color:var(--muted);font-size:10px;margin-top:2px}.pager{display:flex;justify-content:flex-end;align-items:center;gap:10px;margin-top:14px;font-size:11px;color:var(--muted)}.detail-empty{padding-top:180px}.empty-state.compact{padding:16px}
@media(max-width:900px){.health-strip{grid-template-columns:repeat(2,1fr)}.health-item{border-bottom:1px solid var(--ops-line)}.work-grid{grid-template-columns:1fr}.detail-pane{border-left:0;border-top:1px solid var(--ops-line)}.filters{grid-template-columns:1fr 1fr}.detail-empty{padding-top:40px}}
@media(max-width:560px){.health-strip,.filters{grid-template-columns:1fr}.health-item{border-right:0}.queue-pane,.detail-pane{padding:14px}.failure-main small{max-width:220px}}
</style>
