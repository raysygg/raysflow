<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div>
        <div class="brand-block">
          <div class="brand-mark">
            <svg class="brand-logo-svg" viewBox="0 0 38 38" fill="none" xmlns="http://www.w3.org/2000/svg" style="width:30px;height:30px;display:block;">
              <rect width="38" height="38" rx="10" fill="url(#rfNavGrad)"/>
              <!-- Rays & Flow Vector paths -->
              <path d="M10 9h9c3.8 0 6.8 2.8 6.8 6.2 0 2.6-1.8 4.8-4.4 5.7L27 30h-5.2L17.2 22H14.5v8H10V9zm4.5 3.8v5.5h4c1.5 0 2.7-1.2 2.7-2.75s-1.2-2.75-2.7-2.75h-4z" fill="#ffffff" opacity="0.95"/>
              <path d="M17 19c4-2.5 8-1 12-5m-8 11c4-2 8 0 11-2.5" stroke="#fef08a" stroke-width="2.2" stroke-linecap="round"/>
              <circle cx="29" cy="9" r="2.2" fill="#fbbf24"/>
              <circle cx="32" cy="15" r="1.5" fill="#ffffff" opacity="0.9"/>
              <defs>
                <linearGradient id="rfNavGrad" x1="0" y1="0" x2="38" y2="38" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#3730a3"/>
                  <stop offset="0.4" stop-color="#4f46e5"/>
                  <stop offset="0.75" stop-color="#7c3aed"/>
                  <stop offset="1" stop-color="#f59e0b"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <div>
            <h1>Raysflow</h1>
            <p>光流智能引擎</p>
          </div>
        </div>

        <div class="workspace-switcher">
          <span class="workspace-switcher-mark">{{ tenantName ? tenantName.substring(0, 1) : '企' }}</span>
           <span class="workspace-switcher-copy"><small>{{ isSuperAdmin ? '平台范围' : '当前企业' }}</small><strong>{{ workspaceLabel }}</strong></span>
          <span class="workspace-switcher-arrow">⌄</span>
        </div>

        <nav class="side-nav" aria-label="主导航">
          <section v-for="group in capabilityNavigation" :key="group.title" class="nav-section">
            <button class="nav-group" type="button" :aria-expanded="isGroupExpanded(group.title)" @click="toggleGroup(group.title)">
              <span>{{ group.title }}</span><span class="nav-group-chevron" aria-hidden="true">{{ isGroupExpanded(group.title) ? '⌃' : '⌄' }}</span>
            </button>
            <div v-show="isGroupExpanded(group.title)" class="nav-section-items">
              <RouterLink
                v-for="item in group.items"
                :key="item.path"
                :to="item.path"
                class="nav-item"
                :title="item.label"
                active-class="active"
              >
                <span class="nav-icon" aria-hidden="true">{{ item.icon }}</span>
                <span class="nav-label">{{ item.label }}</span>
                <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
              </RouterLink>
            </div>
          </section>
        </nav>
      </div>

      <div class="sidebar-foot">
      <div class="user-profile">
          <div class="user-avatar">{{ username ? username.substring(0, 2).toUpperCase() : 'U' }}</div>
          <div class="user-details">
            <span class="user-name">{{ nickname || username }}</span>
            <span class="role-badge" :class="role">{{ roleLabel }}</span>
          </div>
          <button @click="handleSignOut" class="signout-icon-btn" :disabled="signingOut" title="退出登录">↪</button>
        </div>
        <div class="status-chip">
          <span class="status-dot"></span>
          <span>服务运行正常</span>
        </div>
      </div>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div>
           <div class="breadcrumb-line"><span>{{ isSuperAdmin ? '平台控制台' : '企业工作区' }}</span><b>/</b><span>{{ workspaceLabel }}</span></div>
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="top-actions">
          <span class="top-tenant-code">{{ tenantDisplayLabel }}</span>
          <label class="theme-picker" title="切换全局主题">
            <span aria-hidden="true">◐</span>
            <select v-model="theme" aria-label="全局主题" @change="changeTheme">
              <option v-for="option in themeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>
          <RouterLink to="/security-sessions" class="top-icon-link" title="设备会话">⌁</RouterLink>
          <RouterLink v-if="canManageModels" to="/models" class="ghost-btn">模型底座</RouterLink>
          <div class="user-menu-button" title="当前用户"><span class="user-avatar">{{ username ? username.substring(0, 2).toUpperCase() : 'U' }}</span><span>{{ nickname || username }}</span><span>⌄</span></div>
        </div>
      </header>

      <section class="page-root">
        <router-view />
      </section>
    </main>
    <FeedbackToast />
    <ConfirmDialog />
    <PromptDialog />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import http, { ensureAccessToken } from '../api/http'
import FeedbackToast from '../components/FeedbackToast.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import PromptDialog from '../components/PromptDialog.vue'
import { fetchWorkspaceCapabilities } from '../api/workspace'

const route = useRoute()
const router = useRouter()

const username = ref(localStorage.getItem('username') || '')
const nickname = ref(localStorage.getItem('nickname') || '')
const role = ref(localStorage.getItem('role') || 'STAFF')
const tenantCode = ref(localStorage.getItem('tenantCode') || '')
const tenantName = ref(localStorage.getItem('tenantName') || '')
const allowedPaths = ref([])
const userMenus = ref(JSON.parse(localStorage.getItem('workspaceMenus') || '[]'))
const workspaceCapabilities = ref(null)
const signingOut = ref(false)
const themeOptions = [
  { value: 'light', label: '类白 · 清晰' },
  { value: 'blue', label: '类蓝 · 专注' },
  { value: 'comfort', label: '护眼 · 柔和' },
  { value: 'night', label: '夜间 · 深色' }
]
const storedTheme = localStorage.getItem('asteria-theme') === 'dark' ? 'night' : (localStorage.getItem('asteria-theme') || 'light')
const theme = ref(themeOptions.some(option => option.value === storedTheme) ? storedTheme : 'light')
const savedExpandedGroups = (() => {
  try { return JSON.parse(localStorage.getItem('asteria-nav-groups') || '{}') } catch { return {} }
})()
// Older builds persisted every group as expanded; collapse that state on migration.
const expandedGroups = ref(Object.keys(savedExpandedGroups).filter(key => savedExpandedGroups[key]).length > 1 ? {} : savedExpandedGroups)
const defaultExpandedGroup = computed(() => capabilityNavigation.value.find(group => group.items.some(item => route.path === item.path))?.title || capabilityNavigation.value[0]?.title)

const navGroups = [
  {
    title: '工作区',
    items: [
      { path: '/dashboard', menuPath: '/dashboard', label: '工作台', icon: '⌂' },
      { path: '/applications', menuPath: '/applications', label: '业务应用', icon: '◇' },
      { path: '/chat', menuPath: '/chat', label: '应用工作台', icon: '◌' },
      { path: '/knowledge', menuPath: '/knowledge', label: '知识库', icon: '▤' },
        { path: '/workflow-executions', menuPath: '/workflow-executions', label: '运行中心', icon: '▶' },
        { path: '/runtime-operations', menuPath: '/runtime-operations', label: '故障处理', icon: '!' }
    ]
  },
  {
    title: '运营治理',
    items: [
      { path: '/approval', menuPath: '/approval', label: '审批中心', icon: '✓' },
      { path: '/ops', menuPath: '/ops', label: '运营分析', icon: '◫' },
      { path: '/evaluation', menuPath: '/evaluation', label: '评测中心', icon: '▥' },
      { path: '/audit', menuPath: '/audit', label: '审计与风控', icon: '⊙' },
      { path: '/adoption', menuPath: '/adoption', label: '产品采用', icon: '⌁' },
      { path: '/marketplace', menuPath: '/marketplace', label: '能力市场', icon: '□' }
    ]
  },
  {
    title: '企业管理',
    items: [
      { path: '/organization', menuPath: '/users', label: '组织与权限', icon: '♧' },
      { path: '/identity', menuPath: '/identity', label: '企业身份', icon: '◎' },
      { path: '/entitlements', menuPath: '/entitlements', label: '套餐与用量', icon: '▤' },
      { path: '/billing', menuPath: '/billing', label: '账期与账单', icon: '¥' },
      { path: '/data-governance', menuPath: '/data-governance', label: '数据治理', icon: '◇' },
      { path: '/models', menuPath: '/models', label: '模型底座', icon: '⚙' },
      { path: '/model-prices', menuPath: '/model-prices', label: '模型计费', icon: '¥' },
      { path: '/tenant', menuPath: '/tenants', label: '平台租户', icon: '▦' }
    ]
  }
]

// 能力包是主导航的唯一来源，避免旧版菜单在新产品中重新出现。
const navigationPresentation = {
  '/dashboard': { group: '工作区', icon: '⌂' },
  '/applications': { group: '工作区', icon: '◇' },
  '/chat': { group: '工作区', icon: '◌' },
  '/knowledge': { group: '工作区', icon: '▤' },
    '/workflow-executions': { group: '运营治理', icon: '▶' },
    '/runtime-operations': { group: '运营治理', icon: '!' },
  '/approval': { group: '运营治理', icon: '✓' },
  '/ops': { group: '运营治理', icon: '◫' },
  '/evaluation': { group: '运营治理', icon: '▥' },
  '/audit': { group: '运营治理', icon: '⊙' },
  '/adoption': { group: '运营治理', icon: '⌁' },
  '/marketplace': { group: '运营治理', icon: '□' },
  '/organization': { group: '企业管理', icon: '♧' },
  '/identity': { group: '企业管理', icon: '◎' },
  '/entitlements': { group: '企业管理', icon: '▤' },
  '/billing': { group: '企业管理', icon: '¥' },
  '/data-governance': { group: '企业管理', icon: '◇' },
  '/models': { group: '企业管理', icon: '⚙' },
  '/router': { group: '企业管理', icon: '⇄' },
  '/developer': { group: '企业管理', icon: '⌘' },
  '/model-prices': { group: '企业管理', icon: '¥' },
  '/tenant': { group: '企业管理', icon: '▦' }
}
const navigationLabel = item => ({
  '/dashboard': '我的工作台', '/applications': '业务应用', '/chat': '发起业务', '/knowledge': '知识资源',
    '/workflow-executions': '运行中心', '/runtime-operations': '故障处理', '/approval': '任务中心',
  '/organization': '组织与权限', '/users': '组织与权限', '/models': '模型底座', '/router': '智能路由', '/developer': '连接器管理', '/tenants': '租户管理'
}[item.path] || item.label)

const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')
const canManageModels = computed(() => isSuperAdmin.value || role.value === 'ADMIN')
const isNavVisible = (item) => {
  if (item.menuPath === null) return true
  if (item.menuPath === '/authorization') return isSuperAdmin.value || role.value === 'ADMIN'
  if (isSuperAdmin.value && (item.menuPath === '/models' || item.menuPath === '/model-prices' || item.menuPath === '/tenants' || item.menuPath === '/users')) return true
  if (item.menuPath === '/model-prices') return isSuperAdmin.value
  if (allowedPaths.value.length === 0) {
    return item.menuPath !== '/tenants' && item.menuPath !== '/models'
  }
  return allowedPaths.value.includes(item.menuPath)
}
const visibleNavGroups = computed(() => navGroups
  .map(group => ({ ...group, items: group.items.filter(isNavVisible) }))
  .filter(group => group.items.length > 0))

const capabilityNavigation = computed(() => {
  const configuredMenus = userMenus.value.length
    ? userMenus.value.map(menu => ({ path: menu.path, label: menu.menuName, icon: menu.icon, enabled: menu.status === 'ACTIVE' }))
    : workspaceCapabilities.value?.navigation
  const items = configuredMenus
  if (!Array.isArray(items) || items.length === 0) return []
  const groups = new Map()
  items.filter(item => item.enabled !== false).forEach(item => {
    const configuredIcon = item.icon && !String(item.icon).startsWith('el-icon') ? item.icon : '•'
    const presentation = navigationPresentation[item.path] || { group: item.path.startsWith('/organization') || item.path === '/users' ? '企业管理' : '更多功能', icon: configuredIcon }
    if (!presentation) return
    if (!groups.has(presentation.group)) groups.set(presentation.group, [])
    groups.get(presentation.group).push({
      path: ({ '/users': '/organization', '/tenants': '/tenant', '/workflow': '/workflow-executions' }[item.path] || item.path),
      menuPath: item.path,
      label: navigationLabel(item),
      icon: presentation.icon
    })
  })
  return [...groups.entries()].map(([title, items]) => ({ title, items })).filter(group => group.items.length > 0)
})

const roleLabel = computed(() => ({
  SUPER_ADMIN: '系统超管',
  ADMIN: '企业管理员',
  OPERATOR: '业务运营',
  STAFF: '普通员工'
}[role.value] || '平台用户'))
const workspaceLabel = computed(() => isSuperAdmin.value ? '全部租户' : (tenantName.value || tenantCode.value || '企业工作区'))
const tenantDisplayLabel = computed(() => isSuperAdmin.value ? '平台范围' : (tenantName.value || '当前企业'))
const currentTitle = computed(() => route.meta.title || '企业 AI 平台')
const applyTheme = value => { document.documentElement.dataset.theme = value }
const changeTheme = () => { localStorage.setItem('asteria-theme', theme.value); applyTheme(theme.value) }
const isGroupExpanded = title => expandedGroups.value[title] ?? title === defaultExpandedGroup.value
const toggleGroup = title => {
  expandedGroups.value = { ...expandedGroups.value, [title]: !isGroupExpanded(title) }
  localStorage.setItem('asteria-nav-groups', JSON.stringify(expandedGroups.value))
}

const loadUserInfo = async () => {
  try {
    if (!await ensureAccessToken()) return
    const res = await http.get('/auth/user-info')
    if (res.success) {
      username.value = res.data.username
      nickname.value = res.data.nickname
      role.value = res.data.role
      tenantCode.value = res.data.tenantCode
      tenantName.value = res.data.tenantName

      localStorage.setItem('username', res.data.username)
      localStorage.setItem('nickname', res.data.nickname)
      localStorage.setItem('role', res.data.role)
      localStorage.setItem('tenantCode', res.data.tenantCode)
      localStorage.setItem('tenantName', res.data.tenantName)

      // 保存后端返回的菜单路径，前端只负责展示，不自行扩大权限范围。
      const paths = (res.data.menus || []).map(m => m.path)
      userMenus.value = res.data.menus || []
      localStorage.setItem('workspaceMenus', JSON.stringify(userMenus.value))
      paths.push('/architecture')
      allowedPaths.value = paths
      try {
        const capabilityResult = await fetchWorkspaceCapabilities()
        workspaceCapabilities.value = capabilityResult.data || null
        localStorage.setItem('workspaceCapabilities', JSON.stringify(workspaceCapabilities.value))
      } catch (capabilityError) {
        // 能力接口不可用时继续使用既有菜单权限，避免影响已登录用户的基本使用。
        console.warn('工作区能力包暂时不可用，回退到用户菜单', capabilityError)
      }
    }
  } catch (err) {
    console.error('同步用户信息失败', err)
  }
}

const handleSignOut = async () => {
  if (signingOut.value) return
  signingOut.value = true
  try {
    await http.post('/auth/logout')
  } catch (error) {
    // 本地会话仍需清理，避免退出操作因网络故障把用户留在已失效页面。
    console.warn('注销服务端会话失败', error)
  }
  localStorage.clear()
  await router.replace('/login')
  signingOut.value = false
}

onMounted(() => {
  applyTheme(theme.value)
  loadUserInfo()
})
</script>
