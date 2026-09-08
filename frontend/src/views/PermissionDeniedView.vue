<template>
  <div class="page-grid permission-page">
    <section class="permission-state">
      <span class="permission-mark">!</span>
      <div>
        <div class="eyebrow">ACCESS CONTROL</div>
        <h3>当前账号没有访问权限</h3>
        <p>该工作区由企业角色和能力包控制。请联系管理员开通对应业务权限。</p>
        <div class="permission-actions">
          <RouterLink to="/dashboard" class="primary-btn">返回工作台</RouterLink>
          <RouterLink to="/applications" class="ghost-btn">查看业务应用</RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>

<style scoped>
.permission-state { display: flex; align-items: flex-start; gap: 16px; max-width: 680px; padding: 28px; border: 1px solid rgba(232,111,81,.28); border-radius: var(--radius); background: #fffaf8; }.permission-mark { display: grid; flex: 0 0 34px; place-items: center; width: 34px; height: 34px; border-radius: 8px; background: rgba(232,111,81,.14); color: var(--danger); font-weight: 800; }.permission-state h3 { margin: 8px 0 6px; font-size: 20px; }.permission-state p { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.6; }.permission-actions { display: flex; gap: 8px; margin-top: 18px; }
@media (max-width: 600px) { .permission-state { padding: 20px; }.permission-actions { flex-direction: column; align-items: stretch; } }
</style>
