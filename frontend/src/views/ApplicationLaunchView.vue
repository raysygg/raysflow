<template>
  <div class="page-grid launch-page">
    <PageToolbar eyebrow="应用入口" :title="application?.name || '应用入口'" :description="entryDescription">
      <template #actions><RouterLink :to="`/applications/${route.params.id}`" class="ghost-btn">返回应用工作区</RouterLink></template>
    </PageToolbar>
    <ResourceContextRail label="当前应用入口" :name="application?.name || '业务应用'" :state="entrypoint?.enabled ? '已启用' : '待配置'" :hint="entryTitle" mark="E" />
    <PageState v-if="loading" type="loading" title="正在准备入口" message="正在读取入口和发布版本。" />
    <PageState v-else-if="errorMessage" type="error" title="入口不可用" :message="errorMessage" />
    <SectionCard v-else :title="entryTitle" :description="entryDescription">
      <form class="entry-editor" @submit.prevent="saveEntrypoint">
        <div v-if="matchingEntrypoints.length" class="entry-selector"><label><span>已配置入口</span><select :value="entrypoint?.id || ''" @change="selectEntrypoint"><option v-for="item in matchingEntrypoints" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><button class="ghost-btn" type="button" @click="startNewEntrypoint">新建同类入口</button></div>
        <div class="field-grid">
          <label><span>入口名称</span><input v-model="form.name" required /></label>
          <label><span>版本策略</span><select v-model="form.versionPolicy"><option value="FOLLOW_PRODUCTION">跟随生产版本</option><option value="PINNED_VERSION">固定发布版本</option></select></label>
          <label v-if="form.versionPolicy === 'PINNED_VERSION'"><span>固定版本</span><select v-model="form.pinnedVersionId" required><option v-for="version in versions" :key="version.versionId" :value="version.versionId">第 {{ version.versionNo }} 版</option></select></label>
          <label v-if="type === 'API'"><span>交付方式</span><select v-model="form.deliveryMode"><option value="IMMEDIATE">即时返回</option><option value="REALTIME">实时事件</option><option value="BACKGROUND">后台处理</option></select></label>
        </div>
        <div v-if="type === 'SCHEDULE'" class="field-grid">
          <label><span>执行频率</span><select v-model="form.cronExpression"><option value="0 0 9 * * *">每天 09:00</option><option value="0 0 * * * *">每小时</option><option value="0 0 9 * * MON-FRI">工作日 09:00</option></select></label>
          <label><span>时区</span><input v-model="form.timezone" /></label>
        </div>
        <label class="switch-row"><input v-model="form.enabled" type="checkbox" /><span>启用此入口</span></label>
        <div class="action-row"><button class="primary-btn" type="submit" :disabled="submitting">{{ submitting ? '保存中...' : entrypoint ? '保存入口' : '创建入口' }}</button><button v-if="entrypoint && ['API', 'WEBHOOK'].includes(type)" class="ghost-btn" type="button" :disabled="submitting" @click="rotateSecret">轮换密钥</button></div>
      </form>

      <div v-if="entrypoint && ['API', 'WEBHOOK'].includes(type)" class="endpoint-surface">
        <div class="endpoint-line"><small>调用地址</small><code>{{ externalEndpoint }}</code><button class="ghost-btn" type="button" @click="copyEndpoint">复制</button></div>
        <div class="endpoint-line"><small>密钥状态</small><strong>{{ entrypoint.credentialConfigured ? entrypoint.credentialMask : '未配置' }}</strong></div>
        <p v-if="type === 'API'" class="field-help">请求使用 Bearer 密钥，并通过 Idempotency-Key 请求头提交幂等键。</p>
        <p v-else class="field-help">签名内容依次为时间戳、随机串和原始请求体，使用 HMAC-SHA256 计算。</p>
      </div>
      <PageState v-if="oneTimeSecret" type="success" title="请妥善保管本次密钥" :message="oneTimeSecret" />

      <form v-if="entrypoint && ['CONVERSATION', 'FORM', 'API', 'WEBHOOK'].includes(type)" class="invoke-form" @submit.prevent="submitExecution">
        <TypedInputForm ref="invokeForm" v-model="testPayload" :fields="invokeFields" :resources="inputResources" />
        <button class="primary-btn" type="submit" :disabled="submitting || !entrypoint.enabled">{{ submitting ? '提交中...' : '通过内部身份测试入口' }}</button>
      </form>
      <PageState v-if="result" type="success" title="请求已提交" :message="result" />
    </SectionCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageState from '../components/PageState.vue'
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import SectionCard from '../components/SectionCard.vue'
import TypedInputForm from '../components/TypedInputForm.vue'
import { notify } from '../utils/feedback'
import { createRuntimeEntrypoint, fetchRuntimeApplicationConfiguration, fetchRuntimeApplications, fetchRuntimeEntrypoints, invokeRuntimeEntrypoint, rotateRuntimeEntrypointSecret, updateRuntimeEntrypoint } from '../api/runtime'
import { apiData, apiErrorMessage, requireApiSuccess } from '../api/contracts'

const route = useRoute()
const router = useRouter()
const typeMap = { conversation: 'CONVERSATION', form: 'FORM', api: 'API', webhook: 'WEBHOOK', scheduled: 'SCHEDULE', schedule: 'SCHEDULE', test: 'TEST' }
const type = computed(() => typeMap[String(route.query.trigger || 'conversation').toLowerCase()] || 'CONVERSATION')
const labels = { CONVERSATION: ['对话入口', '以自然语言输入触发应用工作流。'], FORM: ['表单入口', '以结构化字段触发应用工作流。'], API: ['应用接口', '供外部系统通过入口凭证调用。'], WEBHOOK: ['Webhook 入口', '供外部系统通过签名请求触发。'], SCHEDULE: ['定时入口', '按时区和频率自动创建幂等运行。'], TEST: ['测试入口', '用于应用草稿和发布版本验证。'] }
const loading = ref(true); const submitting = ref(false); const errorMessage = ref(''); const result = ref(''); const resultRunId = ref(''); const oneTimeSecret = ref(''); const application = ref(null); const entrypoint = ref(null); const entrypoints = ref([]); const versions = ref([]); const contractFields = ref([]); const testPayload = ref({}); const invokeForm = ref(null); const inputResources = ref({})
const form = reactive({ name: '', versionPolicy: 'FOLLOW_PRODUCTION', pinnedVersionId: '', deliveryMode: 'BACKGROUND', cronExpression: '0 0 9 * * *', timezone: 'Asia/Shanghai', enabled: false })
const entryTitle = computed(() => labels[type.value][0]); const entryDescription = computed(() => labels[type.value][1])
const matchingEntrypoints = computed(() => entrypoints.value.filter(item => item.type === type.value))
const invokeFields = computed(() => entrypoint.value?.inputFields?.length ? entrypoint.value.inputFields : contractFields.value)
const externalEndpoint = computed(() => `${window.location.origin}/api/public/v1/${type.value === 'WEBHOOK' ? 'webhooks' : 'entrypoints'}/${entrypoint.value?.invokeCode || ''}${type.value === 'API' ? '/invoke' : ''}`)

const defaultDelivery = currentType => ({ CONVERSATION: 'REALTIME', FORM: 'IMMEDIATE', API: 'BACKGROUND', WEBHOOK: 'BACKGROUND', SCHEDULE: 'BACKGROUND', TEST: 'REALTIME' }[currentType])
const applyEntrypoint = value => { entrypoint.value = value || null; form.name = value?.name || labels[type.value][0]; form.versionPolicy = value?.versionPolicy || 'FOLLOW_PRODUCTION'; form.pinnedVersionId = value?.pinnedVersionId || versions.value[0]?.versionId || ''; form.deliveryMode = value?.deliveryMode || defaultDelivery(type.value); form.cronExpression = value?.cronExpression || '0 0 9 * * *'; form.timezone = value?.timezone || 'Asia/Shanghai'; form.enabled = Boolean(value?.enabled) }
const load = async () => { try { const [appsResponse, configResponse, entrypointsResponse] = await Promise.all([fetchRuntimeApplications(), fetchRuntimeApplicationConfiguration(route.params.id), fetchRuntimeEntrypoints(route.params.id)]); application.value = apiData(appsResponse, []).find(item => String(item.id) === String(route.params.id)); if (!application.value) throw new Error('未找到该应用'); const configuration = apiData(configResponse, {}); versions.value = configuration.versions || []; contractFields.value = configuration.spec?.input?.fields || []; inputResources.value = { MODEL: configuration.dependencies?.models || [], KNOWLEDGE_DOCUMENT: configuration.dependencies?.documents || [], TOOL_CONNECTOR: configuration.dependencies?.connectors || [] }; entrypoints.value = apiData(entrypointsResponse, []); const requested = route.query.entrypointId; applyEntrypoint(matchingEntrypoints.value.find(item => String(item.id) === String(requested)) || matchingEntrypoints.value[0]) } catch (error) { errorMessage.value = apiErrorMessage(error, '入口配置读取失败') } finally { loading.value = false } }
const requestPayload = () => ({ name: form.name, type: type.value, versionPolicy: form.versionPolicy, pinnedVersionId: form.versionPolicy === 'PINNED_VERSION' ? form.pinnedVersionId : null, deliveryMode: form.deliveryMode, inputFields: entrypoint.value?.inputFields || contractFields.value, cronExpression: type.value === 'SCHEDULE' ? form.cronExpression : null, timezone: type.value === 'SCHEDULE' ? form.timezone : null, enabled: form.enabled })
const saveEntrypoint = async () => { submitting.value = true; oneTimeSecret.value = ''; try { const response = entrypoint.value ? await updateRuntimeEntrypoint(route.params.id, entrypoint.value.id, requestPayload()) : await createRuntimeEntrypoint(route.params.id, requestPayload()); const data = requireApiSuccess(response, '入口保存失败'); const saved = data.entrypoint || data; const index = entrypoints.value.findIndex(item => item.id === saved.id); if (index >= 0) entrypoints.value[index] = saved; else entrypoints.value.push(saved); applyEntrypoint(saved); oneTimeSecret.value = data.secret || ''; notify('应用入口已保存', 'success') } catch (error) { notify(apiErrorMessage(error, '入口保存失败'), 'error') } finally { submitting.value = false } }
const selectEntrypoint = event => applyEntrypoint(matchingEntrypoints.value.find(item => String(item.id) === String(event.target.value)))
const startNewEntrypoint = () => { oneTimeSecret.value = ''; applyEntrypoint(null) }
const rotateSecret = async () => { submitting.value = true; try { const data = requireApiSuccess(await rotateRuntimeEntrypointSecret(route.params.id, entrypoint.value.id), '密钥轮换失败'); applyEntrypoint(data.entrypoint); oneTimeSecret.value = data.secret; notify('入口密钥已轮换', 'success') } catch (error) { notify(apiErrorMessage(error, '密钥轮换失败'), 'error') } finally { submitting.value = false } }
const submitExecution = async () => { if (!invokeForm.value?.validate()) { notify('请先补齐所有必填输入', 'warning'); return } submitting.value = true; try { const data = requireApiSuccess(await invokeRuntimeEntrypoint(entrypoint.value.id, { idempotencyKey: `${entrypoint.value.id}-${Date.now()}`, input: invokeForm.value.payload() }), '请求提交失败'); resultRunId.value = data.runId || data.executionId || ''; result.value = `运行 ${resultRunId.value || '已创建'} 已进入运行中心。`; notify('应用请求已提交', 'success'); if (resultRunId.value) await router.push({ path: '/workflow-executions', query: { appId: route.params.id, runId: resultRunId.value } }) } catch (error) { notify(apiErrorMessage(error, '请求提交失败'), 'error') } finally { submitting.value = false } }
const copyEndpoint = async () => { await navigator.clipboard?.writeText(externalEndpoint.value); notify('调用地址已复制', 'success') }
onMounted(load)
</script>

<style scoped>
.entry-editor,.invoke-form,.endpoint-surface{display:grid;gap:14px;max-width:820px}.entry-selector{display:flex;align-items:end;gap:10px}.entry-selector label{min-width:260px}.field-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.entry-editor label,.invoke-form label{display:grid;gap:7px}.entry-editor label>span,.invoke-form label>span{color:var(--muted);font-size:12px}.entry-editor input,.entry-editor select,.invoke-form textarea{width:100%;border:1px solid var(--line);border-radius:7px;padding:10px 12px;background:var(--panel-muted);color:var(--text);font:inherit}.switch-row{display:flex!important;align-items:center}.switch-row input{width:auto}.action-row{display:flex;gap:8px}.endpoint-surface,.invoke-form{margin-top:22px;padding-top:18px;border-top:1px solid var(--line)}.endpoint-line{display:flex;align-items:center;gap:12px;padding:13px;border:1px solid var(--line);border-radius:7px;background:var(--panel-muted)}.endpoint-line small{color:var(--muted)}.endpoint-line code{flex:1;overflow:auto}.field-help{margin:0;color:var(--muted);font-size:12px;line-height:1.7}@media(max-width:680px){.field-grid{grid-template-columns:1fr}.entry-selector,.endpoint-line{align-items:flex-start;flex-wrap:wrap}}
</style>
