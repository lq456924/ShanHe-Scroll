<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { sendResetPasswordCode, resetPassword } from '@/api/user'
import PuzzleCaptcha from '@/components/PuzzleCaptcha.vue'

const router = useRouter()

const step = ref<'email' | 'reset'>('email')
const email = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
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
    await sendResetPasswordCode(email.value.trim())
    successMsg.value = '验证码已发送，请查收邮件'
    step.value = 'reset'
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

function handleResend() {
  showCaptcha.value = true
}

async function handleReset() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!code.value.trim()) {
    errorMsg.value = '请输入验证码'
    return
  }
  if (!newPassword.value) {
    errorMsg.value = '请输入新密码'
    return
  }
  if (newPassword.value.length < 6) {
    errorMsg.value = '新密码至少6位'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = '两次密码输入不一致'
    return
  }

  loading.value = true
  try {
    await resetPassword({
      email: email.value.trim(),
      code: code.value.trim(),
      newPassword: newPassword.value,
    })
    successMsg.value = '密码重置成功！即将跳转到登录页...'
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (err: any) {
    errorMsg.value = err.message || '重置失败'
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="forgot-page">
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="forgot-card">
      <span class="back-btn" @click="goLogin" title="返回">← 返回登录</span>
      <div class="logo-icon">🏔️</div>
      <h1 class="forgot-title">找回密码</h1>
      <p class="forgot-subtitle">{{ step === 'email' ? '输入注册邮箱获取验证码' : '输入验证码和新密码' }}</p>

      <!-- Step 1: 输入邮箱 -->
      <form v-if="step === 'email'" class="forgot-form" @submit.prevent="handleSendCode">
        <div class="field">
          <label for="email">注册邮箱</label>
          <div class="email-row">
            <input
              id="email"
              v-model="email"
              type="email"
              placeholder="请输入注册时使用的邮箱"
              autocomplete="email"
            />
            <button
              type="submit"
              class="send-code-btn"
              :disabled="sending"
            >
              {{ sending ? '发送中...' : '获取验证码' }}
            </button>
          </div>
        </div>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
      </form>

      <!-- Step 2: 输入验证码 + 新密码 -->
      <form v-if="step === 'reset'" class="forgot-form" @submit.prevent="handleReset">
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

        <div class="field">
          <label for="newPassword">新密码</label>
          <input
            id="newPassword"
            v-model="newPassword"
            type="password"
            placeholder="至少6位密码"
            autocomplete="new-password"
          />
        </div>

        <div class="field">
          <label for="confirmPassword">确认密码</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            autocomplete="new-password"
          />
        </div>

        <div class="resend-row">
          <span v-if="countdown > 0" class="countdown-text">{{ countdown }}s 后可重发</span>
          <span v-else class="resend-link" @click="handleResend">重新发送验证码</span>
        </div>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

        <button type="submit" class="forgot-btn" :disabled="loading">
          {{ loading ? '重置中...' : '重置密码' }}
        </button>
      </form>
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
.forgot-page {
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

.forgot-card {
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

.forgot-title {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #a78bfa, #f093fb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 4px;
}

.forgot-subtitle {
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 24px;
}

.forgot-form {
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

.resend-row {
  text-align: right;
  margin-top: -8px;
}

.countdown-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
}

.resend-link {
  font-size: 13px;
  color: var(--text-accent);
  cursor: pointer;
  user-select: none;
}

.resend-link:hover {
  text-decoration: underline;
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

.forgot-btn {
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

.forgot-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--btn-primary-hover);
  opacity: 0;
  transition: opacity 0.3s;
}

.forgot-btn:hover::before {
  opacity: 1;
}

.forgot-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.forgot-btn:disabled::before {
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
</style>
