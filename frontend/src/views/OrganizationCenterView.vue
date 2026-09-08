<template>
  <div class="page-grid organization-page">
    <PageToolbar eyebrow="企业治理" title="组织与成员" description="维护组织层级、成员关系和负责人范围。" />
    <ResourceContextRail label="企业组织" name="组织与成员" state="可管理" hint="先维护组织层级，再分配成员职责" mark="O" />
    <div class="organization-tabs" role="tablist">
      <button class="tab-button" :class="{ active: activeTab === 'organization' }" @click="activeTab = 'organization'">组织架构</button>
      <button class="tab-button" :class="{ active: activeTab === 'authorization' }" @click="activeTab = 'authorization'">角色与工作区入口</button>
    </div>
    <AuthorizationCenterView v-if="activeTab === 'authorization'" />
    <template v-else>
    <SectionCard eyebrow="企业管理" title="组织与成员" description="先建立企业组织层级，再通过成员关系、直属上级和角色权限管理员工能看什么、能做什么。">
      <template #action><button class="primary-btn" @click="openCreate">新增组织</button></template>
    </SectionCard>

    <div class="organization-layout">
      <SectionCard title="组织目录" description="选择一个组织查看成员，组织层级只在当前企业租户内生效。">
        <div class="org-toolbar"><input v-model.trim="orgSearch" class="text-input" autocomplete="off" spellcheck="false" placeholder="搜索组织名称或编码" /><span>{{ filteredUnits.length }} 个组织</span></div>
        <p v-if="loadError" class="empty-state">{{ loadError }}</p>
        <p v-else-if="units.length === 0" class="empty-state">暂无组织单元，请先创建企业根组织。</p>
        <div v-else class="org-tree">
          <button v-for="unit in filteredUnits" :key="unit.id" class="org-row" :class="{ selected: selectedUnitId === unit.id }" :style="{ paddingLeft: `${12 + unit.treeDepth * 22}px` }" @click="selectedUnitId = unit.id">
            <span class="org-branch" @click.stop="toggleExpanded(unit.id)">{{ unit.hasChildren ? (expandedUnitIds.has(unit.id) ? '−' : '+') : '·' }}</span>
            <div class="org-main"><strong>{{ unit.orgName }}</strong><small>{{ unit.orgCode }} · {{ unit.memberCount }} 名成员</small></div>
            <span class="pill">{{ unit.orgType === 'COMPANY' ? '企业' : '部门' }}</span>
            <span class="org-row-action" @click.stop="openEdit(unit)">编辑</span>
          </button>
        </div>
      </SectionCard>

      <SectionCard title="成员关系" description="成员的主组织、兼职组织和直属上级会影响组织范围授权与审批范围。">
        <div class="member-assignment-head"><strong>调整成员归属</strong><span>选择成员后保存关系</span></div>
        <div class="form-group"><label>成员</label><select v-model="assignment.userId" class="text-input"><option value="">请选择成员</option><option v-for="user in users" :key="user.id" :value="user.id">{{ user.nickname || user.username }}（{{ user.username }}）</option></select></div>
        <div class="form-group"><label>组织单元</label><select v-model="assignment.orgUnitId" class="text-input"><option value="">请选择组织</option><option v-for="unit in units" :key="unit.id" :value="unit.id">{{ unit.orgName }}</option></select></div>
        <div class="form-group"><label>直属上级</label><select v-model="assignment.managerUserId" class="text-input"><option :value="null">无直属上级</option><option v-for="user in users.filter(item => item.id !== assignment.userId)" :key="`manager-${user.id}`" :value="user.id">{{ user.nickname || user.username }}（{{ user.username }}）</option></select></div>
        <label class="check-line"><input v-model="assignment.primary" type="checkbox" /> 设为主组织（一个成员只能有一个主组织）</label>
        <button class="primary-inline-btn" @click="assignUser">保存成员关系</button>
        <p v-if="assignmentMessage" class="form-message">{{ assignmentMessage }}</p>
        <div class="member-list">
          <div class="member-list-head"><strong>当前成员</strong><span>{{ users.length }} 人 <button class="ghost-btn compact-action" :disabled="!selectedUserIds.size" @click="batchAssign">批量应用当前归属</button></span></div>
          <div v-for="user in visibleUsers" :key="user.id" class="member-row">
            <input type="checkbox" :checked="selectedUserIds.has(user.id)" @change="toggleUserSelection(user.id)" />
            <span class="member-avatar">{{ (user.nickname || user.username || 'U').substring(0, 1) }}</span>
            <div class="member-copy"><strong>{{ user.nickname || user.username }}</strong><small>{{ user.username }} · {{ user.managerName ? `上级：${user.managerName}` : '暂未设置直属上级' }}</small></div>
            <select class="member-status-select" :value="user.status" @change="changeUserStatus(user, $event.target.value)"><option value="ACTIVE">启用</option><option value="DISABLED">停用</option><option value="LEAVE">离职</option><option value="FROZEN">冻结</option></select>
          </div>
        </div>
      </SectionCard>
    </div>

    <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
      <div class="modal-card">
        <h3>{{ editingUnit ? '编辑组织单元' : '新增组织单元' }}</h3>
        <div class="form-group"><label>上级组织</label><select v-model="form.parentId" class="text-input"><option :value="0">作为根组织</option><option v-for="unit in units.filter(item => item.id !== editingUnit?.id)" :key="unit.id" :value="unit.id">{{ unit.orgName }}</option></select></div>
        <div class="form-group system-id-note"><label>系统标识</label><strong>自动生成</strong><small>保存组织名称时由系统生成</small></div>
        <div class="form-group"><label>组织名称</label><input v-model.trim="form.orgName" class="text-input" autocomplete="off" spellcheck="false" placeholder="例如 客户成功中心" /></div>
        <div class="form-group"><label>组织类型</label><select v-model="form.orgType" class="text-input"><option value="COMPANY">企业</option><option value="DEPARTMENT">部门</option><option value="TEAM">小组</option></select></div>
        <div class="form-group"><label>负责人</label><select v-model="form.leaderUserId" class="text-input"><option :value="null">暂不设置</option><option v-for="user in users" :key="user.id" :value="user.id">{{ user.nickname || user.username }}</option></select></div>
        <div class="modal-actions"><button class="ghost-btn" @click="dialogVisible = false">取消</button><button class="primary-btn" @click="saveUnit">保存组织</button></div>
      </div>
    </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SectionCard from '../components/SectionCard.vue'
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import AuthorizationCenterView from './AuthorizationCenterView.vue'
import http from '../api/http'
import { notify } from '../utils/feedback'

const units = ref([])
const users = ref([])
const loadError = ref('')
const assignmentMessage = ref('')
const dialogVisible = ref(false)
const editingUnit = ref(null)
const orgSearch = ref('')
const selectedUnitId = ref(null)
const activeTab = ref('organization')
const expandedUnitIds = ref(new Set())
const form = reactive({ parentId: 0, orgCode: '', orgName: '', orgType: 'DEPARTMENT', leaderUserId: null })
const assignment = reactive({ userId: '', orgUnitId: '', primary: true, managerUserId: null })
const selectedUserIds = ref(new Set())
const sortedUnits = computed(() => {
  const children = new Map()
  units.value.forEach(unit => {
    const siblings = children.get(unit.parentId) || []
    siblings.push(unit)
    children.set(unit.parentId, siblings)
  })
  children.forEach(siblings => siblings.sort((a, b) => (a.sortOrder - b.sortOrder) || (a.id - b.id)))
  const result = []
  const visit = (parentId, treeDepth) => {
    ;(children.get(parentId) || []).forEach(unit => {
      const childUnits = children.get(unit.id) || []
      result.push({ ...unit, treeDepth, hasChildren: childUnits.length > 0 })
      if (expandedUnitIds.value.has(unit.id)) visit(unit.id, treeDepth + 1)
    })
  }
  visit(0, 0)
  return result
})
const filteredUnits = computed(() => {
  const keyword = orgSearch.value.toLowerCase()
  if (!keyword) return sortedUnits.value
  return sortedUnits.value.filter(unit => `${unit.orgName} ${unit.orgCode}`.toLowerCase().includes(keyword))
})
const visibleUsers = computed(() => {
  if (!selectedUnitId.value) return users.value
  const selected = units.value.find(unit => unit.id === selectedUnitId.value)
  if (!selected) return users.value
  return users.value.filter(user => user.orgUnitId === selected.id || !user.orgUnitId)
})
const load = async () => {
  try {
    const [unitResponse, userResponse] = await Promise.all([http.get('/organization/units'), http.get('/organization/users')])
    if (!unitResponse.success) throw new Error(unitResponse.message || '组织架构读取失败')
    units.value = unitResponse.data || []
    units.value.filter(unit => unit.parentId === 0).forEach(unit => expandedUnitIds.value.add(unit.id))
    if (!selectedUnitId.value && units.value.length) selectedUnitId.value = units.value[0].id
    if (userResponse.success) users.value = userResponse.data || []
  } catch (error) { loadError.value = error.message || '组织架构读取失败。' }
}
const toggleExpanded = (id) => {
  const next = new Set(expandedUnitIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedUnitIds.value = next
}
const openCreate = () => { editingUnit.value = null; Object.assign(form, { parentId: 0, orgCode: '', orgName: '', orgType: 'DEPARTMENT', leaderUserId: null }); dialogVisible.value = true }
const openEdit = (unit) => { editingUnit.value = unit; Object.assign(form, { parentId: unit.parentId, orgCode: unit.orgCode, orgName: unit.orgName, orgType: unit.orgType, leaderUserId: unit.leaderUserId }); dialogVisible.value = true }
const saveUnit = async () => {
  if (!form.orgName) { notify('请输入组织名称。', 'warning'); return }
  const response = editingUnit.value ? await http.put(`/organization/units/${editingUnit.value.id}`, form) : await http.post('/organization/units', form)
  dialogVisible.value = false; await load()
}
const assignUser = async () => {
  if (!assignment.userId || !assignment.orgUnitId) { assignmentMessage.value = '请选择用户和组织。'; return }
  const response = await http.post('/organization/assignments', assignment)
  assignmentMessage.value = '成员归属已保存。'
  await load()
}
const changeUserStatus = async (user, status) => {
  const response = await http.post(`/organization/users/${user.id}/status`, { status })
  user.status = status
  notify('成员状态已更新', 'success')
}
const toggleUserSelection = (id) => { const next = new Set(selectedUserIds.value); next.has(id) ? next.delete(id) : next.add(id); selectedUserIds.value = next }
const batchAssign = async () => {
  if (!assignment.orgUnitId) { notify('请先选择组织单元', 'warning'); return }
  const response = await http.post('/organization/users/batch-assignment', { userIds: [...selectedUserIds.value], orgUnitId: assignment.orgUnitId, primary: assignment.primary, managerUserId: assignment.managerUserId })
  selectedUserIds.value = new Set(); notify('成员批量调整已保存', 'success'); await load()
}
onMounted(load)
</script>

<style scoped>
.system-id-note { color: var(--muted); font-size: 11px; }
.system-id-note strong { display: block; margin-top: 4px; color: var(--accent); font-size: 12px; }
.organization-layout { display: grid; grid-template-columns: 1.2fr .8fr; gap: 18px; }
.organization-tabs { display: flex; gap: 6px; border-bottom: 1px solid #e7edf3; }
.tab-button { border: 0; border-bottom: 2px solid transparent; background: transparent; color: var(--muted); cursor: pointer; padding: 10px 14px; font-size: 13px; }
.tab-button.active { border-bottom-color: var(--accent); color: var(--text); font-weight: 700; }
.org-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }.org-toolbar .text-input { flex: 1; }.org-toolbar > span { color: var(--muted); font-size: 11px; white-space: nowrap; }
.org-tree { display: flex; flex-direction: column; gap: 7px; }
.org-row { display: flex; align-items: center; gap: 9px; width: 100%; min-height: 50px; padding-right: 10px; border: 1px solid #edf1f5; border-radius: 6px; background: #fbfcfd; color: var(--text); text-align: left; cursor: pointer; }.org-row:hover, .org-row.selected { border-color: #bfdbf4; background: #f3f8fd; }.org-row.selected { box-shadow: inset 3px 0 #3282c3; }
.org-branch { color: var(--accent); font-size: 12px; }
.org-main { flex: 1; min-width: 0; }
.org-main strong, .org-main small { display: block; }
.org-main strong { color: var(--text); font-size: 12px; }
.org-main small { margin-top: 3px; color: var(--muted); font-size: 10px; }
.check-line { display: flex; align-items: center; gap: 7px; margin: 12px 0 16px; color: var(--text); font-size: 12px; }
.primary-inline-btn { width: 100%; }
.form-message { color: var(--success); font-size: 11px; }
.org-row-action { border: 0; background: transparent; color: var(--accent); cursor: pointer; font-size: 11px; }
.member-assignment-head, .member-list-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }.member-assignment-head { margin-bottom: 14px; }.member-assignment-head strong, .member-list-head strong { color: var(--text); font-size: 12px; }.member-assignment-head span, .member-list-head span { color: var(--muted); font-size: 10px; }.member-list { margin-top: 22px; border-top: 1px solid #edf1f5; padding-top: 16px; }.member-list-head { margin-bottom: 9px; }.member-row { display: flex; align-items: center; gap: 9px; padding: 9px 0; border-bottom: 1px solid #f0f3f6; }.member-row:last-child { border-bottom: 0; }.member-avatar { display: grid; flex: 0 0 28px; place-items: center; width: 28px; height: 28px; border-radius: 7px; background: #eaf3ff; color: #2b78b7; font-size: 11px; font-weight: 700; }.member-copy { min-width: 0; flex: 1; }.member-copy strong, .member-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.member-copy strong { color: #40576c; font-size: 11px; }.member-copy small { margin-top: 3px; color: #91a0ae; font-size: 10px; }.status-muted { border-color: #dce4ea; background: #f4f6f8; color: #7e8d9b; }.member-status-select { max-width: 84px; border: 1px solid var(--line); border-radius: 6px; padding: 5px 6px; background: var(--panel-muted); color: var(--text); font-size: 11px; }.compact-action { margin-left: 8px; padding: 4px 7px; font-size: 10px; }
@media (max-width: 900px) { .organization-layout { grid-template-columns: 1fr; } }
</style>
