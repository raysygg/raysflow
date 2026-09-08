<template>
  <div class="page-grid application-wizard-page">
    <PageToolbar eyebrow="新建应用" title="创建业务应用" description="用业务语言描述目标，系统会生成一个可继续配置的运行应用草稿。">
      <template #actions><RouterLink to="/applications" class="ghost-btn">返回应用目录</RouterLink></template>
    </PageToolbar>
    <ResourceContextRail label="应用创建" :name="form.name || '新建业务应用'" state="草稿配置" hint="按步骤完成基础信息后进入工作区" mark="A" />
    <PageState v-if="loading" type="loading" title="正在准备向导" message="正在读取应用创建步骤。" />
    <PageState v-else-if="errorMessage" type="error" title="向导暂时不可用" :message="errorMessage" action-label="重新加载" @action="loadDefinition" />
    <template v-else>
      <div class="wizard-layout">
        <section class="section-card wizard-main">
          <div class="wizard-steps"><div v-for="(step, index) in steps" :key="step.key" class="wizard-step" :class="{ active: index === currentStep, done: index < currentStep }"><span>{{ index < currentStep ? '✓' : index + 1 }}</span><strong>{{ step.title }}</strong><small>{{ step.description }}</small></div></div>
          <form @submit.prevent="submitStep">
            <div v-if="currentStep === 0" class="wizard-form"><h3>先说清楚要解决什么业务</h3><p class="section-description">名称会展示给团队成员，技术编码由系统自动生成。</p><label class="form-group"><span>应用名称</span><input v-model.trim="form.name" class="text-input" maxlength="80" placeholder="例如：合同审查助手" /></label></div>
            <div v-else-if="currentStep === 1" class="wizard-form"><h3>先定义业务输入和目标</h3><p class="section-description">用业务语言描述请求，运行时会把它作为流程的输入。</p><label class="form-group"><span>业务目标</span><textarea v-model.trim="form.goal" class="textarea-input" rows="3" placeholder="例如：识别合同中的付款风险并给出修改建议" /></label><label class="form-group"><span>输入名称</span><input v-model.trim="form.inputName" class="text-input" maxlength="40" placeholder="例如：合同内容" /></label><label class="form-group"><span>输入类型</span><select v-model="form.inputType" class="text-input"><option value="string">文本</option><option value="number">数字</option><option value="boolean">是/否</option></select></label></div>
            <div v-else-if="currentStep === 2" class="wizard-form"><h3>定义输出结果</h3><p class="section-description">输出契约会显示在应用工作区，并在发布前进行校验。</p><label class="form-group"><span>输出格式</span><select v-model="form.outputFormat" class="text-input"><option value="text">文本结果</option><option value="json">结构化数据</option><option value="markdown">格式化报告</option></select></label><label class="form-group"><span>结果名称</span><input v-model.trim="form.outputName" class="text-input" maxlength="40" placeholder="例如：风险分析结果" /></label></div>
            <div v-else class="wizard-form"><h3>准备生成应用草稿</h3><p class="section-description">创建后进入流程设计器，明确添加模型、知识、工具或其他业务节点。</p><div class="wizard-summary"><span><small>应用名称</small><strong>{{ form.name || '未填写' }}</strong></span><span><small>业务目标</small><strong>{{ form.goal || '未填写' }}</strong></span><span><small>输入契约</small><strong>{{ form.inputName || 'request' }}</strong></span><span><small>输出契约</small><strong>{{ form.outputName || 'result' }}</strong></span><span><small>主工作流</small><strong>开始 → 结束空白骨架</strong></span><span><small>发布要求</small><strong>至少配置一个业务能力节点</strong></span></div></div>
            <p v-if="formError" class="status-message">{{ formError }}</p>
            <p v-else-if="draftStatus" class="field-help">{{ draftStatus }}</p>
            <div class="wizard-actions"><button v-if="currentStep > 0" type="button" class="ghost-btn" @click="currentStep--">上一步</button><button v-if="currentStep < steps.length - 1" type="submit" class="primary-btn">下一步</button><button v-else type="submit" class="primary-btn" :disabled="submitting">{{ submitting ? '创建中...' : '创建运行应用' }}</button></div>
          </form>
        </section>
        <aside class="section-card wizard-help"><div class="section-eyebrow">渐进式配置</div><h3>不懂智能体也能开始</h3><p>先完成最少信息，进入应用后再逐步增加行为说明、知识库、工具和流程。每一步都可以保存并稍后继续。</p><div class="help-list"><span>自动生成技术编码</span><span>后端校验可用能力</span><span>草稿可随时恢复</span><span>发布前统一检查</span></div></aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { RouterLink, onBeforeRouteLeave, useRouter } from 'vue-router'
import PageState from '../components/PageState.vue'
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import http from '../api/http'

const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const formError = ref('')
const currentStep = ref(0)
const steps = ref([])
const savedDraft = localStorage.getItem('runtime-application-draft')
const emptyForm = { name: '', goal: '', inputName: 'request', inputType: 'string', outputFormat: 'text', outputName: 'result' }
const form = reactive(savedDraft ? { ...emptyForm, ...JSON.parse(savedDraft) } : { ...emptyForm })
const draftStatus = ref(savedDraft ? '已恢复上次未完成的草稿。' : '')
const persistDraft = () => { localStorage.setItem('runtime-application-draft', JSON.stringify(form)); draftStatus.value = '草稿已保存，可稍后继续。' }
const hasUnsavedChanges = () => JSON.stringify(form) !== JSON.stringify(savedDraft ? { ...emptyForm, ...JSON.parse(savedDraft) } : emptyForm)
const beforeUnload = event => { if (hasUnsavedChanges() && !submitting.value) { event.preventDefault(); event.returnValue = '' } }
const loadDefinition = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const wizardResponse = await http.get('/runtime/application-wizard', { params: { advanced: false } })
    if (!wizardResponse.success) throw new Error(wizardResponse.message || '读取向导定义失败')
    // 新手界面只展示业务阶段，后端高级步骤仍由运行工作区继续承接。
    steps.value = [
      { key: 'identity', title: '应用信息', description: '明确业务目标' },
      { key: 'input', title: '输入与目标', description: '明确业务请求' },
      { key: 'output', title: '输出契约', description: '定义结果格式' },
      { key: 'confirm', title: '确认创建', description: '生成空白骨架' }
    ]
  } catch (error) { errorMessage.value = error?.message || '读取应用向导失败，请稍后重试。' } finally { loading.value = false }
}
const submitStep = async () => {
  formError.value = ''
  persistDraft()
  if (currentStep.value === 0 && !form.name) { formError.value = '请填写应用名称'; return }
  if (currentStep.value === 1 && !form.goal) { formError.value = '请填写业务目标'; return }
  if (currentStep.value < steps.value.length - 1) { currentStep.value++; return }
  submitting.value = true
  try {
    const response = await http.post('/runtime/application-wizard', {
      name: form.name,
      configuration: { ...form }
    })
    if (!response.data) throw new Error('创建应用未返回应用数据')
    localStorage.removeItem('runtime-application-draft')
    await router.replace(`/applications/${response.data.id}`)
  } catch (error) { formError.value = error?.message || '创建应用失败，请稍后重试。' } finally { submitting.value = false }
}
onMounted(loadDefinition)
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
onBeforeRouteLeave(() => {
  if (!hasUnsavedChanges() || submitting.value) return true
  return window.confirm('当前向导还有未保存内容，确定要离开吗？')
})
</script>

<style scoped>
.wizard-layout { display:grid; grid-template-columns:minmax(0,1fr) 320px; gap:18px; align-items:start; }.wizard-main { min-height:520px; }.wizard-steps { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:8px; margin-bottom:30px; }.wizard-step { display:grid; grid-template-columns:26px minmax(0,1fr); gap:2px 8px; padding:11px; border-bottom:2px solid var(--line); color:var(--muted); }.wizard-step span { grid-row:span 2; display:grid; width:24px; height:24px; place-items:center; border-radius:50%; background:var(--panel-muted); font-size:11px; }.wizard-step strong { font-size:12px; }.wizard-step small { font-size:10px; }.wizard-step.active { border-color:var(--accent); color:var(--accent); }.wizard-step.active span,.wizard-step.done span { background:var(--accent); color:#fff; }.wizard-form { max-width:680px; }.wizard-form h3 { margin:0 0 8px; font-size:20px; }.wizard-form > .section-description { margin-bottom:24px; }.wizard-form label span { display:block; margin-bottom:7px; color:var(--muted); font-size:12px; }.wizard-summary { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; margin-top:20px; }.wizard-summary span { display:grid; gap:6px; padding:14px; border:1px solid var(--line); border-radius:8px; background:var(--panel-muted); }.wizard-summary small { color:var(--muted); font-size:11px; }.wizard-actions { display:flex; justify-content:flex-end; gap:8px; margin-top:34px; }.wizard-help { background:var(--panel-muted); }.wizard-help h3 { margin:0 0 10px; font-size:17px; }.wizard-help p { color:var(--muted); font-size:12px; line-height:1.8; }.help-list { display:grid; gap:8px; margin-top:20px; }.help-list span { padding:9px 10px; border-left:3px solid var(--accent); background:var(--panel); color:var(--text); font-size:11px; }
@media (max-width: 800px) { .wizard-layout { display:block; }.wizard-help { margin-top:18px; } }
</style>
