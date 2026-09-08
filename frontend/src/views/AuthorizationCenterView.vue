<template>
  <div class="page-grid authorization-page">
    <PageToolbar eyebrow="治理中心" title="角色与菜单权限" description="按角色配置企业成员可见的业务入口。" />
    <ResourceContextRail label="授权范围" name="角色与菜单权限" state="可配置" hint="选择成员和角色后更新业务入口权限" mark="R" />
    <section class="section-card authorization-hero">
      <div>
        <div class="section-eyebrow">企业管理 / 权限配置</div>
        <h3>只管理业务入口</h3>
        <p class="section-description">应用、模型、知识库和连接器不再单独授权，成员能否使用由菜单权限和租户归属共同决定。</p>
      </div>
      <div class="permission-model"><span>当前策略</span><strong>菜单权限控制</strong></div>
    </section>

    <div class="role-layout">
      <section class="section-card role-list-panel">
        <div class="section-header">
          <div><h3>企业角色</h3><p class="section-description">选择角色配置可见入口。</p></div>
          <button class="primary-btn" @click="openRoleForm()">新增角色</button>
        </div>
        <div v-if="roles.length === 0" class="empty-state">当前企业暂无角色配置。</div>
        <button v-for="role in roles" :key="role.id" class="role-row" :class="{ selected: selectedRole?.id === role.id }" @click="selectRole(role)">
          <span class="role-row-mark">{{ (role.roleName || '角').substring(0, 1) }}</span>
          <span class="role-row-copy"><strong>{{ role.roleName }}</strong><small>{{ role.roleCode }} · {{ role.userCount || 0 }} 人</small></span>
          <span class="status-badge" :class="role.status === 'ACTIVE' ? 'status-success' : 'status-muted'">{{ role.status === 'ACTIVE' ? '启用' : '停用' }}</span>
        </button>
      </section>

      <section class="section-card role-detail-panel">
        <template v-if="selectedRole">
          <div class="section-header">
            <div><div class="section-eyebrow">菜单入口配置</div><h3>{{ selectedRole.roleName }}</h3><p class="section-description">勾选后，该角色成员即可看到对应入口。</p></div>
            <button class="ghost-btn" @click="openRoleForm(selectedRole)">编辑角色</button>
          </div>
          <div class="menu-permission-grid">
            <label v-for="menu in menuTree" :key="menu.id" class="menu-permission-item" :style="{ paddingLeft: `${12 + menu.depth * 20}px` }">
              <input :checked="isMenuSelected(menu.id)" type="checkbox" @change="toggleMenu(menu)" />
              <span><strong>{{ menu.depth ? '└ ' : '' }}{{ menuDisplayName(menu) }}</strong><small>{{ menu.path || '目录入口' }}</small></span>
            </label>
          </div>
          <div class="role-save-bar"><span v-if="message" :class="messageType">{{ message }}</span><button class="primary-btn" :disabled="saving" @click="saveRoleMenus">{{ saving ? '保存中...' : '保存菜单权限' }}</button></div>
          <div class="role-member-assign">
            <div><strong>分配成员</strong><small>新成员会继承该角色的菜单权限</small></div>
            <select v-model="selectedUserId" class="text-input"><option value="">请选择成员</option><option v-for="user in users" :key="user.id" :value="user.id">{{ user.nickname || user.username }}（{{ user.username }}）</option></select>
            <button class="ghost-btn" @click="assignRoleToUser">分配角色</button>
          </div>
        </template>
        <div v-else class="empty-state">从左侧选择一个角色查看菜单权限。</div>
      </section>
    </div>

    <div v-if="roleDialog" class="modal-overlay" @click.self="roleDialog = false"><section class="modal-card role-dialog"><h3>{{ roleForm.id ? '编辑企业角色' : '新增企业角色' }}</h3><div class="form-group"><label>角色名称</label><input v-model.trim="roleForm.roleName" class="text-input" placeholder="例如 内容运营" /></div><div class="form-group"><label>状态</label><select v-model="roleForm.status" class="text-input"><option value="ACTIVE">启用</option><option value="DISABLED">停用</option></select></div><div class="modal-actions"><button class="ghost-btn" @click="roleDialog = false">取消</button><button class="primary-btn" @click="saveRole">保存角色</button></div></section></div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import PageToolbar from '../components/PageToolbar.vue'
import ResourceContextRail from '../components/ResourceContextRail.vue'
import http from '../api/http'

const roles = ref([])
const menus = ref([])
const users = ref([])
const selectedRole = ref(null)
const selectedMenuIds = ref([])
const selectedUserId = ref('')
const roleDialog = ref(false)
const saving = ref(false)
const message = ref('')
const messageType = ref('success-text')
const roleForm = reactive({ id: null, roleCode: '', roleName: '', status: 'ACTIVE' })

// 将菜单平铺为带层级的列表，便于角色管理员快速理解父子入口关系。
const menuTree = computed(() => {
  const children = new Map()
  menus.value.forEach(menu => {
    const parentId = Number(menu.parentId || 0)
    const list = children.get(parentId) || []
    list.push(menu)
    children.set(parentId, list)
  })
  children.forEach(list => list.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0) || a.id - b.id))
  const result = []
  const visit = (parentId, depth) => (children.get(parentId) || []).forEach(menu => { result.push({ ...menu, depth }); visit(Number(menu.id), depth + 1) })
  visit(0, 0)
  return result.length ? result : menus.value.map(menu => ({ ...menu, depth: 0 }))
})

const isMenuSelected = id => selectedMenuIds.value.includes(Number(id))
const menuDisplayName = menu => ({ '/dashboard': '我的工作台', '/applications': '业务应用', '/chat': '发起业务', '/knowledge': '知识资源', '/workflow-executions': '运行中心', '/approval': '任务中心', '/users': '组织与权限', '/models': '模型底座', '/tenants': '租户管理' }[menu.path] || menu.menuName)
const selectRole = role => { selectedRole.value = role; selectedMenuIds.value = (role.menuIds || []).map(Number); message.value = '' }
const responseData = response => response?.data || []

const load = async () => {
  const [roleResponse, menuResponse, userResponse] = await Promise.all([http.get('/iam/roles'), http.get('/iam/menus'), http.get('/organization/users')])
  roles.value = responseData(roleResponse); menus.value = responseData(menuResponse); users.value = responseData(userResponse)
  if (roles.value.length) selectRole(roles.value.find(role => role.id === selectedRole.value?.id) || roles.value[0])
}
const toggleMenu = menu => { const ids = new Set(selectedMenuIds.value); ids.has(Number(menu.id)) ? ids.delete(Number(menu.id)) : ids.add(Number(menu.id)); selectedMenuIds.value = [...ids] }
const openRoleForm = role => { Object.assign(roleForm, role ? { id: role.id, roleCode: role.roleCode, roleName: role.roleName, status: role.status } : { id: null, roleCode: '', roleName: '', status: 'ACTIVE' }); roleDialog.value = true }
const saveRole = async () => {
  if (!roleForm.roleName) { messageType.value = 'error-text'; message.value = '请输入角色名称。'; return }
  const response = roleForm.id ? await http.put(`/iam/roles/${roleForm.id}`, roleForm) : await http.post('/iam/roles', roleForm)
  roleDialog.value = false; messageType.value = 'success-text'; message.value = '角色已保存。'; await load()
}
const saveRoleMenus = async () => {
  if (!selectedRole.value) return
  saving.value = true
  try { await http.put(`/iam/roles/${selectedRole.value.id}/menus`, { menuIds: selectedMenuIds.value }); messageType.value = 'success-text'; message.value = '角色菜单权限已保存。'; await load() } finally { saving.value = false }
}
const assignRoleToUser = async () => {
  if (!selectedRole.value || !selectedUserId.value) { messageType.value = 'error-text'; message.value = '请选择成员后再分配角色。'; return }
  await http.put(`/iam/users/${selectedUserId.value}/role`, { roleId: selectedRole.value.id }); messageType.value = 'success-text'; message.value = '成员角色已更新。'; await load()
}
onMounted(load)
</script>
