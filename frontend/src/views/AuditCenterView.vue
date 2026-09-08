<template>
  <div class="page-grid">
    <PageToolbar eyebrow="企业治理" title="审计与风控" description="查看管理员操作、资源授权和安全事件。" />
    <ResourceContextRail label="审计范围" name="企业安全事件" state="持续记录" hint="按业务操作和安全事件追踪审计证据" mark="S" />
    <SectionCard title="企业运行与风控审计" description="记录所有系统管理员操作、API 密钥生命周期以及安全合规网关拦截拦截。">
      
      <p v-if="loadError" class="status-message">{{ loadError }}</p>
      <p v-else-if="auditEvents.length === 0" class="empty-state">当前租户暂无任何审计事件产生。</p>
      
      <table v-else class="table-card">
        <thead>
          <tr>
            <th>审计ID</th>
            <th>操作账户</th>
            <th>动作类型</th>
            <th>目标对象</th>
            <th>风控评级</th>
            <th>事件时间</th>
            <th>原始日志详情</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in auditEvents" :key="item.id">
            <td>{{ item.id }}</td>
            <td><b>{{ item.operatorId }}</b></td>
            <td><code>{{ item.actionType }}</code></td>
            <td>{{ item.targetType }} : {{ item.targetId }}</td>
            <td>
              <span class="badge" :style="{ 
                  color: item.riskLevel === 'P1' ? '#c24149' : (item.riskLevel === 'P2' ? '#a46d13' : '#64778b'), 
                  borderColor: item.riskLevel === 'P1' ? 'rgba(194, 65, 73, 0.3)' : (item.riskLevel === 'P2' ? 'rgba(164, 109, 19, 0.3)' : 'rgba(100, 119, 139, 0.24)'),
                background: item.riskLevel === 'P1' ? 'rgba(239, 68, 68, 0.05)' : 'none'
              }">
                {{ item.riskLevel }}
              </span>
            </td>
            <td>{{ formatTime(item.createdAt) }}</td>
            <td class="truncate-cell" :title="item.detailJson">
              {{ item.detailJson }}
            </td>
          </tr>
        </tbody>
      </table>
    </SectionCard>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import SectionCard from '../components/SectionCard.vue'
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import http from '../api/http'

const auditEvents = ref([])
const loadError = ref('')

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return timeStr.replace('T', ' ').substring(0, 19)
}

const loadAuditEvents = async () => {
  try {
    const response = await http.get('/system/audit-logs')
    auditEvents.value = response.data || []
  } catch (error) {
    loadError.value = '无法读取审计数据，请检查网络或后端服务配置。'
  }
}

onMounted(() => {
  loadAuditEvents()
})
</script>
