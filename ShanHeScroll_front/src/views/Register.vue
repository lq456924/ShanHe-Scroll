<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { sendRegisterCode, register } from '@/api/user'
import PuzzleCaptcha from '@/components/PuzzleCaptcha.vue'

const router = useRouter()

const email = ref('')
const code = ref('')
const password = ref('')
const nickname = ref('')
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const errorMsg = ref('')
const successMsg = ref('')
const showCaptcha = ref(false)

let timer: ReturnType<typeof setInterval> | null = null

function handleSendCode() {
  errorMsg.value = ''
  if (!email.value.trim()) {
    errorMsg.value = '请输入邮箱'
    return
  }
  showCaptcha.value = true
}

async function onCaptchaVerified() {
  showCaptcha.value = false
  sending.value = true
  try {
    await sendRegisterCode(email.value.trim())
    successMsg.value = '验证码已发送，请查收邮件'
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer!)
        timer = null
      }
    }, 1000)
  } catch (err: any) {
    errorMsg.value = err.message || '发送失败'
  } finally {
    sending.value = false
  }
}

async function handleRegister() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!email.value.trim()) {
    errorMsg.value = '请输入邮箱'
    return
  }
  if (!code.value.trim()) {
    errorMsg.value = '请输入验证码'
    return
  }
  if (!password.value) {
    errorMsg.value = '请输入密码'
    return
  }
  if (password.value.length < 6) {
    errorMsg.value = '密码至少6位'
    return
  }
  if (!nickname.value.trim()) {
    errorMsg.value = '请输入昵称'
    return
  }
  if (!/^[a-zA-Z0-9._-]{2,20}$/.test(nickname.value.trim())) {
    errorMsg.value = '昵称格式：2-20位，仅允许英文、数字、._-'
    return
  }

  loading.value = true
  try {
    await register({
      email: email.value.trim(),
      password: password.value,
      nickname: nickname.value.trim(),
      code: code.value.trim(),
    })
    successMsg.value = '注册成功！即将跳转到登录页...'
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (err: any) {
    errorMsg.value = err.message || '注册失败'
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-page">
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="register-card">
      <div class="logo-icon">🏔️</div>
      <h1 class="register-title">创建账号</h1>
      <p class="register-subtitle">注册山河卷，开始你的旅程</p>

      <form class="register-form" @submit.prevent="handleRegister">
        <!-- 邮箱 -->
        <div class="field">
          <label for="email">邮箱</label>
          <div class="email-row">
            <input
              id="email"
              v-model="email"
              type="email"
              placeholder="请输入邮箱"
              autocomplete="email"
            />
            <button
              type="button"
              class="send-code-btn"
              :disabled="sending || countdown > 0"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : sending ? '发送中...' : '获取验证码' }}
            </button>
          </div>
        </div>

        <!-- 验证码 -->
        <div class="field">
          <label for="code">验证码</label>
          <input
            id="code"
            v-model="code"
            type="text"
            maxlength="6"
            placeholder="请输入6位验证码"
            autocomplete="one-time-code"
          />
        </div>

        <!-- 密码 -->
        <div class="field">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="至少6位密码"
            autocomplete="new-password"
          />
        </div>

        <!-- 昵称 -->
        <div class="field">
          <label for="nickname">昵称</label>
          <input
            id="nickname"
            v-model="nickname"
            type="text"
            maxlength="20"
            placeholder="2-20位，仅允许英文、数字、._-"
            autocomplete="off"
          />
          <span class="field-hint">2-20位，仅允许 a-z A-Z 0-9 . _ -</span>
        </div>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

        <button type="submit" class="register-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注 册' }}
        </button>
      </form>

      <span class="login-link" @click="goLogin">← 返回登录</span>
    </div>

    <!-- 拼图验证 -->
    <PuzzleCaptcha
      v-if="showCaptcha"
      @verified="onCaptchaVerified"
      @close="showCaptcha = false"
    />
  </div>
</template>

<style scoped>
.register-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 73px);
  background: var(--bg-page);
  overflow: hidden;
  position: relative;
}

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

.shape-1 { width: 400px; height: 400px; background: var(--decor-1); top: -100px; right: -100px; animation-delay: 0s; }
.shape-2 { width: 300px; height: 300px; background: var(--decor-2); bottom: -80px; left: -80px; animation-delay: -4s; }
.shape-3 { width: 200px; height: 200px; background: var(--decor-3); top: 50%; left: 50%; animation-delay: -8s; }

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
}

.register-card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 38px 40px 36px;
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-card);
  border-radius: 20px;
  box-shadow: var(--shadow-card);
}

.logo-icon {
  text-align: center;
  font-size: 36px;
  margin-bottom: 4px;
  filter: drop-shadow(0 2px 8px rgba(0,0,0,0.3));
}

.register-title {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #a78bfa, #f093fb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 4px;
}

.register-subtitle {
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 24px;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.field-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
}

.email-row {
  display: flex;
  gap: 10px;
}

.email-row input {
  flex: 1;
}

.send-code-btn {
  width: 110px;
  height: 46px;
  flex-shrink: 0;
  border: 1px solid var(--text-accent);
  border-radius: 12px;
  background: rgba(167, 139, 250, 0.1);
  color: var(--text-accent);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
}

.send-code-btn:hover:not(:disabled) {
  background: rgba(167, 139, 250, 0.25);
}

.send-code-btn:disabled {
  color: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.1);
  cursor: not-allowed;
}

.error-msg {
  margin: -4px 0 0;
  font-size: 13px;
  color: var(--color-error);
  text-align: center;
}

.success-msg {
  margin: -4px 0 0;
  font-size: 13px;
  color: var(--color-success);
  text-align: center;
}

.register-btn {
  height: 46px;
  border: none;
  border-radius: 12px;
  background: var(--btn-primary);
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 2px;
  position: relative;
  overflow: hidden;
}

.register-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--btn-primary-hover);
  opacity: 0;
  transition: opacity 0.3s;
}

.register-btn:hover::before {
  opacity: 1;
}

.register-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.register-btn:disabled::before {
  opacity: 0;
}

.login-link {
  position: absolute;
  left: 40px;
  bottom: 20px;
  font-size: 13px;
  color: var(--text-dim);
  cursor: pointer;
  user-select: none;
  transition: color 0.2s;
}

.login-link:hover {
  color: var(--text-accent);
}
</style>
