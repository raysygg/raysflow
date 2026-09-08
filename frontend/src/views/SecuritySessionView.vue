<template>
  <div class="page-grid">
    <PageToolbar eyebrow="账号安全" title="设备会话" description="管理当前账号的登录设备和活动会话。" />
    <section class="section-card">
      <div class="section-header">
        <div>
          <h3>设备会话</h3>
          <p class="section-description">查看当前账号的登录设备，发现异常时可以立即下线。</p>
        </div>
        <button class="danger-btn" :disabled="loading" @click="revokeAll">全部设备下线</button>
      </div>
      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success-text">{{ successMessage }}</p>
      <div v-if="loading" class="empty-state">正在读取设备会话...</div>
      <div v-else-if="sessions.length === 0" class="empty-state">暂无设备会话记录。</div>
      <table v-else class="table-card">
        <thead><tr><th>设备标识</th><th>网络地址</th><th>客户端</th><th>最后使用</th><th>状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="session in sessions" :key="session.id">
          <td>{{ session.deviceId || '未提供设备标识' }}</td><td>{{ session.ipAddress || '未知' }}</td>
          <td class="user-agent">{{ session.userAgent || '未知客户端' }}</td><td>{{ formatTime(session.lastUsedAt) }}</td>
          <td><span class="pill">{{ session.status === 'ACTIVE' ? '使用中' : '已下线' }}</span></td>
          <td><button v-if="session.status === 'ACTIVE'" class="ghost-btn compact-btn" @click="revoke(session.id)">下线</button><span v-else>--</span></td>
        </tr></tbody>
      </table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import http from '../api/http'
import { confirmAction } from '../utils/feedback'

const sessions = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const formatTime = (value) => value ? new Date(value).toLocaleString('zh-CN') : '暂无记录'
const loadSessions = async () => {
  loading.value = true; errorMessage.value = ''
  try {
    const response = await http.get('/auth/sessions')
    sessions.value = response.data || []
  } catch (error) { errorMessage.value = error.message || '读取设备会话失败。' }
  finally { loading.value = false }
}
const revoke = async (id) => {
  if (!await confirmAction({ title: '下线设备会话', message: '该设备将立即失去当前登录状态，需要重新登录。' })) return
  await http.delete(`/auth/sessions/${id}`)
  successMessage.value = '设备会话已下线。'; await loadSessions()
}
const revokeAll = async () => {
  if (!await confirmAction({ title: '下线全部设备', message: '当前账号的所有设备会话都会失效，需要重新登录。' })) return
  const response = await http.post('/auth/sessions/revoke-all')
  successMessage.value = `已下线 ${response.data?.revokedCount || 0} 个设备会话。`; await loadSessions()
}
onMounted(loadSessions)
</script>

<style scoped>
.section-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.user-agent { max-width: 320px; word-break: break-word; color: var(--text-secondary); }
.error-text { color: #b42318; margin: 12px 0; }.success-text { color: #087443; margin: 12px 0; }
.danger-btn { border: 1px solid #f1b5ae; color: #b42318; background: #fff7f5; padding: 9px 14px; border-radius: 6px; cursor: pointer; }
@media (max-width: 760px) { .section-header { flex-direction: column; } .table-card { display: block; overflow-x: auto; white-space: nowrap; } }
</style>
