<template>
  <main class="saas-workbench page-grid"><PageToolbar title="产品采用分析" description="用真实业务事件观察激活、首次价值、活跃与留存，诊断和测试流量默认排除。"><template #actions><button class="ghost-btn compact-btn" :disabled="loading" @click="load">刷新</button></template></PageToolbar>
    <ResourceContextRail label="分析范围" name="企业产品采用" :state="loading ? '计算中' : qualityLabel" hint="指标仅统计真实业务事件" mark="A" />
    <section class="saas-status-strip"><div class="saas-status-item"><small>获得首次价值</small><strong>{{ report.firstValue?.reached ? '已达成' : '尚未达成' }}</strong></div><div class="saas-status-item"><small>首次价值时间</small><strong>{{ report.firstValue?.minutes==null?'暂无样本':`${report.firstValue.minutes} 分钟` }}</strong></div><div class="saas-status-item"><small>周活跃应用</small><strong>{{ report.active?.weeklyActiveApplications||0 }} 个</strong></div><div class="saas-status-item"><small>数据质量</small><strong>{{ qualityLabel }}</strong></div></section>
    <section class="saas-work-area"><nav class="saas-task-nav"><button :class="{active:tab==='funnel'}" @click="tab='funnel'">激活漏斗</button><button :class="{active:tab==='cohort'}" @click="tab='cohort'">留存分组</button><button :class="{active:tab==='definitions'}" @click="tab='definitions'">指标定义</button><button :class="{active:tab==='quality'}" @click="tab='quality'">数据质量</button></nav><div class="saas-pane">
      <div class="saas-form-grid"><div class="saas-field"><label>开始日期</label><input v-model="range.from" type="date" /></div><div class="saas-field"><label>结束日期</label><input v-model="range.to" type="date" /></div></div><div class="saas-actions"><button class="ghost-btn compact-btn" @click="load">应用时间范围</button></div>
      <PageState v-if="error" type="error" title="产品采用数据加载失败" :message="error" action-label="重新加载" @action="load" /><PageState v-else-if="loading" type="loading" message="正在计算真实采用事件..." />
      <template v-else-if="tab==='funnel'"><div class="saas-pane-head"><div><h2>激活与首次价值</h2><p>首次价值要求应用已发布并至少产生一次成功生产运行。</p></div></div><div v-if="!funnel.length" class="saas-state">所选范围内暂无真实客户采用事件。</div><div v-else class="saas-table-wrap"><table class="saas-table"><thead><tr><th>阶段</th><th>事件数</th><th>相对首阶段</th><th>首次发生</th></tr></thead><tbody><tr v-for="item in funnel" :key="item.eventType"><td>{{ item.label||eventLabel(item.eventType) }}</td><td>{{ item.eventCount }}</td><td>{{ funnelRate(item.eventCount) }}</td><td>{{ time(item.firstOccurredAt) }}</td></tr></tbody></table></div></template>
      <template v-else-if="tab==='cohort'"><div class="saas-pane-head"><div><h2>开通时间分组</h2><p>使用一致事件定义比较不同开通月份的生产成功与周活跃。</p></div></div><div v-if="!cohorts.length" class="saas-state">样本量不足，暂不展示分组结果。</div><div v-else class="saas-table-wrap"><table class="saas-table"><thead><tr><th>开通月份</th><th>生产成功事件</th><th>周活跃事件</th><th>说明</th></tr></thead><tbody><tr v-for="item in cohorts" :key="item.cohortMonth"><td>{{ item.cohortMonth }}</td><td>{{ item.productionSuccessEvents }}</td><td>{{ item.weeklyActiveEvents }}</td><td>仅统计真实客户流量</td></tr></tbody></table></div><div class="saas-state">{{ benchmarkMessage }}</div></template>
      <template v-else-if="tab==='definitions'"><div class="saas-pane-head"><div><h2>指标定义版本</h2><p>目标定义与真实达成结果分开显示，版本变化不会重写历史趋势。</p></div></div><div class="saas-table-wrap"><table class="saas-table"><thead><tr><th>指标</th><th>版本</th><th>价值阶段</th><th>默认排除</th></tr></thead><tbody><tr v-for="item in definitions" :key="item.eventType"><td>{{ item.label||eventLabel(item.eventType) }}</td><td>第 {{ report.definitionVersion||1 }} 版</td><td>{{ item.productionValueEvent?'生产价值事件':'过程里程碑' }}</td><td>草稿测试、平台诊断、自动化和迁移流量</td></tr></tbody></table></div></template>
      <template v-else><div class="saas-pane-head"><div><h2>事件质量监控</h2><p>监控延迟、重复、缺失字段和异常流量，不保存业务正文。</p></div></div><div class="saas-table-wrap"><table class="saas-table"><thead><tr><th>检查项</th><th>结果</th><th>数量</th><th>处理建议</th></tr></thead><tbody><tr v-for="item in qualityItems" :key="item.code"><td>{{ item.label||qualityItemLabel(item.code) }}</td><td><span class="saas-tag" :class="item.status==='HEALTHY'?'':'warn'">{{ item.status==='HEALTHY'?'正常':'需关注' }}</span></td><td>{{ item.count||0 }}</td><td>{{ item.message||'无需处理' }}</td></tr></tbody></table></div></template>
    </div></section>
  </main>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import PageState from '../components/PageState.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import { fetchAdoptionBenchmark, fetchAdoptionDefinitions, fetchAdoptionQuality, fetchAdoptionReport } from '../api/saas'
const tab=ref('funnel'),loading=ref(false),error=ref(''),definitions=ref([]),report=ref({}),quality=ref({}),benchmark=ref({}),range=reactive({from:'',to:''})
const funnel=computed(()=>report.value.funnel||[]),cohorts=computed(()=>report.value.cohorts||[])
const qualityItems=computed(()=>[
  {code:'COMPLETENESS',status:quality.value.incompleteEvents>0?'ATTENTION':'HEALTHY',count:quality.value.incompleteEvents,message:'补齐事件必填业务维度'},
  {code:'LATENCY',status:quality.value.delayedEvents>0?'ATTENTION':'HEALTHY',count:quality.value.delayedEvents,message:'检查事件写入链路延迟'},
  {code:'DUPLICATE',status:quality.value.duplicateEvents>0?'ATTENTION':'HEALTHY',count:quality.value.duplicateEvents,message:'核对幂等标识生成规则'},
  {code:'ABNORMAL_VOLUME',status:quality.value.abnormalTraffic?'ATTENTION':'HEALTHY',count:quality.value.abnormalTraffic?1:0,message:quality.value.conclusion||'无需处理'}])
const qualityLabel=computed(()=>qualityItems.value.some(i=>i.status!=='HEALTHY')?'需要关注':'正常')
const benchmarkMessage=computed(()=>benchmark.value.available?`已使用 ${benchmark.value.sampleTenants} 个匿名样本，生产激活率 ${percent(benchmark.value.productionActivationRate)}。`:`当前仅有 ${benchmark.value.sampleTenants||0} 个匿名样本，至少需要 ${benchmark.value.minimumSampleTenants||0} 个才展示跨租户基准。`)
const percent=v=>v==null?'暂无样本':`${(Number(v)*(Number(v)<=1?100:1)).toFixed(1)}%`
const eventLabel=v=>({TENANT_PROVISIONED:'企业开通',APPLICATION_DRAFT_CREATED:'创建首个应用',DRAFT_TEST_SUCCEEDED:'草稿测试成功',APPLICATION_RELEASED:'首次发布',PRODUCTION_RUN_SUCCEEDED:'首次生产成功',APPLICATION_ACTIVE:'活跃应用',QUALITY_TARGET_MET:'质量达标',RENEWAL_RISK:'续费风险'}[v]||'产品指标')
const qualityItemLabel=v=>({COMPLETENESS:'事件完整性',LATENCY:'事件延迟',DUPLICATE:'重复事件',ABNORMAL_VOLUME:'异常流量'}[v]||'事件检查')
const funnelRate=count=>{const base=Number(funnel.value[0]?.eventCount||0);return base?percent(Number(count)/base):'暂无样本'}
const time=v=>v?String(v).replace('T',' ').slice(0,16):'未发生'
async function load(){loading.value=true;error.value='';try{const params={from:range.from||undefined,to:range.to||undefined};const [a,b,c,d]=await Promise.all([fetchAdoptionDefinitions(),fetchAdoptionReport(params),fetchAdoptionQuality(params),fetchAdoptionBenchmark(params)]);definitions.value=a.data||[];report.value=b.data||{};quality.value=c.data||{};benchmark.value=d.data||{}}catch(e){error.value=e.message||'产品采用数据加载失败，请检查查看权限。'}finally{loading.value=false}}
onMounted(load)
</script>
