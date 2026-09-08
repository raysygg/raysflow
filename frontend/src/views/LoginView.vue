<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">
          <svg viewBox="0 0 56 56" fill="none" xmlns="http://www.w3.org/2000/svg" style="width:52px;height:52px;display:block;margin:0 auto;">
            <rect width="56" height="56" rx="16" fill="url(#rfAuthGrad)"/>
            <path d="M14 13h14c5.5 0 9.8 4 9.8 9 0 3.8-2.6 7-6.3 8.3L40 43h-7.8l-6.5-11.5H21V43h-7V13zm7 5.5V26h6.2c2.4 0 4.3-1.8 4.3-4s-1.9-4-4.3-4H21z" fill="#ffffff" opacity="0.95"/>
            <path d="M25 27c6-3.8 12-1.5 18-7.5m-12 16.5c6-3 12 0 16.5-4" stroke="#fef08a" stroke-width="3" stroke-linecap="round"/>
            <circle cx="43" cy="13" r="3.2" fill="#fbbf24"/>
            <circle cx="47" cy="21" r="2" fill="#ffffff" opacity="0.9"/>
            <defs>
              <linearGradient id="rfAuthGrad" x1="0" y1="0" x2="56" y2="56" gradientUnits="userSpaceOnUse">
                <stop stop-color="#3730a3"/>
                <stop offset="0.4" stop-color="#4f46e5"/>
                <stop offset="0.75" stop-color="#7c3aed"/>
                <stop offset="1" stop-color="#f59e0b"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <h2>Raysflow OS</h2>
        <p>光流智能引擎 · 企业级 Agent 与工作流协作平台</p>
      </div>

      <form @submit.prevent="handleLogin" class="auth-form" autocomplete="off">
        <div class="form-group">
          <label for="tenantCode">企业租户编码 (Tenant Code)</label>
          <input
            id="tenantCode"
            type="text"
            v-model="form.tenantCode"
            autocomplete="off"
            spellcheck="false"
            placeholder="请输入分配的企业租户标识，例如: system, test"
            required
          />
        </div>

        <div class="form-group">
          <label for="username">登录账号 (Username)</label>
          <input
            id="username"
            type="text"
            v-model="form.username"
            autocomplete="off"
            spellcheck="false"
            placeholder="请输入您的登录账号"
            required
          />
        </div>

        <div class="form-group">
          <label for="password">安全密码 (Password)</label>
          <input
            id="password"
            type="password"
            v-model="form.password"
            autocomplete="off"
            spellcheck="false"
            placeholder="请输入密码"
            @keydown.enter.prevent="handleLogin"
            required
          />
        </div>

        <div v-if="error" class="error-msg">
          {{ error }}
        </div>

        <button type="button" :disabled="loading" class="auth-btn" @click="handleLogin">
          {{ loading ? '身份凭证验证中...' : '登录控制台' }}
        </button>
      </form>

      <div class="auth-footer">
        没有企业账号？
        <RouterLink to="/register">申请自助注册新租户 &rarr;</RouterLink>
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
const loading = ref(false)

const form = reactive({
  tenantCode: 'system',
  username: 'admin',
  password: ''
})

const handleLogin = async () => {
  if (loading.value) return
  error.value = ''
  loading.value = true
  try {
    const payload = {
      tenantCode: form.tenantCode.trim(),
      username: form.username.trim(),
      password: form.password
    }
    const res = await http.post('/auth/login', payload)
    localStorage.setItem('accessToken', res.data.token)
    localStorage.setItem('username', res.data.username)
    localStorage.setItem('nickname', res.data.nickname)
    localStorage.setItem('role', res.data.role)
    localStorage.setItem('tenantId', res.data.tenantId)
    localStorage.setItem('tenantCode', res.data.tenantCode)
    localStorage.setItem('tenantName', res.data.tenantName)

    // 登录接口已经返回身份和租户上下文，先完成导航；工作台挂载后再同步动态菜单。
    await router.replace('/dashboard')
  } catch (err) {
    error.value = err.message || '网络连接异常，无法访问鉴权中心。'
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
  max-width: 440px;
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3), 0 10px 10px -5px rgba(0, 0, 0, 0.2);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
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
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  color: #cbd5e1;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
}

.form-group input {
  width: 100%;
  padding: 12px 14px;
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
