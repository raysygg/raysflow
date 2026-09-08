<template>
  <div class="page-grid readiness-page">
    <PageToolbar
      eyebrow="平台就绪检查"
      title="平台就绪诊断"
      description="先确认依赖、运行策略和节点能力，再开始发布应用。"
    >
      <template #actions>
        <RouterLink to="/ops" class="ghost-btn">返回运行观测</RouterLink>
        <button type="button" class="primary-btn" :disabled="loading" @click="loadReadiness">
          {{ loading ? '检查中...' : '重新检查' }}
        </button>
      </template>
    </PageToolbar>

    <PageState
      v-if="errorMessage"
      type="error"
      title="诊断暂时不可用"
      :message="errorMessage"
      action-label="重试"
      @action="loadReadiness"
    />

    <template v-else>
      <section class="readiness-summary section-card">
        <div>
          <span class="section-eyebrow">CURRENT STATUS</span>
          <h3>{{ overallLabel }}</h3>
          <p>最近检查：{{ checkedAtLabel }}</p>
        </div>
        <div class="readiness-counts">
          <span><strong>{{ summary.total || 0 }}</strong><small>检查项</small></span>
          <span class="danger-count"><strong>{{ summary.unavailable || 0 }}</strong><small>不可用</small></span>
          <span class="warning-count"><strong>{{ summary.degraded || 0 }}</strong><small>需关注</small></span>
        </div>
      </section>

      <section class="readiness-grid">
        <article v-for="item in checks" :key="item.key" class="readiness-card" :class="`status-${String(item.status).toLowerCase()}`">
          <div class="readiness-card-head">
            <div>
              <span class="readiness-key">{{ item.key }}</span>
              <h3>{{ item.label }}</h3>
            </div>
            <span class="status-pill">{{ statusLabel(item.status) }}</span>
          </div>
          <p>{{ item.message }}</p>
          <footer>
            <span>耗时 {{ item.latencyMs || 0 }} ms</span>
            <span v-if="item.details?.activeCount !== undefined">启用 {{ item.details.activeCount }}</span>
            <span v-if="item.details?.nodeCount !== undefined">节点 {{ item.details.nodeCount }}</span>
          </footer>
        </article>
      </section>

      <PageState
        v-if="!loading && checks.length === 0"
        type="empty"
        title="暂无诊断结果"
        message="点击重新检查获取平台状态。"
        action-label="重新检查"
        @action="loadReadiness"
      />
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import PageState from '../components/PageState.vue'
import PageToolbar from '../components/PageToolbar.vue'
import { fetchPlatformReadiness } from '../api/workspace'
import { apiData, apiErrorMessage } from '../api/contracts'

const STATUS_LABELS = Object.freeze({ READY: '已就绪', DEGRADED: '需关注', UNAVAILABLE: '不可用' })
const loading = ref(false)
const errorMessage = ref('')
const readiness = ref({ status: 'UNAVAILABLE', checkedAt: '', checks: [], summary: {} })

const checks = computed(() => readiness.value.checks || [])
const summary = computed(() => readiness.value.summary || {})
const overallLabel = computed(() => STATUS_LABELS[readiness.value.status] || '未检查')
const checkedAtLabel = computed(() => readiness.value.checkedAt ? new Date(readiness.value.checkedAt).toLocaleString() : '尚未检查')
const statusLabel = status => STATUS_LABELS[status] || '未知状态'

const loadReadiness = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await fetchPlatformReadiness()
    if (!response?.success) throw new Error(response?.message || '平台诊断请求失败')
    readiness.value = apiData(response, readiness.value)
  } catch (error) {
    errorMessage.value = apiErrorMessage(error, '平台诊断请求失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(loadReadiness)
</script>

<style scoped>
.readiness-summary { display:flex; align-items:center; justify-content:space-between; gap:24px; }
.readiness-summary h3 { margin:6px 0; font-size:24px; color:var(--text); }
.readiness-summary p { margin:0; color:var(--muted); font-size:12px; }
.readiness-counts { display:flex; gap:24px; }
.readiness-counts span { display:grid; gap:4px; min-width:72px; text-align:center; }
.readiness-counts strong { color:var(--text); font-size:24px; }
.readiness-counts small { color:var(--muted); font-size:11px; }
.danger-count strong { color:var(--danger); }
.warning-count strong { color:var(--warning, #b7791f); }
.readiness-grid { display:grid; grid-template-columns:repeat(2, minmax(0, 1fr)); gap:12px; }
.readiness-card { padding:16px; border:1px solid var(--line); border-left:4px solid var(--muted); border-radius:var(--radius); background:var(--panel); }
.readiness-card.status-ready { border-left-color:#2f8f68; }
.readiness-card.status-degraded { border-left-color:#b7791f; }
.readiness-card.status-unavailable { border-left-color:var(--danger); }
.readiness-card-head { display:flex; align-items:flex-start; justify-content:space-between; gap:12px; }
.readiness-key { color:var(--muted); font-size:10px; text-transform:uppercase; }
.readiness-card h3 { margin:4px 0 0; color:var(--text); font-size:16px; }
.readiness-card p { min-height:40px; margin:14px 0; color:var(--muted); font-size:12px; line-height:1.7; }
.readiness-card footer { display:flex; gap:14px; color:var(--muted); font-size:11px; }
.status-pill { padding:4px 8px; border:1px solid var(--line); border-radius:999px; color:var(--text); font-size:11px; white-space:nowrap; }
@media (max-width: 760px) { .readiness-summary { display:block; }.readiness-counts { margin-top:18px; justify-content:space-between; }.readiness-grid { grid-template-columns:1fr; } }
</style>
