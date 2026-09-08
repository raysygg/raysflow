<template>
  <div class="page-grid">
    <PageToolbar eyebrow="能力市场" title="企业业务能力市场" description="浏览并安装适用于当前租户的行业应用模板和规范流程。" />
    <ResourceContextRail label="安装范围" name="当前企业租户" state="可浏览" hint="安装后进入当前租户运行空间" mark="P" />
    <SectionCard title="企业级业务能力市场" description="在这里，企业可以浏览由服务商提供的行业应用模板和规范流程。点击安装即可部署到当前租户的运行空间。">
      <template #action>
        <div class="status-chip">
          <span>云端中心可用</span>
        </div>
      </template>
    </SectionCard>

    <p v-if="loadError" class="status-message">{{ loadError }}</p>
    <p v-else-if="marketplaceItems.length === 0" class="empty-state">能力市场目前没有任何模板公开。</p>
    
    <div v-else class="agent-grid">
      <article v-for="item in marketplaceItems" :key="item.name" class="agent-card">
        <div class="agent-top">
          <div>
            <h3 class="agent-title">{{ item.name }}</h3>
            <p class="agent-code"><span class="badge">{{ item.type }}</span></p>
          </div>
          <span class="badge status-success">
            {{ item.status }}
          </span>
        </div>
        
        <div class="agent-meta">
          <div class="publisher-row"><strong>模板服务商</strong><span>{{ item.publisher || '官方' }}</span></div>
        </div>

        <div class="marketplace-action">
          <button 
            @click="handleInstall(item)"
            class="primary-btn marketplace-install-btn"
          >
            一键安装部署 &darr;
          </button>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import { onMounted, ref } from 'vue'
import SectionCard from '../components/SectionCard.vue'
import http from '../api/http'
import { notify } from '../utils/feedback'

const marketplaceItems = ref([])
const loadError = ref('')

const loadMarketplace = async () => {
  try {
    const response = await http.get('/commercial/marketplace')
    marketplaceItems.value = response.data || []
  } catch (error) {
    loadError.value = '无法读取能力市场数据，请检查网络或后端服务配置。'
  }
}

const handleInstall = async (item) => {
  try {
    await http.post('/commercial/marketplace/install', { marketplaceItemId: item.id })
    notify(`已创建应用草稿「${item.name}」，请进入应用工作区继续构建、测试和发布。`, 'success')
  } catch (e) {
    notify('模板安装失败，请检查网络连接。', 'error')
  }
}

onMounted(() => {
  loadMarketplace()
})
</script>

<style scoped>
.agent-title { margin: 0; font-size: 15px; }
.agent-code { margin: 5px 0 0; font-size: 12px; }
.publisher-row { display: flex; align-items: center; gap: 10px; color: var(--muted); font-size: 12px; }
.publisher-row strong { color: var(--text); }
.marketplace-action { margin-top: 20px; }
.marketplace-install-btn { width: 100%; }
.status-success { color: var(--success); border-color: #bce4d1; background: #effaf4; }
</style>
