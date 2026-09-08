<template>
  <main class="saas-workbench page-grid">
    <PageToolbar title="套餐、权益与用量" description="查看当前额度、重置时间和超额影响，并在阻断前采取处理动作。"><template #actions><button class="ghost-btn compact-btn" :disabled="loading" @click="load">刷新</button></template></PageToolbar>
    <ResourceContextRail label="商业权益" name="当前租户套餐" :state="loading ? '加载中' : dominantPolicy" hint="准入检查始终使用当前登录租户" mark="E" />
    <section class="saas-status-strip"><div class="saas-status-item"><small>权益项目</small><strong>{{ entitlements.length }} 项</strong></div><div class="saas-status-item"><small>接近限制</small><strong>{{ warningCount }} 项</strong></div><div class="saas-status-item"><small>当前策略</small><strong>{{ dominantPolicy }}</strong></div><div class="saas-status-item"><small>下次重置</small><strong>{{ nextReset }}</strong></div></section>
    <section class="saas-work-area"><nav class="saas-task-nav"><button :class="{active:tab==='quota'}" @click="tab='quota'">权益额度</button><button :class="{active:tab==='usage'}" @click="tab='usage'">用量归集</button><button :class="{active:tab==='check'}" @click="tab='check'">准入检查</button></nav><div class="saas-pane">
      <PageState v-if="error" type="error" title="权益数据加载失败" :message="error" action-label="重新加载" @action="load" /><PageState v-else-if="loading" type="loading" message="正在加载权益与用量..." />
      <template v-else-if="tab==='quota'"><div class="saas-pane-head"><div><h2>权益额度</h2><p>硬限制会在消费前阻断；告警策略允许继续，但会记录超额影响。</p></div></div><div v-if="!entitlements.length" class="saas-state">当前租户尚未绑定套餐权益，请联系企业管理员。</div><div v-else class="saas-table-wrap"><table class="saas-table"><thead><tr><th>功能</th><th>已使用</th><th>软提醒</th><th>硬限制</th><th>超额策略</th><th>重置时间</th><th>处理建议</th></tr></thead><tbody><tr v-for="item in entitlements" :key="item.id"><td>{{ featureLabel(item.featureCode) }}</td><td>{{ usageFor(item) }} {{ unitLabel(item.unit) }}</td><td>{{ item.softLimit || '未设置' }}</td><td>{{ item.hardLimit || '不限' }}</td><td><span class="saas-tag" :class="policyClass(item.overagePolicy)">{{ policyLabel(item.overagePolicy) }}</span></td><td>{{ time(item.resetAt) }}</td><td>{{ advice(item) }}</td></tr></tbody></table></div></template>
      <template v-else-if="tab==='usage'"><div class="saas-pane-head"><div><h2>用量归集</h2><p>按功能、应用、模型和成本中心汇总，不展示请求或响应正文。</p></div></div><div v-if="!usage.length" class="saas-state">当前账期暂无可归集用量。</div><div v-else class="saas-table-wrap"><table class="saas-table"><thead><tr><th>功能</th><th>应用</th><th>模型</th><th>成本中心</th><th>数量</th><th>成本状态</th></tr></thead><tbody><tr v-for="(item,index) in usage" :key="index"><td>{{ featureLabel(item.featureCode) }}</td><td>{{ item.applicationId || '平台公共消耗' }}</td><td>{{ item.modelKey || '未关联' }}</td><td>{{ item.costCenter || '未分配' }}</td><td>{{ item.quantity }} {{ unitLabel(item.unit) }}</td><td>{{ item.costStatus==='UNKNOWN'?'成本待确认':'已计算' }}</td></tr></tbody></table></div></template>
      <template v-else><div class="saas-pane-head"><div><h2>消费前检查</h2><p>模拟真实消费前的统一准入结果，不产生实际资源消耗。</p></div></div><div class="saas-form-grid"><div class="saas-field"><label>功能</label><select v-model="check.featureCode"><option value="MODEL_TOKEN">模型用量</option><option value="WORKFLOW_RUN">流程运行</option><option value="KNOWLEDGE_STORAGE">知识存储</option><option value="CONNECTOR_CALL">连接器调用</option></select></div><div class="saas-field"><label>预计数量</label><input v-model.number="check.estimatedQuantity" type="number" min="1" /></div></div><div class="saas-actions"><button class="primary-btn" :disabled="submitting" @click="runCheck">检查是否可用</button></div><div v-if="decision" class="saas-state" :class="decision.decision==='DENY'?'error':decision.decision==='WARN'?'processing':''">{{ decisionLabel(decision.decision) }}：{{ decision.message }}<template v-if="decision.remediation"> {{ decision.remediation }}</template></div></template>
    </div></section>
  </main>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import PageState from '../components/PageState.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import { checkAdmission, fetchEntitlements, fetchUsageSummary } from '../api/saas'
import { useMutationState } from '../composables/useMutationState.js'
const tab=ref('quota'),loading=ref(false),submitting=ref(false),error=ref(''),entitlements=ref([]),usage=ref([]),decision=ref(null)
const mutation = useMutationState()
const check=reactive({featureCode:'MODEL_TOKEN',estimatedQuantity:1000})
const warningCount=computed(()=>entitlements.value.filter(item=>item.hardLimit>0&&usageFor(item)>=Number(item.softLimit||item.hardLimit)).length)
const dominantPolicy=computed(()=>policyLabel(entitlements.value[0]?.overagePolicy))
const nextReset=computed(()=>time(entitlements.value.map(item=>item.resetAt).filter(Boolean).sort()[0]))
const featureLabel=v=>({MODEL_TOKEN:'模型用量',WORKFLOW_RUN:'流程运行',KNOWLEDGE_STORAGE:'知识存储',CONNECTOR_CALL:'连接器调用',RUNTIME:'运行能力'}[v]||'其他功能')
const unitLabel=v=>({TOKEN:'令牌',RUN:'次',MB:'兆字节',CALL:'次'}[v]||v||'单位')
const policyLabel=v=>({HARD_STOP:'达到上限后停止',WARN_ONLY:'仅提醒',ALLOW_OVERAGE:'允许超额'}[v]||'未设置')
const policyClass=v=>v==='HARD_STOP'?'bad':v==='WARN_ONLY'?'warn':''
const time=v=>v?String(v).replace('T',' ').slice(0,16):'不自动重置'
const usageFor=item=>Number(usage.value.find(row=>row.featureCode===item.featureCode)?.quantity||0)
const advice=item=>item.hardLimit>0&&usageFor(item)>=item.hardLimit?'清理资源或升级套餐':item.hardLimit>0&&usageFor(item)>=Number(item.softLimit||item.hardLimit)?'关注增长并调整预算':'无需处理'
const decisionLabel=v=>({ALLOW:'允许使用',WARN:'允许使用并提醒',DENY:'已阻止使用'}[v]||'检查完成')
async function load(){loading.value=true;error.value='';try{const [a,b]=await Promise.all([fetchEntitlements(),fetchUsageSummary({})]);entitlements.value=a.data||[];const summary=b.data;usage.value=Array.isArray(summary)?summary:(summary?[{featureCode:'全部功能',quantity:summary.quantity,unit:'单位',applicationId:null,modelKey:null,costCenter:null,costStatus:summary.unknownCostCount>0?'UNKNOWN':'CALCULATED'}]:[])}catch(e){error.value=e.message||'权益数据加载失败，请稍后重试。'}finally{loading.value=false}}
async function runCheck(){submitting.value=true;error.value='';try{await mutation.run('entitlement-check',async()=>{const res=await checkAdmission({request:{featureCode:check.featureCode,estimatedQuantity:check.estimatedQuantity,requestId:`preview-${Date.now()}`},securityAllowed:true,shadowMode:false});decision.value=res.data;return res},null)}catch(e){error.value=e.message||'准入检查未完成，请稍后重试。'}finally{submitting.value=false}}
onMounted(load)
</script>
