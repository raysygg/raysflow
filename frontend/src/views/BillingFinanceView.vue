<template>
  <main class="saas-workbench page-grid">
    <PageToolbar title="账期与账单" description="复核账期、账单、成本中心、调整项和外部财务同步状态。"><template #actions><button class="ghost-btn compact-btn" :disabled="loading" @click="load">刷新</button></template></PageToolbar>
    <ResourceContextRail label="财务范围" name="企业账期与账单" :state="currentPeriod ? '账期进行中' : '待开启账期'" hint="账单确认后再同步外部财务系统" mark="B" />
    <section class="saas-status-strip"><div class="saas-status-item"><small>当前账期</small><strong>{{ currentPeriod ? `${currentPeriod.periodStart} 至 ${currentPeriod.periodEnd}` : '尚未开启' }}</strong></div><div class="saas-status-item"><small>草稿账单</small><strong>{{ invoices.filter(i=>i.status==='DRAFT').length }} 份</strong></div><div class="saas-status-item"><small>已确认金额</small><strong>{{ money(finalizedTotal) }}</strong></div><div class="saas-status-item"><small>待确认同步</small><strong>{{ syncs.filter(i=>['UNKNOWN','PENDING','FAILED'].includes(i.syncStatus)).length }} 项</strong></div></section>
    <section class="saas-work-area"><nav class="saas-task-nav"><button v-for="item in tabs" :key="item.key" :class="{active:tab===item.key}" @click="tab=item.key">{{ item.label }}</button></nav><div class="saas-pane">
      <PageState v-if="error" type="error" title="账期与账单加载失败" :message="error" action-label="重新加载" @action="load" /><PageState v-else-if="loading" type="loading" message="正在恢复账期与账单状态..." />
      <template v-else-if="tab==='periods'"><div class="saas-pane-head"><div><h2>账期管理</h2><p>开启账期后，可从已结算用量生成可复核草稿账单。</p></div></div><div class="saas-form-grid"><div class="saas-field"><label>开始日期</label><input v-model="period.start" type="date" /></div><div class="saas-field"><label>结束日期</label><input v-model="period.end" type="date" /></div><div class="saas-field"><label>结算币种</label><select v-model="period.currency"><option value="CNY">人民币</option><option value="USD">美元</option></select></div></div><div class="saas-actions"><button class="primary-btn" :disabled="submitting" @click="openPeriod">开启账期</button></div><div v-if="periods.length" class="saas-table-wrap"><table class="saas-table"><thead><tr><th>账期</th><th>币种</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in periods" :key="item.id"><td>{{ item.periodStart }} 至 {{ item.periodEnd }}</td><td>{{ currencyLabel(item.currency) }}</td><td>{{ periodStatus(item.periodStatus) }}</td><td><button v-if="item.periodStatus==='OPEN'" class="ghost-btn compact-btn" @click="draft(item)">生成草稿账单</button></td></tr></tbody></table></div></template>
      <template v-else-if="tab==='invoices'"><div class="saas-pane-head"><div><h2>账单与调整</h2><p>最终账单保持不可变，退款或补收通过独立调整单保留历史。</p></div></div><div v-if="!invoices.length" class="saas-state">当前没有账单，请先开启账期并生成草稿。</div><div v-else class="saas-table-wrap"><table class="saas-table"><thead><tr><th>账单编号</th><th>状态</th><th>金额</th><th>确认时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in invoices" :key="item.id"><td>{{ item.invoiceNumber||`草稿 ${item.id}` }}</td><td><span class="saas-tag" :class="item.status==='DRAFT'?'warn':''">{{ invoiceStatus(item.status) }}</span></td><td>{{ money(item.total,item.currency) }}</td><td>{{ time(item.finalizedAt) }}</td><td><div class="saas-actions"><button v-if="item.status==='DRAFT'" class="primary-btn compact-btn" @click="finalize(item)">确认账单</button><button v-else class="ghost-btn compact-btn" @click="download(item)">导出账单</button></div></td></tr></tbody></table></div></template>
      <template v-else><div class="saas-pane-head"><div><h2>外部同步状态</h2><p>超时保持待确认，可使用相同幂等标识安全重试。</p></div></div><div v-if="!syncs.length" class="saas-state">尚无外部财务同步记录。</div><div v-else class="saas-table-wrap"><table class="saas-table"><thead><tr><th>账单</th><th>目标系统</th><th>状态</th><th>安全摘要</th><th>下次重试</th></tr></thead><tbody><tr v-for="item in syncs" :key="item.id"><td>{{ item.invoiceId }}</td><td>{{ item.adapterType||'财务系统' }}</td><td><span class="saas-tag" :class="item.syncStatus==='CONFIRMED'?'':item.syncStatus==='FAILED'?'bad':'warn'">{{ syncStatus(item.syncStatus) }}</span></td><td>{{ item.safeSummary||'未返回确认信息' }}</td><td>{{ time(item.nextRetryAt) }}</td></tr></tbody></table></div></template>
    </div></section>
  </main>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import PageState from '../components/PageState.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import { createBillingPeriod, exportInvoice, fetchBillingOverview, finalizeInvoice, generateInvoice } from '../api/saas'
import { notify } from '../utils/feedback'
import { useMutationState } from '../composables/useMutationState.js'
const tabs=[{key:'periods',label:'账期'},{key:'invoices',label:'账单与调整'},{key:'sync',label:'外部同步'}]
const tab=ref('periods'),loading=ref(false),submitting=ref(false),error=ref(''),periods=ref([]),invoices=ref([]),syncs=ref([])
const mutation = useMutationState()
const now=new Date(),period=reactive({start:`${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-01`,end:new Date(now.getFullYear(),now.getMonth()+1,0).toISOString().slice(0,10),currency:'CNY'})
const currentPeriod=computed(()=>periods.value.find(i=>i.periodStatus==='OPEN')||periods.value[0])
const finalizedTotal=computed(()=>invoices.value.filter(i=>i.status==='FINALIZED').reduce((sum,i)=>sum+Number(i.total||0),0))
const currencyLabel=v=>v==='CNY'?'人民币':v==='USD'?'美元':'未知币种'
const money=(v,c='CNY')=>`${c==='USD'?'美元':'人民币'} ${Number(v||0).toFixed(2)}`
const periodStatus=v=>({OPEN:'进行中',FINALIZED:'已结算'}[v]||'未知')
const invoiceStatus=v=>({DRAFT:'待复核',FINALIZED:'已确认'}[v]||'未知')
const syncStatus=v=>({CONFIRMED:'已确认',UNKNOWN:'待确认',PENDING:'处理中',FAILED:'同步失败'}[v]||'未知')
const time=v=>v?String(v).replace('T',' ').slice(0,16):'未记录'
async function load(){loading.value=true;error.value='';try{const res=await fetchBillingOverview();const data=res.data||{};periods.value=data.periods||[];invoices.value=data.invoices||[];syncs.value=data.syncs||[]}catch(e){error.value=e.message||'账单信息加载失败，当前角色可能没有财务权限。'}finally{loading.value=false}}
async function action(key,fn,message){submitting.value=true;error.value='';try{await mutation.run(key,fn,load);notify(message,'success')}catch(e){error.value=e.message||'操作未完成，请稍后重试。'}finally{submitting.value=false}}
const openPeriod=()=>action('billing-period',()=>createBillingPeriod({...period}),'账期已开启')
const draft=item=>action(`billing-draft-${item.id}`,()=>generateInvoice(item.id),'草稿账单已生成')
const finalize=item=>action(`billing-finalize-${item.id}`,()=>finalizeInvoice(item.id),'账单已确认并锁定')
async function download(item){try{const res=await exportInvoice(item.id);const blob=new Blob([res.data?.content||''],{type:'text/csv;charset=utf-8'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=res.data?.fileName||`账单-${item.id}.csv`;a.click();URL.revokeObjectURL(url)}catch(e){error.value=e.message||'账单导出失败，请检查财务导出权限。'}}
onMounted(load)
</script>
