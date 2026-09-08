<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">EA</div>
        <h2>注册企业租户</h2>
        <p>SaaS 模式自助开启您的企业级智能底座</p>
      </div>

      <form @submit.prevent="handleRegister" class="auth-form" autocomplete="off">
        <div class="form-group">
          <label for="tenantCode">企业租户唯一标识码 (Tenant Code)</label>
          <input
            id="tenantCode"
            type="text"
            v-model="form.tenantCode"
            autocomplete="off"
            spellcheck="false"
            placeholder="仅支持小写字母和数字，例如: acme, demo"
            required
          />
        </div>

        <div class="form-group">
          <label for="tenantName">企业组织全称 (Tenant Name)</label>
          <input
            id="tenantName"
            type="text"
            v-model="form.tenantName"
            autocomplete="off"
            spellcheck="false"
            placeholder="例如: ACME 智能科技有限公司"
            required
          />
        </div>

        <div class="form-group">
          <label for="adminUsername">系统超级管理员账号 (Admin Account)</label>
          <input
            id="adminUsername"
            type="text"
            v-model="form.adminUsername"
            autocomplete="off"
            spellcheck="false"
            placeholder="用于初始系统后台管理的登录账户名"
            required
          />
        </div>

        <div class="form-group">
          <label for="nickname">管理员昵称 (Nickname)</label>
          <input
            id="nickname"
            type="text"
            v-model="form.nickname"
            autocomplete="off"
            spellcheck="false"
            placeholder="管理员的显示昵称"
            required
          />
        </div>

        <div class="form-group">
          <label for="adminPassword">管理员高强度密码 (Password)</label>
          <input
            id="adminPassword"
            type="password"
            v-model="form.adminPassword"
            autocomplete="off"
            spellcheck="false"
            placeholder="长度>=8，必须包含大小写字母、数字和特殊字符"
            required
          />
        </div>

        <div v-if="error" class="error-msg">
          {{ error }}
        </div>
        <div v-if="successMsg" class="success-msg">
          {{ successMsg }}
        </div>

        <button type="submit" :disabled="loading" class="auth-btn">
          {{ loading ? '企业租户资源初始化中...' : '注册并初始化租户' }}
        </button>
      </form>

      <div class="auth-footer">
        已有企业租户？
        <RouterLink to="/login">返回登录控制台 &rarr;</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()
const error = ref('')
const successMsg = ref('')
const loading = ref(false)

const form = reactive({
  tenantCode: '',
  tenantName: '',
  adminUsername: '',
  adminPassword: '',
  nickname: ''
})

const handleRegister = async () => {
  error.value = ''
  successMsg.value = ''

  // 前端本地的密码强度校验
  const pw = form.adminPassword
  const hasUppercase = /[A-Z]/.test(pw)
  const hasLowercase = /[a-z]/.test(pw)
  const hasNumbers = /\d/.test(pw)
  const hasNonalphas = /[^a-zA-Z0-9]/.test(pw)

  if (pw.length < 8 || !hasUppercase || !hasLowercase || !hasNumbers || !hasNonalphas) {
    error.value = '密码强度不足：长度必须至少 8 位，且必须包含大写字母、小写字母、数字和特殊字符。'
    return
  }

  loading.value = true
  try {
    const res = await http.post('/auth/register-tenant', form)
    successMsg.value = '企业租户与管理账户初始化成功！3秒后自动为您跳转到登录页面。'
    setTimeout(() => {
      router.push('/login')
    }, 3000)
  } catch (err) {
    error.value = err.message || '网络连接异常，无法访问注册中心。'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: radial-gradient(circle at top left, #1e293b, #0f172a);
  padding: 20px;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
}

.auth-card {
  width: 100%;
  max-width: 480px;
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3), 0 10px 10px -5px rgba(0, 0, 0, 0.2);
}

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}

.auth-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: white;
  font-size: 20px;
  font-weight: 700;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.auth-header h2 {
  color: #f8fafc;
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.auth-header p {
  color: #94a3b8;
  font-size: 14px;
  margin: 0;
}

.auth-form {
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  color: #cbd5e1;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #f8fafc;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
}

.form-group input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.error-msg {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #fca5a5;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 20px;
}

.success-msg {
  background: rgba(16, 185, 129, 0.15);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #a7f3d0;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 20px;
}

.auth-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(99, 102, 241, 0.3);
  transition: all 0.2s ease;
  margin-top: 10px;
}

.auth-btn:hover:not(:disabled) {
  opacity: 0.95;
  transform: translateY(-1px);
}

.auth-btn:disabled {
  background: #475569;
  cursor: not-allowed;
  box-shadow: none;
}

.auth-footer {
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}

.auth-footer a {
  color: #6366f1;
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.auth-footer a:hover {
  text-decoration: underline;
}
</style>
