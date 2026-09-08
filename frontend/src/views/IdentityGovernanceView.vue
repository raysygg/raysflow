<template>
  <main class="saas-workbench page-grid">
    <PageToolbar title="企业身份" description="配置单点登录、成员同步和强化认证，并保留可恢复的历史版本。"><template #actions><button class="ghost-btn compact-btn" :disabled="loading" @click="load">刷新</button></template></PageToolbar>
    <ResourceContextRail label="身份配置" name="企业身份与认证" :state="active ? '已启用' : '待配置'" hint="验证连接后启用身份配置" mark="I" />
    <section class="saas-status-strip" aria-label="身份配置状态">
      <div class="saas-status-item"><small>当前生效版本</small><strong>{{ active ? `第 ${active.versionNo} 版` : '尚未启用' }}</strong></div>
      <div class="saas-status-item"><small>连接协议</small><strong>{{ protocolLabel(active?.protocol) }}</strong></div>
      <div class="saas-status-item"><small>成员同步</small><strong>{{ active?.scimEnabled ? '已启用' : '未启用' }}</strong></div>
      <div class="saas-status-item"><small>待验证草稿</small><strong>{{ configs.filter(item => item.status === 'DRAFT').length }} 个</strong></div>
    </section>
    <section class="saas-work-area">
      <nav class="saas-task-nav" aria-label="身份任务"><button v-for="item in tabs" :key="item.key" :class="{active:tab===item.key}" @click="tab=item.key">{{ item.label }}</button></nav>
      <div class="saas-pane">
        <PageState v-if="error" type="error" title="身份配置加载失败" :message="error" action-label="重新加载" @action="load" />
        <PageState v-else-if="loading" type="loading" message="正在恢复身份配置，请稍候..." />
        <template v-else-if="tab==='versions'">
          <div class="saas-pane-head"><div><h2>配置版本</h2><p>只有通过连接验证的版本才能启用，恢复会生成新草稿。</p></div></div>
          <div v-if="!configs.length" class="saas-state">尚无身份配置，请先创建草稿。</div>
          <div v-else class="saas-table-wrap"><table class="saas-table"><thead><tr><th>版本</th><th>协议</th><th>状态</th><th>组织声明</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in configs" :key="item.id"><td>第 {{ item.versionNo }} 版</td><td>{{ protocolLabel(item.protocol) }}</td><td><span class="saas-tag" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span></td><td>{{ item.organizationClaim || '未配置' }}</td><td>{{ time(item.updatedAt) }}</td><td><div class="saas-actions"><button v-if="item.status==='DRAFT'" class="ghost-btn compact-btn" @click="validate(item)">验证连接</button><button v-if="item.status==='VALIDATED'" class="primary-btn compact-btn" @click="activate(item)">启用</button><button v-if="item.status==='DISABLED'" class="ghost-btn compact-btn" @click="restore(item)">恢复为草稿</button></div></td></tr></tbody></table></div>
        </template>
        <template v-else-if="tab==='draft'">
          <div class="saas-pane-head"><div><h2>新建配置草稿</h2><p>密钥仅填写安全存储引用，不在页面保存凭证原文。</p></div></div>
          <div class="saas-form-grid"><div class="saas-field"><label>连接协议</label><select v-model="draft.protocol"><option value="OIDC">开放身份连接</option><option value="SAML">安全断言连接</option></select></div><div class="saas-field"><label>颁发方地址</label><input v-model.trim="draft.issuer" placeholder="请输入安全地址" /></div><div class="saas-field"><label>实体标识</label><input v-model.trim="draft.entityId" placeholder="请输入实体标识" /></div><div class="saas-field"><label>回调地址</label><input v-model.trim="draft.callbackUrl" placeholder="请输入平台回调地址" /></div><div class="saas-field"><label>组织声明字段</label><input v-model.trim="draft.organizationClaim" placeholder="例如组织编号字段" /></div><div class="saas-field"><label>身份凭证引用</label><ResourceSelect v-model="draft.secretRef" :options="credentialOptions" resource-label="身份凭证" placeholder="请选择安全凭证" create-label="填写新凭证" @create="showCredentialEditor = true" /></div><div class="saas-field wide"><label>声明映射</label><KeyValueEditor v-model="draft.claimMappings" /></div></div>
          <div v-if="showCredentialEditor" class="credential-editor"><label>凭证名称<input v-model.trim="credentialForm.name" placeholder="例如：企业身份生产凭证" /></label><label>凭证内容<input v-model="credentialForm.secret" type="password" autocomplete="new-password" placeholder="仅保存一次，不会再次显示" /></label><button class="ghost-btn" type="button" @click="createCredential">保存并绑定</button></div>
          <div class="saas-actions"><button class="primary-btn" :disabled="submitting" @click="saveDraft">保存草稿</button></div>
        </template>
        <template v-else-if="tab==='mfa'">
          <div class="saas-pane-head"><div><h2>强化认证策略</h2><p>按角色控制强化认证，并为高风险操作设置再次验证。</p></div></div>
          <div class="saas-form-grid"><div class="saas-field"><label>适用角色</label><select v-model="mfa.roleCode"><option value="ADMIN">企业管理员</option><option value="SUPER_ADMIN">平台管理员</option><option value="FINANCE">财务人员</option></select></div><div class="saas-field"><label>再次验证有效分钟</label><input v-model.number="mfa.stepUpMinutes" type="number" min="1" max="120" /></div><div class="saas-field wide"><label><input v-model="mfa.required" type="checkbox" /> 强制启用强化认证</label></div><div class="saas-field wide"><label>高风险操作</label><RepeatableListEditor v-model="mfa.highRiskActions" /></div></div><div class="saas-actions"><button class="primary-btn" :disabled="submitting" @click="saveMfa">保存策略</button></div>
        </template>
        <template v-else>
          <div class="saas-pane-head"><div><h2>成员同步检查</h2><p>用于验证单个成员同步与去配置，批量任务由身份提供方触发。</p></div></div>
          <div class="saas-form-grid"><div class="saas-field"><label>来源系统</label><input v-model.trim="scim.sourceSystem" placeholder="请输入来源名称" /></div><div class="saas-field"><label>外部成员标识</label><input v-model.trim="scim.externalId" /></div><div class="saas-field"><label>账号</label><input v-model.trim="scim.userName" /></div><div class="saas-field"><label>显示名称</label><input v-model.trim="scim.displayName" /></div><div class="saas-field"><label>邮箱</label><input v-model.trim="scim.email" /></div><div class="saas-field"><label>同步动作</label><select v-model="scim.active"><option :value="true">启用成员</option><option :value="false">去配置成员</option></select></div></div><div class="saas-actions"><button class="primary-btn" :disabled="submitting" @click="syncMember">执行同步</button></div>
        </template>
      </div>
    </section>
  </main>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import PageState from '../components/PageState.vue'
import ResourceSelect from '../components/structured/ResourceSelect.vue'
import KeyValueEditor from '../components/structured/KeyValueEditor.vue'
import RepeatableListEditor from '../components/structured/RepeatableListEditor.vue'
import { activateIdentityConfig, fetchIdentityConfigs, restoreIdentityConfig, saveIdentityDraft, saveMfaPolicy, syncScimUser, validateIdentityConfig } from '../api/saas'
import { useMutationState } from '../composables/useMutationState.js'
import { notify } from '../utils/feedback'
import http from '../api/http'
const tabs=[{key:'versions',label:'配置版本'},{key:'draft',label:'新建草稿'},{key:'mfa',label:'强化认证'},{key:'scim',label:'成员同步'}]
const tab=ref('versions'),loading=ref(false),submitting=ref(false),error=ref(''),configs=ref([])
const mutation = useMutationState()
const draft=reactive({protocol:'OIDC',issuer:'',entityId:'',organizationClaim:'',callbackUrl:'',claimMappings:[],scimEnabled:true,secretRef:''})
const mfa=reactive({roleCode:'ADMIN',required:true,stepUpMinutes:30,highRiskActions:['身份配置','数据治理']})
const scim=reactive({sourceSystem:'企业身份源',externalId:'',userName:'',displayName:'',email:'',active:true,roleCodes:[]})
const credentialOptions=ref([]),showCredentialEditor=ref(false)
const credentialForm=reactive({name:'',secret:''})
const active=computed(()=>configs.value.find(item=>item.status==='ACTIVE'))
const protocolLabel=value=>({OIDC:'开放身份连接',SAML:'安全断言连接'}[value]||'未配置')
const statusLabel=value=>({DRAFT:'草稿',VALIDATED:'已验证',ACTIVE:'使用中',DISABLED:'已停用'}[value]||'未知')
const statusClass=value=>value==='ACTIVE'?'':value==='VALIDATED'?'warn':value==='DISABLED'?'bad':'warn'
const time=value=>value?String(value).replace('T',' ').slice(0,16):'未记录'
async function load(){loading.value=true;error.value='';try{const [res,credentialResponse]=await Promise.all([fetchIdentityConfigs(), http.get('/saas/identity/credentials')]);configs.value=res.data||[];credentialOptions.value=(credentialResponse.data||[]).map(item=>({value:String(item.id),label:item.name,status:item.status==='ACTIVE'?'可用':'已停用'}))}catch(e){error.value=e.message||'身份配置加载失败，请检查权限后重试。'}finally{loading.value=false}}
async function run(key,action,message){submitting.value=true;error.value='';try{await mutation.run(key,action,load);notify(message,'success')}catch(e){error.value=e.message||'操作未完成，请稍后重试。'}finally{submitting.value=false}}
const saveDraft=()=>run('identity-draft',()=>saveIdentityDraft({...draft,claimMappingJson:JSON.stringify(Object.fromEntries(draft.claimMappings.filter(item=>item.key))),mfaPolicyJson:'{}'}),'身份配置草稿已保存')
const validate=item=>run(`identity-validate-${item.id}`,()=>validateIdentityConfig(item.id),'连接验证已完成')
const activate=item=>run(`identity-activate-${item.id}`,()=>activateIdentityConfig(item.id),'身份配置已启用')
const restore=item=>run(`identity-restore-${item.id}`,()=>restoreIdentityConfig(item.id),'旧版本已恢复为新草稿')
const saveMfa=()=>run('identity-mfa',()=>saveMfaPolicy({...mfa,highRiskActionsJson:JSON.stringify(mfa.highRiskActions.filter(Boolean))}),'强化认证策略已保存')
const syncMember=()=>run('identity-scim',()=>syncScimUser({...scim}),'成员同步已完成')
const createCredential=async()=>{if(!credentialForm.name||!credentialForm.secret){notify('请填写凭证名称和内容。','warning');return}try{const response=await http.post('/saas/identity/credentials',credentialForm);const created=response.data;credentialOptions.value=[...credentialOptions.value,{value:String(created.id),label:created.name,status:'可用'}];draft.secretRef=String(created.id);showCredentialEditor.value=false;Object.assign(credentialForm,{name:'',secret:''});notify('身份凭证已保存并绑定。','success')}catch(error){notify(error.message||'身份凭证保存失败。','error')}}
onMounted(load)
</script>
