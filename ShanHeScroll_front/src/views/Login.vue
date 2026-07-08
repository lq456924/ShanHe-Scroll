<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/user'

const router = useRouter()

const account = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  if (!account.value.trim()) {
    errorMsg.value = '请输入邮箱或用户名'
    return
  }
  if (!password.value) {
    errorMsg.value = '请输入密码'
    return
  }

  loading.value = true
  try {
    const res = await login(account.value.trim(), password.value)
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify(res.user))
    router.replace('/')
  } catch (err: any) {
    errorMsg.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/')
}

function goRegister() {
  router.push('/register')
}

function goForgotPassword() {
  router.push('/forgot-password')
}
</script>

<template>
  <div class="login-page">
    <!-- 动态背景 -->
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="login-card">
      <span class="back-btn" @click="goBack" title="返回">← 返回</span>
      <div class="logo-icon">🏔️</div>
      <h1 class="login-title">山河卷</h1>
      <p class="login-subtitle">登录你的账号</p>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="field">
          <label for="account">邮箱 / 用户名</label>
          <input
            id="account"
            v-model="account"
            type="text"
            placeholder="请输入邮箱或用户名"
            autocomplete="username"
          />
        </div>

        <div class="field">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>

        <div class="forgot-row">
          <span class="forgot-link" @click="goForgotPassword">忘记密码？</span>
        </div>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>

      <span class="register-link" @click="goRegister">注册</span>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 73px);
  background: var(--bg-page);
  overflow: hidden;
  position: relative;
}

/* 动态几何背景 */
.bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  animation: float 12s ease-in-out infinite;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: var(--decor-1);
  top: -100px;
  right: -100px;
  animation-delay: 0s;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: var(--decor-2);
  bottom: -80px;
  left: -80px;
  animation-delay: -4s;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: var(--decor-3);
  top: 50%;
  left: 50%;
  animation-delay: -8s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
}

/* 玻璃卡片 */
.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 44px 40px 36px;
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-card);
  border-radius: 20px;
  box-shadow: var(--shadow-card);
}

.logo-icon {
  text-align: center;
  font-size: 40px;
  margin-bottom: 8px;
  filter: drop-shadow(0 2px 8px rgba(0,0,0,0.3));
}

.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #a78bfa, #f093fb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 4px;
  letter-spacing: 2px;
}

.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 32px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 13px;
  color: var(--text-label);
  font-weight: 500;
}

.field input {
  height: 46px;
  padding: 0 16px;
  border: 1px solid var(--border-input);
  border-radius: 12px;
  font-size: 15px;
  color: var(--text-primary);
  background: var(--bg-input);
  outline: none;
  transition: all 0.3s;
}

.field input:focus {
  border-color: var(--border-input-focus);
  background: var(--bg-input-focus);
  box-shadow: var(--shadow-focus);
}

.field input::placeholder {
  color: var(--text-placeholder);
}

.error-msg {
  margin: -8px 0 0;
  font-size: 13px;
  color: var(--color-error);
  text-align: center;
}

.login-btn {
  height: 46px;
  border: none;
  border-radius: 12px;
  background: var(--btn-primary);
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 4px;
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--btn-primary-hover);
  opacity: 0;
  transition: opacity 0.3s;
}

.login-btn:hover::before {
  opacity: 1;
}

.login-btn span {
  position: relative;
  z-index: 1;
}

.login-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.login-btn:disabled::before {
  opacity: 0;
}

.back-btn {
  position: absolute;
  left: 20px;
  top: 20px;
  font-size: 13px;
  color: var(--text-dim);
  cursor: pointer;
  user-select: none;
  transition: color 0.2s;
}

.back-btn:hover {
  color: var(--text-accent);
}

.forgot-row {
  display: flex;
  justify-content: flex-end;
  margin-top: -10px;
}

.forgot-link {
  font-size: 13px;
  color: var(--text-dim);
  cursor: pointer;
  user-select: none;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: var(--text-accent);
}

.register-link {
  position: absolute;
  right: 40px;
  bottom: 20px;
  font-size: 13px;
  color: var(--text-accent);
  cursor: pointer;
  user-select: none;
}

.register-link:hover {
  text-decoration: underline;
}
</style>
