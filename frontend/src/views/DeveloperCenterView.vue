<template>
  <div class="page-grid connector-workbench">
    <PageToolbar eyebrow="开发者资源" title="连接器管理" description="集中配置工作流可调用的外部服务，凭证通过安全引用绑定且不会再次显示。" />

    <section class="workbench-section">
      <div class="section-heading"><div><h3>{{ editingId ? '编辑连接器' : '新增连接器' }}</h3><p>服务协议、超时和重试策略均使用受控选项，不需要填写 JSON。</p></div></div>
      <form class="connector-form" @submit.prevent="saveConnector">
        <label><span>连接器名称</span><input v-model.trim="form.name" maxlength="80" placeholder="例如：客户资料查询服务" /></label>
        <label><span>连接器类型</span><select v-model="form.type"><option value="HTTP">网页接口</option><option value="OPENAPI">开放接口规范</option><option value="MCP">工具服务协议</option><option value="WEBHOOK">回调通知</option></select></label>
        <label class="wide"><span>服务地址</span><input v-model.trim="form.endpoint" type="url" placeholder="https://service.example.com" /></label>
        <label><span>超时时间（毫秒）</span><input v-model.number="form.timeoutMs" type="number" min="1000" max="120000" step="1000" /></label>
        <label><span>失败重试次数</span><select v-model.number="form.retryCount"><option v-for="count in 6" :key="count - 1" :value="count - 1">{{ count - 1 }} 次</option></select></label>
        <label><span>重试等待时间</span><select v-model.number="form.backoffMs"><option :value="0">立即重试</option><option :value="500">0.5 秒</option><option :value="1000">1 秒</option><option :value="3000">3 秒</option><option :value="5000">5 秒</option></select></label>
        <label><span>安全凭证（可选）</span><ResourceSelect v-model="form.credentialRefId" :options="credentialOptions" resource-label="连接器凭证" placeholder="无需凭证" create-label="填写新凭证" @create="showCredentialEditor = true" /></label>
        <div v-if="showCredentialEditor" class="credential-editor wide">
          <label><span>凭证名称</span><input v-model.trim="credentialForm.name" placeholder="例如：客户系统生产凭证" /></label>
          <label><span>凭证内容</span><input v-model="credentialForm.secret" type="password" autocomplete="new-password" placeholder="仅保存一次，不会再次显示" /></label>
          <button class="ghost-btn" type="button" :disabled="creatingCredential" @click="createCredential">保存并绑定凭证</button>
        </div>
        <div class="form-actions wide"><button class="primary-btn" type="submit" :disabled="saving">{{ saving ? '保存中...' : editingId ? '保存修改' : '创建连接器' }}</button><button v-if="editingId" class="ghost-btn" type="button" @click="resetForm">取消编辑</button></div>
      </form>
    </section>

    <section class="workbench-section">
      <div class="section-heading"><div><h3>可用连接器</h3><p>启用后的连接器会自动出现在工作流节点的资源选择器中。</p></div><button class="ghost-btn" type="button" @click="load">刷新</button></div>
      <PageState v-if="loading" type="loading" title="正在读取连接器" />
      <PageState v-else-if="errorMessage" type="error" title="连接器读取失败" :message="errorMessage" action-label="重新加载" @action="load" />
      <PageState v-else-if="!connectors.length" type="empty" title="暂无连接器" message="创建连接器后，流程设计器才能选择并调用外部服务。" />
      <table v-else class="workbench-table"><thead><tr><th>连接器</th><th>类型</th><th>服务地址</th><th>凭证状态</th><th>超时与重试</th><th>操作</th></tr></thead><tbody><tr v-for="item in connectors" :key="item.id"><td>{{ item.name }}</td><td>{{ typeLabel(item.type) }}</td><td class="endpoint-cell" :title="item.endpoint">{{ item.endpoint }}</td><td>{{ item.credentialName || '无需凭证' }}</td><td>{{ item.timeoutMs / 1000 }} 秒 · {{ item.retryCount }} 次</td><td><div class="row-actions"><button class="link-btn" type="button" @click="editConnector(item)">编辑</button><button class="link-btn danger" type="button" @click="removeConnector(item)">删除</button></div></td></tr></tbody></table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import PageState from '../components/PageState.vue'
import ResourceSelect from '../components/structured/ResourceSelect.vue'
import http from '../api/http'
import { confirmAction, notify } from '../utils/feedback'

const loading = ref(true)
const saving = ref(false)
const creatingCredential = ref(false)
const errorMessage = ref('')
const connectors = ref([])
const credentialOptions = ref([])
const editingId = ref(null)
const showCredentialEditor = ref(false)
const defaultForm = { name: '', type: 'HTTP', endpoint: '', credentialRefId: null, timeoutMs: 30000, retryCount: 1, backoffMs: 1000 }
const form = reactive({ ...defaultForm })
const credentialForm = reactive({ name: '', secret: '' })

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [connectorResponse, credentialResponse] = await Promise.all([http.get('/system/connectors'), http.get('/system/connectors/credentials')])
    connectors.value = connectorResponse.data || []
    credentialOptions.value = (credentialResponse.data || []).map(item => ({ value: item.id, label: item.name, status: item.status === 'ACTIVE' ? '可用' : '已停用' }))
  } catch (error) {
    errorMessage.value = error.message || '连接器配置读取失败。'
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingId.value = null
  Object.assign(form, defaultForm)
  Object.assign(credentialForm, { name: '', secret: '' })
  showCredentialEditor.value = false
}

const editConnector = item => {
  editingId.value = item.id
  Object.assign(form, { name: item.name, type: item.type, endpoint: item.endpoint, credentialRefId: item.credentialRefId || null, timeoutMs: item.timeoutMs, retryCount: item.retryCount, backoffMs: item.backoffMs })
  showCredentialEditor.value = false
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const saveConnector = async () => {
  if (!form.name || !form.endpoint) {
    notify('请填写连接器名称和服务地址。', 'warning')
    return
  }
  saving.value = true
  try {
    if (editingId.value) await http.put(`/system/connectors/${editingId.value}`, form)
    else await http.post('/system/connectors', form)
    notify(editingId.value ? '连接器已更新。' : '连接器已创建。', 'success')
    resetForm()
    await load()
  } catch (error) {
    notify(error.message || '连接器保存失败。', 'error')
  } finally {
    saving.value = false
  }
}

const createCredential = async () => {
  if (!credentialForm.name || !credentialForm.secret) {
    notify('请填写凭证名称和内容。', 'warning')
    return
  }
  creatingCredential.value = true
  try {
    const response = await http.post('/system/connectors/credentials', credentialForm)
    const created = response.data
    credentialOptions.value = [...credentialOptions.value, { value: created.id, label: created.name, status: '可用' }]
    form.credentialRefId = created.id
    showCredentialEditor.value = false
    Object.assign(credentialForm, { name: '', secret: '' })
    notify('凭证已保存并绑定。', 'success')
  } catch (error) {
    notify(error.message || '凭证保存失败。', 'error')
  } finally {
    creatingCredential.value = false
  }
}

const removeConnector = async item => {
  if (!await confirmAction({ title: '删除连接器', message: `删除“${item.name}”后，引用它的流程将无法继续调用外部服务。` })) return
  try {
    await http.delete(`/system/connectors/${item.id}`)
    notify('连接器已删除。', 'success')
    await load()
  } catch (error) {
    notify(error.message || '连接器删除失败。', 'error')
  }
}

const typeLabel = type => ({ HTTP: '网页接口', OPENAPI: '开放接口规范', MCP: '工具服务协议', WEBHOOK: '回调通知' }[type] || '其他连接器')
onMounted(load)
</script>

<style scoped>
.connector-workbench { gap: 18px; }.workbench-section { display: grid; gap: 16px; padding: 18px 20px; background: var(--panel); border: 1px solid var(--line); border-radius: var(--radius-md); }.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.section-heading h3 { margin: 0; font-size: 16px; }.section-heading p { margin: 4px 0 0; color: var(--muted); font-size: 12px; }.connector-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }.connector-form label { display: grid; gap: 7px; min-width: 0; }.connector-form label > span { color: var(--muted); font-size: 12px; }.connector-form input,.connector-form select { width: 100%; min-height: var(--control-height); padding: 8px 10px; border: 1px solid var(--line); border-radius: var(--radius-sm); background: var(--panel); color: var(--text); font: inherit; }.wide { grid-column: 1 / -1; }.credential-editor { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)) auto; align-items: end; gap: 12px; padding: 12px; background: var(--panel-muted); border: 1px solid var(--line); border-radius: var(--radius-sm); }.form-actions,.row-actions { display: flex; gap: 8px; }.endpoint-cell { max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.link-btn { border: 0; background: transparent; color: var(--accent); cursor: pointer; }.link-btn.danger { color: var(--critical); }@media (max-width: 720px) { .connector-form,.credential-editor { grid-template-columns: 1fr; }.wide { grid-column: auto; }.section-heading { align-items: flex-start; flex-direction: column; }.endpoint-cell { max-width: 160px; } }
</style>
