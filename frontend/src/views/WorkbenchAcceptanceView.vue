<template>
  <main class="acceptance-page page-grid" data-layout-check>
    <PageToolbar eyebrow="界面质量验收" title="企业工作台状态样本" description="用于验证共享控件在桌面和移动设备上的稳定布局。">
      <template #actions><button type="button" class="primary-btn">创建业务应用</button></template>
    </PageToolbar>
    <ResourceContextRail label="当前应用" name="跨区域供应链异常分析与处置协同应用" state="发布受阻" :blocking-count="3" action-label="处理问题" mark="A" />
    <WorkbenchTabs v-model="activeTab" :items="tabs" label="验收状态分区" />
    <section class="acceptance-grid" data-layout-check>
      <PageState type="loading" title="正在加载" message="正在恢复服务端状态，请稍候。" />
      <PageState type="empty" title="暂无数据" message="当前范围没有记录，请创建第一条业务配置。" action-label="创建配置" />
      <PageState type="processing" title="后台处理中" message="文档正在解析和建立索引，完成后会自动刷新。" />
      <PageState type="partial" title="部分步骤失败" message="主数据库已完成，向量索引仍需重试。" action-label="重试失败步骤" />
      <PageState type="error" title="加载失败" message="服务暂时不可用。请检查网络连接和当前账号权限，然后重新加载；已完成的数据不会丢失。" action-label="重新加载" />
      <PageState type="forbidden" title="权限不足" message="当前角色不能修改此配置，请联系企业管理员分配权限。" />
    </section>
    <section class="acceptance-workbench" data-layout-check>
      <div class="workbench-filter-bar">
        <label>业务状态<select><option>全部状态</option><option>处理中</option></select></label>
        <label>资源名称<input value="跨区域供应链异常分析与处置协同应用" /></label>
        <button type="button" class="ghost-btn">应用筛选</button>
      </div>
      <div class="workbench-table-wrap">
        <table class="workbench-table"><thead><tr><th>资源</th><th>状态</th><th>说明</th><th>操作</th></tr></thead><tbody><tr><td>供应链异常协同流程</td><td><StatusBadge label="部分失败" tone="warning" /></td><td>主流程已保存，备用模型连接检查未完成。</td><td><button type="button" class="ghost-btn compact-btn" title="查看处理详情">查看详情</button></td></tr></tbody></table>
      </div>
    </section>
    <p class="acceptance-result" role="status">布局检测结果：{{ issueCount === 0 ? '未发现溢出或控件重叠' : `发现 ${issueCount} 项问题` }}</p>
  </main>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import PageState from '../components/PageState.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import StatusBadge from '../components/StatusBadge.vue'
import WorkbenchTabs from '../components/WorkbenchTabs.vue'
import { collectLayoutIssues } from '../utils/layout-quality.js'

const activeTab = ref('states')
const issueCount = ref(0)
const tabs = [{ value: 'states', label: '状态样本', count: 6 }, { value: 'layout', label: '布局样本', count: 1 }]
onMounted(async () => { await nextTick(); issueCount.value = collectLayoutIssues().length })
</script>

<style scoped>
.acceptance-page{max-width:1180px;margin:0 auto;padding:20px}.acceptance-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.acceptance-workbench{display:grid;gap:12px}.workbench-filter-bar label{display:grid;gap:5px;color:var(--muted);font-size:11px}.workbench-filter-bar input,.workbench-filter-bar select{width:100%;min-height:var(--control-height);border:1px solid var(--line);background:var(--panel);color:var(--text)}.acceptance-result{margin:0;color:var(--muted);font-size:12px}
@media(max-width:680px){.acceptance-page{padding:12px}.acceptance-grid{grid-template-columns:1fr}}
</style>
