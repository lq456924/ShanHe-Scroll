<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout as apiLogout, updateProfile } from '@/api/user'
import { useTheme } from '@/composables/useTheme'
import MessageDropdown from '@/components/MessageDropdown.vue'
import http, { extractData } from '@/api'

const router = useRouter()
const route = useRoute()
const { theme, toggle: toggleTheme } = useTheme()
const fileInput = ref<HTMLInputElement>()

const navItems = [
  { path: '/', name: 'home', label: '首页' },
  { path: '/album', name: 'album', label: '我的旅游相册' },
  { path: '/bottle', name: 'bottle', label: '漂流瓶' },
  { path: '/profile', name: 'profile', label: '个人中心' },
]

function navigate(path: string) {
  if (!isLoggedIn.value && (path === '/album' || path === '/bottle' || path === '/profile')) {
    router.push('/login')
    return
  }
  router.push(path)
}

// ---- 用户头像状态 ----
interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string | null
  role: number | null
}

const user = ref<UserInfo | null>(null)
const isLoggedIn = ref(false)
const showDropdown = ref(false)

function loadUser() {
  const token = localStorage.getItem('token')
  if (!token) {
    isLoggedIn.value = false
    user.value = null
    return
  }
  try {
    const raw = localStorage.getItem('user')
    if (raw) {
      user.value = JSON.parse(raw)
    }
    isLoggedIn.value = true
  } catch {
    isLoggedIn.value = false
    user.value = null
  }
}

function handleAvatarClick() {
  if (isLoggedIn.value) {
    showDropdown.value = !showDropdown.value
    return
  }
  router.push('/login')
}

function goProfile() {
  showDropdown.value = false
  router.push('/profile')
}

function goAdmin() {
  showDropdown.value = false
  router.push('/admin')
}

function triggerUpload() {
  showDropdown.value = false
  fileInput.value?.click()
}

async function handleAvatarUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  const form = new FormData()
  form.append('file', file)

  try {
    const url = await http.post<string>('/file/upload', form).then(extractData)
    await updateProfile({ avatar: url } as any)
    // 更新本地用户信息
    if (user.value) {
      user.value.avatar = url
      const raw = localStorage.getItem('user')
      if (raw) {
        const u = JSON.parse(raw)
        u.avatar = url
        localStorage.setItem('user', JSON.stringify(u))
      }
    }
  } catch {
    // 上传失败静默处理
  }
}

async function handleLogout() {
  showDropdown.value = false
  try {
    await apiLogout()
  } catch {
    // 即使后端请求失败也清除本地状态
  }
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  isLoggedIn.value = false
  user.value = null
  router.push('/')
}

function closeDropdown() {
  showDropdown.value = false
}

// 监听路由变化（登录/注册完成后自动刷新用户状态）
watch(() => route.path, () => {
  loadUser()
  closeDropdown()
}, { immediate: true })
</script>

<template>
  <header class="top-nav">
    <div class="nav-brand"><span class="brand-text">山河卷</span></div>

    <nav class="nav-links">
      <button
        v-for="item in navItems"
        :key="item.name"
        :class="['nav-btn', { active: route.path === item.path }]"
        @click="navigate(item.path)"
      >
        {{ item.label }}
      </button>
    </nav>

    <!-- 主题切换 -->
    <button class="theme-toggle" @click="toggleTheme" :title="theme === 'dark' ? '切换浅色' : '切换深色'">
      {{ theme === 'dark' ? '☀' : '☾' }}
    </button>

    <!-- 右侧区域：消息图标 + 用户头像 -->
    <div class="right-group">
      <MessageDropdown v-if="isLoggedIn" />

      <div class="user-area" @click="handleAvatarClick">
        <span class="user-name">{{ isLoggedIn ? (user?.nickname || user?.username) : '未登录' }}</span>
        <div class="user-avatar">
          <img
            v-if="isLoggedIn && user?.avatar"
            :src="user.avatar"
            class="avatar-img"
            alt="头像"
          />
          <img
            v-else
            src="/default-avatar.svg"
            class="avatar-img default"
            alt="默认头像"
          />
        </div>

        <!-- 下拉菜单 -->
        <Transition name="dropdown">
          <div v-if="showDropdown && isLoggedIn" class="dropdown-menu" @click.stop>
            <div class="dropdown-item" @click="goProfile">个人中心</div>
            <div class="dropdown-item" @click="triggerUpload">修改头像</div>
            <div v-if="user?.role >= 1" class="dropdown-divider"></div>
            <div v-if="user?.role >= 1" class="dropdown-item admin" @click="goAdmin">🔧 管理面板</div>
            <div class="dropdown-divider"></div>
            <div class="dropdown-item danger" @click="handleLogout">退出登录</div>
          </div>
        </Transition>
      </div>
    </div>

    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      style="display:none"
      @change="handleAvatarUpload"
    />
  </header>
</template>

<style scoped>
.top-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 0 32px;
  height: 73px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
}

.top-nav::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.5), transparent);
}

.nav-brand {
  position: absolute;
  left: 32px;
  user-select: none;
}

.brand-text {
  font-size: 36px;
  font-weight: 700;
  font-family: 'KaiTi', 'STKaiti', '楷体', 'Noto Serif SC', serif;
  letter-spacing: 10px;
  color: rgba(255,255,255,0.95);
  text-shadow: 0 2px 8px rgba(0,0,0,0.25);
}

.nav-links {
  display: flex;
  gap: 6px;
}

.nav-btn {
  padding: 10px 26px;
  border: none;
  border-radius: 22px;
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
  font-size: 19px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  transform: translateY(-1px);
}

.nav-btn.active {
  background: rgba(255, 255, 255, 0.28);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

/* ---- 右侧分组（消息图标 + 用户区） ---- */
.right-group {
  position: absolute;
  right: 32px;
  display: flex;
  align-items: center;
  gap: 14px;
}

/* ---- 用户区 ---- */
.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.user-name {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
}

.user-area:hover .user-name {
  color: #fff;
}

.user-avatar {
  width: 47px;
  height: 47px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.6);
  transition: border-color 0.2s;
  flex-shrink: 0;
}

.user-area:hover .user-avatar {
  border-color: #fff;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-img.default {
  padding: 3px;
  box-sizing: border-box;
}

/* ---- 下拉菜单 ---- */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 140px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  z-index: 200;
}

.dropdown-item {
  padding: 12px 20px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}

.dropdown-item:hover {
  background: #f5f5ff;
}

.dropdown-item.admin {
  color: #667eea;
  font-weight: 500;
}

.dropdown-item.admin:hover {
  background: #f0edff;
}

.dropdown-item.danger {
  color: #e74c3c;
}

.dropdown-item.danger:hover {
  background: #fff5f5;
}

.dropdown-divider {
  height: 1px;
  background: #eee;
}

/* 下拉动画 */
.dropdown-enter-active {
  animation: dropIn 0.2s ease-out;
}

.dropdown-leave-active {
  animation: dropIn 0.15s ease-in reverse;
}

/* ---- 主题切换 ---- */
.theme-toggle {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(255,255,255,0.25);
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
  line-height: 1;
  flex-shrink: 0;
}
.theme-toggle:hover {
  background: rgba(255,255,255,0.25);
  border-color: rgba(255,255,255,0.5);
  transform: rotate(15deg);
}

@keyframes dropIn {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
