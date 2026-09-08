<template>
  <div class="page-grid">
    <SectionCard
      title="项目架构"
      description="架构页只展示工程结构和模块边界，不承载业务样例数据。"
    />

    <SectionCard title="后端分层">
      <div class="info-list">
        <div v-for="item in architectureLayers" :key="item.name" class="info-row">
          <strong>{{ item.name }}</strong>
          <span>{{ item.description }}</span>
        </div>
      </div>
    </SectionCard>

    <div class="split-grid">
      <SectionCard title="前端目录">
        <pre class="code-block">frontend/
  src/
    api/
    router/
    layouts/
    components/
    views/</pre>
      </SectionCard>

      <SectionCard title="后端目录">
        <pre class="code-block">backend/
  src/main/java/com/acme/agentstudio/
    common/
    config/
    interfaces/rest/
    application/
    domain/
    infrastructure/</pre>
      </SectionCard>
    </div>
  </div>
</template>

<script setup>
import SectionCard from '../components/SectionCard.vue'

const architectureLayers = [
  { name: '接口层', description: '应用接口、事件流和定时入口' },
  { name: '应用层', description: '会话编排、Agent 管理、知识任务、工作流执行' },
  { name: '领域层', description: '业务实体、状态常量、规则与领域模型' },
  { name: '基础设施层', description: 'MySQL、RAG、Prompt 渲染、模型、工具与监控实现' }
]
</script>
