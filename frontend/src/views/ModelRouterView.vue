<template>
  <div class="page-grid router-workbench">
    <PageToolbar eyebrow="模型治理" title="智能路由配置" description="按用户请求内容选择合适的模型，并为主模型配置可控的备用模型。" />

    <section class="workbench-section">
      <div class="section-heading"><div><h3>新增路由规则</h3><p>普通配置只需要选择条件和模型，系统会在服务端生成匹配规则。</p></div></div>
      <form class="router-form" @submit.prevent="saveRule">
        <label><span>规则名称</span><input v-model.trim="form.ruleName" maxlength="80" placeholder="例如：简单问候使用轻量模型" /></label>
        <div class="condition-field"><span class="field-label">请求条件</span><ConditionBuilder v-model="form.conditions" :fields="conditionFields" :operator-values="['equals', 'contains']" :max-items="1" /></div>
        <label><span>主模型</span><ResourceSelect v-model="form.primaryModelKey" :options="modelOptions" resource-label="模型" placeholder="请选择主模型" create-label="去模型中心配置" @create="goModels" /></label>
        <label><span>备用模型（可选）</span><ResourceSelect v-model="form.backupModelKey" :options="modelOptions" resource-label="模型" placeholder="不设置备用模型" create-label="去模型中心配置" @create="goModels" /></label>
        <div class="form-actions"><button class="primary-btn" type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存路由规则' }}</button></div>
      </form>
      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
    </section>

    <section class="workbench-section">
      <div class="section-heading"><div><h3>已保存规则</h3><p>规则按创建顺序参与匹配，服务端不会向前端返回内部正则表达式。</p></div><button class="ghost-btn" type="button" @click="load">刷新</button></div>
      <PageState v-if="loading" type="loading" title="正在读取路由规则" />
      <PageState v-else-if="!rules.length" type="empty" title="暂无路由规则" message="添加一条规则后，模型路由才会按请求内容生效。" />
      <table v-else class="workbench-table"><thead><tr><th>规则名称</th><th>匹配条件</th><th>主模型</th><th>备用模型</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="rule in rules" :key="rule.id"><td>{{ rule.ruleName }}</td><td>{{ rule.conditionSummary }}</td><td><code>{{ rule.primaryModelKey }}</code></td><td><code>{{ rule.backupModelKey || '未设置' }}</code></td><td>{{ rule.status === 'ACTIVE' ? '已启用' : '已停用' }}</td><td><button class="link-btn danger" type="button" @click="removeRule(rule)">删除</button></td></tr></tbody></table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageToolbar from '../components/PageToolbar.vue'
import PageState from '../components/PageState.vue'
import ConditionBuilder from '../components/structured/ConditionBuilder.vue'
import ResourceSelect from '../components/structured/ResourceSelect.vue'
import http from '../api/http'
import { confirmAction, notify } from '../utils/feedback'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const errorMessage = ref('')
const rules = ref([])
const modelOptions = ref([])
const conditionFields = [{ value: 'requestText', label: '用户请求内容' }]
const form = reactive({ ruleName: '', conditions: [], primaryModelKey: '', backupModelKey: '' })

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [ruleResponse, modelResponse] = await Promise.all([http.get('/system/routers'), http.get('/system/models')])
    rules.value = ruleResponse.data || []
    modelOptions.value = (modelResponse.data || []).filter(item => item.status === 'ACTIVE').map(item => ({ value: item.modelKey, label: item.modelName, status: item.modelCapability === 'CHAT' ? '对话模型' : item.modelCapability === 'EMBEDDING' ? '向量模型' : '重排模型' }))
  } catch (error) {
    errorMessage.value = error.message || '路由配置读取失败。'
  } finally {
    loading.value = false
  }
}

const saveRule = async () => {
  if (!form.ruleName || form.conditions.length !== 1 || !form.conditions[0].field || !form.conditions[0].value || !form.primaryModelKey) {
    notify('请补齐规则名称、请求条件和主模型。', 'warning')
    return
  }
  saving.value = true
  try {
    await http.post('/system/routers', { ...form, backupModelKey: form.backupModelKey || null })
    notify('智能路由规则已保存。', 'success')
    Object.assign(form, { ruleName: '', conditions: [], primaryModelKey: '', backupModelKey: '' })
    await load()
  } catch (error) {
    notify(error.message || '路由规则保存失败。', 'error')
  } finally {
    saving.value = false
  }
}

const removeRule = async rule => {
  if (!await confirmAction({ title: '删除路由规则', message: `删除“${rule.ruleName}”后，匹配请求将不再自动选择该模型。` })) return
  try {
    await http.delete(`/system/routers/${rule.id}`)
    notify('路由规则已删除。', 'success')
    await load()
  } catch (error) {
    notify(error.message || '路由规则删除失败。', 'error')
  }
}

const goModels = () => router.push('/models')
onMounted(load)
</script>

<style scoped>
.router-workbench { gap: 18px; }.workbench-section { display: grid; gap: 16px; padding: 18px 20px; background: var(--panel); border: 1px solid var(--line); border-radius: var(--radius-md); }.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.section-heading h3 { margin: 0; font-size: 16px; }.section-heading p { margin: 4px 0 0; color: var(--muted); font-size: 12px; }.router-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }.router-form label,.condition-field { display: grid; gap: 7px; min-width: 0; }.router-form label > span,.field-label { color: var(--muted); font-size: 12px; }.router-form input { width: 100%; min-height: var(--control-height); padding: 8px 10px; border: 1px solid var(--line); border-radius: var(--radius-sm); background: var(--panel); color: var(--text); font: inherit; }.condition-field { grid-column: 1 / -1; }.form-actions { display: flex; align-items: end; }.form-error { margin: 0; color: var(--critical); }.link-btn { border: 0; background: transparent; color: var(--accent); cursor: pointer; }.link-btn.danger { color: var(--critical); }code { color: var(--muted); }@media (max-width: 720px) { .router-form { grid-template-columns: 1fr; }.condition-field { grid-column: auto; }.form-actions { align-items: stretch; } }
</style>
