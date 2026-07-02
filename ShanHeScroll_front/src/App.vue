<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import TopNav from './components/TopNav.vue'

const router = useRouter()
const showExpiredDialog = ref(false)

function onAuthExpired() {
  showExpiredDialog.value = true
}

function goLogin() {
  showExpiredDialog.value = false
  router.push('/login')
}

onMounted(() => {
  window.addEventListener('auth:expired', onAuthExpired)
})

onBeforeUnmount(() => {
  window.removeEventListener('auth:expired', onAuthExpired)
})
</script>

<template>
  <TopNav />
  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>

  <!-- 登录失效对话框 -->
  <div v-if="showExpiredDialog" class="expired-overlay">
    <div class="expired-dialog">
      <div class="expired-icon">🔐</div>
      <p>您的登录信息已失效，请重新登录</p>
      <button class="expired-btn" @click="goLogin">确 定</button>
    </div>
  </div>
</template>

<style>
.expired-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}

.expired-dialog {
  background: #fff;
  border-radius: 16px;
  padding: 36px 40px 28px;
  text-align: center;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.25);
  animation: scaleIn 0.25s ease;
}

.expired-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.expired-dialog p {
  font-size: 15px;
  color: #555;
  margin: 0 0 24px;
}

.expired-btn {
  padding: 10px 48px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.expired-btn:hover { opacity: 0.9; }

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}
</style>
